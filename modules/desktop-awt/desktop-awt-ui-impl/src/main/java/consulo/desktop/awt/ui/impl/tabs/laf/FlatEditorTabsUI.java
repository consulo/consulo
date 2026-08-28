/*
 * Copyright 2013-2024 consulo.io
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
package consulo.desktop.awt.ui.impl.tabs.laf;

import consulo.desktop.awt.ui.impl.tabs.laf.IntelliJEditorTabsUI;
import consulo.desktop.awt.ui.impl.tabs.JBTabsImpl;
import consulo.desktop.awt.ui.impl.tabs.TabLabel;
import consulo.desktop.awt.ui.impl.tabs.laf.JBEditorTabsUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.desktop.awt.ui.impl.tabs.TabPainter;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2024-12-13
 */
public class FlatEditorTabsUI extends IntelliJEditorTabsUI {
    public static JBEditorTabsUI createUI(JComponent c) {
        return new FlatEditorTabsUI();
    }

    @Override
    protected void fillInactiveTab(JBTabsImpl tabs, Graphics2D g2d, TabLabel label, ShapeInfo shape) {
        g2d.setColor(UIUtil.getPanelBackground());

        g2d.fill(shape.fillPath.getShape());

        Rectangle rect = tabPaintRect(tabs, label.getBounds());
        Color tabColor = label.getInfo().getTabColor();

        if (tabColor != null) {
            TabPainter.paintTab(g2d, rect.x, rect.y, rect.width, rect.height, tabColor, SwingConstants.TOP, false);
        }
        else if (tabs.isPaintFocus() && tabs.isHoveredTab(label)) {
            TabPainter.paintTabHover(g2d, rect.x, rect.y, rect.width, rect.height, SwingConstants.TOP);
        }
    }

    @Override
    protected void fillActiveTabWithColor(TabLabel label, JBTabsImpl tabs, Graphics2D g2d) {
        ShapeInfo shape = computeLabelShape(tabs, label);

        g2d.setColor(UIUtil.getPanelBackground());

        g2d.fill(shape.fillPath.getShape());
    }
}
