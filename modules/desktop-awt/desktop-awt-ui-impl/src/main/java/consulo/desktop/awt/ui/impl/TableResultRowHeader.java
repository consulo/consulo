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

import consulo.application.ui.wm.IdeFocusManager;
import consulo.codeEditor.EditorColors;
import consulo.colorScheme.EditorColorsScheme;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.CellRendererPanel;
import consulo.ui.ex.awt.CustomLineBorder;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.grid.GridHitArea;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import static javax.swing.SwingUtilities.isLeftMouseButton;

/**
 * The row numbers of a {@link DesktopDataGridImpl}, the row header of its table.
 * <p/>
 * A press of the left button on a number selects its whole row, a drag over the numbers the rows between, Shift extends the selection
 * and Ctrl adds to it (or takes the row out of it). The few pixels around the border of two rows resize the row above it, and a
 * double click there gives it back its height. A popup trigger on a number opens the context menu of the grid for its row
 * ({@link DesktopDataGridImpl#showContextMenu}), which selects the row unless it is selected already.
 * <p/>
 * The colours are those of the colour layers of the grid: the background of the pending changes of a row
 * ({@link consulo.ui.internal.DataGridController#getRowHeaderBackground(int)}), and the selection, which highlights the number of a
 * selected row like the current row of an editor.
 * <p/>
 * The numbers are always on the left of the rows, at the height of the first text line of a row. The grid has no transposed mode,
 * hover highlighting, striped rows or markup effects, so its row header has none of them either.
 *
 * @author gregsh
 */
class TableResultRowHeader extends GridRowHeader {
    /**
     * The height, in pixels, of the band on each side of the border of two rows which resizes the row above it.
     */
    private static final int RESIZE_AREA = 4;

    private final DesktopDataGridImpl myResultPanel;
    private final DesktopDataGridImpl.MyTable myTable;
    private final RowHeaderCellRenderer myRenderer;

    TableResultRowHeader(DesktopDataGridImpl resultPanel, DesktopDataGridImpl.MyTable view) {
        super(view, view::getShowHorizontalLines, view::getShowLastHorizontalLine);
        myResultPanel = resultPanel;
        myTable = view;
        myRenderer = new MyRowHeaderCellRenderer(createRegularRenderer());

        GutterMouseListener mouseListener = new GutterMouseListener();
        addMouseMotionListener(mouseListener);
        addMouseListener(mouseListener);
    }

    protected RowHeaderCellComponentBase createRegularRenderer() {
        return new RowNumberRowHeaderCellComponent();
    }

    @Override
    public RowHeaderCellRenderer getCellRenderer() {
        return myRenderer;
    }

    @Override
    public JTable getTable() {
        return myTable;
    }

    public int rowForPoint(@Nullable Point point) {
        return point != null ? myTable.rowAtPoint(point) : -1;
    }

    /**
     * The width of the widest number. The row of the widest number is looked for: the grid numbers a row by its row in the data
     * source, so once the rows are sorted the last row does not need to have the highest number.
     */
    @Override
    protected int calcPreferredWidthWithoutInsets() {
        if (myTable.isEmpty()) {
            return super.calcPreferredWidthWithoutInsets();
        }

        int rowCount = myTable.getModel().getRowCount();
        int widestRow = rowCount - 1;
        int widestLength = -1;
        for (int row = 0; row < rowCount; row++) {
            int length = getRowName(row).length();
            if (length > widestLength) {
                widestLength = length;
                widestRow = row;
            }
        }
        return ((RowNumberRowHeaderCellComponent) getCellRenderer().getRendererComponent(widestRow, false)).getPreferredWidth();
    }

    private String getRowName(int row) {
        return myResultPanel.getController().getRowNumberText(row);
    }

    // region colours

    /**
     * The colour layers of the grid in their order: the background of the pending changes of the row, then the selection - the
     * number of a selected row is highlighted as the current row, the other numbers get the header background, unless the row header
     * is transparent.
     *
     * @return {@code null} when the row header shows the background of the table
     */
    private @Nullable Color getRowHeaderBackground(int row) {
        ColorValue mutationBackground = myResultPanel.getController().getRowHeaderBackground(row);
        Color color = mutationBackground == null ? null : TargetAWT.to(mutationBackground);
        if (myTable.isRowSelected(row)) {
            return color == null ? getCurrentRowColor() : softHighlightOf(color);
        }
        if (color != null) {
            return color;
        }
        return isTransparentRowHeaderBackground() ? null : softHighlightOf(myTable.getBackground());
    }

