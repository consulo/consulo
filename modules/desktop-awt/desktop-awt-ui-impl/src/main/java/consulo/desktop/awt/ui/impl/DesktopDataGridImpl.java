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

import consulo.application.Application;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.EditorFontType;
import consulo.colorScheme.event.EditorColorsListener;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Point2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.ContextMenuEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.KeyboardInputDetails;
import consulo.ui.event.details.ModifiedInputDetails;
import consulo.ui.ex.ColoredTextContainer;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.ColoredTableCellRenderer;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.SideBorder;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.awt.TableCellState;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.table.JBTable;
import consulo.ui.ex.awt.util.TableUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.DataGridAppearance;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridDataHookUp;
import consulo.ui.grid.GridHitArea;
import consulo.ui.grid.GridMutator;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.ModelIndex;
import consulo.ui.grid.RowSortOrder;
import consulo.ui.grid.editor.GridEditInitiator;
import consulo.ui.internal.DataGridAppearanceImpl;
import consulo.ui.internal.DataGridController;
import consulo.ui.internal.DataGridControllerOwner;
import consulo.ui.style.ComponentColors;
import consulo.util.lang.StringUtil;

import com.formdev.flatlaf.FlatClientProperties;
import org.jspecify.annotations.Nullable;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.EventObject;
import java.util.List;
import java.util.function.BiConsumer;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.CellRendererPane;
import javax.swing.DefaultListSelectionModel;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * A {@link DataGrid} shown in a {@link JBTable} over the {@link DataGridController}, under the paging bar of the controller.
 * <p/>
 * The table has no row sorter - the controller sorts - so a table row is a view row of the controller, and a model column of
 * the table is a view column of the controller. Only the view columns of the table differ, as the user may drag them around,
 * which is why every column index read from the table goes through {@link #toGridColumn(int)}.
 * <p/>
 * A grid which edits its cells ({@link DataGridController#isCellEditingAllowed()}) edits them in a {@link GridTableCellEditor}: a
 * double click, a typed key, Enter or F2 open it, and the controller decides which cell is edited and what the edit writes.
 * <p/>
 * The table, its header, its row numbers ({@link TableResultRowHeader}) and its cell editors use the plain font of the global editor
 * colours scheme, with its line spacing, and follow the scheme when it changes. A row is as high as the text lines the controller
 * gives it ({@link DataGridController#getRowLines()}); with more than one line its cells are painted by a {@link MultiLineCellRenderer}.
 * <p/>
 * A column gets the width the controller keeps for it ({@link DataGridController#getViewColumnWidth}), in characters of the grid
 * font; a column without one is sized from its header and its first rows. Both a width the user drags and a width sized from the
 * rows are reported back, so a column keeps its width when the columns are built again.
 * <p/>
 * A click on a column header selects the column, a click on its sort marker sorts by it, and dropping a dragged column moves it in
 * the data source, when the controller allows it ({@link DataGridController#canMoveColumns()}).
 * <p/>
 * The grid fires its {@link ContextMenuEvent} itself: a popup trigger on a cell, a column header, a row number or the empty part of
 * the grid, the context menu key or Shift+F10 tell the controller first what the menu is opened on
 * ({@link DataGridController#onContextMenuRequested}), and then the listeners of the grid get the event, placed relative to the
 * grid.
 *
 * @since 2026-10-03
 */
class DesktopDataGridImpl extends SwingComponentDelegate<DesktopDataGridImpl.MyPanel> implements DataGridControllerOwner {
    class MyPanel extends JPanel implements FromSwingComponentWrapper {
        MyPanel() {
            super(new BorderLayout());
        }

        @Override
        public Component toUIComponent() {
            return DesktopDataGridImpl.this;
        }
    }

    class MyTable extends JBTable {
        private class MyTableHeader extends JBTableHeader {
            @Override
            public @Nullable String getToolTipText(MouseEvent event) {
                int column = toGridColumn(columnAtPoint(event.getPoint()));
                if (column < 0) {
                    return super.getToolTipText(event);
                }
                return StringUtil.nullize(myController.getColumnTooltip(column));
            }

            /**
             * A popup trigger asks for the context menu of the column under the mouse; only the left button drags or resizes a
             * column.
             */
            @Override
            @RequiredUIAccess
            protected void processMouseEvent(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int column = toGridColumn(columnAtPoint(e.getPoint()));
                    showContextMenu(column >= 0 ? GridHitArea.COLUMN_HEADER : GridHitArea.EMPTY, -1, column, e);
                    e.consume();
                    return;
                }
                if (e.getID() == MouseEvent.MOUSE_PRESSED && !SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                super.processMouseEvent(e);
            }

            /**
             * Asked when a press may start dragging a column. A data source which moves its columns allows it while the controller
             * does ({@link DataGridController#canMoveColumns()}), and only while the columns are in the order of the controller - a
             * move on its way to the data source keeps the dragged order until the columns are built again. A data source which
             * cannot move columns leaves the move to the view.
             */
            @Override
            public boolean getReorderingAllowed() {
                if (!super.getReorderingAllowed()) {
                    return false;
                }
                if (!(myController.getHookUp().getMutator() instanceof GridMutator.ColumnsMutator)) {
                    return true;
                }
                return myController.canMoveColumns() && isInModelOrder(getColumnModel());
            }
        }

        /**
         * What starts the edit the controller asks for ({@link GridView#editCellAt}), kept while the edit opens. {@code null} for
         * an edit the table starts itself.
         */
        private @Nullable GridEditInitiator myEditInitiator;
        /**
         * The key event the table processes, and whether its key press tried to start an edit already.
         */
        private @Nullable KeyEvent myProcessedKeyEvent;
        private boolean myKeyEditAttempted;
        /**
         * The line spacing of the editor colours scheme, which the height of the rows follows ({@link DesktopDataGridImpl#updateFonts()}).
         */
        private float myLineSpacing = 1f;
        /**
         * The rows show more than one text line ({@link DesktopDataGridImpl#updateRowHeight()}).
         */
        private boolean myMultiLineRows;

        MyTable(GridTableModel model) {
            super(model);
        }

        @Override
        protected JTableHeader createDefaultTableHeader() {
            return new MyTableHeader();
        }

        /**
         * The height of a line of text in the font of the table, with the line spacing of the editor.
         */
        int getTextLineHeight() {
            return (int) Math.ceil(getFontMetrics(getFont()).getHeight() * myLineSpacing);
        }

        /**
         * The height of every row - the text lines of a row ({@link DataGridController#getRowLines()}) and the padding - which the
         * user may change for a single row by dragging the border below its number.
         */
        private int computeRowHeight() {
            int lines = Math.max(1, myController.getRowLines());
            return lines * getTextLineHeight() + getRowMargin() + (isStriped() || !getShowHorizontalLines() ? 1 : 0) + JBUI.scale(4);
        }

        /**
         * Rows which show more than one text line paint their cells with the {@link MultiLineCellRenderer}.
         */
        @Override
        public TableCellRenderer getCellRenderer(int row, int column) {
            if (myMultiLineRows) {
                return myMultiLineCellRenderer;
            }
            return super.getCellRenderer(row, column);
        }

        /**
         * A popup trigger asks for the context menu of the cell under the mouse. A press of the right button selects nothing here
         * - the controller selects what the menu is opened on - so it is not passed to the table, which would select the row.
         */
        @Override
        @RequiredUIAccess
        protected void processMouseEvent(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showTableContextMenu(e);
                e.consume();
                return;
            }
            if (e.getID() == MouseEvent.MOUSE_PRESSED && SwingUtilities.isRightMouseButton(e)) {
                e.consume();
                return;
            }
            super.processMouseEvent(e);
        }

        /**
         * A line is painted in the space between the cells, so showing lines makes that space one pixel wide.
         */
        @Override
        public void setShowHorizontalLines(boolean show) {
            if (show) {
                setIntercellSpacing(new Dimension(getIntercellSpacing().width, 1));
            }
            super.setShowHorizontalLines(show);
        }

        @Override
        public void setShowVerticalLines(boolean show) {
            if (show) {
                setIntercellSpacing(new Dimension(1, getIntercellSpacing().height));
            }
            super.setShowVerticalLines(show);
        }

        /**
         * Striped rows replace the horizontal lines; the columns keep their lines.
         */
        @Override
        public void setStriped(boolean striped) {
            super.setStriped(striped);
            if (striped) {
                setShowHorizontalLines(false);
                setShowVerticalLines(true);
            }
        }

        /**
         * The line below the last row is painted also when the table paints no horizontal lines - by the row numbers too.
         */
        boolean getShowLastHorizontalLine() {
            return true;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (!getShowHorizontalLines() && getShowLastHorizontalLine() && getRowCount() > 0 && getColumnCount() > 0) {
                Color color = g.getColor();
                Rectangle leftCell = getCellRect(getRowCount() - 1, 0, true);
                Rectangle rightCell = getCellRect(getRowCount() - 1, getColumnCount() - 1, true);
                g.setColor(getGridColor());
                int x1 = leftCell.x;
                int x2 = rightCell.x + rightCell.width;
                g.fillRect(Math.min(x1, x2), leftCell.y + leftCell.height, Math.abs(x2 - x1) + 1, 1);
                g.setColor(color);
            }
        }

        /**
         * The row numbers follow the new height of the row.
         */
        @Override
        public void setRowHeight(int row, int rowHeight) {
            if (row < 0 || row >= getRowCount()) {
                return;
            }

            super.setRowHeight(row, rowHeight);

            Container parent = getParent();
            if (parent instanceof JViewport && parent.getParent() instanceof JScrollPane scrollPane) {
                JViewport rowHeader = scrollPane.getRowHeader();
                if (rowHeader != null) {
                    rowHeader.revalidate();
                    rowHeader.repaint();
                }
            }
        }

        /**
         * A cell has an editor when the controller has an editor factory for it, and its column is neither a row id nor virtual.
         */
        @Override
        public @Nullable TableCellEditor getCellEditor(int row, int column) {
            int gridColumn = toGridColumn(column);
            if (gridColumn < 0 || row < 0 || row >= myController.getViewRowCount() || !myController.isCellEditable(row, gridColumn)) {
                return null;
            }
            GridEditInitiator initiator = myEditInitiator;
            return new GridTableCellEditor(myController, row, gridColumn, initiator == null ? GridEditInitiator.ACTION : initiator);
        }

        @Override
        @RequiredUIAccess
        public boolean editCellAt(int row, int column, @Nullable EventObject e) {
            EventObject starter = e;
            KeyEvent keyEvent = myProcessedKeyEvent;
            if (starter == null && myEditInitiator == null && keyEvent != null && keyEvent.getID() == KeyEvent.KEY_TYPED) {
                // BasicTableUI starts editing on a typed key the table has no binding for. When the press of that key tried already,
                // the typed key opens nothing - a boolean took its value from the key press at once, and must not be edited again
                if (myKeyEditAttempted) {
                    return false;
                }
                starter = keyEvent;
            }
            if (starter instanceof KeyEvent keyStarter && isTypedKey(keyStarter)) {
                // only a key JBTable lets through asks the editor - a key press it refuses (on Windows an AltGr character comes
                // with Ctrl+Alt down) leaves the start of the edit to its typed key, as in a plain JBTable
                myKeyEditAttempted = true;
            }
            return super.editCellAt(row, column, starter);
        }

        /**
         * The check {@code JBTable.editCellAt} makes before it starts editing on a key.
         */
        private static boolean isTypedKey(KeyEvent e) {
            return UIUtil.isReallyTypedEvent(e) && e.getKeyChar() != KeyEvent.CHAR_UNDEFINED;
        }

        /**
         * The same as {@link JTable#columnMarginChanged}, except that it does not stop editing - resizing a column keeps the editor,
         * with what is typed in it. The width of a column the user resizes is reported to the controller.
         */
        @Override
        @RequiredUIAccess
        public void columnMarginChanged(ChangeEvent e) {
            JTableHeader tableHeader = getTableHeader();
            TableColumn resizingColumn = tableHeader != null ? tableHeader.getResizingColumn() : null;
            if (resizingColumn != null && autoResizeMode == AUTO_RESIZE_OFF) {
                resizingColumn.setPreferredWidth(resizingColumn.getWidth());
                reportColumnWidth(resizingColumn);
            }
            resizeAndRepaint();
        }

        @Override
        protected void processKeyEvent(KeyEvent e) {
            if (e.getID() != KeyEvent.KEY_TYPED) {
                myKeyEditAttempted = false;
            }
            KeyEvent previous = myProcessedKeyEvent;
            myProcessedKeyEvent = e;
            try {
                super.processKeyEvent(e);
            }
            finally {
                myProcessedKeyEvent = previous;
            }
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            Dimension size = super.getPreferredScrollableViewportSize();
            int visibleRows = myController.getAppearance().getVisibleRowCount();
            return visibleRows > 0 ? new Dimension(size.width, visibleRows * getRowHeight()) : size;
        }
    }

    /**
     * A column without a width of the controller is sized by its header and the text of its first rows, within
     * {@link DataGridController#FIT_MIN_CHARS} and {@link DataGridController#FIT_MAX_CHARS}.
     */
    private static final int FIT_ROW_COUNT = 100;
    /**
     * The room of a cell beside its text: a width in characters of the controller is this plus the characters.
     */
    private static final int CELL_PADDING = 16;
    /**
     * The characters whose average width is the width of a character of the grid font.
     */
    private static final String AVERAGE_CHAR_SAMPLE = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    /**
     * Room a sized column keeps for the priority of its sort marker, shown when more than one column is sorted.
     */
    private static final String SORT_PRIORITY_SAMPLE = "9";
    private static final String CONTEXT_MENU_ACTION_KEY = "consulo.dataGrid.contextMenu";

    private final DataGridController myController;
    private final HeaderRenderer myHeaderRenderer = new HeaderRenderer();
    private final MultiLineCellRenderer myMultiLineCellRenderer = new MultiLineCellRenderer();

    private final GridTableModel myTableModel;
    private final MyTable myTable;
    /**
     * Created the first time the row numbers are shown.
     */
    private @Nullable TableResultRowHeader myRowHeader;
    private final JPanel myRowHeaderCorner;
    private final JBScrollPane myScrollPane;
    /**
     * Holds the paging bar of the controller, and is hidden with it.
     */
    private final JPanel myToolbarPanel;

    /**
     * Set while the grid changes the selection of the table itself, so the change is not reported back as the user's - or while
     * a gesture changes both selection models, so it is reported once, when it is complete.
     */
    private boolean myApplyingSelection;
    /**
     * Columns were sized before any row arrived, so the first rows size them again.
     */
    private boolean myColumnsNeedFit;
    private boolean myRowHeaderInstalled;
    /**
     * The height the rows were given last ({@link #updateRowHeight()}), so the same height does not reset the heights the user
     * gave single rows.
     */
    private int myRowHeight = -1;
    /**
     * The view column of the controller whose sort marker is under the mouse, or -1.
     */
    private int myHoveredSortColumn = -1;

    @RequiredUIAccess
    DesktopDataGridImpl(GridDataHookUp<GridRow, GridColumn> hookUp, BiConsumer<DataGrid, DataGridAppearance> configurator) {
        myController = new DataGridController(this, hookUp);
        // the configurator fills the appearance in before anything of the view reads it
        configurator.accept(this, myController.getAppearance());

        myTableModel = new GridTableModel();
        myTable = new MyTable(myTableModel);
        myTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        myTable.setCellSelectionEnabled(true);
        myTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        DataGridAppearanceImpl appearance = myController.getAppearance();
        myTable.setStriped(appearance.isStriped());
        if (!appearance.isStriped()) {
            myTable.setShowVerticalLines(true);
            myTable.setShowHorizontalLines(appearance.isShowHorizontalLines());
        }
        myTable.setDefaultRenderer(Object.class, new CellRenderer());
        // an editor opened by a typed key gets the focus (JBTable.editCellAt), so Enter reaches the editor and not the table
        myTable.setSurrendersFocusOnKeystroke(true);
        installEditActions();
        installContextMenuActions();

        myRowHeaderCorner = new RowHeaderCorner();

        myScrollPane = new JBScrollPane(myTable) {
            /**
             * Where the table has no rows, every viewport - also the one of the row numbers - shows the background of the table.
             */
            @Override
            protected JViewport createViewport() {
                return new JViewport() {
                    @Override
                    public Color getBackground() {
                        return myTable.getBackground();
                    }
                };
            }
        };
        // the part of the grid no cell, header or row number covers: a popup trigger there opens the menu of the grid itself
        myScrollPane.addMouseListener(new MouseAdapter() {
            @Override
            @RequiredUIAccess
            public void mousePressed(MouseEvent e) {
                showEmptyAreaContextMenu(e);
            }

            @Override
            @RequiredUIAccess
            public void mouseReleased(MouseEvent e) {
                showEmptyAreaContextMenu(e);
            }
        });

        myTable.getSelectionModel().addListSelectionListener(this::onNativeSelectionEvent);
        myTable.getColumnModel().getSelectionModel().addListSelectionListener(this::onNativeSelectionEvent);
        installHeaderListener();

        myToolbarPanel = new JPanel(new BorderLayout());
        // only above and below: the paging bar keeps its first control off the edge of the grid itself
        myToolbarPanel.setBorder(JBUI.Borders.empty(2, 0));
        myToolbarPanel.add(TargetAWT.to(myController.getToolbar()), BorderLayout.WEST);

        // built here and not in createComponent(): the configurator may already have created the panel, and the
        // appearance it fills in is only complete once it returned
        MyPanel panel = toAWTComponent();
        panel.add(myToolbarPanel, BorderLayout.NORTH);
        panel.add(myScrollPane, BorderLayout.CENTER);
        // the paging bar may have hidden itself before the view listens
        updateToolbarVisibility();

        installColumns();
        // sizes the columns and the rows too
        updateFonts();
        updateRowHeader();
        Application.get().getMessageBus().connect(myController).subscribe(EditorColorsListener.class, scheme -> updateFonts());

        myController.setView(new GridView());
        myController.start();
    }

    @Override
    protected MyPanel createComponent() {
        // only the empty panel - the constructor fills it, and nothing here may reach the listener dispatcher or the
        // data object, which need the component this method creates
        return new MyPanel();
    }

    @Override
    public DataGridController getController() {
        return myController;
    }

    /**
     * The table takes the focus, not the panel around it.
     */
    @Override
    @RequiredUIAccess
    public void focus() {
        IdeFocusManager.getGlobalInstance().requestFocus(myTable, true);
    }

    /**
     * A context menu listener is only kept: the grid fires the {@link ContextMenuEvent} itself, once it knows what the gesture was on
     * ({@link #showContextMenu}). The dispatch of the base class, which fires on every popup trigger of the root panel without
     * asking the controller, is not installed.
     */
    @Override
    public <C extends Component, E extends ComponentEvent<C>> Disposable addListener(Class<? extends E> eventClass,
                                                                                     ComponentEventListener<C, E> listener) {
        if (eventClass == ContextMenuEvent.class) {
            return dataObject().addListener(eventClass, listener);
        }
        return super.addListener(eventClass, listener);
    }

    @Override
    public void dispose() {
        // the controller drops its view and its listeners on the data source; disposing it twice is harmless
        Disposer.dispose(myController);
    }

    // region columns

    /**
     * @return the view column of the controller shown at the given view column of the table, or -1
     */
    private int toGridColumn(int tableColumn) {
        if (tableColumn < 0 || tableColumn >= myTable.getColumnCount()) {
            return -1;
        }
        int column = myTable.convertColumnIndexToModel(tableColumn);
        return column >= 0 && column < myController.getViewColumnCount() ? column : -1;
    }

    /**
     * @return the view column of the table showing the given view column of the controller, or -1
     */
    private int toTableColumn(int gridColumn) {
        if (gridColumn < 0 || gridColumn >= myTableModel.getColumnCount()) {
            return -1;
        }
        return myTable.convertColumnIndexToView(gridColumn);
    }

    /**
     * A structure change recreates the columns of the table, so the header renderer is set again each time.
     */
    private void installColumns() {
        TableColumnModel columns = myTable.getColumnModel();
        for (int i = 0; i < columns.getColumnCount(); i++) {
            columns.getColumn(i).setHeaderRenderer(myHeaderRenderer);
        }
    }

    // endregion

    // region column widths

    /**
     * Sets the width of every column: the width the controller keeps for it, in characters of the grid font, or - for a column it
     * keeps none for - the width of its header and its first rows. A width sized from rows is reported to the controller, so the
     * column keeps it when the columns are built again; a width sized from the header alone is sized again by the first rows.
     *
     * @param fitUnsized whether a column without a width of the controller is sized; otherwise it keeps the width it has
     */
    @RequiredUIAccess
    private void applyColumnWidths(boolean fitUnsized) {
        TableColumnModel columns = myTable.getColumnModel();
        FontMetrics metrics = getGridFontMetrics();
        int rowCount = Math.min(myController.getViewRowCount(), FIT_ROW_COUNT);
        boolean needFit = false;

        for (int i = 0; i < columns.getColumnCount(); i++) {
            int gridColumn = toGridColumn(i);
            if (gridColumn < 0) {
                continue;
            }

            TableColumn column = columns.getColumn(i);
            int chars = myController.getViewColumnWidth(gridColumn);
            if (chars > 0) {
                column.setPreferredWidth(toPixels(chars, metrics));
            }
            else if (fitUnsized) {
                int width = computeFitWidth(i, gridColumn, metrics, rowCount);
                column.setPreferredWidth(width);
                if (rowCount > 0) {
                    myController.onColumnResized(gridColumn, toChars(width, metrics));
                }
                else {
                    needFit = true;
                }
            }
        }

        if (fitUnsized) {
            myColumnsNeedFit = needFit;
        }
    }

    /**
     * The width of a column sized by its header - with room for the priority of the sort marker - and the lines of its first rows,
     * within a minimum and a maximum width.
     */
    @RequiredUIAccess
    private int computeFitWidth(int tableColumn, int gridColumn, FontMetrics metrics, int rowCount) {
        int padding = JBUI.scale(CELL_PADDING);
        java.awt.Component header = myHeaderRenderer.getTableCellRendererComponent(myTable, null, false, false, -1, tableColumn);
        int width = header.getPreferredSize().width;
        if (myController.isColumnSortable(gridColumn)) {
            width += metrics.stringWidth(SORT_PRIORITY_SAMPLE);
        }
        for (int row = 0; row < rowCount; row++) {
            for (String line : myController.getCellLines(row, gridColumn)) {
                width = Math.max(width, metrics.stringWidth(line) + padding);
            }
        }
        int minWidth = toPixels(DataGridController.FIT_MIN_CHARS, metrics);
        int maxWidth = toPixels(DataGridController.FIT_MAX_CHARS, metrics);
        return Math.max(minWidth, Math.min(width, maxWidth));
    }

    /**
     * Reports the width of a column the user resized to the controller.
     */
    @RequiredUIAccess
    private void reportColumnWidth(TableColumn column) {
        int gridColumn = column.getModelIndex();
        if (gridColumn >= 0 && gridColumn < myController.getViewColumnCount()) {
            myController.onColumnResized(gridColumn, toChars(column.getWidth(), getGridFontMetrics()));
        }
    }

    private FontMetrics getGridFontMetrics() {
        return myTable.getFontMetrics(myTable.getFont());
    }

    /**
     * The width of a character of the grid font: the average width of the letters and the digits.
     */
    private static float getAverageCharWidth(FontMetrics metrics) {
        return Math.max(1f, metrics.stringWidth(AVERAGE_CHAR_SAMPLE) / (float) AVERAGE_CHAR_SAMPLE.length());
    }

    /**
     * A width in characters of the controller, in pixels: the characters and the padding of the cell.
     */
    private static int toPixels(int chars, FontMetrics metrics) {
        return Math.round(chars * getAverageCharWidth(metrics)) + JBUI.scale(CELL_PADDING);
    }

    /**
     * A width in pixels, in characters of the controller: without the padding of the cell, at least one.
     */
    private static int toChars(int pixels, FontMetrics metrics) {
        return Math.max(1, Math.round((pixels - JBUI.scale(CELL_PADDING)) / getAverageCharWidth(metrics)));
    }

    // endregion

    // region column header

    /**
     * Installs the mouse handling of the column header: clicks select and sort, a dropped column is moved.
     */
    private void installHeaderListener() {
        JTableHeader header = myTable.getTableHeader();
        HeaderMouseListener listener = new HeaderMouseListener(header);
        header.addMouseListener(listener);
        header.addMouseMotionListener(listener);
        myTable.getColumnModel().addColumnModelListener(listener);
    }

    /**
     * Where the header of a table column shows its sort marker, in the coordinates of the header, with a little room around it and
     * the whole height of the header, or {@code null} when the column has no marker. The header renderer is laid out the way a
     * label lays itself out to paint.
     */
    private @Nullable Rectangle getSortMarkerBounds(int tableColumn) {
        JTableHeader header = myTable.getTableHeader();
        if (header == null || tableColumn < 0 || tableColumn >= myTable.getColumnCount()) {
            return null;
        }
        java.awt.Component component = myHeaderRenderer.getTableCellRendererComponent(myTable, null, false, false, -1, tableColumn);
        if (!(component instanceof JLabel label) || !(label.getIcon() instanceof SortMarkerIcon icon)) {
            return null;
        }

        Rectangle cell = header.getHeaderRect(tableColumn);
        Insets insets = label.getInsets();
        Rectangle view = new Rectangle(insets.left, insets.top, cell.width - insets.left - insets.right,
            cell.height - insets.top - insets.bottom);
        Rectangle iconBounds = new Rectangle();
        Rectangle textBounds = new Rectangle();
        SwingUtilities.layoutCompoundLabel(label, label.getFontMetrics(label.getFont()), label.getText(), icon,
            label.getVerticalAlignment(), label.getHorizontalAlignment(), label.getVerticalTextPosition(),
            label.getHorizontalTextPosition(), view, iconBounds, textBounds, label.getIconTextGap());

        int room = JBUI.scale(2);
        Rectangle bounds = new Rectangle(cell.x + iconBounds.x - room, cell.y, iconBounds.width + 2 * room, cell.height);
        return bounds.intersection(cell);
    }

    /**
     * @return the view column of the controller whose sort marker is at the point of the header, or -1
     */
    private int getSortColumnAt(Point point) {
        int tableColumn = myTable.getTableHeader().columnAtPoint(point);
        int gridColumn = toGridColumn(tableColumn);
        if (gridColumn < 0 || !myController.isColumnSortable(gridColumn)) {
            return -1;
        }
        Rectangle bounds = getSortMarkerBounds(tableColumn);
        return bounds != null && bounds.contains(point) ? gridColumn : -1;
    }

    private void setHoveredSortColumn(int gridColumn) {
        if (myHoveredSortColumn != gridColumn) {
            myHoveredSortColumn = gridColumn;
            myTable.getTableHeader().repaint();
        }
    }

    /**
     * Whether the view columns of the table are in the order of their model, which is the order of the controller.
     */
    private static boolean isInModelOrder(TableColumnModel columns) {
        for (int i = 0; i < columns.getColumnCount(); i++) {
            if (columns.getColumn(i).getModelIndex() != i) {
                return false;
            }
        }
        return true;
    }

    /**
     * A dragged column was dropped. A data source which moves its columns gets the move; when the controller does not send it, or
     * the order is not the move of a single column, the columns go back to the order of the controller. A data source which cannot
     * move columns keeps the move in the view.
     */
    @RequiredUIAccess
    private void onColumnDropped() {
        if (!(myController.getHookUp().getMutator() instanceof GridMutator.ColumnsMutator)) {
            return;
        }
        ColumnMove move = findColumnMove(myTable.getColumnModel());
        if (move == null) {
            restoreColumnOrder();
            return;
        }
        if (!myController.onColumnMoved(move.from(), move.to())) {
            restoreColumnOrder();
        }
        // a sent move keeps the dragged order until the controller builds the columns again
    }

    /**
     * The move of a single column, read from the order of the columns: the column out of order with its neighbour is the moved one,
     * and the order holds no other inversion.
     *
     * @return the view columns of the controller of the move, or {@code null} when no column moved, or more than one did
     */
    private static @Nullable ColumnMove findColumnMove(TableColumnModel columns) {
        int count = columns.getColumnCount();
        int from = -1;
        int to = -1;
        for (int i = 0; i < count - 1; i++) {
            int current = columns.getColumn(i).getModelIndex();
            int next = columns.getColumn(i + 1).getModelIndex();
            if (current > next) {
                // a second inversion
                if (from != -1) {
                    return null;
                }
                if (i + 2 < count && current > columns.getColumn(i + 2).getModelIndex()) {
                    from = current;
                    to = next;
                }
                else {
                    from = next;
                    to = current;
                }
            }
        }
        return from == -1 ? null : new ColumnMove(from, to);
    }

    private record ColumnMove(int from, int to) {
    }

    /**
     * Puts the columns of the table back in the order of the controller. The selected columns stay the same - the column model
     * moves the selection along - so nothing is reported, and the selection of the controller is put on the table again.
     */
    private void restoreColumnOrder() {
        TableColumnModel columns = myTable.getColumnModel();
        myApplyingSelection = true;
        try {
            for (int i = 0; i < columns.getColumnCount(); i++) {
                int current = myTable.convertColumnIndexToView(i);
                if (current > i) {
                    columns.moveColumn(current, i);
                }
            }
        }
        finally {
            myApplyingSelection = false;
        }
        applySelection();
    }

    static int toSwingAlignment(HorizontalAlignment alignment) {
        return switch (alignment) {
            case LEFT -> SwingConstants.LEFT;
            case CENTER -> SwingConstants.CENTER;
            case RIGHT -> SwingConstants.RIGHT;
        };
    }

    // endregion

    // region fonts

    /**
     * The table uses the plain font of the editor colours scheme - and so do the cell renderer, the cell editors and the row
     * numbers, which take the font of the table - and its header too. The rows take the height of their lines in that font with the
     * line spacing of the scheme, and the columns get their widths in that font again.
     */
    @RequiredUIAccess
    private void updateFonts() {
        EditorColorsScheme scheme = getColorsScheme();
        Font font = TargetAWT.to(scheme.getFont(EditorFontType.PLAIN));
        myTable.myLineSpacing = scheme.getLineSpacing();
        myTable.setFont(font);
        // the lines between the cells and the separators of the column header take the border colour of the style
        Color border = TargetAWT.to(ComponentColors.BORDER);
        myTable.setGridColor(border);
        updateRowHeight();
        JTableHeader tableHeader = myTable.getTableHeader();
        if (tableHeader != null) {
            tableHeader.setFont(font);
            String borderHex = String.format("#%06x", border.getRGB() & 0xFFFFFF);
            tableHeader.putClientProperty(FlatClientProperties.STYLE,
                "separatorColor: " + borderHex + "; bottomSeparatorColor: " + borderHex);
        }

        applyColumnWidths(true);
        TableResultRowHeader rowHeader = myRowHeader;
        if (rowHeader != null) {
            rowHeader.updatePreferredSize();
        }
    }

    /**
     * Gives the rows the height of the text lines of the controller ({@link DataGridController#getRowLines()}) in the grid font. The
     * same height is not set again, as setting it drops the heights the user gave single rows.
     */
    private void updateRowHeight() {
        myTable.myMultiLineRows = myController.getRowLines() > 1;
        int height = myTable.computeRowHeight();
        if (height != myRowHeight) {
            myRowHeight = height;
            myTable.setRowHeight(height);
        }
    }

    /**
     * The global editor colours scheme, which gives the grid its font, its line spacing and the colours of its row numbers.
     */
    EditorColorsScheme getColorsScheme() {
        return EditorColorsManager.getInstance().getGlobalScheme();
    }

    // endregion

    // region row numbers

    /**
     * Shows or hides the row numbers as the appearance says, and refreshes them.
     */
    private void updateRowHeader() {
        if (!myController.isShowRowNumbers()) {
            if (myRowHeaderInstalled) {
                myRowHeaderInstalled = false;
                myScrollPane.setRowHeader(null);
                myScrollPane.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, null);
            }
            return;
        }

        TableResultRowHeader rowHeader = myRowHeader;
        if (rowHeader == null) {
            rowHeader = new TableResultRowHeader(this, myTable);
            myRowHeader = rowHeader;
        }
        if (!myRowHeaderInstalled) {
            myRowHeaderInstalled = true;
            myScrollPane.setRowHeaderView(rowHeader);
            myScrollPane.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, myRowHeaderCorner);
        }
        rowHeader.updatePreferredSize();
    }

    /**
     * The row numbers show the selection of their rows.
     */
    private void repaintRowHeader() {
        TableResultRowHeader rowHeader = myRowHeader;
        if (myRowHeaderInstalled && rowHeader != null) {
            rowHeader.repaint();
        }
    }

    // endregion

    // region paging bar

    /**
     * Shows the holder of the paging bar while the bar shows anything ({@link DataGridController#isToolbarVisible()}), and the line
     * between the bar and the rows with it.
     */
    private void updateToolbarVisibility() {
        boolean visible = myController.isToolbarVisible();
        myToolbarPanel.setVisible(visible);
        myScrollPane.setBorder(visible ? IdeBorderFactory.createBorder(SideBorder.TOP) : JBUI.Borders.empty());
        MyPanel panel = toAWTComponent();
        panel.revalidate();
        panel.repaint();
    }

    // endregion

    // region context menu

    /**
     * Opens the context menu of the grid for a mouse gesture on one of its parts, at the point of the gesture.
     *
     * @param viewRow    the row of the controller the gesture is on, or -1
     * @param viewColumn the view column of the controller the gesture is on, or -1
     */
    @RequiredUIAccess
    void showContextMenu(GridHitArea area, int viewRow, int viewColumn, MouseEvent event) {
        showContextMenu(area, viewRow, viewColumn, DesktopAWTInputDetails.convert(toAWTComponent(), event));
    }

    /**
     * The controller hears first what the menu is opened on - it commits the open edit, and selects that part of the grid unless it
     * is selected already - and then the listeners of the grid get the {@link ContextMenuEvent}, placed relative to the grid, and
     * show the menu; no menu opens while the edit could not be committed. The table takes the focus, so the menu gives it back to
     * the table.
     */
    @RequiredUIAccess
    private void showContextMenu(GridHitArea area, int viewRow, int viewColumn, InputDetails details) {
        if (!myTable.isEditing() && !myTable.isFocusOwner()) {
            IdeFocusManager.getGlobalInstance().requestFocus(myTable, true);
        }
        if (!myController.onContextMenuRequested(area, viewRow, viewColumn)) {
            return;
        }
        getListenerDispatcher(ContextMenuEvent.class).onEvent(new ContextMenuEvent(this, details));
    }

    /**
     * A popup trigger on the table: the menu of the cell under the mouse, or of the grid where no cell is. A trigger without a mouse
     * button is the context menu key, sent as a mouse event at the lead cell - the menu is opened on the lead cell then.
     */
    @RequiredUIAccess
    private void showTableContextMenu(MouseEvent event) {
        if (event.getButton() == MouseEvent.NOBUTTON) {
            int leadRow = myController.getLeadViewRow();
            int leadColumn = myController.getLeadViewColumn();
            if (leadRow >= 0 && leadColumn >= 0) {
                showContextMenu(GridHitArea.CELL, leadRow, leadColumn, event);
                return;
            }
        }
        Point point = event.getPoint();
        int row = myTable.rowAtPoint(point);
        int column = toGridColumn(myTable.columnAtPoint(point));
        boolean cell = row >= 0 && row < myController.getViewRowCount() && column >= 0;
        showContextMenu(cell ? GridHitArea.CELL : GridHitArea.EMPTY, cell ? row : -1, cell ? column : -1, event);
    }

    @RequiredUIAccess
    private void showEmptyAreaContextMenu(MouseEvent event) {
        if (event.isPopupTrigger()) {
            showContextMenu(GridHitArea.EMPTY, -1, -1, event);
            event.consume();
        }
    }

    /**
     * The context menu key and Shift+F10 open the menu of the lead cell, below the cell, when the table has the focus.
     */
    private void installContextMenuActions() {
        InputMap inputMap = myTable.getInputMap(JComponent.WHEN_FOCUSED);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0), CONTEXT_MENU_ACTION_KEY);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.SHIFT_DOWN_MASK), CONTEXT_MENU_ACTION_KEY);
        myTable.getActionMap().put(CONTEXT_MENU_ACTION_KEY, new AbstractAction() {
            @Override
            @RequiredUIAccess
            public void actionPerformed(ActionEvent e) {
                showLeadCellContextMenu();
            }
        });
    }

    /**
     * Opens the menu of the lead cell, scrolled into view, at the bottom left corner of the cell - or the menu of the grid, at the
     * top left corner of its visible part, when nothing is selected.
     */
    @RequiredUIAccess
    private void showLeadCellContextMenu() {
        int row = myController.getLeadViewRow();
        int column = myController.getLeadViewColumn();
        int tableColumn = toTableColumn(column);
        GridHitArea area;
        Point point;
        if (row >= 0 && row < myTable.getRowCount() && tableColumn >= 0) {
            Rectangle cell = myTable.getCellRect(row, tableColumn, false);
            myTable.scrollRectToVisible(cell);
            area = GridHitArea.CELL;
            point = new Point(cell.x, cell.y + cell.height - 1);
        }
        else {
            Rectangle visible = myTable.getVisibleRect();
            area = GridHitArea.EMPTY;
            point = new Point(visible.x, visible.y);
            row = -1;
            column = -1;
        }

        Point onGrid = SwingUtilities.convertPoint(myTable, point, toAWTComponent());
        Point onScreen = new Point(point);
        SwingUtilities.convertPointToScreen(onScreen, myTable);
        AWTEvent current = EventQueue.getCurrentEvent();
        EnumSet<ModifiedInputDetails.Modifier> modifiers = current instanceof InputEvent inputEvent
            ? DesktopAWTInputDetails.toModifiers(inputEvent)
            : EnumSet.noneOf(ModifiedInputDetails.Modifier.class);
        int keyCode = current instanceof KeyEvent keyEvent ? keyEvent.getKeyCode() : KeyEvent.VK_CONTEXT_MENU;
        InputDetails details = new KeyboardInputDetails(new Point2D(onGrid.x, onGrid.y), new Point2D(onScreen.x, onScreen.y),
            modifiers, KeyCode.of(keyCode));
        showContextMenu(area, row, column, details);
    }

    // endregion

    // region selection

    @RequiredUIAccess
    private void onNativeSelectionEvent(ListSelectionEvent event) {
        if (myApplyingSelection) {
            return;
        }
        reportNativeSelection(event.getValueIsAdjusting());
    }

    /**
     * Makes a change of both selection models of the table - the rows and the columns change one after the other - and reports the
     * selection once, when both are set, so no listener sees the new rows with the old columns as a finished selection.
     */
    @RequiredUIAccess
    void changeSelection(Runnable change, boolean adjusting) {
        myApplyingSelection = true;
        try {
            change.run();
        }
        finally {
            myApplyingSelection = false;
        }
        reportNativeSelection(adjusting);
    }

    @RequiredUIAccess
    void reportNativeSelection(boolean adjusting) {
        int rowCount = myController.getViewRowCount();
        int[] rows = Arrays.stream(myTable.getSelectedRows())
            .filter(row -> row >= 0 && row < rowCount)
            .sorted()
            .toArray();
        int[] columns = Arrays.stream(myTable.getSelectedColumns())
            .map(this::toGridColumn)
            .filter(column -> column >= 0)
            .distinct()
            .sorted()
            .toArray();
        // the lead cell is the one Enter and F2 edit
        int leadRow = myTable.getSelectionModel().getLeadSelectionIndex();
        int leadColumn = toGridColumn(myTable.getColumnModel().getSelectionModel().getLeadSelectionIndex());
        myController.onNativeSelectionChanged(rows, columns, adjusting, leadRow, leadColumn);

        repaintRowHeader();
    }

    /**
     * Puts the selection of the controller on the table, without reporting it back.
     */
    private void applySelection() {
        myApplyingSelection = true;
        try {
            ListSelectionModel rowModel = myTable.getSelectionModel();
            ListSelectionModel columnModel = myTable.getColumnModel().getSelectionModel();
            rowModel.setValueIsAdjusting(true);
            columnModel.setValueIsAdjusting(true);
            try {
                rowModel.clearSelection();
                columnModel.clearSelection();

                addIntervals(rowModel, myController.getSelectedViewRows(), myTable.getRowCount());

                for (int gridColumn : myController.getSelectedViewColumns()) {
                    int tableColumn = toTableColumn(gridColumn);
                    if (tableColumn >= 0) {
                        columnModel.addSelectionInterval(tableColumn, tableColumn);
                    }
                }

                // the lead cell of the controller - the one Enter and F2 edit - is where the arrow keys go on from, not the last
                // interval added
                moveLead(rowModel, myController.getLeadViewRow(), myTable.getRowCount());
                moveLead(columnModel, toTableColumn(myController.getLeadViewColumn()), myTable.getColumnCount());
            }
            finally {
                rowModel.setValueIsAdjusting(false);
                columnModel.setValueIsAdjusting(false);
            }
        }
        finally {
            myApplyingSelection = false;
        }

        repaintRowHeader();
    }

    /**
     * Puts the lead and the anchor of a selection model at an index, without changing what is selected.
     */
    private static void moveLead(ListSelectionModel model, int index, int limit) {
        if (index < 0 || index >= limit) {
            return;
        }
        model.setAnchorSelectionIndex(index);
        if (model instanceof DefaultListSelectionModel defaultModel) {
            defaultModel.moveLeadSelectionIndex(index);
        }
    }

    /**
     * Adds the indices as runs, so selecting a whole page does not add its rows one by one.
     */
    private static void addIntervals(ListSelectionModel model, int[] sortedIndices, int limit) {
        int start = -1;
        int end = -1;
        for (int index : sortedIndices) {
            if (index < 0 || index >= limit) {
                continue;
            }
            if (start >= 0 && index <= end + 1) {
                end = Math.max(end, index);
            }
            else {
                if (start >= 0) {
                    model.addSelectionInterval(start, end);
                }
                start = index;
                end = index;
            }
        }
        if (start >= 0) {
            model.addSelectionInterval(start, end);
        }
    }

    // endregion

    // region editing

    /**
     * Enter and F2 edit the lead cell when the grid edits its cells, in place of the actions the table binds to them. Space starts
     * editing the lead cell the way a typed key does, so a boolean toggles, instead of adding the lead cell to the selection. A grid
     * which does not edit keeps the actions of the look and feel.
     */
    private void installEditActions() {
        installEditAction(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), true);
        installEditAction(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), true);
        installEditAction(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), false);
    }

    private void installEditAction(KeyStroke keyStroke, boolean editSelectedCell) {
        // the table's own input map: the input map of the look and feel, shared by every table, is its parent
        String actionKey = "consulo.dataGrid.edit " + keyStroke;
        myTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keyStroke, actionKey);
        myTable.getActionMap().put(actionKey, new GridEditAction(keyStroke, editSelectedCell));
    }

    /**
     * Commits the open editor, like the table does before its own navigation.
     *
     * @return {@code false} when the commit is refused and the editor stays open
     */
    @RequiredUIAccess
    boolean stopTableEditing() {
        TableCellEditor editor = myTable.getCellEditor();
        return editor == null || editor.stopCellEditing();
    }

    private final class GridEditAction extends AbstractAction {
        private final KeyStroke myKeyStroke;
        private final boolean myEditSelectedCell;

        GridEditAction(KeyStroke keyStroke, boolean editSelectedCell) {
            myKeyStroke = keyStroke;
            myEditSelectedCell = editSelectedCell;
        }

        /**
         * Enabled while nothing is edited. A disabled binding makes the table pass the key on: to the open editor, or - for Space -
         * to its start of an edit by a typed key.
         */
        @Override
        public boolean accept(@Nullable Object sender) {
            if (!myController.isCellEditingAllowed()) {
                Action original = getOriginalAction();
                return original != null && original.accept(sender);
            }
            return myEditSelectedCell && !myTable.isEditing() && !myController.isEditing();
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(ActionEvent e) {
            if (!myController.isCellEditingAllowed()) {
                Action original = getOriginalAction();
                if (original != null) {
                    original.actionPerformed(e);
                }
                return;
            }
            if (myEditSelectedCell && !myTable.isEditing()) {
                myController.editSelectedCell();
            }
        }

        /**
         * The action the look and feel binds to the key, looked up each time, as a look and feel change replaces it.
         */
        private @Nullable Action getOriginalAction() {
            InputMap lookAndFeelMap = myTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).getParent();
            Object actionKey = lookAndFeelMap == null ? null : lookAndFeelMap.get(myKeyStroke);
            return actionKey == null ? null : myTable.getActionMap().get(actionKey);
        }
    }

    // endregion

    private final class GridView implements DataGridController.View {
        /**
         * The columns are built again in the order of the controller - which also ends the view of a column move sent to the data
         * source - with the widths of the controller, and the rows take the height of its row lines.
         */
        @Override
        @RequiredUIAccess
        public void structureChanged() {
            // a structure change clears the selection of the table - the controller resets its own and says so
            myApplyingSelection = true;
            try {
                myTableModel.fireTableStructureChanged();
            }
            finally {
                myApplyingSelection = false;
            }
            myHoveredSortColumn = -1;
            installColumns();
            applyColumnWidths(true);
            updateRowHeight();
        }

        @Override
        @RequiredUIAccess
        public void rowsChanged() {
            // a data change clears the selection of the table, which is not the user's doing - the selection of the
            // controller is put back below
            myApplyingSelection = true;
            try {
                myTableModel.fireTableDataChanged();
            }
            finally {
                myApplyingSelection = false;
            }

            if (myColumnsNeedFit && myController.getViewRowCount() > 0) {
                applyColumnWidths(true);
            }
            updateRowHeader();
            applySelection();
        }

        /**
         * A column without a width of the controller keeps the width it has.
         */
        @Override
        @RequiredUIAccess
        public void columnWidthsChanged() {
            applyColumnWidths(false);
        }

        /**
         * The rows take their new height, and their cells are painted again - by the multi-line renderer when a row shows more than
         * one line.
         */
        @Override
        @RequiredUIAccess
        public void rowHeightsChanged() {
            updateRowHeight();
            myTable.repaint();
            repaintRowHeader();
        }

        @Override
        @RequiredUIAccess
        public void toolbarVisibilityChanged() {
            updateToolbarVisibility();
        }

        @Override
        @RequiredUIAccess
        public void cellsChanged(int firstViewRow, int lastViewRow) {
            int first = Math.max(firstViewRow, 0);
            int last = Math.min(lastViewRow, myTableModel.getRowCount() - 1);
            if (first > last) {
                return;
            }
            myTableModel.fireTableRowsUpdated(first, last);
        }

        @Override
        @RequiredUIAccess
        public void headersChanged() {
            // the header renderer asks the controller for the text, so a repaint shows the new sort markers
            myTable.getTableHeader().repaint();
        }

        @Override
        @RequiredUIAccess
        public void selectionChanged() {
            applySelection();
        }

        @Override
        @RequiredUIAccess
        public void scrollToCell(int viewRow, int viewColumn) {
            if (viewRow < 0 || viewRow >= myTable.getRowCount()) {
                return;
            }
            int tableColumn = Math.max(toTableColumn(viewColumn), 0);
            myTable.scrollRectToVisible(myTable.getCellRect(viewRow, tableColumn, true));
        }

        /**
         * {@code TableUtil.editCellAt} opens the editor and focuses it.
         */
        @Override
        @RequiredUIAccess
        public void editCellAt(int viewRow, int viewColumn, GridEditInitiator initiator) {
            int tableColumn = toTableColumn(viewColumn);
            if (viewRow < 0 || viewRow >= myTable.getRowCount() || tableColumn < 0) {
                return;
            }
            myTable.myEditInitiator = initiator;
            try {
                TableUtil.editCellAt(myTable, viewRow, tableColumn);
            }
            finally {
                myTable.myEditInitiator = null;
            }
        }

        @Override
        @RequiredUIAccess
        public boolean stopCellEditor() {
            return stopTableEditing();
        }

        @Override
        @RequiredUIAccess
        public void cancelCellEditor() {
            TableCellEditor editor = myTable.getCellEditor();
            if (editor != null) {
                editor.cancelCellEditing();
                if (myTable.getCellEditor() == editor) {
                    // the editor kept itself open for the answer the grid waits for - the grid cancels the edit anyway
                    myTable.removeEditor();
                }
            }
        }
    }

    private final class GridTableModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return myController.getViewRowCount();
        }

        @Override
        public int getColumnCount() {
            return myController.getViewColumnCount();
        }

        @Override
        public String getColumnName(int column) {
            return myController.getColumnHeaderText(column);
        }

        @Override
        public @Nullable Object getValueAt(int rowIndex, int columnIndex) {
            // painted by the cell renderer through the controller - the raw value only serves the table's own needs
            return myController.getValueAt(rowIndex, columnIndex);
        }

        /**
         * Every cell of a grid which edits its cells - whether a cell has an editor is up to {@link MyTable#getCellEditor(int, int)}.
         */
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return myController.isCellEditingAllowed();
        }

        /**
         * Nothing to do: the controller has written the value of the editor when it accepted the commit, before the table calls this.
         */
        @Override
        public void setValueAt(@Nullable Object value, int rowIndex, int columnIndex) {
        }
    }

    private final class CellRenderer extends ColoredTableCellRenderer {
        CellRenderer() {
            setCellState(new GridCellState());
        }

        @Override
        @RequiredUIAccess
        protected void customizeCellRenderer(JTable table,
                                             @Nullable Object value,
                                             boolean selected,
                                             boolean hasFocus,
                                             int row,
                                             int column) {
            int gridColumn = toGridColumn(column);
            if (gridColumn < 0 || row < 0 || row >= myController.getViewRowCount()) {
                return;
            }
            setTextAlign(toSwingAlignment(myController.getColumnAlignment(gridColumn)));
            // a selected cell keeps the selection colours over the colours of its pending change and of its column
            myController.renderCell(new DesktopTextItemPresentationImpl(this), row, gridColumn, !selected, !selected);
        }
    }

    /**
     * Paints a cell of a row which shows more than one text line ({@link DataGridController#getRowLines()}). A {@link CellRenderer}
     * renders the cell - its colours, its border and its text, whose lines the controller separates with {@code \n} - and each line
     * is painted by a line renderer of its own, from the top of the cell down, in the attributes of its text.
     */
    private final class MultiLineCellRenderer extends JComponent implements TableCellRenderer {
        private final CellRenderer myCellRenderer = new CellRenderer();
        private final SimpleColoredComponent myLineRenderer = new SimpleColoredComponent();
        private final CellRendererPane myLinePane = new CellRendererPane();
        private final List<List<LineFragment>> myLines = new ArrayList<>();
        private int myTextAlign = SwingConstants.LEFT;

        MultiLineCellRenderer() {
            add(myLinePane);
            myLineRenderer.setOpaque(false);
        }

        @Override
        @RequiredUIAccess
        public java.awt.Component getTableCellRendererComponent(JTable table,
                                                                @Nullable Object value,
                                                                boolean isSelected,
                                                                boolean hasFocus,
                                                                int row,
                                                                int column) {
            myCellRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setOpaque(true);
            setBackground(myCellRenderer.getBackground());
            setForeground(myCellRenderer.getForeground());
            setFont(myCellRenderer.getFont());
            setBorder(myCellRenderer.getBorder());
            int gridColumn = toGridColumn(column);
            myTextAlign = gridColumn < 0 ? SwingConstants.LEFT : toSwingAlignment(myController.getColumnAlignment(gridColumn));
            collectLines();
            return this;
        }

        /**
         * Splits the rendered text into its lines, each with the attributes of its parts.
         */
        private void collectLines() {
            myLines.clear();
            List<LineFragment> line = new ArrayList<>();
            myLines.add(line);
            ColoredTextContainer.ColoredIterator iterator = myCellRenderer.iterator();
            while (iterator.hasNext()) {
                String fragment = iterator.next();
                SimpleTextAttributes attributes = iterator.getTextAttributes();
                int start = 0;
                while (true) {
                    int end = fragment.indexOf('\n', start);
                    String part = end < 0 ? fragment.substring(start) : fragment.substring(start, end);
                    if (!part.isEmpty()) {
                        line.add(new LineFragment(part, attributes));
                    }
                    if (end < 0) {
                        break;
                    }
                    line = new ArrayList<>();
                    myLines.add(line);
                    start = end + 1;
                }
            }
        }

        /**
         * Sets the line renderer up for a line.
         */
        private SimpleColoredComponent prepareLine(List<LineFragment> line) {
            SimpleColoredComponent renderer = myLineRenderer;
            renderer.clear();
            renderer.setFont(getFont());
            renderer.setForeground(getForeground());
            renderer.setIpad(myCellRenderer.getIpad());
            renderer.setTextAlign(myTextAlign);
            for (LineFragment fragment : line) {
                renderer.append(fragment.text(), fragment.attributes());
            }
            return renderer;
        }

        /**
         * The first line starts where the text of a cell showing all the row lines starts, so the lines of the cells of a row line
         * up whatever number of lines each cell has.
         */
        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());

            Insets insets = getInsets();
            int lineHeight = myTable.getTextLineHeight();
            int width = getWidth() - insets.left - insets.right;
            int innerHeight = getHeight() - insets.top - insets.bottom;
            int rowLines = Math.max(1, myController.getRowLines());
            int y = insets.top + Math.max(0, (innerHeight - rowLines * lineHeight) / 2);
            for (List<LineFragment> line : myLines) {
                if (y >= getHeight()) {
                    break;
                }
                myLinePane.paintComponent(g, prepareLine(line), this, insets.left, y, width, lineHeight, true);
                y += lineHeight;
            }
        }

        @Override
        public Dimension getPreferredSize() {
            Insets insets = getInsets();
            int width = 0;
            for (List<LineFragment> line : myLines) {
                width = Math.max(width, prepareLine(line).getPreferredSize().width);
            }
            int height = myLines.size() * myTable.getTextLineHeight() + JBUI.scale(4);
            return new Dimension(width + insets.left + insets.right, height + insets.top + insets.bottom);
        }
    }

    private record LineFragment(String text, SimpleTextAttributes attributes) {
    }

    /**
     * {@link TableCellState} paints the focused cell of a table with editable cells in the focus cell colours of the look and feel,
     * which tell it apart from the selection. Here a selected cell keeps the selection colours, also when it has the focus.
     */
    private static final class GridCellState extends TableCellState {
        private @Nullable Color mySelectionForeground;
        private @Nullable Color mySelectionBackground;

        @Override
        public void collectState(JTable table, boolean isSelected, boolean hasFocus, int row, int column) {
            super.collectState(table, isSelected, hasFocus, row, column);
            mySelectionForeground = isSelected ? table.getSelectionForeground() : null;
            mySelectionBackground = isSelected ? table.getSelectionBackground() : null;
        }

        @Override
        public void updateRenderer(JComponent renderer) {
            super.updateRenderer(renderer);
            Color foreground = mySelectionForeground;
            Color background = mySelectionBackground;
            if (foreground != null && background != null) {
                renderer.setForeground(foreground);
                renderer.setBackground(background);
            }
        }

        @Override
        public SimpleTextAttributes modifyAttributes(SimpleTextAttributes attributes) {
            Color foreground = mySelectionForeground;
            return foreground == null
                ? super.modifyAttributes(attributes)
                : SimpleTextAttributes.of(attributes.getStyle(), TargetAWT.from(foreground));
        }
    }

    /**
     * Keeps the look of the header: the default renderer of the header paints, only the text and the sort marker come from the
     * controller, so a repaint is enough to show a new sort order. The default renderer is asked each time, as a look and feel
     * change replaces it.
     * <p/>
     * A sortable column shows the name of the column with a {@link SortMarkerIcon} right of it, which the label of the default
     * renderer lays out as its icon ({@link #getSortMarkerBounds}). A renderer which is not a label shows the sort order in the text
     * ({@link DataGridController#getColumnHeaderText}), and has no marker to click.
     */
    private final class HeaderRenderer implements TableCellRenderer {
        private final SortMarkerIcon mySortMarker = new SortMarkerIcon();

        @Override
        public java.awt.Component getTableCellRendererComponent(@Nullable JTable table,
                                                                @Nullable Object value,
                                                                boolean isSelected,
                                                                boolean hasFocus,
                                                                int row,
                                                                int column) {
            int gridColumn = toGridColumn(column);
            GridColumn gridColumnValue = gridColumn < 0 ? null : myController.getColumn(gridColumn);
            String name = gridColumnValue == null ? "" : myController.getName(gridColumnValue);
            boolean sortable = gridColumn >= 0 && myController.isColumnSortable(gridColumn);

            TableCellRenderer delegate = myTable.getTableHeader().getDefaultRenderer();
            java.awt.Component component = delegate.getTableCellRendererComponent(myTable, name, isSelected, hasFocus, row, column);
            if (!(component instanceof JLabel label)) {
                String text = gridColumn < 0 ? "" : myController.getColumnHeaderText(gridColumn);
                return delegate.getTableCellRendererComponent(myTable, text, isSelected, hasFocus, row, column);
            }

            if (sortable) {
                SortState sort = sortStateOf(gridColumn);
                mySortMarker.setState(sort.order(), sort.priority(), gridColumn == myHoveredSortColumn,
                    label.getFontMetrics(label.getFont()));
                label.setIcon(mySortMarker);
                label.setHorizontalTextPosition(SwingConstants.LEADING);
                label.setIconTextGap(JBUI.scale(4));
            }
            else {
                // the label is shared by every column: the marker of the column painted before goes
                label.setIcon(null);
            }
            return label;
        }
    }

    /**
     * The sort order of a view column of the controller, and the priority the marker shows - only while more than one column is
     * sorted.
     */
    private SortState sortStateOf(int gridColumn) {
        ModelIndex<GridColumn> column = myController.toModelColumn(gridColumn);
        RowSortOrder.Type order = myController.getSortOrder(column);
        int priority = myController.getThenBySortOrder(column);
        boolean showPriority = order != RowSortOrder.Type.UNSORTED && myController.countSortedColumns() > 1 && priority > 0;
        return new SortState(order, showPriority ? Integer.toString(priority) : "");
    }

    private record SortState(RowSortOrder.Type order, String priority) {
    }

    /**
     * The sort marker of a sortable column, right of its name in the header: an arrow up or down for a sorted column - with its
     * priority when more than one column is sorted - or both arrows, dimmed, for a column which is not sorted. A click on it sorts
     * by the column; under the mouse it gets the hover background.
     */
    private static final class SortMarkerIcon implements Icon {
        private static final int ARROW_WIDTH = 7;
        private static final int ARROW_HEIGHT = 4;
        private static final int ARROW_GAP = 2;
        private static final int PADDING = 2;
        private static final int TEXT_GAP = 2;

        private RowSortOrder.Type myOrder = RowSortOrder.Type.UNSORTED;
        private String myPriority = "";
        private boolean myHovered;
        private @Nullable FontMetrics myMetrics;

        void setState(RowSortOrder.Type order, String priority, boolean hovered, FontMetrics metrics) {
            myOrder = order;
            myPriority = priority;
            myHovered = hovered;
            myMetrics = metrics;
        }

        @Override
        public int getIconWidth() {
            int width = JBUI.scale(PADDING * 2 + ARROW_WIDTH);
            FontMetrics metrics = myMetrics;
            if (!myPriority.isEmpty() && metrics != null) {
                width += JBUI.scale(TEXT_GAP) + metrics.stringWidth(myPriority);
            }
            return width;
        }

        /**
         * As high as a line of the header text, so the marker does not make the header higher.
         */
        @Override
        public int getIconHeight() {
            FontMetrics metrics = myMetrics;
            int arrows = JBUI.scale(ARROW_HEIGHT * 2 + ARROW_GAP + PADDING);
            return metrics == null ? arrows : Math.max(arrows, metrics.getHeight());
        }

        @Override
        public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int width = getIconWidth();
                int height = getIconHeight();
                if (myHovered) {
                    int arc = JBUI.scale(4);
                    g2.setColor(TargetAWT.to(ComponentColors.HOVER_BACKGROUND));
                    g2.fillRoundRect(x, y, width, height, arc, arc);
                }

                g2.setColor(myOrder == RowSortOrder.Type.UNSORTED ? TargetAWT.to(ComponentColors.DISABLED_TEXT) : c.getForeground());
                int arrowWidth = JBUI.scale(ARROW_WIDTH);
                int arrowHeight = JBUI.scale(ARROW_HEIGHT);
                int left = x + JBUI.scale(PADDING);
                int middle = y + height / 2;
                switch (myOrder) {
                    case ASC -> fillArrow(g2, left, middle - arrowHeight / 2, arrowWidth, arrowHeight, true);
                    case DESC -> fillArrow(g2, left, middle - arrowHeight / 2, arrowWidth, arrowHeight, false);
                    case UNSORTED -> {
                        int gap = Math.max(1, JBUI.scale(ARROW_GAP) / 2);
                        fillArrow(g2, left, middle - gap - arrowHeight, arrowWidth, arrowHeight, true);
                        fillArrow(g2, left, middle + gap, arrowWidth, arrowHeight, false);
                    }
                }

                FontMetrics metrics = myMetrics;
                if (!myPriority.isEmpty() && metrics != null) {
                    g2.setFont(metrics.getFont());
                    int textX = left + arrowWidth + JBUI.scale(TEXT_GAP);
                    int baseline = y + (height - metrics.getHeight()) / 2 + metrics.getAscent();
                    g2.drawString(myPriority, textX, baseline);
                }
            }
            finally {
                g2.dispose();
            }
        }

        private static void fillArrow(Graphics2D g, int x, int y, int width, int height, boolean up) {
            Path2D.Float path = new Path2D.Float();
            if (up) {
                path.moveTo(x, y + height);
                path.lineTo(x + width / 2f, y);
                path.lineTo(x + width, y + height);
            }
            else {
                path.moveTo(x, y);
                path.lineTo(x + width / 2f, y + height);
                path.lineTo(x + width, y);
            }
            path.closePath();
            g.fill(path);
        }
    }

    /**
     * The mouse on the column header: a click on the sort marker of a column sorts by it (Shift or Alt adds the column to the sorted
     * ones), a click elsewhere on it selects the column (Shift extends the selection, Ctrl or Meta adds the column to it or takes it
     * out), and a column dragged to another place is moved in the data source when it is dropped - a port of the listener which
     * tells the move from the order of the columns. The sort marker under the mouse is highlighted.
     */
    private final class HeaderMouseListener extends MouseAdapter implements TableColumnModelListener {
        private final JTableHeader myHeader;
        /**
         * The mouse dragged since it was pressed: a column moved or resized by the user is not a click on it.
         */
        private boolean myDragged;
        /**
         * A press of the left button may drag a column, and whether the columns moved since.
         */
        private boolean myGrabbed;
        private boolean myMoved;

        HeaderMouseListener(JTableHeader header) {
            myHeader = header;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            myDragged = false;
            myGrabbed = SwingUtilities.isLeftMouseButton(e);
            myMoved = false;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            myDragged = true;
        }

        @Override
        @RequiredUIAccess
        public void mouseReleased(MouseEvent e) {
            boolean dropped = myGrabbed && myMoved;
            myGrabbed = false;
            myMoved = false;
            if (dropped) {
                onColumnDropped();
            }
        }

        @Override
        @RequiredUIAccess
        public void mouseClicked(MouseEvent e) {
            // the way the header of the look and feel tells a click
            if (myDragged || !myHeader.isEnabled() || !SwingUtilities.isLeftMouseButton(e) || e.getClickCount() % 2 != 1) {
                return;
            }
            int column = toGridColumn(myHeader.columnAtPoint(e.getPoint()));
            if (column < 0) {
                return;
            }
            if (getSortColumnAt(e.getPoint()) == column) {
                // sorting reloads the rows, which would drop what is typed in the editor
                if (stopTableEditing()) {
                    myController.onColumnHeaderSortClicked(column, e.isAltDown() || e.isShiftDown());
                }
            }
            else {
                // the controller commits the open edit first, and keeps the selection when the commit is refused
                myController.onColumnHeaderClicked(column, e.isShiftDown(), e.isControlDown() || e.isMetaDown());
            }
            // the keys act on the selected column; an editor kept open keeps the focus
            if (!myTable.isEditing() && !myTable.isFocusOwner()) {
                IdeFocusManager.getGlobalInstance().requestFocus(myTable, true);
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            setHoveredSortColumn(getSortColumnAt(e.getPoint()));
        }

        @Override
        public void mouseExited(MouseEvent e) {
            setHoveredSortColumn(-1);
        }

        @Override
        public void columnMoved(TableColumnModelEvent e) {
            if (myGrabbed && e.getFromIndex() != e.getToIndex()) {
                myMoved = true;
            }
        }

        @Override
        public void columnAdded(TableColumnModelEvent e) {
        }

        @Override
        public void columnRemoved(TableColumnModelEvent e) {
        }

        @Override
        public void columnMarginChanged(ChangeEvent e) {
        }

        @Override
        public void columnSelectionChanged(ListSelectionEvent e) {
        }
    }

    /**
     * The corner above the row numbers belongs to the row of the column header, whose background it shows.
     */
    private final class RowHeaderCorner extends JPanel {
        @Override
        public Color getBackground() {
            return myTable.getTableHeader().getBackground();
        }
    }
}
