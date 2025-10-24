// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.ui.popup.actionPopup;

import consulo.application.AllIcons;
import consulo.ide.impl.idea.ui.popup.list.ListPopupImpl;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.KeepPopupOnPerform;
import consulo.ui.ex.awt.internal.PopupInlineActionsSupport;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.Collections;
import java.util.List;

class NonActionsPopupInlineSupport implements PopupInlineActionsSupport {
    private final ListPopupImpl myListPopup;

    NonActionsPopupInlineSupport(ListPopupImpl listPopup) {
        myListPopup = listPopup;
    }

    @Override
    public int calcExtraButtonsCount(Object element) {
        return hasMoreButton(element) ? 1 : 0;
    }

    @Override
    public Integer calcButtonIndex(Object element, Point point) {
        if (element == null || !hasMoreButton(element)) return null;
        return PopupInlineActionsSupportKt.calcButtonIndex(myListPopup.getList(), 1, point);
    }

    @Override
    public String getToolTipText(Object element, int index) {
        if (isMoreButton(element, index)) {
            return ActionLocalize.inlineActionsMoreActionsText().get();
        }
        return null;
    }

    @Override
    public KeepPopupOnPerform getKeepPopupOnPerform(Object element, int index) {
        return KeepPopupOnPerform.Always;
    }

    @Override
    public void performAction(Object element, int index, InputEvent event) {
        if (isMoreButton(element, index)) {
            myListPopup.showNextStepPopup(myListPopup.getListStep().onChosen(element, false), element);
        }
    }

    @Override
    public List<JComponent> createExtraButtons(Object value, boolean isSelected, int activeButtonIndex) {
        if (hasMoreButton(value) && isSelected) {
            return Collections.<JComponent>singletonList(
                PopupInlineActionsSupportKt.createExtraButton(
                   AllIcons.Actions.More, activeButtonIndex == 0
                )
            );
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isMoreButton(Object element, int index) {
        return hasMoreButton(element) && index == 0;
    }

    private boolean hasMoreButton(Object element) {
        return myListPopup.getListStep().hasSubstep(element)
            && !myListPopup.isShowSubmenuOnHover()
            && myListPopup.getListStep().isFinal(element);
    }
}
