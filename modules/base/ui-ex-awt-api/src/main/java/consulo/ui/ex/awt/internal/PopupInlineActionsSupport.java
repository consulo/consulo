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
package consulo.ui.ex.awt.internal;

import consulo.ui.ex.action.KeepPopupOnPerform;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.List;

public interface PopupInlineActionsSupport {
    int calcExtraButtonsCount(Object element);

    Integer calcButtonIndex(Object element, Point point);

    String getToolTipText(Object element, int index);

    KeepPopupOnPerform getKeepPopupOnPerform(Object element, int index);

    void performAction(Object element, int index, InputEvent event);

    List<JComponent> createExtraButtons(Object value, boolean isSelected, int activeButtonIndex);

    boolean isMoreButton(Object element, int index);

    default boolean hasExtraButtons(Object element) {
        return calcExtraButtonsCount(element) > 0;
    }

    default Integer getActiveButtonIndex(JList<?> list) {
        if (list instanceof ListWithInlineButtons listWithInlineButtons) {
            return listWithInlineButtons.getSelectedButtonIndex();
        }
        return null;
    }
}