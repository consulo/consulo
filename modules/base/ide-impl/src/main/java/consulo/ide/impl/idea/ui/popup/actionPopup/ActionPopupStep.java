// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ui.popup.actionPopup;

import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionImplUtil;
import consulo.ide.impl.idea.openapi.actionSystem.impl.ActionUpdater;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.StatusText;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.popup.ListPopupStepEx;
import consulo.ui.ex.popup.ListPopupStep;
import consulo.ui.ex.popup.MnemonicNavigationFilter;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.ex.popup.SpeedSearchFilter;
import consulo.ui.image.Image;
import consulo.ui.util.TextWithMnemonic;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ActionPopupStep implements ListPopupStepEx<ActionPopupItem>, MnemonicNavigationFilter<ActionPopupItem>, SpeedSearchFilter<ActionPopupItem> {
    private final List<ActionPopupItem> myItems;
    private final String myTitle;
    private final Supplier<? extends DataContext> myContext;
    private final String myActionPlace;
    private final boolean myEnableMnemonics;
    @Nonnull
    private final PresentationFactory myPresentationFactory;
    private final int myDefaultOptionIndex;
    private final boolean myAutoSelectionEnabled;
    private final boolean myShowDisabledActions;
    private Runnable myFinalRunnable;
    private final Predicate<? super AnAction> myPreselectActionCondition;
    @Nonnull
    private BiFunction<DataContext, AnAction, DataContext> mySubStepContextAdjuster = (c, a) -> c;

    public ActionPopupStep(
        @Nonnull List<ActionPopupItem> items,
        String title,
        @Nonnull Supplier<? extends DataContext> context,
        @Nullable String actionPlace,
        boolean enableMnemonics,
        @Nullable Predicate<? super AnAction> preselectActionCondition,
        boolean autoSelection,
        boolean showDisabledActions,
        @Nonnull PresentationFactory presentationFactory
    ) {
        myItems = items;
        myTitle = title;
        myContext = context;
        myActionPlace = ObjectUtil.notNull(actionPlace, ActionPlaces.UNKNOWN);
        myEnableMnemonics = enableMnemonics;
        myPresentationFactory = presentationFactory;
        myDefaultOptionIndex = getDefaultOptionIndexFromSelectCondition(preselectActionCondition, items);
        myPreselectActionCondition = preselectActionCondition;
        myAutoSelectionEnabled = autoSelection;
        myShowDisabledActions = showDisabledActions;
    }

    private static int getDefaultOptionIndexFromSelectCondition(
        @Nullable Predicate<? super AnAction> preselectActionCondition,
        @Nonnull List<? extends ActionPopupItem> items
    ) {
        int defaultOptionIndex = 0;
        if (preselectActionCondition != null) {
            for (int i = 0; i < items.size(); i++) {
                AnAction action = items.get(i).getAction();
                if (preselectActionCondition.test(action)) {
                    defaultOptionIndex = i;
                    break;
                }
            }
        }
        return defaultOptionIndex;
    }

    @Nonnull
    public static ListPopupStep<ActionPopupItem> createActionsStep(
        @Nonnull ActionGroup actionGroup,
        @Nonnull DataContext dataContext,
        boolean showNumbers,
        boolean useAlphaAsNumbers,
        boolean showDisabledActions,
        String title,
        boolean honorActionMnemonics,
        boolean autoSelectionEnabled,
        Supplier<? extends DataContext> contextSupplier,
        @Nullable String actionPlace,
        Predicate<? super AnAction> preselectCondition,
        int defaultOptionIndex,
        @Nonnull PresentationFactory presentationFactory
    ) {
        List<ActionPopupItem> items = createActionItems(
            actionGroup,
            dataContext,
            showNumbers,
            useAlphaAsNumbers,
            showDisabledActions,
            honorActionMnemonics,
            actionPlace,
            presentationFactory
        );
        boolean enableMnemonics = showNumbers || honorActionMnemonics && items.stream().anyMatch(
            actionItem -> TextWithMnemonic.parse(actionItem.getAction().getTemplatePresentation().getTextWithMnemonic()).getMnemonic() != 0
        );

        return new ActionPopupStep(
            items,
            title,
            contextSupplier,
            actionPlace,
            enableMnemonics,
            preselectCondition != null
                ? preselectCondition
                : action -> defaultOptionIndex >= 0 && defaultOptionIndex < items.size()
                && items.get(defaultOptionIndex).getAction().equals(action),
            autoSelectionEnabled,
            showDisabledActions,
            presentationFactory
        );
    }

    @Nonnull
    public static List<ActionPopupItem> createActionItems(
        @Nonnull ActionGroup actionGroup,
        @Nonnull DataContext dataContext,
        boolean showNumbers,
        boolean useAlphaAsNumbers,
        boolean showDisabledActions,
        boolean honorActionMnemonics,
        @Nullable String actionPlace,
        @Nonnull PresentationFactory presentationFactory
    ) {
        ActionStepBuilder builder = new ActionStepBuilder(
            dataContext,
            showNumbers,
            useAlphaAsNumbers,
            showDisabledActions,
            honorActionMnemonics,
            actionPlace,
            presentationFactory
        );
        builder.buildGroup(actionGroup);
        return builder.getItems();
    }

    public void setSubStepContextAdjuster(@Nonnull BiFunction<DataContext, AnAction, DataContext> subStepContextAdjuster) {
        mySubStepContextAdjuster = subStepContextAdjuster;
    }

    @Nonnull
    public BiFunction<DataContext, AnAction, DataContext> getSubStepContextAdjuster() {
        return mySubStepContextAdjuster;
    }

    @Override
    @Nonnull
    public List<ActionPopupItem> getValues() {
        return myItems;
    }

    @Nonnull
    public List<ActionPopupItem> getInlineItems(@Nonnull ActionPopupItem value) {
        return value.getInlineItems();
    }

    @Override
    public boolean isSelectable(ActionPopupItem value) {
        return value.isEnabled();
    }

    @Override
    public int getMnemonicPos(ActionPopupItem value) {
        String text = getTextFor(value);
        int i = text.indexOf(UIUtil.MNEMONIC);
        if (i < 0) {
            i = text.indexOf('&');
        }
        if (i < 0) {
            i = text.indexOf('_');
        }
        return i;
    }

    @Override
    public Image getIconFor(ActionPopupItem aValue) {
        return aValue.getIcon(false);
    }

    @Override
    public Image getSelectedIconFor(ActionPopupItem value) {
        return value.getIcon(true);
    }

    @Override
    @Nonnull
    public String getTextFor(ActionPopupItem value) {
        return value.getText().getValue();
    }

    @Nullable
    @Override
    public String getTooltipTextFor(ActionPopupItem value) {
        return value.getDescription().get();
    }

    @Override
    public void setEmptyText(@Nonnull StatusText emptyText) {
    }

    @Override
    public boolean isSeparator(ActionPopupItem value) {
        return value.isSeparator();
    }

    @Override
    public int getDefaultOptionIndex() {
        return myDefaultOptionIndex;
    }

    @Override
    public String getTitle() {
        return myTitle;
    }

    @Override
    public PopupStep onChosen(ActionPopupItem actionChoice, boolean finalChoice) {
        return onChosen(actionChoice, finalChoice, 0);
    }

    @Override
    public PopupStep<ActionPopupItem> onChosen(
        ActionPopupItem actionChoice,
        boolean finalChoice,
        int eventModifiers
    ) {
        if (!actionChoice.isEnabled()) {
            return FINAL_CHOICE;
        }
        AnAction action = actionChoice.getAction();
        DataContext dataContext = myContext.get();
        if (action instanceof ActionGroup group && (!finalChoice || !group.canBePerformed(dataContext))) {
            return createActionsStep(
                group,
                dataContext,
                myEnableMnemonics,
                true,
                myShowDisabledActions,
                null,
                false,
                false,
                myContext,
                myActionPlace,
                myPreselectActionCondition,
                -1,
                myPresentationFactory
            );
        }
        else {
            myFinalRunnable = () -> performAction(action, eventModifiers);
            return FINAL_CHOICE;
        }
    }

    public void performAction(@Nonnull AnAction action, int modifiers) {
        performAction(action, modifiers, null);
    }

    public void performAction(@Nonnull AnAction action, int modifiers, InputEvent inputEvent) {
        DataContext dataContext = myContext.get();
        AnActionEvent event = new AnActionEvent(
            inputEvent,
            dataContext,
            myActionPlace,
            action.getTemplatePresentation().clone(),
            ActionManager.getInstance(),
            modifiers
        );
        event.setInjectedContext(action.isInInjectedContext());
        if (ActionImplUtil.lastUpdateAndCheckDumb(action, event, false)) {
            ActionImplUtil.performActionDumbAwareWithCallbacks(action, event, dataContext);
        }
    }

    public void performActionItem(@Nonnull ActionPopupItem item, @Nullable InputEvent inputEvent) {
        AnAction action = item.getAction();
        AnActionEvent event = createAnActionEvent(item, inputEvent);
        event.setInjectedContext(action.isInInjectedContext());

        ActionImplUtil.performActionDumbAware(action, event);
    }

    @Nonnull
    public AnActionEvent createAnActionEvent(@Nonnull ActionPopupItem item, @Nullable InputEvent inputEvent) {
        DataContext dataContext = myContext.get();
        Presentation presentation = item.clonePresentation();
        return new AnActionEvent(inputEvent,
            dataContext,
            myActionPlace,
            presentation,
            ActionManager.getInstance(),
            0
        );
    }

    @RequiredUIAccess
    public void updateStepItems(@Nonnull JComponent component) {
        DataContext dataContext = myContext.get();
        List<ActionPopupItem> values = getValues();

        ActionUpdater actionUpdater = new ActionUpdater(
            ActionManager.getInstance(),
            myPresentationFactory,
            DataManager.getInstance().createAsyncDataContext(dataContext),
            myActionPlace,
            false,
            false,
            UIAccess.current()
        );

        ActionGroup group = ActionGroup.newImmutableBuilder().addAll(ContainerUtil.map(values, ActionPopupItem::getAction)).build();

        actionUpdater.expandActionGroupAsync(group, true).whenComplete((actions, throwable) -> {
            for (ActionPopupItem actionItem : values) {
                actionItem.updateFromPresentation(myPresentationFactory, myActionPlace);
            }

            component.repaint();
        });
    }

    @Override
    public Runnable getFinalRunnable() {
        return myFinalRunnable;
    }

    @Override
    public boolean hasSubstep(ActionPopupItem selectedValue) {
        return selectedValue != null && selectedValue.isEnabled() && selectedValue.getAction() instanceof ActionGroup;
    }

    @Override
    public void canceled() {
    }

    @Override
    public boolean isMnemonicsNavigationEnabled() {
        return myEnableMnemonics;
    }

    @Override
    public MnemonicNavigationFilter<ActionPopupItem> getMnemonicNavigationFilter() {
        return this;
    }

    @Override
    public String getIndexedString(ActionPopupItem value) {
        return getTextFor(value);
    }

    @Override
    public boolean isSpeedSearchEnabled() {
        return true;
    }

    @Override
    public boolean isAutoSelectionEnabled() {
        return myAutoSelectionEnabled;
    }

    @Override
    public SpeedSearchFilter<ActionPopupItem> getSpeedSearchFilter() {
        return this;
    }

    @Override
    public boolean isFinal(@Nonnull ActionPopupItem item) {
        if (!item.isEnabled()) {
            return true;
        }
        return !(item.getAction() instanceof ActionGroup) || item.isPerformGroup();
    }
}
