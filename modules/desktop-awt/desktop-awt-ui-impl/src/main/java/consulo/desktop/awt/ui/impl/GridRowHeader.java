// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

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

import consulo.ui.ex.awt.util.GraphicsUtil;

import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.JTable;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.function.BooleanSupplier;

/**
 * The row header of a table: one cell per row of the table, painted by a {@link RowHeaderCellRenderer} in the height of its row,
 * with the horizontal grid lines of the table.
 * <p/>
 * The row header is the view of the row header viewport of the scroll pane of the table, so it is as high as the table, and the
 * scroll pane keeps it aligned with the rows.
 */
abstract class GridRowHeader extends JComponent {
    private static final int MIN_PREFERRED_WIDTH = 15;

    private final CellRendererPane myRendererPane = new CellRendererPane();
    private final BooleanSupplier myPaintHorizontalLines;
    private final BooleanSupplier myPaintLastHorizontalLine;
    private int myPreferredWidth = 1;

    protected GridRowHeader(JTable table, BooleanSupplier paintHorizontalLines, BooleanSupplier paintLastHorizontalLine) {
        myPaintHorizontalLines = paintHorizontalLines;
        myPaintLastHorizontalLine = paintLastHorizontalLine;
        add(myRendererPane);
        initListeners(table);
    }

    public abstract RowHeaderCellRenderer getCellRenderer();

    public abstract JTable getTable();

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(myPreferredWidth, getTable().getHeight());
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        GraphicsUtil.setupAntialiasing(g);

        super.paintComponent(g);

        JTable table = getTable();
        Rectangle clip = g.getClipBounds();
        int rowMargin = table.getRowMargin();
        BasicStroke gridLineStroke = new BasicStroke(rowMargin);

        processVisibleRows(table, g, (row, y) -> {
            int rowHeight = table.getRowHeight(row);
            int cellHeight = Math.max(0, rowHeight - rowMargin);

            Component cellRenderer = getCellRenderer().getRendererComponent(row, true);
            myRendererPane.paintComponent(g2, cellRenderer, this, 0, y, getWidth(), cellHeight, true);

            int gridY = y + cellHeight + rowMargin / 2;
            Stroke backupStroke = g2.getStroke();
            Color backupColor = g2.getColor();

            if (myPaintHorizontalLines.getAsBoolean() || myPaintLastHorizontalLine.getAsBoolean() && row == table.getRowCount() - 1) {
                g2.setStroke(gridLineStroke);
                g2.setColor(table.getGridColor());
                drawHLine(g2, clip.x, clip.x + clip.width, gridY);
                g2.setStroke(backupStroke);
                g2.setColor(backupColor);
            }
            return true;
        });
    }

    private void initListeners(JTable table) {
        table.getModel().addTableModelListener(e -> updatePreferredSize());
        // the row height of the table: the header itself has none
        table.addPropertyChangeListener("rowHeight", e -> updatePreferredSize());
        table.addComponentListener(new ComponentAdapter() {
            private int myPreviousTableHeight;

            @Override
            public void componentResized(ComponentEvent e) {
                Component c = e.getComponent();
                if (myPreviousTableHeight != c.getHeight()) {
                    updatePreferredSize(getPreferredSize().width, false);
                    myPreviousTableHeight = c.getHeight();
                }
            }
        });
    }

    public void updatePreferredSize() {
        Insets insets = getInsets();
        updatePreferredSize(insets.left + insets.right + calcPreferredWidthWithoutInsets(), true);
    }

    /**
     * The row header viewport does not follow a new preferred size by itself - the row header asks for the layout again, also when
     * only the height of the table changed, which is part of its preferred size.
     */
    protected void updatePreferredSize(int preferredWidth, boolean checkMinWidth) {
        if (checkMinWidth && preferredWidth < MIN_PREFERRED_WIDTH) {
            preferredWidth = MIN_PREFERRED_WIDTH;
        }
        Dimension prevSize = getPreferredSize();
        myPreferredWidth = preferredWidth;
        Dimension newSize = getPreferredSize();
        firePropertyChange("preferredSize", prevSize, newSize);
        revalidate();
        repaint();
    }

    protected int calcPreferredWidthWithoutInsets() {
        int maxCellRendererWidth = 0;
        for (int row = 0; row < getTable().getModel().getRowCount(); row++) {
            Component renderer = getCellRenderer().getRendererComponent(row, false);
            Dimension preferredSize = renderer.getPreferredSize();
            if (preferredSize.width > maxCellRendererWidth) {
                maxCellRendererWidth = preferredSize.width;
            }
        }
        return Math.max(1, maxCellRendererWidth);
    }

    /**
     * Calls the processor for each row of the table within the clip of the graphics, with the top of the row.
     */
    static void processVisibleRows(JTable table, Graphics g, RowProcessor proc) {
        Rectangle clip = g.getClipBounds();
        int rowCount = table.getRowCount();
        int startY = table.getRowMargin() / 2;
        int startRow = 0;
        while (startRow < rowCount) {
            int rowHeight = table.getRowHeight(startRow);
            if (startY + rowHeight >= clip.y) {
                break;
            }
            startY += rowHeight;
            startRow++;
        }

        int y = startY;
        for (int row = startRow; row < rowCount && y <= clip.y + clip.height; row++) {
            if (!proc.process(row, y)) {
                break;
            }
            int rowHeight = table.getRowHeight(row);
            y += rowHeight;
        }
    }

    /**
     * A line one pixel high, from {@code x1} to {@code x2}, both included.
     */
    private static void drawHLine(Graphics g, int x1, int x2, int y) {
        g.fillRect(Math.min(x1, x2), y, Math.abs(x2 - x1) + 1, 1);
    }

    public interface RowHeaderCellRenderer {
        Component getRendererComponent(int row, boolean forDisplay);
    }

    interface RowProcessor {
        boolean process(int row, int y);
    }
}
