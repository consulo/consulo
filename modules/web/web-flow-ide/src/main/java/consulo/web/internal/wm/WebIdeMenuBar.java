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
package consulo.web.internal.wm;

import com.vaadin.flow.dom.DebouncePhase;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.component.ProcessCanceledException;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.ide.ui.customization.CustomActionsSchemaImpl;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.MenuSeparator;
import consulo.ui.UIAccess;
import consulo.ui.event.details.InputDetails;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.impl.internal.action.ActionRunnerAsync;
import consulo.ui.ex.impl.internal.action.ActionUpdater;
import consulo.ui.ex.impl.internal.action.MenuItemPresentationFactory;
import consulo.ui.ex.impl.internal.action.UnifiedActionMenuExpander;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.image.Image;
import consulo.web.internal.ui.WebMenuBarImpl;
import consulo.web.internal.ui.WebMenuImpl;
import consulo.web.internal.ui.WebMenuItemImpl;
import consulo.web.internal.ui.base.WebFocusTracker;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Web analog of {@code consulo.desktop.awt.wm.impl.DefaultIdeMenuBar}. Builds main menu from {@link IdeActions#GROUP_MAIN_MENU}
 * using async action expansion.
 *
 * @author VISTALL
 * @since 2026-07-31
 */
public class WebIdeMenuBar {
    private static final Logger LOG = Logger.getInstance(WebIdeMenuBar.class);

    private static final int MAX_DEPTH = 10;
    private static final String PERFORM_ONLY = "actionGroup.perform.only";

    private record MenuNode(
        @Nullable AnAction action,
        LocalizeValue text,
        @Nullable Image icon,
        @Nullable Image disabledIcon,
        boolean enabled,
        @Nullable Boolean checked,
        LocalizeValue shortcutText,
        @Nullable List<MenuNode> children
    ) {
        static MenuNode separator() {
            return new MenuNode(null, LocalizeValue.empty(), null, null, true, null, LocalizeValue.empty(), null);
        }

        boolean isSeparator() {
            return action == null && children == null;
        }
    }

    private final consulo.ui.Component myContextComponent;
    private final WebMenuBarImpl myMenuBar = new WebMenuBarImpl();
    private final MenuItemPresentationFactory myPresentationFactory = new MenuItemPresentationFactory();

    private String mySignature = "";
    private @Nullable ProgressIndicator myUpdateIndicator;

    @RequiredUIAccess
    public WebIdeMenuBar(consulo.ui.Component contextComponent) {
        myContextComponent = contextComponent;

        // without the full frame width the bar measures only its natural width and vaadin collapses the
        // trailing items into the overflow button even though the frame has room to spare
        myMenuBar.toVaadinComponent().setWidthFull();

        // there is no server side 'menu about to open' event in vaadin menu bar, so presentations are refreshed when pointer
        // reaches the menu bar - that gives one round trip before any menu is actually opened
        // mouseover bubbles from the individual buttons, unlike mouseenter, so moving along the bar keeps the
        // presentations fresh instead of refreshing them only on the first entry. trailing catches the last move
        myMenuBar.toVaadinComponent()
            .getElement()
            .addEventListener("mouseover", event -> updateMenuActions())
            .debounce(300, DebouncePhase.LEADING, DebouncePhase.TRAILING);
    }

    public consulo.ui.MenuBar getMenuBar() {
        return myMenuBar;
    }

    @RequiredUIAccess
    public void updateMenuActions() {
        UIAccess uiAccess = UIAccess.current();

        ProgressIndicator previousIndicator = myUpdateIndicator;
        if (previousIndicator != null) {
            previousIndicator.cancel();
        }

        ProgressIndicator indicator = new EmptyProgressIndicator();
        myUpdateIndicator = indicator;

        expandMainMenuAsync(createDataContext(), uiAccess, indicator).whenCompleteAsync((nodes, throwable) -> {
            if (myUpdateIndicator != indicator) {
                return;
            }

            myUpdateIndicator = null;

            if (throwable != null) {
                if (!isProcessCanceled(throwable)) {
                    LOG.warn("Failed to expand main menu", throwable);
                }
                return;
            }

            applyNodes(nodes);
        }, uiAccess);
    }

    private DataContext createDataContext() {
        return DataManager.getInstance().createAsyncDataContext(WebFocusTracker.createDataContext(myContextComponent));
    }

    private CompletableFuture<List<MenuNode>> expandMainMenuAsync(DataContext context, UIAccess uiAccess, ProgressIndicator indicator) {
        AnAction mainMenuAction = CustomActionsSchemaImpl.getInstance().getCorrectedAction(IdeActions.GROUP_MAIN_MENU);
        if (!(mainMenuAction instanceof ActionGroup mainMenuGroup)) {
            return CompletableFuture.completedFuture(List.of());
        }

        return expandGroupAsync(mainMenuGroup, context, uiAccess, indicator, 0);
    }

    private CompletableFuture<List<MenuNode>> expandGroupAsync(
        ActionGroup group,
        DataContext context,
        UIAccess uiAccess,
        ProgressIndicator indicator,
        int depth
    ) {
        if (depth > MAX_DEPTH) {
            return CompletableFuture.completedFuture(List.of());
        }

        ActionUpdater updater = new ActionUpdater(
            ActionManager.getInstance(),
            myPresentationFactory,
            context,
            ActionPlaces.MAIN_MENU,
            false,
            false,
            uiAccess
        );

        return updater.expandActionGroupAsync(group, group instanceof CompactActionGroup, indicator)
            .thenCompose(children -> buildNodesAsync(children, 0, new ArrayList<>(), context, uiAccess, indicator, depth));
    }

    private CompletableFuture<List<MenuNode>> buildNodesAsync(
        List<? extends AnAction> children,
        int startIndex,
        List<MenuNode> result,
        DataContext context,
        UIAccess uiAccess,
        ProgressIndicator indicator,
        int depth
    ) {
        for (int i = startIndex; i < children.size(); i++) {
            AnAction action = children.get(i);
            Presentation presentation = myPresentationFactory.getPresentation(action);

            if (action instanceof AnSeparator) {
                result.add(MenuNode.separator());
                continue;
            }

            if (action instanceof ActionGroup actionGroup && !Boolean.TRUE.equals(presentation.getClientProperty(PERFORM_ONLY))) {
                int nextIndex = i + 1;

                return expandGroupAsync(actionGroup, context, uiAccess, indicator, depth + 1).thenCompose(groupChildren -> {
                    result.add(new MenuNode(
                        action,
                        presentation.getTextValue(),
                        presentation.getIcon(),
                        presentation.getDisabledIcon(),
                        presentation.isEnabled(),
                        null,
                        LocalizeValue.empty(),
                        groupChildren
                    ));

                    return buildNodesAsync(children, nextIndex, result, context, uiAccess, indicator, depth);
                });
            }

            result.add(createActionNode(action, presentation));
        }

        return CompletableFuture.completedFuture(result);
    }

    private MenuNode createActionNode(AnAction action, Presentation presentation) {
        Boolean checked = null;
        if (action instanceof Toggleable) {
            checked = Toggleable.isSelected(presentation);
        }

        String shortcutText = KeymapUtil.getFirstKeyboardShortcutText(action);

        return new MenuNode(
            action,
            presentation.getTextValue(),
            presentation.getIcon(),
            presentation.getDisabledIcon(),
            presentation.isEnabled(),
            checked,
            LocalizeValue.of(shortcutText),
            null
        );
    }

    @RequiredUIAccess
    private void applyNodes(List<MenuNode> nodes) {
        StringBuilder builder = new StringBuilder();
        appendSignature(nodes, builder);
        String signature = builder.toString();

        if (signature.equals(mySignature)) {
            return;
        }

        mySignature = signature;

        myMenuBar.clear();

        for (MenuNode node : nodes) {
            if (node.children() == null) {
                continue;
            }

            myMenuBar.add(createMenuItem(node));
        }
    }

    @RequiredUIAccess
    private consulo.ui.MenuItem createMenuItem(MenuNode node) {
        if (node.isSeparator()) {
            return MenuSeparator.create();
        }

        List<MenuNode> children = node.children();
        if (children != null) {
            WebMenuImpl menu = new WebMenuImpl(node.text());
            menu.setIcon(UnifiedActionMenuExpander.toDisplayIcon(node.icon(), node.disabledIcon(), node.enabled()));
            menu.setEnabled(node.enabled());

            for (MenuNode child : children) {
                menu.add(createMenuItem(child));
            }

            return menu;
        }

        WebMenuItemImpl item = new WebMenuItemImpl(node.text());
        item.setIcon(UnifiedActionMenuExpander.toDisplayIcon(node.icon(), node.disabledIcon(), node.enabled()));
        item.setEnabled(node.enabled());
        item.setChecked(node.checked());
        item.setShortcutText(node.shortcutText());

        AnAction action = node.action();
        if (action != null) {
            item.addClickListener(event -> performAction(action, event.getInputDetails()));
        }

        return item;
    }

    @RequiredUIAccess
    private void performAction(AnAction action, @Nullable InputDetails inputDetails) {
        UIAccess uiAccess = UIAccess.current();

        ActionManagerEx actionManager = (ActionManagerEx) ActionManager.getInstance();

        DataContext context = createDataContext();

        Presentation presentation = myPresentationFactory.getPresentation(action);

        AnActionEvent event =
            new AnActionEvent(null, context, ActionPlaces.MAIN_MENU, presentation, actionManager, 0, true, false, inputDetails);
        event.setInjectedContext(action.isInInjectedContext());

        ActionRunnerAsync.lastUpdateAndCheckDumbAsync(action, event, false).whenCompleteAsync((enabled, throwable) -> {
            if (throwable != null) {
                if (!isProcessCanceled(throwable)) {
                    LOG.warn("Failed to update action before performing: " + action, throwable);
                }
                return;
            }

            if (Boolean.TRUE.equals(enabled)) {
                actionManager.fireBeforeActionPerformed(action, context, event);
                actionManager.performActionDumbAware(action, event);
                actionManager.queueActionPerformedEvent(action, context, event);
            }
        }, uiAccess);
    }

    private static void appendSignature(List<MenuNode> nodes, StringBuilder builder) {
        for (MenuNode node : nodes) {
            if (node.isSeparator()) {
                builder.append("|-");
                continue;
            }

            builder.append('|')
                .append(node.text().get())
                .append(node.enabled() ? '+' : '-')
                .append(node.checked() == null ? "" : node.checked() ? "x" : "o")
                .append(node.shortcutText().get());

            List<MenuNode> children = node.children();
            if (children != null) {
                builder.append('{');
                appendSignature(children, builder);
                builder.append('}');
            }
        }
    }

    private static boolean isProcessCanceled(Throwable throwable) {
        Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null ? throwable.getCause() : throwable;

        return cause instanceof ProcessCanceledException || cause instanceof CancellationException;
    }
}
