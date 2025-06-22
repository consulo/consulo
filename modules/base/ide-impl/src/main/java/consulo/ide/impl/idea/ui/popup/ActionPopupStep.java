// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ui.popup;

import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionImplUtil;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.StatusText;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.popup.ListPopupStepEx;
import consulo.ui.ex.popup.*;
import consulo.ui.image.Image;
import consulo.ui.util.TextWithMnemonic;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.event.InputEvent;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ActionPopupStep implements ListPopupStepEx<PopupFactoryImpl.ActionItem>, MnemonicNavigationFilter<PopupFactoryImpl.ActionItem>, SpeedSearchFilter<PopupFactoryImpl.ActionItem> {
    private final List<PopupFactoryImpl.ActionItem> myItems;
    private final String myTitle;
    private final Supplier<? extends DataContext> myContext;
    private final String myActionPlace;
    private final boolean myEnableMnemonics;
    private final BasePresentationFactory myPresentationFactory;
    private final int myDefaultOptionIndex;
    private final boolean myAutoSelectionEnabled;
    private final boolean myShowDisabledActions;
    private Runnable myFinalRunnable;
    private final Predicate<? super AnAction> myPreselectActionCondition;

    public ActionPopupStep(
        @Nonnull List<PopupFactoryImpl.ActionItem> items,
        String title,
        @Nonnull Supplier<? extends DataContext> context,
        @Nullable String actionPlace,
        boolean enableMnemonics,
        @Nullable Predicate<? super AnAction> preselectActionCondition,
        boolean autoSelection,
        boolean showDisabledActions,
        @Nullable BasePresentationFactory presentationFactory
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
        @Nonnull List<? extends PopupFactoryImpl.ActionItem> items
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
    public static ListPopupStep<PopupFactoryImpl.ActionItem> createActionsStep(
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
        @Nullable BasePresentationFactory presentationFactory
    ) {
        List<PopupFactoryImpl.ActionItem> items = createActionItems(
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
    public static List<PopupFactoryImpl.ActionItem> createActionItems(
        @Nonnull ActionGroup actionGroup,
        @Nonnull DataContext dataContext,
        boolean showNumbers,
        boolean useAlphaAsNumbers,
        boolean showDisabledActions,
        boolean honorActionMnemonics,
        @Nullable String actionPlace,
        @Nullable BasePresentationFactory presentationFactory
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

    @Override
    @Nonnull
    public List<PopupFactoryImpl.ActionItem> getValues() {
        return myItems;
    }

    @Override
    public boolean isSelectable(PopupFactoryImpl.ActionItem value) {
        return value.isEnabled();
    }

    @Override
    public int getMnemonicPos(PopupFactoryImpl.ActionItem value) {
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
    public Image getIconFor(PopupFactoryImpl.ActionItem aValue) {
        return aValue.getIcon(false);
    }

    @Override
    public Image getSelectedIconFor(PopupFactoryImpl.ActionItem value) {
        return value.getIcon(true);
    }

    @Override
    @Nonnull
    public String getTextFor(PopupFactoryImpl.ActionItem value) {
        return value.getText().getValue();
    }

    @Nullable
    @Override
    public String getTooltipTextFor(PopupFactoryImpl.ActionItem value) {
        return value.getDescription();
    }

    @Override
    public void setEmptyText(@Nonnull StatusText emptyText) {
    }

    @Override
    public ListSeparator getSeparatorAbove(PopupFactoryImpl.ActionItem value) {
        return value.isPrependWithSeparator() ? new ListSeparator(value.getSeparatorText()) : null;
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
    public PopupStep onChosen(PopupFactoryImpl.ActionItem actionChoice, boolean finalChoice) {
        return onChosen(actionChoice, finalChoice, 0);
    }

    @Override
    public PopupStep<PopupFactoryImpl.ActionItem> onChosen(
        PopupFactoryImpl.ActionItem actionChoice,
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

    @Override
    public Runnable getFinalRunnable() {
        return myFinalRunnable;
    }

    @Override
    public boolean hasSubstep(PopupFactoryImpl.ActionItem selectedValue) {
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
    public MnemonicNavigationFilter<PopupFactoryImpl.ActionItem> getMnemonicNavigationFilter() {
        return this;
    }

    @Override
    public String getIndexedString(PopupFactoryImpl.ActionItem value) {
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
    public SpeedSearchFilter<PopupFactoryImpl.ActionItem> getSpeedSearchFilter() {
        return this;
    }
}
