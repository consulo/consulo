// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.popup;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.MenuItemPresentationFactory;
import com.intellij.openapi.actionSystem.impl.PresentationFactory;
import com.intellij.openapi.actionSystem.impl.Utils;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ui.UIUtil;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.lang.ObjectUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

class ActionStepBuilder {
  private final List<PopupFactoryImpl.ActionItem> myListModel;
  private final DataContext myDataContext;
  private final boolean myShowNumbers;
  private final boolean myUseAlphaAsNumbers;
  private final PresentationFactory myPresentationFactory;
  private final boolean myShowDisabled;
  private int myCurrentNumber;
  private boolean myPrependWithSeparator;
  private String mySeparatorText;
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
                    @Nullable PresentationFactory presentationFactory) {
    myUseAlphaAsNumbers = useAlphaAsNumbers;
    if (presentationFactory == null) {
      myPresentationFactory = new PresentationFactory();
    }
    else {
      myPresentationFactory = ObjectUtil.notNull(presentationFactory);
    }
    myListModel = new ArrayList<>();
    myDataContext = dataContext;
    myShowNumbers = showNumbers;
    myShowDisabled = showDisabled;
    myCurrentNumber = 0;
    myPrependWithSeparator = false;
    mySeparatorText = null;
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
      myListModel.add(new PopupFactoryImpl.ActionItem(Utils.EMPTY_MENU_FILLER, Utils.NOTHING_HERE, null, false, null, null, false, null));
    }
  }

  private void calcMaxIconSize(final ActionGroup actionGroup) {
    if (myPresentationFactory instanceof MenuItemPresentationFactory && ((MenuItemPresentationFactory)myPresentationFactory).shallHideIcons()) return;
    AnAction[] actions = actionGroup.getChildren(createActionEvent(actionGroup));
    for (AnAction action : actions) {
      if (action == null) continue;
      if (action instanceof ActionGroup) {
        final ActionGroup group = (ActionGroup)action;
        if (!group.isPopup()) {
          calcMaxIconSize(group);
          continue;
        }
      }

      Image icon = action.getTemplatePresentation().getIcon();
      if (icon == null && action instanceof Toggleable) icon = Image.empty(Image.DEFAULT_ICON_SIZE);
      if (icon != null) {
        final int width = icon.getWidth();
        final int height = icon.getHeight();
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
    AnActionEvent actionEvent = AnActionEvent.createFromDataContext(myActionPlace, myPresentationFactory.getPresentation(action), myDataContext);
    actionEvent.setInjectedContext(action.isInInjectedContext());
    return actionEvent;
  }

  private void appendActionsFromGroup(@Nonnull ActionGroup actionGroup) {
    List<AnAction> newVisibleActions = Utils.expandActionGroup(false, actionGroup, myPresentationFactory, myDataContext, myActionPlace);
    for (AnAction action : newVisibleActions) {
      if (action instanceof AnSeparator) {
        myPrependWithSeparator = true;
        mySeparatorText = ((AnSeparator)action).getText();
      }
      else {
        appendAction(action);
      }
    }
  }

  private void appendAction(@Nonnull AnAction action) {
    Presentation presentation = myPresentationFactory.getPresentation(action);
    boolean enabled = presentation.isEnabled();
    LocalizeValue textValue = presentation.getTextValue().map(Presentation.NO_MNEMONIC);
    if ((myShowDisabled || enabled) && presentation.isVisible()) {
      if (myShowNumbers) {
        String text = presentation.getText();
        if (myCurrentNumber < 9) {
          text = "&" + (myCurrentNumber + 1) + ". " + text;
        }
        else if (myCurrentNumber == 9) {
          text = "&" + 0 + ". " + text;
        }
        else if (myUseAlphaAsNumbers) {
          text = "&" + (char)('A' + myCurrentNumber - 10) + ". " + text;
        }
        myCurrentNumber++;

        textValue = LocalizeValue.of(StringUtil.notNullize(text));
      }
      else if (myHonorActionMnemonics || presentation.isDisabledMnemonic()) {
        textValue = presentation.getTextValue();
      }

      boolean hideIcon = Boolean.TRUE.equals(presentation.getClientProperty(MenuItemPresentationFactory.HIDE_ICON));
      Image icon = hideIcon ? null : presentation.getIcon();
      Image selectedIcon = hideIcon ? null : presentation.getSelectedIcon();
      Image disabledIcon = hideIcon ? null : presentation.getDisabledIcon();

      if (icon == null && selectedIcon == null) {
        final String actionId = ActionManager.getInstance().getId(action);
        if (actionId != null && actionId.startsWith("QuickList.")) {
          //icon =  null; // AllIcons.Actions.QuickList;
        }
        else if (action instanceof Toggleable && Toggleable.isSelected(presentation)) {
          icon = Image.empty(Image.DEFAULT_ICON_SIZE);
          selectedIcon = AllIcons.Actions.Checked;
          disabledIcon = null;
        }
      }
      if (!enabled) {
        icon = disabledIcon != null || icon == null ? disabledIcon : ImageEffects.grayed(icon);
        selectedIcon = disabledIcon != null || selectedIcon == null ? disabledIcon : ImageEffects.grayed(selectedIcon);
      }

      if (myMaxIconWidth != -1 && myMaxIconHeight != -1) {
        if (icon != null) icon = ImageEffects.resize(icon, myMaxIconWidth, myMaxIconHeight);
        if (selectedIcon != null) selectedIcon = ImageEffects.resize(selectedIcon, myMaxIconWidth, myMaxIconHeight);
      }

      if (icon == null) icon = selectedIcon != null ? selectedIcon : myEmptyIcon;
      boolean prependSeparator = (!myListModel.isEmpty() || mySeparatorText != null) && myPrependWithSeparator;
      assert textValue != LocalizeValue.empty() : action + " has no presentation";
      myListModel.add(new PopupFactoryImpl.ActionItem(action, textValue, (String)presentation.getClientProperty(UIUtil.TOOL_TIP_TEXT_KEY), enabled, icon, selectedIcon, prependSeparator,
                                                      mySeparatorText));
      myPrependWithSeparator = false;
      mySeparatorText = null;
    }
  }
}
