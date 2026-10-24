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

import consulo.desktop.awt.ui.impl.DesktopLength;
import consulo.desktop.awt.ui.impl.DesktopSpace;
import consulo.ui.Length;
import consulo.ui.Space;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
class DesktopTileLayoutManager implements DesktopViewportLayoutManager {
    private final Length myMinTileWidth;
    private final Space myGap;

    private int myLastHeight = -1;

    DesktopTileLayoutManager(Length minTileWidth, Space gap) {
        myMinTileWidth = minTileWidth;
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
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            List<Component> tiles = visibleTiles(parent);

            int preferredWidth = Math.max(DesktopLength.toPixels(parent, myMinTileWidth), maxPreferredWidth(tiles));

            int width = availableWidth(parent);
            int height = arrange(parent, tiles, width > 0 ? width : preferredWidth, false, Integer.MAX_VALUE);
            myLastHeight = height;
            return new Dimension(preferredWidth + insets.left + insets.right, height + insets.top + insets.bottom);
        }
    }

    @Override
    public Dimension preferredViewportSize(Container parent, int rows) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            List<Component> tiles = visibleTiles(parent);

            int preferredWidth = Math.max(DesktopLength.toPixels(parent, myMinTileWidth), maxPreferredWidth(tiles));

            int width = availableWidth(parent);
            int height = arrange(parent, tiles, width > 0 ? width : preferredWidth, false, rows);
            return new Dimension(preferredWidth + insets.left + insets.right, height + insets.top + insets.bottom);
        }
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            Dimension preferred = preferredLayoutSize(parent);
            return new Dimension(insets.left + insets.right, preferred.height);
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

            int height = arrange(parent, visibleTiles(parent), width, true, Integer.MAX_VALUE);
            if (height != myLastHeight) {
                myLastHeight = height;

                SwingUtilities.invokeLater(() -> {
                    parent.revalidate();
                    parent.repaint();
                });
            }
        }
    }

    private int arrange(Container parent, List<Component> tiles, int width, boolean apply, int maxRows) {
        if (tiles.isEmpty()) {
            return 0;
        }

        Insets insets = parent.getInsets();
        int gap = DesktopSpace.toPixels(myGap);
        int columns = columnCount(width, DesktopLength.toPixels(parent, myMinTileWidth), gap, tiles.size());

        int[] lefts = new int[columns];
        int[] widths = new int[columns];
        int tracks = Math.max(0, width - gap * (columns - 1));
        for (int column = 0; column < columns; column++) {
            int left = column * tracks / columns + column * gap;
            int right = (column + 1) * tracks / columns + column * gap;
            lefts[column] = left;
            widths[column] = right - left;
        }

        int y = 0;
        int rows = 0;
        for (int rowStart = 0; rowStart < tiles.size() && rows < maxRows; rowStart += columns, rows++) {
            int rowEnd = Math.min(tiles.size(), rowStart + columns);

            int rowHeight = 0;
            for (int i = rowStart; i < rowEnd; i++) {
                rowHeight = Math.max(rowHeight, heightFor(tiles.get(i), widths[i - rowStart]));
            }

            if (apply) {
                for (int i = rowStart; i < rowEnd; i++) {
                    int column = i - rowStart;
                    tiles.get(i).setBounds(insets.left + lefts[column], insets.top + y, widths[column], rowHeight);
                }
            }

            y += rowHeight;
            if (rowEnd < tiles.size() && rows + 1 < maxRows) {
                y += gap;
            }
        }
        return y;
    }

    private static int heightFor(Component tile, int width) {
        if (tile.getWidth() != width) {
            tile.setSize(width, Math.max(tile.getHeight(), tile.getPreferredSize().height));
            tile.validate();
        }
        return tile.getPreferredSize().height;
    }

    private static int columnCount(int width, int minTileWidth, int gap, int tileCount) {
        int fit = (width + gap) / Math.max(1, minTileWidth + gap);
        return Math.max(1, Math.min(fit, tileCount));
    }

    private static int availableWidth(Container parent) {
        Insets insets = parent.getInsets();
        int width = parent.getWidth();
        if (width <= 0 && SwingUtilities.getAncestorOfClass(JViewport.class, parent) instanceof JViewport viewport) {
            width = viewport.getWidth();
        }
        return width - insets.left - insets.right;
    }

    private static int maxPreferredWidth(List<Component> tiles) {
        int width = 0;
        for (Component tile : tiles) {
            width = Math.max(width, tile.getPreferredSize().width);
        }
        return width;
    }

    private static List<Component> visibleTiles(Container parent) {
        List<Component> tiles = new ArrayList<>(parent.getComponentCount());
        for (Component component : parent.getComponents()) {
            if (component.isVisible()) {
                tiles.add(component);
            }
        }
        return tiles;
    }
}
