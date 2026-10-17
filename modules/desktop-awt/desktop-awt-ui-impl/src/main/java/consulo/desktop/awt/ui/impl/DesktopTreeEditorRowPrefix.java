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

import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
final class DesktopTreeEditorRowPrefix extends JComponent {
    private final JTree myTree;
    private final int myWidth;
    private final CellRendererPane myRendererPane = new CellRendererPane();

    DesktopTreeEditorRowPrefix(JTree tree, int width, BiConsumer<TreePath, MouseEvent> onPress) {
        myTree = tree;
        myWidth = width;

        add(myRendererPane);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                TreePath path = myTree.getEditingPath();
                if (path != null && SwingUtilities.isLeftMouseButton(e)) {
                    onPress.accept(path, SwingUtilities.convertMouseEvent(DesktopTreeEditorRowPrefix.this, e, myTree));
                    repaint();
                }
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(myWidth, 0);
    }

    @Override
    protected void paintComponent(Graphics g) {
        TreePath path = myTree.getEditingPath();
        TreeCellRenderer renderer = myTree.getCellRenderer();
        if (path == null || renderer == null) {
            return;
        }

        Object value = path.getLastPathComponent();
        int row = myTree.getRowForPath(path);
        Component component = renderer.getTreeCellRendererComponent(
            myTree,
            value,
            true,
            myTree.isExpanded(path),
            myTree.getModel().isLeaf(value),
            row,
            false
        );

        int width = Math.max(myWidth, component.getPreferredSize().width);
        myRendererPane.paintComponent(g, component, this, 0, 0, width, getHeight(), true);
    }
}
