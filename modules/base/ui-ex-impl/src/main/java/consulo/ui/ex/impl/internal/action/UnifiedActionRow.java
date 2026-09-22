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

import consulo.application.Application;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.component.messagebus.MessageBusConnection;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.ModalityState;
import consulo.ui.PopupMenu;
import consulo.ui.Separator;
import consulo.ui.SeparatorStyle;
import consulo.ui.Space;
import consulo.ui.ToggleButton;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.ComboBoxWithCustomPopup;
import consulo.ui.ex.action.ComboBoxAction;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.PresentationFactory;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import kava.beans.PropertyChangeListener;
import consulo.ui.ex.internal.ActionTicker;
import consulo.ui.ex.internal.TimerListener;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.Layout;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static final int POPUP_REOPEN_THRESHOLD_MS = 200;

    private static final Space GAP = Space.X_SMALL;
    // a labeled button is as wide as its text, and two of them standing 2px apart read as one control - the gap
    // which suits a band of 24x24 icons is not enough for the ok/cancel row of a dialog
    private static final Space BUTTON_GAP = Space.LARGE;

    private final Supplier<ActionGroup> myGroupSupplier;
    private final Supplier<DataContext> myContextSupplier;
    private final String myPlace;
    private final String myPopupPlace;
    private final PresentationFactory myPresentationFactory;
    private final ActionToolbar.Style myStyle;
    private final Layout<?> myLayout;

    private final Map<AnAction, ToggleButton> myToggleButtons = new HashMap<>();

    private String mySignature = "";
    private List<AnAction> myActions = List.of();
    private @Nullable ProgressIndicator myIndicator;
    private @Nullable Disposable myTickerRegistration;
    private @Nullable MessageBusConnection myActionConnection;
    private boolean myUpdatePending;

    private final TimerListener myTimerListener = new TimerListener() {
        @Override
        public ModalityState getModalityState() {
            return ModalityState.any();
        }

        @Override
        @RequiredUIAccess
        public void run() {
            updateAsync();
        }
    };

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

        Space gap = style == ActionToolbar.Style.BUTTON ? BUTTON_GAP : GAP;
        myLayout = style.isHorizontal() ? HorizontalLayout.create(gap) : VerticalLayout.create(gap);

        myLayout.addAttachListener(event -> startTicking());
        myLayout.addDetachListener(event -> stopTicking());
    }

    @RequiredUIAccess
    private void startTicking() {
        if (myTickerRegistration != null) {
            return;
        }

        myTickerRegistration = ActionTicker.getInstance().addListener(UIAccess.current(), myTimerListener);

        MessageBusConnection connection = Application.get().getMessageBus().connect();
        connection.subscribe(AnActionListener.class, new AnActionListener() {
            @Override
            public void afterActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
                requestUpdate();
            }
        });
        myActionConnection = connection;

        updateAsync();
    }

    private void stopTicking() {
        myUpdatePending = false;

        MessageBusConnection connection = myActionConnection;
        myActionConnection = null;
        if (connection != null) {
            connection.disconnect();
        }

        Disposable registration = myTickerRegistration;
        if (registration == null) {
            return;
        }

        myTickerRegistration = null;
        Disposer.dispose(registration);
    }

    private void requestUpdate() {
        UIAccess uiAccess = myLayout.getUIAccess();
        if (uiAccess == null) {
            return;
        }

        uiAccess.giveIfNeed(() -> {
            if (myIndicator != null) {
                myUpdatePending = true;
                return;
            }

            updateAsync();
        });
    }

    @RequiredUIAccess
    private void drainPendingUpdate() {
        if (!myUpdatePending) {
            return;
        }

        myUpdatePending = false;

        updateAsync();
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
            indicator,
            true
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
                drainPendingUpdate();
                return;
            }

            apply(nodes);

            result.complete(myActions);
            drainPendingUpdate();
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
            // a toggle button flips itself on click, but the state belongs to the action - if performing it left
            // the action where it was, the optimistic flip has to be taken back
            syncToggleStates(nodes);
            return;
        }

        mySignature = signature;

        myLayout.removeAll();
        myToggleButtons.clear();

        for (UnifiedActionMenuExpander.MenuNode node : nodes) {
            if (node.isSeparator()) {
                add(Separator.create(myLayout instanceof HorizontalLayout ? SeparatorStyle.VERTICAL : SeparatorStyle.HORIZONTAL));
                continue;
            }

            if (node.action() instanceof ComboBoxAction comboBoxAction) {
                add(createComboBox(node, comboBoxAction));
                continue;
            }

            add(node.children() == null ? createActionButton(node) : createActionMenu(node));
        }
    }

    @RequiredUIAccess
    private void syncToggleStates(List<UnifiedActionMenuExpander.MenuNode> nodes) {
        for (UnifiedActionMenuExpander.MenuNode node : nodes) {
            AnAction action = node.action();
            Boolean checked = node.checked();
            if (action == null || checked == null) {
                continue;
            }

            ToggleButton button = myToggleButtons.get(action);
            if (button != null) {
                button.setValue(checked, false);
            }
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
    private Component createComboBox(UnifiedActionMenuExpander.MenuNode node, ComboBoxAction action) {
        Presentation presentation = myPresentationFactory.getPresentation(action);

        Object initialValue = new Object();

        ComboBoxWithCustomPopup<Object> comboBox;
        try {
            comboBox = ComboBoxWithCustomPopup.create(FlatDataModel.of(List.of(initialValue)));
        }
        catch (UnsupportedOperationException e) {
            // a frontend without the control still has to show the action, and the selector of the run
            // configuration is unusable without its text
            return createActionButton(node, true);
        }

        comboBox.setRender((itemPresentation, item) -> {
            itemPresentation.append(presentation.getTextValue().map(Presentation.NO_MNEMONIC));
            itemPresentation.withIcon(presentation.getIcon());
        });

        comboBox.setValue(initialValue, false);
        comboBox.setEnabled(presentation.isEnabled());
        comboBox.setToolTipText(presentation.getDescription());

        presentation.putClientProperty(ComboBoxAction.COMPONENT_KEY, comboBox);

        PropertyChangeListener listener = event -> {
            String propertyName = event.getPropertyName();

            if (Presentation.PROP_TEXT.equals(propertyName)
                || Presentation.PROP_ICON.equals(propertyName)
                || Presentation.PROP_ENABLED.equals(propertyName)
                || Presentation.PROP_DESCRIPTION.equals(propertyName)) {
                applyPresentation(comboBox, presentation, propertyName);
            }
        };
        comboBox.addAttachListener(event -> {
            presentation.addPropertyChangeListener(listener);

            comboBox.setEnabled(presentation.isEnabled());
            comboBox.setToolTipText(presentation.getDescription());
            revalidateValue(comboBox);
        });
        comboBox.addDetachListener(event -> presentation.removePropertyChangeListener(listener));

        PopupState popupState = new PopupState();

        comboBox.addClickListener(event -> {
            JBPopup current = popupState.myPopup;
            if (current != null) {
                popupState.myPopup = null;
                current.cancel();
                return;
            }

            // the press which takes a popup away arrives here as a click of its own, and a control answering
            // that one would put the popup straight back
            if (System.currentTimeMillis() - popupState.myHiddenAt < POPUP_REOPEN_THRESHOLD_MS) {
                return;
            }

            DataContext context = myContextSupplier.get();

            JBPopup popup = action.createPopup(context, () -> {
                popupState.myPopup = null;
                popupState.myHiddenAt = System.currentTimeMillis();
            });

            if (popup != null) {
                popupState.myPopup = popup;

                // a list dropped by a combo hangs from the control rather than from wherever it was pressed,
                // which is what passing no details of the press asks for
                popup.showBy(comboBox, null);
            }
        });

        return comboBox;
    }

    private void applyPresentation(ComboBoxWithCustomPopup<Object> comboBox, Presentation presentation, String propertyName) {
        UIAccess uiAccess = comboBox.getUIAccess();
        if (uiAccess == null) {
            return;
        }

        uiAccess.giveIfNeed(() -> {
            if (Presentation.PROP_ENABLED.equals(propertyName)) {
                comboBox.setEnabled(presentation.isEnabled());
            }
            else if (Presentation.PROP_DESCRIPTION.equals(propertyName)) {
                comboBox.setToolTipText(presentation.getDescription());
            }
            else {
                revalidateValue(comboBox);
            }
        });
    }

    private static final class PopupState {
        private @Nullable JBPopup myPopup;
        private long myHiddenAt;
    }

    @RequiredUIAccess
    private void revalidateValue(ComboBoxWithCustomPopup<Object> comboBox) {
        Object value = new Object();

        MutableFlatDataModel<Object> model = (MutableFlatDataModel<Object>) comboBox.getDataModel();
        model.replaceAll(List.of(value));

        comboBox.setValue(value, false);
    }

    @RequiredUIAccess
    private Component createActionButton(UnifiedActionMenuExpander.MenuNode node) {
        return createActionButton(node, myStyle == ActionToolbar.Style.BUTTON || node.icon() == null);
    }

    @RequiredUIAccess
    private Component createActionButton(UnifiedActionMenuExpander.MenuNode node, boolean showText) {
        Button button = createButton(node, showText);

        AnAction action = node.action();
        if (action != null) {
            button.addClickListener(event -> {
                UnifiedActionMenuExpander.performAction(
                    action,
                    myContextSupplier.get(),
                    myPlace,
                    myPresentationFactory,
                    event.getInputDetails(),
                    true
                ).whenComplete((result, throwable) -> requestUpdate());
            });
        }

        return button;
    }

    @RequiredUIAccess
    private Component createActionMenu(UnifiedActionMenuExpander.MenuNode node) {
        // awt gives a ComboBoxAction a labeled combo instead of an icon button, and the selector of the run
        // configuration is unusable without its text - it is the only thing which says what would be run
        boolean showText = myStyle == ActionToolbar.Style.BUTTON
            || node.icon() == null
            || node.action() instanceof consulo.ui.ex.awt.action.ComboBoxAction;

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
        LocalizeValue text = showText ? node.text() : LocalizeValue.empty();

        // an icon action is shown by its icon, its text is what the user gets on hover
        Boolean checked = node.checked();

        Button button;
        if (checked == null) {
            button = Button.create(text);
        }
        else {
            ToggleButton toggleButton = ToggleButton.create(text, checked);
            button = toggleButton;

            AnAction action = node.action();
            if (action != null) {
                myToggleButtons.put(action, toggleButton);
            }
        }

        button.setIcon(UnifiedActionMenuExpander.toDisplayIcon(node.icon(), node.disabledIcon(), node.enabled()));
        button.setToolTipText(node.text());
        button.setEnabled(node.enabled());

        if (myStyle == ActionToolbar.Style.INPLACE) {
            button.addStyle(ButtonStyle.INPLACE);
        }
        else if (myStyle != ActionToolbar.Style.BUTTON) {
            button.addStyle(ButtonStyle.TOOLBAR);
        }

        return button;
    }
}
