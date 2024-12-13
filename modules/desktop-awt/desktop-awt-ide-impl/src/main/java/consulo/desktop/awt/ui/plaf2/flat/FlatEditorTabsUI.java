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
package consulo.desktop.awt.ui.plaf2.flat;

import consulo.desktop.awt.ui.plaf.intellij.IntelliJEditorTabsUI;
import consulo.ide.impl.idea.ui.tabs.impl.JBTabsImpl;
import consulo.ide.impl.idea.ui.tabs.impl.TabLabel;
import consulo.ide.impl.ui.laf.JBEditorTabsUI;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.ColorUtil;

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

        Color tabColor = label.getInfo().getTabColor();
        if (tabs.isPaintFocus() && tabs.isHoveredTab(label)) {
            tabColor = tabColor != null ? tabColor : JBColor.border();
        }
        else {
            tabColor = tabColor != null ? ColorUtil.toAlpha(tabColor, 100) : UIUtil.getPanelBackground();
        }

        g2d.setColor(tabColor);
        g2d.fill(shape.fillPath.getShape());
    }

    @Override
    protected void fillActiveTabWithColor(TabLabel label, JBTabsImpl tabs, Graphics2D g2d) {
        ShapeInfo shape = computeLabelShape(tabs, label);

        g2d.setColor(UIUtil.getPanelBackground());

        g2d.fill(shape.fillPath.getShape());

        Color tabColor = label.getInfo().getTabColor();
        if (tabs.isPaintFocus() && tabs.isHoveredTab(label)) {
            tabColor = tabColor != null ? tabColor : JBColor.border();
        }
        else {
            tabColor = tabColor != null ? tabColor : UIUtil.getPanelBackground();
        }

        g2d.setColor(tabColor);
        g2d.fill(shape.fillPath.getShape());

        if (tabs.isPaintFocus() && tabs.isHoveredTab(label) && tabColor != null) {
            g2d.setColor(ColorUtil.toAlpha(JBColor.border(), 100));
            g2d.fill(shape.fillPath.getShape());
        }
    }
}
