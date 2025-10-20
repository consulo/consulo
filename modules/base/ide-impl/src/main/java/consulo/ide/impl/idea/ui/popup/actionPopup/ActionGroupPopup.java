// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ui.popup.actionPopup;

import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionImplUtil;
import consulo.ide.impl.idea.ui.popup.PopupFactoryImpl;
import consulo.ide.impl.idea.ui.popup.WizardPopup;
import consulo.ide.impl.idea.ui.popup.list.ListPopupImpl;
import consulo.language.editor.PlatformDataKeys;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.ListPopupStep;
import consulo.ui.util.TextWithMnemonic;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.function.Predicate;

public class ActionGroupPopup extends ListPopupImpl {
    private static final Logger LOG = Logger.getInstance(ActionGroupPopup.class);

    private final Runnable myDisposeCallback;
    private final Component myComponent;
    private final String myActionPlace;

    public ActionGroupPopup(
        String title,
        @Nonnull ActionGroup actionGroup,
        @Nonnull DataContext dataContext,
        boolean showNumbers,
        boolean useAlphaAsNumbers,
        boolean showDisabledActions,
        boolean honorActionMnemonics,
        Runnable disposeCallback,
        int maxRowCount,
        Predicate<? super AnAction> preselectActionCondition,
        @Nullable String actionPlace,
        boolean forceHeavyPopup
    ) {
        this(
            title,
            actionGroup,
            dataContext,
            showNumbers,
            useAlphaAsNumbers,
            showDisabledActions,
            honorActionMnemonics,
            disposeCallback,
            maxRowCount,
            preselectActionCondition,
            actionPlace,
            new BasePresentationFactory(),
            false,
            forceHeavyPopup
        );
    }

    public ActionGroupPopup(
        String title,
        @Nonnull ActionGroup actionGroup,
        @Nonnull DataContext dataContext,
        boolean showNumbers,
        boolean useAlphaAsNumbers,
        boolean showDisabledActions,
        boolean honorActionMnemonics,
        Runnable disposeCallback,
        int maxRowCount,
        Predicate<? super AnAction> preselectActionCondition,
        @Nullable String actionPlace,
        boolean autoSelection,
        boolean forceHeavyPopup
    ) {
        this(
            title,
            actionGroup,
            dataContext,
            showNumbers,
            useAlphaAsNumbers,
            showDisabledActions,
            honorActionMnemonics,
            disposeCallback,
            maxRowCount,
            preselectActionCondition,
            actionPlace,
            new BasePresentationFactory(),
            autoSelection,
            forceHeavyPopup
        );
    }

    public ActionGroupPopup(
        String title,
        @Nonnull ActionGroup actionGroup,
        @Nonnull DataContext dataContext,
        boolean showNumbers,
        boolean useAlphaAsNumbers,
        boolean showDisabledActions,
        boolean honorActionMnemonics,
        Runnable disposeCallback,
        int maxRowCount,
        Predicate<? super AnAction> preselectActionCondition,
        @Nullable String actionPlace,
        @Nonnull PresentationFactory presentationFactory,
        boolean autoSelection,
        boolean forceHeavyPopup
    ) {
        this(
            null,
            createStep(
                title,
                actionGroup,
                dataContext,
                showNumbers,
                useAlphaAsNumbers,
                showDisabledActions,
                honorActionMnemonics,
                preselectActionCondition,
                actionPlace,
                presentationFactory,
                autoSelection
            ),
            disposeCallback,
            dataContext,
            actionPlace,
            maxRowCount,
            forceHeavyPopup
        );
    }

