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
package consulo.desktop.awt.ui.impl;

import consulo.ui.ex.awt.ThreeStateCheckBox;
import consulo.util.lang.ThreeState;
import org.jspecify.annotations.Nullable;

import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.TreeCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
class DesktopCheckBoxTreeCellRenderer extends JPanel implements TreeCellRenderer {
    private final DesktopTreeCellRenderer myTextRenderer = new DesktopTreeCellRenderer();
    private final ThreeStateCheckBox myCheckBox = new ThreeStateCheckBox();
    private final DesktopTreeCheckBoxes<?> myCheckBoxes;

    DesktopCheckBoxTreeCellRenderer(DesktopTreeCheckBoxes<?> checkBoxes) {
        super(new BorderLayout());
        myCheckBoxes = checkBoxes;

        myCheckBox.setThirdStateEnabled(false);
        myCheckBox.setOpaque(false);
        setOpaque(false);

        add(myCheckBox, BorderLayout.WEST);
        add(myTextRenderer, BorderLayout.CENTER);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree,
                                                  Object value,
                                                  boolean selected,
                                                  boolean expanded,
                                                  boolean leaf,
                                                  int row,
                                                  boolean hasFocus) {
        invalidate();

        ThreeState state = myCheckBoxes.checkStateOf(value);
        if (state == null) {
            myCheckBox.setVisible(false);
        }
        else {
            myCheckBox.setVisible(true);
            myCheckBox.setEnabled(myCheckBoxes.isCheckEnabledOf(value));
            myCheckBox.setState(switch (state) {
                case YES -> ThreeStateCheckBox.State.SELECTED;
                case NO -> ThreeStateCheckBox.State.NOT_SELECTED;
                case UNSURE -> ThreeStateCheckBox.State.DONT_CARE;
            });
        }

        myTextRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        setFont(tree.getFont());

        revalidate();
        return this;
    }

    Rectangle getCheckBoxBounds(Rectangle rowBounds) {
        setBounds(rowBounds);
        doLayout();

        Rectangle bounds = myCheckBox.getBounds();
        bounds.translate(rowBounds.x, rowBounds.y);
        if (bounds.height == 0) {
            bounds.width = rowBounds.height;
            bounds.height = rowBounds.height;
        }
        return bounds;
    }

    @Override
    public @Nullable String getToolTipText(MouseEvent event) {
        return myTextRenderer.getToolTipText();
    }
}
