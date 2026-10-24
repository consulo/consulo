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
package consulo.desktop.awt.ui.impl.layout;

import consulo.desktop.awt.ui.impl.DesktopSpace;
import consulo.ui.Space;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Children at their preferred width, wrapped into rows at the width of the container.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
class DesktopFlowLayoutManager implements DesktopViewportLayoutManager {
    private final Space myGap;

    private int myLastHeight = -1;

    DesktopFlowLayoutManager(Space gap) {
        myGap = gap;
    }

    @Override
    public void addLayoutComponent(String name, Component component) {
    }

    @Override
    public void removeLayoutComponent(Component component) {
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return preferredViewportSize(parent, Integer.MAX_VALUE);
    }

    @Override
    public Dimension preferredViewportSize(Container parent, int rows) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            List<Component> children = visibleChildren(parent);

            int gap = DesktopSpace.toPixels(myGap);
            int singleRowWidth = 0;
            for (int i = 0; i < children.size(); i++) {
                singleRowWidth += children.get(i).getPreferredSize().width + (i > 0 ? gap : 0);
            }

            int width = availableWidth(parent);
            int height = arrange(parent, children, width > 0 ? width : singleRowWidth, false, rows);
            if (rows == Integer.MAX_VALUE) {
                myLastHeight = height;
            }
            return new Dimension(singleRowWidth + insets.left + insets.right, height + insets.top + insets.bottom);
        }
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            int width = 0;
            for (Component child : visibleChildren(parent)) {
                width = Math.max(width, child.getMinimumSize().width);
            }
            return new Dimension(width + insets.left + insets.right, preferredLayoutSize(parent).height);
        }
    }

    @Override
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            int width = parent.getWidth() - insets.left - insets.right;
            if (width <= 0) {
                return;
            }

            int height = arrange(parent, visibleChildren(parent), width, true, Integer.MAX_VALUE);
            if (height != myLastHeight) {
                myLastHeight = height;

                SwingUtilities.invokeLater(() -> {
                    parent.revalidate();
                    parent.repaint();
                });
            }
        }
    }

    private int arrange(Container parent, List<Component> children, int width, boolean apply, int maxRows) {
        if (children.isEmpty()) {
            return 0;
        }

        Insets insets = parent.getInsets();
        int gap = DesktopSpace.toPixels(myGap);

        int y = 0;
        int rows = 0;
        int rowStart = 0;
        while (rowStart < children.size() && rows < maxRows) {
            int rowEnd = rowStart;
            int rowWidth = 0;
            int rowHeight = 0;
            while (rowEnd < children.size()) {
                Dimension size = children.get(rowEnd).getPreferredSize();
                int childWidth = Math.min(size.width, width);
                int next = rowWidth + (rowEnd > rowStart ? gap : 0) + childWidth;
                if (rowEnd > rowStart && next > width) {
                    break;
                }
                rowWidth = next;
                rowHeight = Math.max(rowHeight, size.height);
                rowEnd++;
            }

            if (apply) {
                int x = 0;
                for (int i = rowStart; i < rowEnd; i++) {
                    Component child = children.get(i);
                    int childWidth = Math.min(child.getPreferredSize().width, width);
                    child.setBounds(insets.left + x, insets.top + y, childWidth, rowHeight);
                    x += childWidth + gap;
                }
            }

            y += rowHeight;
            rows++;
            if (rowEnd < children.size() && rows < maxRows) {
                y += gap;
            }
            rowStart = rowEnd;
        }
        return y;
    }

    private static int availableWidth(Container parent) {
        Insets insets = parent.getInsets();
        int width = parent.getWidth();
        if (width <= 0 && SwingUtilities.getAncestorOfClass(JViewport.class, parent) instanceof JViewport viewport) {
            width = viewport.getWidth();
        }
        return width - insets.left - insets.right;
    }

    private static List<Component> visibleChildren(Container parent) {
        List<Component> children = new ArrayList<>(parent.getComponentCount());
        for (Component component : parent.getComponents()) {
            if (component.isVisible()) {
                children.add(component);
            }
        }
        return children;
    }
}
