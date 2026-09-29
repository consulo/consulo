// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.ui.popup.actionPopup;

import consulo.ide.impl.idea.ui.popup.list.ListPopupImpl;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.KeepPopupOnPerform;
import consulo.ui.ex.awt.internal.PopupInlineActionsSupport;
import consulo.ui.ex.impl.internal.popup.action.ActionPopupItem;
import consulo.ui.ex.impl.internal.popup.action.ActionPopupStep;
import consulo.ui.ex.impl.internal.popup.action.PopupInlineActions;
import consulo.ui.image.Image;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;

class PopupInlineActionsSupportImpl implements PopupInlineActionsSupport {
    private final ListPopupImpl myListPopup;
    private final PopupInlineActions myInlineActions;

    PopupInlineActionsSupportImpl(ListPopupImpl listPopup) {
        myListPopup = listPopup;
        myInlineActions = new PopupInlineActions(listPopup.getListStep(), listPopup::isShowSubmenuOnHover);
    }

    @Override
    public int calcExtraButtonsCount(Object element) {
        return myInlineActions.getButtonsCount(element);
    }

    @Override
    public Integer calcButtonIndex(Object element, Point point) {
        if (element == null) {
            return null;
        }
        int buttonsCount = calcExtraButtonsCount(element);
        if (buttonsCount <= 0) {
            return null;
        }
        return PopupInlineActionsSupportKt.calcButtonIndex(myListPopup.getList(), buttonsCount, point);
    }

    @Override
    public String getToolTipText(Object element, int index) {
        LocalizeValue toolTip = myInlineActions.getToolTip(element, index);
        return toolTip.isEmpty() ? null : toolTip.get();
    }

    @Override
    public KeepPopupOnPerform getKeepPopupOnPerform(Object element, int index) {
        return myInlineActions.getKeepPopupOnPerform(element, index);
    }

    @Override
    @RequiredUIAccess
    public void performAction(Object element, int index, InputEvent event) {
        if (myInlineActions.isMoreButton(element, index)) {
            myListPopup.showNextStepPopup(myListPopup.getListStep().onChosen(element, false), element);
            return;
        }

        ActionPopupItem item = myInlineActions.getInlineItem(element, index);
        if (item != null && myListPopup.getListStep() instanceof ActionPopupStep step) {
            step.performActionItem(item, event);
            step.updateStepItems(myListPopup.getList());
        }
    }

    @Override
    public List<JComponent> createExtraButtons(Object value, boolean isSelected, int activeIndex) {
        List<JComponent> buttons = new ArrayList<>();
        for (PopupInlineActions.Button button : myInlineActions.getButtons(value)) {
            if (!isSelected && !button.alwaysVisible()) {
                continue;
            }

            Image icon = myInlineActions.getIcon(value, button, isSelected);
            if (icon == null) {
                throw new AssertionError("null inline item icon for action '" + button.item().getAction().getClass().getName() + "'");
            }
            buttons.add(PopupInlineActionsSupportKt.createExtraButton(icon, buttons.size() == activeIndex));
        }
        return buttons;
    }

    @Override
    public boolean isMoreButton(Object element, int index) {
        return myInlineActions.isMoreButton(element, index);
    }
}
