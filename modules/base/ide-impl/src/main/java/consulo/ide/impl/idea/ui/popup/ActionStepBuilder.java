// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ui.popup;

import consulo.application.localize.ApplicationLocalize;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.openapi.actionSystem.impl.ActionGroupExpander;
import consulo.ide.impl.idea.openapi.actionSystem.impl.MenuItemPresentationFactory;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

class ActionStepBuilder {

    private final List<PopupFactoryImpl.ActionItem> myListModel;
    private final DataContext myDataContext;
    private final boolean myShowNumbers;
    private final boolean myUseAlphaAsNumbers;
    private final BasePresentationFactory myPresentationFactory;
    private final boolean myShowDisabled;
    private int myCurrentNumber;
    private final boolean myHonorActionMnemonics;
    private final String myActionPlace;
    private Image myEmptyIcon;
    private int myMaxIconWidth = -1;
    private int myMaxIconHeight = -1;

    ActionStepBuilder(@Nonnull DataContext dataContext,
                      boolean showNumbers,
                      boolean useAlphaAsNumbers,
                      boolean showDisabled,
                      boolean honorActionMnemonics,
                      @Nullable String actionPlace,
                      @Nullable BasePresentationFactory presentationFactory) {
        myUseAlphaAsNumbers = useAlphaAsNumbers;
        if (presentationFactory == null) {
            myPresentationFactory = new BasePresentationFactory();
        }
        else {
            myPresentationFactory = ObjectUtil.notNull(presentationFactory);
        }
        myListModel = new ArrayList<>();
        myDataContext = dataContext;
        myShowNumbers = showNumbers;
        myShowDisabled = showDisabled;
        myCurrentNumber = 0;
        myHonorActionMnemonics = honorActionMnemonics;
        myActionPlace = ObjectUtil.notNull(actionPlace, ActionPlaces.UNKNOWN);
    }

    @Nonnull
    public List<PopupFactoryImpl.ActionItem> getItems() {
        return myListModel;
    }

    public void buildGroup(@Nonnull ActionGroup actionGroup) {
        calcMaxIconSize(actionGroup);
        myEmptyIcon = myMaxIconHeight != -1 && myMaxIconWidth != -1 ? Image.empty(myMaxIconWidth, myMaxIconHeight) : null;

        appendActionsFromGroup(actionGroup);

        if (myListModel.isEmpty()) {
            myListModel.add(new PopupFactoryImpl.ActionItem(NothingHereAction.INSTANCE,
                ApplicationLocalize.nothingHere(),
                null,
                false,
                null,
                null,
                false));
        }
    }

    private void calcMaxIconSize(ActionGroup actionGroup) {
        if (myPresentationFactory instanceof MenuItemPresentationFactory factory && factory.shallHideIcons()) {
            return;
        }
        AnAction[] actions = actionGroup.getChildren(createActionEvent(actionGroup));
        for (AnAction action : actions) {
            if (action == null) {
                continue;
            }
            if (action instanceof ActionGroup group) {
                if (!group.isPopup()) {
                    calcMaxIconSize(group);
                    continue;
                }
            }

            Image icon = action.getTemplatePresentation().getIcon();
            if (icon == null && action instanceof Toggleable) {
                icon = Image.empty(Image.DEFAULT_ICON_SIZE);
            }
            if (icon != null) {
                int width = icon.getWidth();
                int height = icon.getHeight();
                if (myMaxIconWidth < width) {
                    myMaxIconWidth = width;
                }
                if (myMaxIconHeight < height) {
                    myMaxIconHeight = height;
                }
            }
        }
    }

    @Nonnull
    private AnActionEvent createActionEvent(@Nonnull AnAction action) {
        AnActionEvent actionEvent =
            AnActionEvent.createFromDataContext(myActionPlace, myPresentationFactory.getPresentation(action), myDataContext);
        actionEvent.setInjectedContext(action.isInInjectedContext());
        return actionEvent;
    }

    private void appendActionsFromGroup(@Nonnull ActionGroup actionGroup) {
        List<AnAction> newVisibleActions =
            ActionGroupExpander.expandActionGroup(actionGroup, myPresentationFactory, myDataContext, myActionPlace);
        for (AnAction action : newVisibleActions) {
            appendAction(action);
        }
    }

    private void appendAction(@Nonnull AnAction action) {
        if (action instanceof AnSeparator separator) {
            if (myListModel.isEmpty()) {
                // do not add separator at first
                return;
            }

            myListModel.add(new PopupFactoryImpl.ActionItem(action,
                separator.getTextValue(),
                null,
                true,
                null,
                null,
                true)
            );
        }
        else {

            Presentation presentation = myPresentationFactory.getPresentation(action);
            boolean enabled = presentation.isEnabled();
            LocalizeValue textValue = presentation.getTextValue();
            if ((myShowDisabled || enabled) && presentation.isVisible()) {
                if (myShowNumbers) {
                    textValue = textValue.map((localizeManager, text) -> {
                        if (myCurrentNumber < 9) {
                            text = "&" + (myCurrentNumber + 1) + ". " + text;
                        }
                        else if (myCurrentNumber == 9) {
                            text = "&" + 0 + ". " + text;
                        }
                        else if (myUseAlphaAsNumbers) {
                            text = "&" + (char) ('A' + myCurrentNumber - 10) + ". " + text;
                        }
                        return text;
                    });
                    myCurrentNumber++;
                }
                else if (presentation.isDisabledMnemonic()) {
                    // do nothing, in this case we will show '_' as part of action item
                }
                else if (!myHonorActionMnemonics) {
                    textValue = presentation.getTextValue().map(Presentation.NO_MNEMONIC);
                }

                boolean hideIcon = Boolean.TRUE.equals(presentation.getClientProperty(MenuItemPresentationFactory.HIDE_ICON));
                Image icon = hideIcon ? null : presentation.getIcon();
                Image selectedIcon = hideIcon ? null : presentation.getSelectedIcon();
                Image disabledIcon = hideIcon ? null : presentation.getDisabledIcon();

                if (icon == null && selectedIcon == null) {
                    if (action instanceof Toggleable && Toggleable.isSelected(presentation)) {
                        selectedIcon = TargetAWT.wrap(UIManager.getIcon("Menu.selectedCheckboxIcon"));
                        disabledIcon = null;
                    }
                }
                if (!enabled) {
                    icon = disabledIcon != null || icon == null ? disabledIcon : ImageEffects.grayed(icon);
                    selectedIcon = disabledIcon != null || selectedIcon == null ? disabledIcon : ImageEffects.grayed(selectedIcon);
                }

                if (myMaxIconWidth != -1 && myMaxIconHeight != -1) {
                    if (icon != null) {
                        icon = ImageEffects.resize(icon, myMaxIconWidth, myMaxIconHeight);
                    }
                    if (selectedIcon != null) {
                        selectedIcon = ImageEffects.resize(selectedIcon, myMaxIconWidth, myMaxIconHeight);
                    }
                }

                if (icon == null) {
                    icon = selectedIcon != null ? selectedIcon : myEmptyIcon;
                }
                assert textValue != LocalizeValue.empty() : action + " has no presentation";
                myListModel.add(new PopupFactoryImpl.ActionItem(action,
                    textValue,
                    (String) presentation.getClientProperty(UIUtil.TOOL_TIP_TEXT_KEY),
                    enabled,
                    icon,
                    selectedIcon,
                    false));
            }
        }
    }
}