    public ActionGroupPopup(
        @Nullable WizardPopup aParent,
        @Nonnull ListPopupStep step,
        @Nullable Runnable disposeCallback,
        @Nonnull DataContext dataContext,
        @Nullable String actionPlace,
        int maxRowCount,
        boolean forceHeavyPopup
    ) {
        super(dataContext.getData(Project.KEY), aParent, step, null, forceHeavyPopup);
        setMaxRowCount(maxRowCount);
        myDisposeCallback = disposeCallback;
        myComponent = dataContext.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
        myActionPlace = actionPlace == null ? ActionPlaces.UNKNOWN : actionPlace;

        registerAction("handleActionToggle1", KeyEvent.VK_SPACE, 0, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleToggleAction();
            }
        });

        addListSelectionListener(e -> {
            JList list = (JList) e.getSource();
            ActionPopupItem actionItem = (ActionPopupItem) list.getSelectedValue();
            if (actionItem == null) {
                return;
            }
            updateActionItem(actionItem);
        });
    }

    @Nonnull
    private Presentation updateActionItem(@Nonnull ActionPopupItem actionItem) {
        AnAction action = actionItem.getAction();
        Presentation presentation = new Presentation();
        presentation.setDescriptionValue(action.getTemplatePresentation().getDescriptionValue());

        AnActionEvent actionEvent = new AnActionEvent(
            null,
            DataManager.getInstance().getDataContext(myComponent),
            myActionPlace,
            presentation,
            ActionManager.getInstance(),
            0
        );
        actionEvent.setInjectedContext(action.isInInjectedContext());
        ActionImplUtil.performDumbAwareUpdate(action, actionEvent, false);
        return presentation;
    }

    private static ListPopupStep<ActionPopupItem> createStep(
        String title,
        @Nonnull ActionGroup actionGroup,
        @Nonnull DataContext dataContext,
        boolean showNumbers,
        boolean useAlphaAsNumbers,
        boolean showDisabledActions,
        boolean honorActionMnemonics,
        Predicate<? super AnAction> preselectActionCondition,
        @Nullable String actionPlace,
        @Nonnull PresentationFactory presentationFactory,
        boolean autoSelection
    ) {
        Component component = dataContext.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
        consulo.ui.Component uiCompoment = dataContext.getData(PlatformDataKeys.CONTEXT_UI_COMPONENT);
        if (component == null && uiCompoment != null) {
            component = TargetAWT.to(uiCompoment);
        }

        LOG.assertTrue(component != null, "dataContext has no component for new ListPopupStep");

        java.util.List<ActionPopupItem> items = ActionPopupStep.createActionItems(
            actionGroup,
            dataContext,
            showNumbers,
            useAlphaAsNumbers,
            showDisabledActions,
            honorActionMnemonics,
            actionPlace,
            presentationFactory
        );

        return new ActionPopupStep(
            items,
            title,
            PopupFactoryImpl.getComponentContextSupplier(component),
            actionPlace,
            showNumbers || honorActionMnemonics && itemsHaveMnemonics(items),
            preselectActionCondition,
            autoSelection,
            showDisabledActions,
            presentationFactory
        );
    }

    /**
     * @deprecated Use {@link ActionPopupStep#createActionItems(ActionGroup, DataContext, boolean, boolean, boolean, boolean, String, BasePresentationFactory)} instead.
     */
    @Deprecated
    @Nonnull
    public static java.util.List<ActionPopupItem> getActionItems(
        @Nonnull ActionGroup actionGroup,
        @Nonnull DataContext dataContext,
        boolean showNumbers,
        boolean useAlphaAsNumbers,
        boolean showDisabledActions,
        boolean honorActionMnemonics,
        @Nullable String actionPlace
    ) {
        return ActionPopupStep.createActionItems(
            actionGroup,
            dataContext,
            showNumbers,
            useAlphaAsNumbers,
            showDisabledActions,
            honorActionMnemonics,
            actionPlace,
            new BasePresentationFactory()
        );
    }

    @Override
    public void dispose() {
        if (myDisposeCallback != null) {
            myDisposeCallback.run();
        }
        super.dispose();
    }

    @Override
    public void handleSelect(boolean handleFinalChoices, InputEvent e) {
        Object selectedValue = getList().getSelectedValue();
        ActionPopupStep actionPopupStep = ObjectUtil.tryCast(getListStep(), ActionPopupStep.class);

        if (actionPopupStep != null) {
            KeepingPopupOpenAction dontClosePopupAction =
                getActionByClass(selectedValue, actionPopupStep, KeepingPopupOpenAction.class);
            if (dontClosePopupAction != null) {
                actionPopupStep.performAction((AnAction) dontClosePopupAction, e != null ? e.getModifiers() : 0, e);
                for (ActionPopupItem item : actionPopupStep.getValues()) {
                    updateActionItem(item);
                }
                getList().repaint();
                return;
            }
        }

        super.handleSelect(handleFinalChoices, e);
    }

    protected void handleToggleAction() {
        Object[] selectedValues = getList().getSelectedValues();

        ListPopupStep<Object> listStep = getListStep();
        ActionPopupStep actionPopupStep = ObjectUtil.tryCast(listStep, ActionPopupStep.class);
        if (actionPopupStep == null) {
            return;
        }

        List<ToggleAction> filtered =
            ContainerUtil.mapNotNull(selectedValues, o -> getActionByClass(o, actionPopupStep, ToggleAction.class));

        for (ToggleAction action : filtered) {
            actionPopupStep.performAction(action, 0);
        }

        for (ActionPopupItem item : actionPopupStep.getValues()) {
            updateActionItem(item);
        }

        getList().repaint();
    }

    @Nullable
    private static <T> T getActionByClass(
        @Nullable Object value,
        @Nonnull ActionPopupStep actionPopupStep,
        @Nonnull Class<T> actionClass
    ) {
        ActionPopupItem item = value instanceof ActionPopupItem ? (ActionPopupItem) value : null;
        if (item == null) {
            return null;
        }
        if (!actionPopupStep.isSelectable(item)) {
            return null;
        }
        return actionClass.isInstance(item.getAction()) ? actionClass.cast(item.getAction()) : null;
    }

    private static boolean itemsHaveMnemonics(List<? extends ActionPopupItem> items) {
        for (ActionPopupItem item : items) {
            if (TextWithMnemonic.parse(item.getAction().getTemplatePresentation().getTextWithMnemonic()).getMnemonic() != 0) {
                return true;
            }
        }

        return false;
    }
}
