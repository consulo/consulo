/*
 * Copyright 2013-2020 consulo.io
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
package consulo.desktop.awt.editor.impl;

import consulo.desktop.awt.editor.impl.stickyLine.StickyLinesPanel;
import consulo.desktop.awt.language.editor.DesktopEditorPanelLayer;
import consulo.ui.ex.awt.JBLayeredPane;
import consulo.ui.ex.awt.JBUI;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2020-06-19
 */
public class DesktopEditorLayeredPanel {
    private final DesktopEditorImpl myEditor;

    private final JBLayeredPane myLayeredPane;

    private int myLayerPosition = JLayeredPane.POPUP_LAYER;

    public DesktopEditorLayeredPanel(DesktopEditorImpl editor) {
        myEditor = editor;
        myLayeredPane = new JBLayeredPane() {
            @Override
            public void doLayout() {
                Rectangle bounds = getBounds();

                int alignPadding = getAlignPadding();

                int stickyLineAllHeight = 0;
                Component[] components = getComponents();
                for (Component component : components) {
                    if (component instanceof StickyLinesPanel linesPanel) {
                        int stickyLinesCount = linesPanel.getStickyLinesCount();
                        stickyLineAllHeight = stickyLinesCount * editor.getLineHeight() + JBUI.scale(stickyLinesCount); // extra border
                    }
                }

                for (Component component : components) {
                    if (component instanceof StickyLinesPanel) {
                        continue;
                    }

                    if (component instanceof DesktopEditorPanelLayer) {
                        DesktopEditorPanelLayer layer = (DesktopEditorPanelLayer) component;

                        Dimension preferredSize = component.getPreferredSize();

                        component.setBounds(
                            bounds.width - preferredSize.width - alignPadding,
                            layer.getPositionYInLayer() + stickyLineAllHeight,
                            preferredSize.width,
                            preferredSize.height
                        );
                    }
                    else {
                        component.setBounds(0, 0, bounds.width, bounds.height);
                    }
                }
            }

            @Override
            public Dimension getPreferredSize() {
                return myEditor.getScrollPane().getPreferredSize();
            }
        };
    }

    private int getAlignPadding() {
        JScrollBar bar = myEditor.getVerticalScrollBar();

        int padding = bar.getWidth();
        padding += JBUI.scale(1); // just spacing
        return padding;
    }

    public void addLayerPanel(JComponent panel) {
        myLayeredPane.add(panel, Integer.valueOf(myLayerPosition++));
    }

    public void setMainPanel(JComponent rootPanel) {
        myLayeredPane.add(rootPanel, JBLayeredPane.DEFAULT_LAYER);
    }

    @Nonnull
    public JComponent getPanel() {
        return myLayeredPane;
    }
}