    /**
     * The foreground of the table, or - when the row header is transparent - the colours of the line numbers of an editor.
     */
    private Color getRowHeaderForeground(int row) {
        if (!isTransparentRowHeaderBackground()) {
            return myTable.getForeground();
        }
        EditorColorsScheme scheme = myResultPanel.getColorsScheme();
        ColorValue lineNumbersColor = scheme.getColor(EditorColors.LINE_NUMBERS_COLOR);
        ColorValue color = myTable.isRowSelected(row) ? scheme.getColor(EditorColors.LINE_NUMBER_ON_CARET_ROW_COLOR) : lineNumbersColor;
        if (color == null) {
            color = lineNumbersColor;
        }
        return color == null ? myTable.getForeground() : TargetAWT.to(color);
    }

    private @Nullable Color getCurrentRowColor() {
        ColorValue color = myResultPanel.getColorsScheme().getColor(EditorColors.CARET_ROW_COLOR);
        return color == null ? null : TargetAWT.to(color);
    }

    private boolean isTransparentRowHeaderBackground() {
        return myResultPanel.getController().getAppearance().isTransparentRowHeaderBackground();
    }

    /**
     * A colour a little darker than the given one - a little lighter under a dark theme - with its alpha.
     */
    private static Color softHighlightOf(Color c1) {
        int i = 0x10;
        int alpha = c1.getAlpha();
        return new JBColor(new Color(Math.max(0, c1.getRed() - i), Math.max(0, c1.getGreen() - i), Math.max(0, c1.getBlue() - i), alpha),
            new Color(Math.min(255, c1.getRed() + i), Math.min(255, c1.getGreen() + i), Math.min(255, c1.getBlue() + i), alpha));
    }

    // endregion

    // region selection

    private static boolean isIntervalModifierSet(MouseEvent e) {
        return 0 != (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK);
    }

    private static boolean isExclusiveModifierSet(MouseEvent e) {
        return 0 != (e.getModifiersEx() & (InputEvent.CTRL_DOWN_MASK | InputEvent.META_DOWN_MASK));
    }

    private void setColumnSelectionInterval(int idx0, int idx1) {
        setSelectionInterval(myTable.getColumnModel().getSelectionModel(), false, myTable.getColumnCount() - 1, idx0, idx1);
    }

    private void addColumnSelectionInterval(int idx0, int idx1) {
        setSelectionInterval(myTable.getColumnModel().getSelectionModel(), true, myTable.getColumnCount() - 1, idx0, idx1);
    }

    private void setRowSelectionInterval(int idx0, int idx1) {
        setSelectionInterval(myTable.getSelectionModel(), false, myTable.getRowCount() - 1, idx0, idx1);
    }

    private void addRowSelectionInterval(int idx0, int idx1) {
        setSelectionInterval(myTable.getSelectionModel(), true, myTable.getRowCount() - 1, idx0, idx1);
    }

    private void addRowSelection(int row) {
        setSelection(myTable.getSelectionModel(), true, myTable.getRowCount() - 1, row);
    }

    private static void setSelection(ListSelectionModel selectionModel, boolean add, int maxSelectionIdx, int... selection) {
        if (maxSelectionIdx < 0) {
            return;
        }
        if (!add) {
            selectionModel.clearSelection();
        }
        for (int index : selection) {
            if (index == -1) {
                continue;
            }
            setSelectionInterval(selectionModel, true, maxSelectionIdx, index, index);
        }
    }

    private static void setSelectionInterval(ListSelectionModel selectionModel, boolean add, int maxSelectionIdx, int idx0, int idx1) {
        if (maxSelectionIdx < 0) {
            return;
        }
        idx0 = index(idx0, maxSelectionIdx);
        idx1 = index(idx1, maxSelectionIdx);
        if (add) {
            selectionModel.addSelectionInterval(idx0, idx1);
        }
        else {
            selectionModel.setSelectionInterval(idx0, idx1);
        }
    }

    private static int index(int idx, int maxIdx) {
        return Math.max(0, Math.min(idx, maxIdx));
    }

    // endregion

    private class GutterMouseListener extends MouseInputAdapter {
        private boolean mySelectWhileDraggingInExclusiveMode;
        private int myResizingRow = -1;
        /**
         * The press selected - the open editor took its value - so the drag which follows selects too.
         */
        private boolean mySelecting;
        /**
         * A drag reported the selection as adjusting, so its release reports it as complete.
         */
        private boolean mySelectionAdjusting;

