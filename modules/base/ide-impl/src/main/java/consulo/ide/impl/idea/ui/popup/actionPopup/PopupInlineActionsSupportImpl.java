// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.ui.popup.actionPopup;

import consulo.ide.impl.idea.ui.popup.list.ListPopupImpl;
import consulo.ide.localize.IdeLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.KeepPopupOnPerform;
import consulo.ui.ex.action.util.ActionUtil;
import consulo.ui.ex.awt.internal.PopupInlineActionsSupport;
import consulo.ui.image.Image;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class PopupInlineActionsSupportImpl implements PopupInlineActionsSupport {
    private final ListPopupImpl myListPopup;
    private final ActionPopupStep myStep;

    PopupInlineActionsSupportImpl(ListPopupImpl myListPopup) {
        this.myListPopup = myListPopup;
        this.myStep = (ActionPopupStep) myListPopup.getListStep();
    }

    @Override
    public int calcExtraButtonsCount(Object element) {
        if (!(element instanceof ActionPopupItem)) {
            return 0;
        }
        int res = 0;
        res += myStep.getInlineItems((ActionPopupItem) element).size();
        if (hasMoreButton((ActionPopupItem) element)) {
            res++;
        }
        return res;
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
        if (!(element instanceof ActionPopupItem)) {
            return null;
        }
        if (isMoreButton(element, index)) {
            return IdeLocalize.inlineActionsMoreActionsText().get();
        }
        ActionPopupItem item =
            myStep.getInlineItems((ActionPopupItem) element).get(index);
        return item != null ? item.getText().get() : null;
    }

    @Override
    public KeepPopupOnPerform getKeepPopupOnPerform(Object element, int index) {
        if (!(element instanceof ActionPopupItem)) {
            return KeepPopupOnPerform.Always;
        }
        if (isMoreButton(element, index)) {
            return KeepPopupOnPerform.Always;
        }
        return myStep.getInlineItems((ActionPopupItem) element).get(index).getKeepPopupOnPerform();
    }

    @Override
    public void performAction(Object element, int index, InputEvent event) {
        if (!(element instanceof ActionPopupItem)) {
            return;
        }
        if (isMoreButton(element, index)) {
            myListPopup.showNextStepPopup(myStep.onChosen((ActionPopupItem) element, false),
                (ActionPopupItem) element);
        }
        else {
            ActionPopupItem item =
                myStep.getInlineItems((ActionPopupItem) element).get(index);
            myStep.performActionItem(item, event);
            myStep.updateStepItems(myListPopup.getList());
        }
    }

    @Override
    public List<JComponent> createExtraButtons(Object value, boolean isSelected, int activeIndex) {
        if (!(value instanceof ActionPopupItem)) {
            return Collections.emptyList();
        }
        List<ActionPopupItem> inlineItems =
            myStep.getInlineItems((ActionPopupItem) value);

        ArrayList<JComponent> buttons = new ArrayList<>();

        for (int i = 0; i < inlineItems.size(); i++) {
            ActionPopupItem item = inlineItems.get(i);
            if (isSelected || Boolean.TRUE.equals(item.getClientProperty(ActionUtil.ALWAYS_VISIBLE_INLINE_ACTION))) {
                buttons.add(createActionButton(item, i == activeIndex, isSelected));
            }
        }

        if ((isSelected || !buttons.isEmpty()) && hasMoreButton((ActionPopupItem) value)) {
            Image icon = myStep.isFinal((ActionPopupItem) value)
                ? PlatformIconGroup.actionsMorevertical()
                : PlatformIconGroup.ideMenuarrow();
            buttons.add(PopupInlineActionsSupportKt.createExtraButton(icon, buttons.size() == activeIndex));
        }

        return buttons;
    }

    private JComponent createActionButton(ActionPopupItem item, boolean active, boolean isSelected) {
        Image icon = item.getIcon(isSelected);
        if (icon == null) {
            throw new AssertionError("null inline item icon for action '" + item.getAction().getClass().getName() + "'");
        }
        return PopupInlineActionsSupportKt.createExtraButton(icon, active);
    }

    @Override
    public boolean isMoreButton(Object element, int index) {
        if (!(element instanceof ActionPopupItem) || !hasMoreButton((ActionPopupItem) element)) {
            return false;
        }
        int count = calcExtraButtonsCount(element);
        return count > 0 && index == count - 1;
    }

    private boolean hasMoreButton(ActionPopupItem element) {
        return myStep.hasSubstep(element) && !myListPopup.isShowSubmenuOnHover() && myStep.isFinal(element);
    }
}
