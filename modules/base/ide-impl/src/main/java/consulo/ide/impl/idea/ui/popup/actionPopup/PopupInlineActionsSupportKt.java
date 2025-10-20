/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ide.impl.idea.ui.popup.actionPopup;

import consulo.ide.impl.idea.ui.popup.list.ListPopupImpl;
import consulo.ui.ex.action.KeepPopupOnPerform;
import consulo.ui.ex.awt.internal.PopupInlineActionsSupport;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.lang.Boolean;
import java.util.Collections;
import java.util.List;

public class PopupInlineActionsSupportKt {
    private static final PopupInlineActionsSupport Empty = new PopupInlineActionsSupport() {
        @Override
        public int calcExtraButtonsCount(Object element) {
            return 0;
        }

        @Override
        public Integer calcButtonIndex(Object element, Point point) {
            return null;
        }

        @Override
        public String getToolTipText(Object element, int index) {
            return null;
        }

        @Override
        public KeepPopupOnPerform getKeepPopupOnPerform(Object element, int index) {
            return KeepPopupOnPerform.Always;
        }

        @Override
        public void performAction(Object element, int index, InputEvent event) {
        }

        @Override
        public List<JComponent> createExtraButtons(Object value, boolean isSelected, int activeButtonIndex) {
            return Collections.emptyList();
        }

        @Override
        public boolean isMoreButton(Object element, int index) {
            return false;
        }

        @Override
        public Integer getActiveButtonIndex(JList<?> list) {
            return null;
        }
    };

    static JButton createExtraButton(Image icon, boolean active) {
        JButton button = new JButton(TargetAWT.to(icon));
        button.setSelected(active);
        button.setOpaque(active);
        button.setFocusable(false);

        button.putClientProperty("INLINE_BUTTON", Boolean.TRUE);
        button.putClientProperty("INLINE_BUTTON_ACTIVE", active);

        button.putClientProperty("FlatLaf.styleClass", "inTextField");
        button.putClientProperty("JButton.buttonType", "toolBarButton");

        return button;
    }

    static Integer calcButtonIndex(JList<?> list, int buttonsCount, Point point) {
        if (list == null || buttonsCount <= 0 || point == null) return null;
        int row = list.locationToIndex(point);
        if (row < 0) return null;
        Rectangle cell = list.getCellBounds(row, row);
        if (cell == null || !cell.contains(point)) return null;

        int buttonSize = 22;
        int gap = 4;
        int totalWidth = buttonsCount * buttonSize + (buttonsCount - 1) * gap;

        int rightPadding = 8;
        int leftOfButtons = cell.x + cell.width - rightPadding - totalWidth;

        if (point.x < leftOfButtons || point.x > cell.x + cell.width - rightPadding) return null;

        int offset = point.x - leftOfButtons;
        int slot = offset / (buttonSize + gap);
        int inSlot = offset % (buttonSize + gap);

        if (slot < 0 || slot >= buttonsCount) return null;
        if (inSlot >= buttonSize) return null;

        return slot;
    }

    public static PopupInlineActionsSupport createSupport(ListPopupImpl popup) {
        if (popup.getListStep() instanceof ActionPopupStep) return new PopupInlineActionsSupportImpl(popup);
        return new NonActionsPopupInlineSupport(popup);
    }
}