        @Override
        @RequiredUIAccess
        public void mouseClicked(MouseEvent e) {
            if (isLeftMouseButton(e) && e.getClickCount() % 2 == 0) {
                packRow(e);
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (shouldResizeRow(e)) {
                setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
            }
            else {
                setCursor(Cursor.getDefaultCursor());
            }
        }

        private boolean shouldResizeRow(MouseEvent e) {
            return getRowToResize(e) != -1;
        }

        @Override
        @RequiredUIAccess
        public void mousePressed(MouseEvent e) {
            mySelecting = false;
            mySelectionAdjusting = false;
            if (e.isPopupTrigger()) {
                showContextMenu(e);
                return;
            }
            // the other buttons select nothing: the context menu selects the row it is opened on
            if (!isLeftMouseButton(e)) {
                return;
            }
            int rowToResize = getRowToResize(e);
            if (rowToResize != -1) {
                myResizingRow = rowToResize;
                return;
            }
            // like a press on a cell: the editor commits first, and a refused commit keeps it open
            if (!myResultPanel.stopTableEditing()) {
                e.consume();
                return;
            }
            mySelecting = true;
            processSelectionEvent(e, false);
        }

        @Override
        @RequiredUIAccess
        public void mouseReleased(MouseEvent e) {
            myResizingRow = -1;
            mySelecting = false;
            if (mySelectionAdjusting) {
                mySelectionAdjusting = false;
                myResultPanel.reportNativeSelection(false);
            }
            if (e.isPopupTrigger()) {
                showContextMenu(e);
            }
        }

        /**
         * The context menu of the grid for the row under the mouse, or for the grid below the last row.
         */
        @RequiredUIAccess
        private void showContextMenu(MouseEvent e) {
            int row = rowForPoint(e.getPoint());
            myResultPanel.showContextMenu(row >= 0 ? GridHitArea.ROW_HEADER : GridHitArea.EMPTY, row, -1, e);
            e.consume();
        }

        @Override
        @RequiredUIAccess
        public void mouseDragged(MouseEvent e) {
            if (myResizingRow != -1) {
                Rectangle cellRect = myTable.getCellRect(myResizingRow, 0, true);
                int oldRowHeight = myTable.getRowHeight(myResizingRow);
                int newRowHeight = Math.max(myTable.getRowHeight(), e.getY() - cellRect.y);
                if (oldRowHeight != newRowHeight) {
                    myTable.setRowHeight(myResizingRow, newRowHeight);
                }
            }
            else if (mySelecting) {
                mySelectionAdjusting = true;
                processSelectionEvent(e, true);
            }
        }

        private int getRowToResize(MouseEvent e) {
            int rowAtPoint = rowForPoint(e.getPoint());
            if (rowAtPoint == -1) {
                return -1;
            }

            Rectangle cellRect = myTable.getCellRect(rowAtPoint, 0, true);
            int yInCellRect = e.getY() - cellRect.y;
            if (yInCellRect < RESIZE_AREA) {
                return rowAtPoint == 0 ? -1 : rowAtPoint - 1;
            }
            return yInCellRect > cellRect.height - RESIZE_AREA ? rowAtPoint : -1;
        }

        /**
         * The rows and the columns of the table change one after the other - the selection is reported once, when both are set, and
         * as adjusting while the mouse drags.
         */
        @RequiredUIAccess
        private void processSelectionEvent(MouseEvent e, boolean isDragEvent) {
            int currentRow = rowForPoint(e.getPoint());
            if (currentRow == -1) {
                e.consume();
                return;
            }
            if (!myTable.hasFocus()) {
                IdeFocusManager.getGlobalInstance().requestFocus(myTable, true);
            }
            myResultPanel.changeSelection(() -> processSelectionEventInternal(e, isDragEvent, currentRow), isDragEvent);
            e.consume();
        }

        private void processSelectionEventInternal(MouseEvent e, boolean isDragEvent, int currentRow) {
            boolean interval = isIntervalModifierSet(e);
            boolean exclusive = isExclusiveModifierSet(e);
            if (interval) {
                int lead = myTable.getSelectionModel().getLeadSelectionIndex();
                if (exclusive) {
                    addRowSelectionInterval(currentRow, lead);
                    addColumnSelectionInterval(myTable.getColumnCount() - 1, 0);
                }
                else {
                    setRowSelectionInterval(currentRow, lead);
                    setColumnSelectionInterval(myTable.getColumnCount() - 1, 0);
                }
            }
            else if (exclusive) {
                if (!isDragEvent) {
                    mySelectWhileDraggingInExclusiveMode = !myTable.isRowSelected(currentRow) ||
                        myTable.getSelectedColumnCount() != myTable.getColumnCount();
                }
                if (mySelectWhileDraggingInExclusiveMode) {
                    addRowSelection(currentRow);
                    addColumnSelectionInterval(myTable.getColumnCount() - 1, 0);
                }
                else {
                    myTable.removeRowSelectionInterval(currentRow, currentRow);
                }
            }
            else {
                int lead = isDragEvent ? myTable.getSelectionModel().getLeadSelectionIndex() : currentRow;
                setRowSelectionInterval(currentRow, lead);
                setColumnSelectionInterval(myTable.getColumnCount() - 1, 0);
            }
        }

        @RequiredUIAccess
        private void packRow(MouseEvent e) {
            int rowToResize = getRowToResize(e);
            if (e.isConsumed() || rowToResize == -1) {
                return;
            }

            int expandedHeight = getExpandedRowHeight(rowToResize);
            int newHeight = myTable.getRowHeight(rowToResize) >= expandedHeight ? myTable.getRowHeight() : expandedHeight;

            myTable.setRowHeight(rowToResize, newHeight);
        }

        @RequiredUIAccess
        private int getExpandedRowHeight(int row) {
            int expandedHeight = myTable.getRowHeight();
            for (int column = 0; column < myTable.getColumnCount(); column++) {
                TableCellRenderer renderer = myTable.getCellRenderer(row, column);
                if (renderer != null) {
                    Component c = myTable.prepareRenderer(renderer, row, column);
                    expandedHeight = Math.max(expandedHeight, c.getPreferredSize().height);
                }
            }
            return expandedHeight;
        }
    }

