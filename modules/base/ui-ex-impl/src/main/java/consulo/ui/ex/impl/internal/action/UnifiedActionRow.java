/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ui.ex.impl.internal.action;

import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.dataContext.DataContext;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.PopupMenu;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.PresentationFactory;
import consulo.ui.ex.awt.action.ComboBoxAction;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.Layout;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Line of widgets built out of an action group for the frontends which render {@link consulo.ui} components rather
 * than swing. The presentations are expanded off the ui thread, so the row keeps the indicator of the running
 * expansion and drops the result of an expansion which was superseded.
 *
 * @author VISTALL
 * @since 2026-07-31
 */
public class UnifiedActionRow {
    private static final Logger LOG = Logger.getInstance(UnifiedActionRow.class);

    private final Supplier<ActionGroup> myGroupSupplier;
    private final Supplier<DataContext> myContextSupplier;
    private final String myPlace;
    private final String myPopupPlace;
    private final PresentationFactory myPresentationFactory;
    private final ActionToolbar.Style myStyle;
    private final Layout<?> myLayout;

    private String mySignature = "";
    private List<AnAction> myActions = List.of();
    private @Nullable ProgressIndicator myIndicator;

    public UnifiedActionRow(
        Supplier<ActionGroup> groupSupplier,
        Supplier<DataContext> contextSupplier,
        String place,
        String popupPlace,
        PresentationFactory presentationFactory,
        ActionToolbar.Style style
    ) {
        myGroupSupplier = groupSupplier;
        myContextSupplier = contextSupplier;
        myPlace = place;
        myPopupPlace = popupPlace;
        myPresentationFactory = presentationFactory;
        myStyle = style;
        myLayout = style.isHorizontal() ? HorizontalLayout.create(0) : VerticalLayout.create(0);
    }

    public Component getComponent() {
        return myLayout;
    }

    public List<AnAction> getActions() {
        return myActions;
    }

    @RequiredUIAccess
    public CompletableFuture<List<? extends AnAction>> updateAsync() {
        UIAccess uiAccess = UIAccess.current();

        ProgressIndicator previousIndicator = myIndicator;
        if (previousIndicator != null) {
            previousIndicator.cancel();
        }

        ProgressIndicator indicator = new EmptyProgressIndicator();
        myIndicator = indicator;

        CompletableFuture<List<? extends AnAction>> result = new CompletableFuture<>();

        UnifiedActionMenuExpander.expandAsync(
            myGroupSupplier.get(),
            myContextSupplier.get(),
            myPlace,
            myPresentationFactory,
            uiAccess,
            indicator
        ).whenCompleteAsync((nodes, throwable) -> {
            if (myIndicator != indicator) {
                result.complete(myActions);
                return;
            }

            myIndicator = null;

            if (throwable != null) {
                if (!UnifiedActionMenuExpander.isProcessCanceled(throwable)) {
                    LOG.warn("Failed to expand actions of " + myPlace, throwable);
                }

                result.complete(myActions);
                return;
            }

            apply(nodes);

            result.complete(myActions);
        }, uiAccess);

        return result;
    }

    public void cancel() {
        ProgressIndicator indicator = myIndicator;
        if (indicator != null) {
            indicator.cancel();
            myIndicator = null;
        }
    }

    @RequiredUIAccess
    private void apply(List<UnifiedActionMenuExpander.MenuNode> nodes) {
        List<AnAction> actions = new ArrayList<>();
        for (UnifiedActionMenuExpander.MenuNode node : nodes) {
            AnAction action = node.action();
            if (action != null) {
                actions.add(action);
            }
        }
        myActions = List.copyOf(actions);

        StringBuilder builder = new StringBuilder();
        UnifiedActionMenuExpander.appendSignature(nodes, builder);
        String signature = builder.toString();

        if (signature.equals(mySignature)) {
            return;
        }

        mySignature = signature;

        myLayout.removeAll();

        for (UnifiedActionMenuExpander.MenuNode node : nodes) {
            if (node.isSeparator()) {
                continue;
            }

            add(node.children() == null ? createActionButton(node) : createActionMenu(node));
        }
    }

    @RequiredUIAccess
    private void add(Component component) {
        if (myLayout instanceof HorizontalLayout horizontalLayout) {
            horizontalLayout.add(component);
        }
        else {
            ((VerticalLayout) myLayout).add(component);
        }
    }

    @RequiredUIAccess
    private Component createActionButton(UnifiedActionMenuExpander.MenuNode node) {
        boolean showText = myStyle == ActionToolbar.Style.BUTTON || node.icon() == null;

        Button button = createButton(node, showText);

        AnAction action = node.action();
        if (action != null) {
            button.addClickListener(event -> UnifiedActionMenuExpander.performAction(
                action,
                myContextSupplier.get(),
                myPlace,
                myPresentationFactory
            ));
        }

        return button;
    }

    @RequiredUIAccess
    private Component createActionMenu(UnifiedActionMenuExpander.MenuNode node) {
        // awt gives a ComboBoxAction a labeled combo instead of an icon button, and the selector of the run
        // configuration is unusable without its text - it is the only thing which says what would be run
        boolean showText = myStyle == ActionToolbar.Style.BUTTON
            || node.icon() == null
            || node.action() instanceof ComboBoxAction;

        // a popup group is the very same button a leaf action gets, the group only differs in what the click
        // does. a dedicated menu widget was tried and never lined up with the buttons standing next to it
        Button button = createButton(node, showText);

        PopupMenu popupMenu = PopupMenu.create(button);
        popupMenu.setOpenOnClick(true);

        List<UnifiedActionMenuExpander.MenuNode> children = node.children();
        if (children != null) {
            for (UnifiedActionMenuExpander.MenuNode child : children) {
                popupMenu.add(UnifiedActionMenuExpander.createMenuItem(
                    child,
                    myContextSupplier,
                    myPopupPlace,
                    myPresentationFactory
                ));
            }
        }

        return button;
    }

    @RequiredUIAccess
    private Button createButton(UnifiedActionMenuExpander.MenuNode node, boolean showText) {
        // an icon action is shown by its icon, its text is what the user gets on hover
        Button button = Button.create(showText ? node.text() : LocalizeValue.empty());
        button.setIcon(node.icon());
        button.setToolTipText(node.text());
        button.setEnabled(node.enabled());

        if (myStyle != ActionToolbar.Style.BUTTON) {
            button.addStyle(ButtonStyle.BORDERLESS);
        }

        return button;
    }
}
