/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.awt.ui.impl.plaf2.flat;

import com.formdev.flatlaf.ui.FlatTabbedPaneUI;
import com.formdev.flatlaf.ui.FlatUIUtils;
import consulo.desktop.awt.ui.impl.tabs.TabPainter;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2026-08-28
 */
public class FlatTabbedPaneUI2 extends FlatTabbedPaneUI {
    public static ComponentUI createUI(JComponent c) {
        return new FlatTabbedPaneUI2();
    }

    @Override
    protected Color getTabBackground(int tabPlacement, int tabIndex, boolean isSelected) {
        if (isSelected && tabPane.isEnabled() && tabPane.isEnabledAt(tabIndex) && selectedBackground != null
            && tabPane.getBackgroundAt(tabIndex) == tabPane.getBackground()) {
            return selectedBackground;
        }
        return super.getTabBackground(tabPlacement, tabIndex, isSelected);
    }

    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
        if (getTabType() == TAB_TYPE_CARD) {
            super.paintTabBackground(g, tabPlacement, tabIndex, x, y, w, h, isSelected);
            return;
        }

        Color background = getTabBackground(tabPlacement, tabIndex, isSelected);
        if (background == tabPane.getBackground()) {
            return;
        }

        Color fill = FlatUIUtils.deriveColor(background, tabPane.getBackground());

        TabPainter.paintTab((Graphics2D) g, x, y, w, h, fill, tabPane.getTabPlacement());
    }
}