    protected abstract class RowHeaderCellComponentBase extends CellRendererPanel {
        private int myRow;

        protected RowHeaderCellComponentBase() {
        }

        public void setRow(int row, boolean forDisplay) {
            myRow = row;
        }

        public int getRow() {
            return myRow;
        }

        @Override
        public boolean isShowing() {
            return true;
        }

        @Override
        protected void paintComponent(Graphics g) {
            paintBackground(g);
        }

        @Override
        public Color getBackground() {
            Color background = getRowHeaderBackground(myRow);
            return background != null ? background : myTable.getBackground();
        }

        @Override
        public Color getForeground() {
            return getRowHeaderForeground(myRow);
        }

        private void paintBackground(Graphics g) {
            Color backup = g.getColor();
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(backup);
        }
    }

    protected class RowNumberRowHeaderCellComponent extends RowHeaderCellComponentBase {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            paintLineNumber(g);
        }

        @Override
        public Dimension getPreferredSize() {
            int preferredWidth = getPreferredWidth();
            return new Dimension(preferredWidth, myTable.getRowHeight(getRow()));
        }

        private int getPreferredWidth() {
            Insets insets = getInsets();
            return preferredWidthOf(getRowNumberString()) + (insets.right + insets.left);
        }

        private void paintLineNumber(Graphics g) {
            Font fontBackup = g.getFont();
            g.setFont(myTable.getFont());
            Insets insets = getInsets();
            int middleX = insets.left + (getWidth() - insets.left - insets.right) / 2;
            String rowNum = getRowNumberString();
            int stringWidth = g.getFontMetrics().stringWidth(rowNum);
            int baseline = getBaseline();
            g.drawString(rowNum, middleX - stringWidth / 2, baseline);
            g.setFont(fontBackup);
        }

        private String getRowNumberString() {
            return getRowName(getRow());
        }

        private int getBaseline() {
            FontMetrics metrics = myTable.getFontMetrics(myTable.getFont());
            return (myTable.getTextLineHeight() + metrics.getAscent() - metrics.getDescent() + JBUI.scale(4)) / 2;
        }

        private int preferredWidthOf(String text) {
            return myTable.getFontMetrics(myTable.getFont()).stringWidth(text);
        }
    }

    private class MyRowHeaderCellRenderer implements RowHeaderCellRenderer {
        final RowHeaderCellComponentBase myRegular;

        MyRowHeaderCellRenderer(RowHeaderCellComponentBase regular) {
            myRegular = regular;
        }

        @Override
        public Component getRendererComponent(int row, boolean forDisplay) {
            RowHeaderCellComponentBase c = myRegular;
            // the line between the numbers and the cells, on the right: the numbers are on the left of the rows
            c.setBorder(BorderFactory.createCompoundBorder(
                new CustomLineBorder(myTable.getGridColor(), 0, 0, 0, 1),
                JBUI.Borders.empty(0, 8)));

            c.setRow(row, forDisplay);
            return c;
        }
    }
}
