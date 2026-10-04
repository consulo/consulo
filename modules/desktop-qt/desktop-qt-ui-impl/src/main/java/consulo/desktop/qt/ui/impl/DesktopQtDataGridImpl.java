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
package consulo.desktop.qt.ui.impl;

import consulo.application.Application;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.EditorFontType;
import consulo.colorScheme.event.EditorColorsListener;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Point2D;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.ContextMenuEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.KeyboardInputDetails;
import consulo.ui.font.Font;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.DataGridAppearance;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridDataHookUp;
import consulo.ui.grid.GridHitArea;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.editor.GridEditInitiator;
import consulo.ui.internal.DataGridAppearanceImpl;
import consulo.ui.internal.DataGridController;
import consulo.ui.internal.DataGridControllerOwner;
import consulo.ui.style.ComponentColors;
import io.qt.core.QAbstractItemModel;
import io.qt.core.QAbstractTableModel;
import io.qt.core.QItemSelection;
import io.qt.core.QItemSelectionModel;
import io.qt.core.QItemSelectionRange;
import io.qt.core.QModelIndex;
import io.qt.core.QObject;
import io.qt.core.QPoint;
import io.qt.core.QPointF;
import io.qt.core.QRect;
import io.qt.core.Qt;
import io.qt.gui.QBrush;
import io.qt.gui.QFont;
import io.qt.gui.QFontMetrics;
import io.qt.gui.QGuiApplication;
import io.qt.widgets.QAbstractItemView;
import io.qt.widgets.QHeaderView;
import io.qt.widgets.QStyle;
import io.qt.widgets.QTableView;
import io.qt.widgets.QVBoxLayout;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * The {@link DataGrid} of the qt frontend: the paging bar of the {@link DataGridController} on top of a {@link QTableView}
 * whose model reads every cell, header and row number straight from the controller.
 * <p/>
 * A cell is edited in the widget of {@link DesktopQtDataGridItemDelegate}, inside the cell: {@link DesktopQtDataGridTableView} maps
 * the gestures of the user to the controller, which opens and ends every edit, and the model paints the colours of the pending
 * changes the edits leave.
 * <p/>
 * The context menu is asked for on the cells and on both headers: the gesture is hit-tested there, the controller is told what it
 * hit, and the grid raises its {@link ContextMenuEvent}. The widths of the columns are kept by the controller, in characters of the
 * grid font, and the rows are as tall as the lines it shows in them.
 * <p/>
 * The widget is built lazily and may be torn down and built again, so the model holds no data of its own - a fresh
 * widget shows whatever the controller holds at that moment, and every callback of the controller asks first whether
 * there is a widget to update.
 *
 * @since 2026-10-03
 */
public class DesktopQtDataGridImpl extends QtComponentDelegate<QWidget> implements DataGridControllerOwner {
    /**
     * The room a row leaves around its lines of text, besides the line of the grid - a multi-line editor needs its margins and one
     * pixel more to show a line without a scroll bar.
     */
    static final int ROW_TEXT_PADDING = 5;
    /**
     * The OpenType features which draw several characters as one glyph.
     */
    private static final String[] LIGATURE_FEATURES = {"liga", "clig", "calt"};
    /**
     * The key which opens the context menu from the keyboard.
     */
    private static final KeyCode CONTEXT_MENU_KEY = KeyCode.of(0x20D, "VK_CONTEXT_MENU");

    private final DataGridController myController;
    private final Component myToolbar;

    private @Nullable DesktopQtDataGridTableView myTable;
    private @Nullable GridTableModel myModel;

    /**
     * Set while the selection of the controller is written to the widget, so that it is not reported back as a choice
     * of the user.
     */
    private boolean myApplyingSelection;

    /**
     * Set while the widths of the columns are set here, so that they are not reported back as widths the user dragged.
     */
    private boolean myApplyingWidths;

    /**
     * Whether the columns were sized to the rows they show - done once per set of columns, so a width the user
     * dragged is not taken back by the next page.
     */
    private boolean myColumnsSized;

    /**
     * The height of a line of text in a row: a line of the editor font, spaced as the scheme spaces the lines of the editor.
     */
    private int myLineHeight;

    @RequiredUIAccess
    public DesktopQtDataGridImpl(GridDataHookUp<GridRow, GridColumn> hookUp, BiConsumer<DataGrid, DataGridAppearance> configurator) {
        myController = new DataGridController(this, hookUp);
        configurator.accept(this, myController.getAppearance());

        myToolbar = myController.getToolbar();

        // the values are shown in the font of the editor, which follows the editor settings
        Application.get().getMessageBus().connect(myController).subscribe(EditorColorsListener.class, scheme -> onEditorSchemeChanged());

        // the widget does not exist yet - every callback of the view checks for it, and binding reads the whole state
        myController.setView(new ViewImpl());
        myController.start();
    }

    @Override
    public DataGridController getController() {
        return myController;
    }

    /**
     * The grid raises its {@link ContextMenuEvent} itself, once the controller knows what the menu was opened on - the cells and both
     * headers hit-test the gesture. The generic dispatch of the root widget is not installed: it would raise a second event, for any
     * spot of the grid and without telling the controller.
     */
    @Override
    public <C extends Component, E extends ComponentEvent<C>> Disposable addListener(
        Class<? extends E> eventClass,
        ComponentEventListener<C, E> listener
    ) {
        if (eventClass == ContextMenuEvent.class) {
            return myDataObject.addListener(eventClass, listener);
        }
        return super.addListener(eventClass, listener);
    }

    @Override
    protected QWidget createQt(QWidget parent) {
        QWidget panel = new QWidget(parent);
        // the cells and both headers open the menu of the grid; a gesture anywhere else in it - on the paging bar - opens none,
        // neither the menu of the grid nor the one of a component around it
        panel.setContextMenuPolicy(Qt.ContextMenuPolicy.PreventContextMenu);

        QVBoxLayout layout = new QVBoxLayout(panel);
        layout.setContentsMargins(0, 0, 0, 0);
        layout.setSpacing(0);

        if (myToolbar instanceof QtComponentDelegate<?> toolbar) {
            toolbar.setParent(this);
            toolbar.bind(panel, null);

            QWidget toolbarWidget = toolbar.toQtComponent();
            if (toolbarWidget != null) {
                layout.addWidget(toolbarWidget);
            }
        }

        // the table sets its own item delegate and column header, and keeps the edit triggers and the sorting of qt off - an editor
        // opens through the controller alone, and the controller sorts
        DesktopQtDataGridTableView table = new DesktopQtDataGridTableView(panel, myController);
        table.setSelectionBehavior(QAbstractItemView.SelectionBehavior.SelectItems);
        table.setSelectionMode(QAbstractItemView.SelectionMode.ExtendedSelection);
        table.setHorizontalScrollMode(QAbstractItemView.ScrollMode.ScrollPerPixel);
        // a value of several lines is painted line by line by the delegate, never wrapped
        table.setWordWrap(false);
        table.setTextElideMode(Qt.TextElideMode.ElideRight);

        GridTableModel model = new GridTableModel(table);
        table.setModel(model);

        QHeaderView horizontalHeader = table.horizontalHeader();
        horizontalHeader.setSectionsClickable(true);
        horizontalHeader.setSortIndicatorShown(false);
        horizontalHeader.setHighlightSections(false);
        horizontalHeader.setTextElideMode(Qt.TextElideMode.ElideRight);
        horizontalHeader.setDefaultAlignment(Qt.AlignmentFlag.AlignLeft, Qt.AlignmentFlag.AlignVCenter);
        // a width the user drags is kept by the controller, so it survives a change of the columns
        horizontalHeader.sectionResized.connect((section, oldSize, newSize) -> onColumnResized(table, section, newSize));

        QHeaderView verticalHeader = table.verticalHeader();

        // the menu is asked for on the cells and on both headers, each hit-tested on its own
        table.setContextMenuPolicy(Qt.ContextMenuPolicy.CustomContextMenu);
        table.customContextMenuRequested.connect(position -> onCellsContextMenu(table, position));
        horizontalHeader.setContextMenuPolicy(Qt.ContextMenuPolicy.CustomContextMenu);
        horizontalHeader.customContextMenuRequested.connect(position -> onColumnHeaderContextMenu(table, position));
        verticalHeader.setContextMenuPolicy(Qt.ContextMenuPolicy.CustomContextMenu);
        verticalHeader.customContextMenuRequested.connect(position -> onRowHeaderContextMenu(table, position));

        applyEditorFont(table);

        QItemSelectionModel selectionModel = table.selectionModel();
        if (selectionModel != null) {
            selectionModel.selectionChanged.connect((selected, deselected) -> reportNativeSelection());
            // the lead cell, which Enter and F2 edit, moves without the selection changing too
            selectionModel.currentChanged.connect((current, previous) -> reportNativeSelection());
        }

        layout.addWidget(table, 1);
        panel.setFocusProxy(table);

        myTable = table;
        myModel = model;
        myColumnsSized = false;

        panel.destroyed.connect(() -> {
            if (myTable == table) {
                // an open edit loses its editor with the table
                table.discardEditor();

                myTable = null;
                myModel = null;
            }
        });

        return panel;
    }

    @Override
    protected void initialize(QWidget component) {
        super.initialize(component);

        DesktopQtDataGridTableView table = aliveTable();
        if (table == null) {
            return;
        }

        // the controller may have loaded its data long before the widget was asked for, or while it was gone
        applyAppearance(table);
        applyRowHeight(table);
        applyColumnWidths(table);
        sizeColumnsOnce(table);
        applySelection();
        table.updateColumnMoving();
    }

    /**
     * The widgets go, the toolbar stays: binding again gives it a widget over, the way a layout treats its children.
     */
    @Override
    public void disposeQt() {
        if (myToolbar instanceof QtComponentDelegate<?> toolbar) {
            toolbar.disposeQt();
        }

        // an open edit goes with its editor - a fresh widget would not show it
        DesktopQtDataGridTableView table = aliveTable();
        if (table != null) {
            table.discardEditor();
        }

        myTable = null;
        myModel = null;

        super.disposeQt();
    }

    @Override
    public void dispose() {
        Disposer.dispose(myController);

        if (UIAccess.isUIThread()) {
            disposeQt();
        }
        else {
            getUIAccess().give(this::disposeQt);
        }
    }

    private @Nullable DesktopQtDataGridTableView aliveTable() {
        DesktopQtDataGridTableView table = myTable;
        return table != null && !table.isDisposed() ? table : null;
    }

    private @Nullable GridTableModel aliveModel() {
        GridTableModel model = myModel;
        return model != null && !model.isDisposed() ? model : null;
    }

    // region context menu

    /**
     * A context menu on the cells: the one of the cell under the pointer, or of the empty part of the table below and beside the
     * cells. The menu key opens the one of the lead cell, under it.
     */
    @RequiredUIAccess
    private void onCellsContextMenu(DesktopQtDataGridTableView table, QPoint position) {
        QWidget viewport = table.viewport();
        if (table.isKeyboardContextMenu()) {
            int row = myController.getLeadViewRow();
            int column = myController.getLeadViewColumn();

            // under the lead cell, scrolled into view; the top left corner of the cells when there is none
            QPoint anchor = new QPoint(0, 0);
            QAbstractItemModel model = table.model();
            if (model != null && row >= 0 && column >= 0) {
                QModelIndex index = model.index(row, column);
                if (index.isValid()) {
                    table.scrollTo(index);
                    QRect cell = table.visualRect(index);
                    anchor = new QPoint(
                        Math.max(0, Math.min(cell.left(), viewport.width() - 1)),
                        Math.max(0, Math.min(cell.bottom(), viewport.height() - 1))
                    );
                }
            }

            requestContextMenu(table, GridHitArea.CELL, row, column, viewport.mapToGlobal(anchor), true);
            return;
        }

        QModelIndex index = table.indexAt(position);
        if (index.isValid()) {
            requestContextMenu(table, GridHitArea.CELL, index.row(), index.column(), viewport.mapToGlobal(position), false);
        }
        else {
            requestContextMenu(table, GridHitArea.EMPTY, -1, -1, viewport.mapToGlobal(position), false);
        }
    }

    /**
     * A context menu on the column header: the one of the column under the pointer.
     */
    @RequiredUIAccess
    private void onColumnHeaderContextMenu(DesktopQtDataGridTableView table, QPoint position) {
        QHeaderView header = table.horizontalHeader();
        int column = header.logicalIndexAt(position);
        GridHitArea area = column >= 0 ? GridHitArea.COLUMN_HEADER : GridHitArea.EMPTY;
        requestContextMenu(table, area, -1, column, header.viewport().mapToGlobal(position), false);
    }

    /**
     * A context menu on the row header: the one of the row under the pointer.
     */
    @RequiredUIAccess
    private void onRowHeaderContextMenu(DesktopQtDataGridTableView table, QPoint position) {
        QHeaderView header = table.verticalHeader();
        int row = header.logicalIndexAt(position);
        GridHitArea area = row >= 0 ? GridHitArea.ROW_HEADER : GridHitArea.EMPTY;
        requestContextMenu(table, area, row, -1, header.viewport().mapToGlobal(position), false);
    }

    /**
     * Tells the controller what the menu was opened on - it selects that, unless it is selected already - and raises the
     * {@link ContextMenuEvent} of the grid, whose listener shows the menu at the point of the gesture.
     *
     * @param globalPosition the point of the gesture on the screen
     * @param keyboard       whether the menu key asked for the menu
     */
    @RequiredUIAccess
    private void requestContextMenu(
        DesktopQtDataGridTableView table,
        GridHitArea area,
        int viewRow,
        int viewColumn,
        QPoint globalPosition,
        boolean keyboard
    ) {
        // the controller commits the open edit first; a refused commit keeps the editor and opens no menu
        if (!myController.onContextMenuRequested(area, viewRow, viewColumn)) {
            return;
        }

        // the point is relative to the root widget of the grid, the component the event is raised on
        QWidget root = toQtComponent();
        InputDetails details;
        if (keyboard) {
            QPoint position = root != null && !root.isDisposed() ? root.mapFromGlobal(globalPosition) : globalPosition;
            details = new KeyboardInputDetails(
                new Point2D(position.x(), position.y()),
                new Point2D(globalPosition.x(), globalPosition.y()),
                DesktopQtInputDetails.modifiers(QGuiApplication.keyboardModifiers()),
                CONTEXT_MENU_KEY
            );
        }
        else {
            details = DesktopQtInputDetails.mouse(
                root,
                new QPointF(globalPosition),
                Qt.MouseButton.RightButton,
                QGuiApplication.keyboardModifiers()
            );
        }

        ComponentEventListener<Component, ContextMenuEvent> dispatcher = getListenerDispatcher(ContextMenuEvent.class);
        dispatcher.onEvent(new ContextMenuEvent(this, details));
    }

    // endregion

    /**
     * The controller keeps a selection of whole rows times whole columns, so the cells qt selected are reduced to the
     * rows and the columns they touch. The current cell of qt is the lead cell, which Enter and F2 edit.
     */
    @RequiredUIAccess
    private void reportNativeSelection() {
        if (myApplyingSelection) {
            return;
        }

        QTableView table = aliveTable();
        if (table == null) {
            return;
        }

        QItemSelectionModel selectionModel = table.selectionModel();
        if (selectionModel == null) {
            return;
        }

        int rowCount = myController.getViewRowCount();
        int columnCount = myController.getViewColumnCount();

        BitSet rows = new BitSet();
        BitSet columns = new BitSet();
        for (QItemSelectionRange range : selectionModel.selection()) {
            if (!range.isValid()) {
                continue;
            }

            int top = Math.max(0, range.top());
            int bottom = Math.min(rowCount, range.bottom() + 1);
            int left = Math.max(0, range.left());
            int right = Math.min(columnCount, range.right() + 1);
            if (top >= bottom || left >= right) {
                continue;
            }

            rows.set(top, bottom);
            columns.set(left, right);
        }

        // read here rather than taken from the signal: a refused commit puts the current cell back while qt is still notifying
        QModelIndex current = selectionModel.currentIndex();
        int leadRow = current.isValid() ? current.row() : -1;
        int leadColumn = current.isValid() ? current.column() : -1;

        myController.onNativeSelectionChanged(rows.stream().toArray(), columns.stream().toArray(), false, leadRow, leadColumn);
    }

    /**
     * Writes the selection of the controller to the widget. Consecutive rows and columns are merged into ranges, since
     * a selection of every row of a column would otherwise be as many ranges as there are rows.
     */
    @RequiredUIAccess
    private void applySelection() {
        QTableView table = aliveTable();
        GridTableModel model = aliveModel();
        if (table == null || model == null) {
            return;
        }

        QItemSelectionModel selectionModel = table.selectionModel();
        if (selectionModel == null) {
            return;
        }

        List<int[]> rowRuns = toRuns(myController.getSelectedViewRows(), myController.getViewRowCount());
        List<int[]> columnRuns = toRuns(myController.getSelectedViewColumns(), myController.getViewColumnCount());

        boolean wasApplying = myApplyingSelection;
        myApplyingSelection = true;
        try {
            if (rowRuns.isEmpty() || columnRuns.isEmpty()) {
                selectionModel.clearSelection();
                return;
            }

            QItemSelection selection = new QItemSelection();
            for (int[] rowRun : rowRuns) {
                for (int[] columnRun : columnRuns) {
                    selection.select(model.index(rowRun[0], columnRun[0]), model.index(rowRun[1], columnRun[1]));
                }
            }

            selectionModel.select(selection, QItemSelectionModel.SelectionFlag.ClearAndSelect);

            // the current cell is the lead cell of the controller - the one Enter and F2 edit, which stays where the user put it
            // while it is selected - or else the first selected cell. It moves without the scroll qt does on a current change:
            // this runs after every reload and sort, and the viewport must not jump back to the selection each time - showCell
            // scrolls on its own
            QModelIndex current = selectionModel.currentIndex();
            QModelIndex target = current.isValid() && selectionModel.isSelected(current)
                ? current
                : model.index(rowRuns.get(0)[0], columnRuns.get(0)[0]);
            int leadRow = myController.getLeadViewRow();
            int leadColumn = myController.getLeadViewColumn();
            if (leadRow >= 0 && leadColumn >= 0) {
                QModelIndex lead = model.index(leadRow, leadColumn);
                if (lead.isValid() && selectionModel.isSelected(lead)) {
                    target = lead;
                }
            }
            if (!current.isValid() || current.row() != target.row() || current.column() != target.column()) {
                boolean autoScroll = table.hasAutoScroll();
                table.setAutoScroll(false);
                try {
                    selectionModel.setCurrentIndex(target, QItemSelectionModel.SelectionFlag.NoUpdate);
                }
                finally {
                    table.setAutoScroll(autoScroll);
                }
            }
        }
        finally {
            myApplyingSelection = wasApplying;
        }
    }

    /**
     * @return the runs of consecutive indices of the sorted {@code indices} below {@code count}, each as {start, end}
     */
    private static List<int[]> toRuns(int[] indices, int count) {
        List<int[]> runs = new ArrayList<>();
        int start = -1;
        int end = -1;
        for (int index : indices) {
            if (index < 0 || index >= count) {
                continue;
            }

            if (start >= 0 && index <= end + 1) {
                end = Math.max(end, index);
                continue;
            }

            if (start >= 0) {
                runs.add(new int[]{start, end});
            }
            start = index;
            end = index;
        }

        if (start >= 0) {
            runs.add(new int[]{start, end});
        }
        return runs;
    }

    @RequiredUIAccess
    private void reloadModel() {
        GridTableModel model = aliveModel();
        if (model == null) {
            return;
        }

        // a reset drops the selection of the widget; whatever qt reports meanwhile is not a choice of the user, nor is a width it
        // gives a column
        boolean wasApplying = myApplyingSelection;
        boolean wasApplyingWidths = myApplyingWidths;
        myApplyingSelection = true;
        myApplyingWidths = true;
        try {
            model.reload();
        }
        finally {
            myApplyingSelection = wasApplying;
            myApplyingWidths = wasApplyingWidths;
        }

        // the controller still holds its selection, and not every reload is followed by a selection change
        applySelection();
    }

    private void applyAppearance(QTableView table) {
        DataGridAppearanceImpl appearance = myController.getAppearance();

        table.verticalHeader().setVisible(myController.isShowRowNumbers());
        table.setShowGrid(appearance.isShowHorizontalLines());
        table.setAlternatingRowColors(appearance.isStriped());
    }

    // region column widths

    /**
     * Sets the columns the controller keeps a width for to that width; the others keep theirs.
     */
    private void applyColumnWidths(QTableView table) {
        int columnCount = myController.getViewColumnCount();

        boolean wasApplyingWidths = myApplyingWidths;
        myApplyingWidths = true;
        try {
            for (int column = 0; column < columnCount; column++) {
                int chars = myController.getViewColumnWidth(column);
                if (chars > 0) {
                    table.setColumnWidth(column, toPixels(table, chars));
                }
            }
        }
        finally {
            myApplyingWidths = wasApplyingWidths;
        }
    }

    /**
     * Sizes every column the controller keeps no width for to its header and the rows it shows, capped. Columns without rows yet
     * are sized to their headers and sized again once the rows arrive; a width sized to rows is given to the controller, so it
     * stays when the columns change.
     */
    private void sizeColumnsOnce(DesktopQtDataGridTableView table) {
        int columnCount = myController.getViewColumnCount();
        if (myColumnsSized || columnCount == 0) {
            return;
        }

        boolean withRows = myController.getViewRowCount() > 0;
        myColumnsSized = withRows;

        // a long text value would otherwise push every other column out of sight
        int minWidth = toPixels(table, DataGridController.FIT_MIN_CHARS);
        int maxWidth = toPixels(table, DataGridController.FIT_MAX_CHARS);
        // the header is sized with room for the sort marker, so sorting a column does not elide its name
        QHeaderView header = table.horizontalHeader();
        int markerWidth = table.getSortAreaWidth();

        boolean wasApplyingWidths = myApplyingWidths;
        myApplyingWidths = true;
        try {
            for (int column = 0; column < columnCount; column++) {
                if (myController.getViewColumnWidth(column) > 0) {
                    // the controller keeps a width for the column - the user's, or one sized before
                    continue;
                }

                table.resizeColumnToContents(column);

                int width = Math.max(minWidth,
                    Math.min(Math.max(table.columnWidth(column), header.sectionSizeHint(column) + markerWidth), maxWidth));
                if (!withRows) {
                    table.setColumnWidth(column, width);
                    continue;
                }

                // the controller keeps whole characters: the column gets the pixels those characters give back, so it does not
                // change by a fraction of a character when the width is applied again after a change of the columns
                int chars = toChars(table, width, true);
                table.setColumnWidth(column, toPixels(table, chars));
                myController.onColumnResized(column, chars);
            }
        }
        finally {
            myApplyingWidths = wasApplyingWidths;
        }
    }

    /**
     * A column the user resized - by dragging the edge of its header, or by a double click on it - tells the controller its width.
     */
    @RequiredUIAccess
    private void onColumnResized(QTableView table, int section, int width) {
        if (myApplyingWidths || table.isDisposed() || section < 0 || section >= myController.getViewColumnCount()) {
            return;
        }

        myController.onColumnResized(section, toChars(table, width, false));
    }

    private static int averageCharWidth(QTableView table) {
        return Math.max(1, table.fontMetrics().averageCharWidth());
    }

    /**
     * The room a cell leaves around its text: the text margin of the style on either side, and the line of the grid.
     */
    private static int cellPadding(QTableView table) {
        return 2 * (table.style().pixelMetric(QStyle.PixelMetric.PM_FocusFrameHMargin) + 1) + 1;
    }

    /**
     * @return the width in pixels of a column of {@code chars} average characters of the grid font
     */
    private static int toPixels(QTableView table, int chars) {
        return chars * averageCharWidth(table) + cellPadding(table);
    }

    /**
     * @return the width of a column of {@code pixels} in average characters of the grid font, at least one
     */
    private static int toChars(QTableView table, int pixels, boolean roundUp) {
        float chars = Math.max(0, pixels - cellPadding(table)) / (float) averageCharWidth(table);
        return Math.max(1, roundUp ? (int) Math.ceil(chars) : Math.round(chars));
    }

    // endregion

    /**
     * Every row is as tall as the lines the controller shows in it ({@link DataGridController#getRowLines()}).
     */
    private void applyRowHeight(QTableView table) {
        int lines = Math.max(1, myController.getRowLines());
        int lineHeight = Math.max(1, myLineHeight);
        // qt keeps the default height of a row whatever its font - a larger font would be cut off. One more pixel for the grid line
        table.verticalHeader().setDefaultSectionSize(lines * lineHeight + ROW_TEXT_PADDING + 1);
    }

    /**
     * The colours scheme of the editor changed - its font with it, maybe: the rows and the columns are laid out again for it.
     */
    private void onEditorSchemeChanged() {
        if (UIAccess.isUIThread()) {
            reapplyEditorFont();
        }
        else {
            getUIAccess().give(this::reapplyEditorFont);
        }
    }

    @RequiredUIAccess
    private void reapplyEditorFont() {
        DesktopQtDataGridTableView table = aliveTable();
        if (table == null) {
            // a widget built later reads the font of the scheme then
            return;
        }

        applyEditorFont(table);

        // the widths are kept in characters, which are other pixels in another font
        applyColumnWidths(table);
        myColumnsSized = false;
        sizeColumnsOnce(table);
    }

    /**
     * The cells, both headers and the cell editors show the plain font of the editor colours scheme - the editors are children of
     * the table and inherit it - and a line of a row is as tall as a line of that font, spaced as the scheme spaces the lines of
     * the editor.
     */
    private void applyEditorFont(DesktopQtDataGridTableView table) {
        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        QFont font = toQFont(scheme.getFont(EditorFontType.PLAIN));

        table.setFont(font);
        table.horizontalHeader().setFont(font);
        table.verticalHeader().setFont(font);

        float lineSpacing = scheme.getLineSpacing();
        myLineHeight = (int) Math.ceil(new QFontMetrics(font).height() * (lineSpacing > 0 ? lineSpacing : 1));
        table.getGridDelegate().setLineHeight(myLineHeight);

        applyRowHeight(table);
    }

    /**
     * The size of a font of the editor colours scheme is a pixel height, as the editor of this frontend reads it - qt would scale a
     * point size by the resolution of the screen, and the values would come out larger than the text of the editor. A value is shown
     * character by character, without the ligatures qt would apply by default.
     */
    private static QFont toQFont(Font font) {
        QFont qFont = new QFont(font.getFamily());
        qFont.setPixelSize(Math.max(font.getFontSize(), 1));
        qFont.setBold((font.getFontStyle() & Font.BOLD) != 0);
        qFont.setItalic((font.getFontStyle() & Font.ITALIC) != 0);
        for (String feature : LIGATURE_FEATURES) {
            qFont.setFeature(feature, 0);
        }
        return qFont;
    }

    private static Qt.Alignment toAlignment(HorizontalAlignment alignment) {
        return switch (alignment) {
            case LEFT -> new Qt.Alignment(Qt.AlignmentFlag.AlignLeft, Qt.AlignmentFlag.AlignVCenter);
            case CENTER -> new Qt.Alignment(Qt.AlignmentFlag.AlignHCenter, Qt.AlignmentFlag.AlignVCenter);
            case RIGHT -> new Qt.Alignment(Qt.AlignmentFlag.AlignRight, Qt.AlignmentFlag.AlignVCenter);
        };
    }

    private final class ViewImpl implements DataGridController.View {
        @RequiredUIAccess
        @Override
        public void structureChanged() {
            // new columns are sized again, to the rows which come with them - but for those the controller keeps a width for
            myColumnsSized = false;

            reloadModel();

            DesktopQtDataGridTableView table = aliveTable();
            if (table != null) {
                // the columns come in the order of the controller - a column the user dragged was moved by it, or refused
                table.resetColumnOrder();
                applyAppearance(table);
                applyRowHeight(table);
                applyColumnWidths(table);
                sizeColumnsOnce(table);
                table.updateColumnMoving();
            }
        }

        @RequiredUIAccess
        @Override
        public void rowsChanged() {
            reloadModel();

            DesktopQtDataGridTableView table = aliveTable();
            if (table != null) {
                sizeColumnsOnce(table);
                table.updateColumnMoving();
            }
        }

        @RequiredUIAccess
        @Override
        public void cellsChanged(int firstViewRow, int lastViewRow) {
            GridTableModel model = aliveModel();
            if (model != null) {
                model.cellsChanged(firstViewRow, lastViewRow);
            }
        }

        @RequiredUIAccess
        @Override
        public void headersChanged() {
            GridTableModel model = aliveModel();
            if (model != null) {
                model.headersChanged();
            }
        }

        @RequiredUIAccess
        @Override
        public void selectionChanged() {
            applySelection();
        }

        @RequiredUIAccess
        @Override
        public void scrollToCell(int viewRow, int viewColumn) {
            QTableView table = aliveTable();
            GridTableModel model = aliveModel();
            if (table == null || model == null) {
                return;
            }

            if (viewRow < 0 || viewRow >= myController.getViewRowCount()) {
                return;
            }

            if (viewColumn < 0 || viewColumn >= myController.getViewColumnCount()) {
                return;
            }

            table.scrollTo(model.index(viewRow, viewColumn));
        }

        @RequiredUIAccess
        @Override
        public void editCellAt(int viewRow, int viewColumn, GridEditInitiator initiator) {
            DesktopQtDataGridTableView table = aliveTable();
            if (table != null) {
                table.editCell(viewRow, viewColumn, initiator);
            }
        }

        @RequiredUIAccess
        @Override
        public boolean stopCellEditor() {
            DesktopQtDataGridTableView table = aliveTable();
            return table == null || table.stopCellEditor();
        }

        @RequiredUIAccess
        @Override
        public void cancelCellEditor() {
            DesktopQtDataGridTableView table = aliveTable();
            if (table != null) {
                table.cancelCellEditor();
            }
        }

        @RequiredUIAccess
        @Override
        public void columnWidthsChanged() {
            QTableView table = aliveTable();
            if (table != null) {
                applyColumnWidths(table);
            }
        }

        @RequiredUIAccess
        @Override
        public void rowHeightsChanged() {
            QTableView table = aliveTable();
            if (table != null) {
                applyRowHeight(table);
                // the cells show another number of lines
                table.viewport().update();
            }
        }

        /**
         * The paging bar is a child of the layout of the panel, which gives the room of a hidden child to the table. The bar hides
         * and shows itself; this keeps the two in step whoever changed it. The columns may be dragged once the grid is not busy.
         */
        @RequiredUIAccess
        @Override
        public void toolbarVisibilityChanged() {
            boolean visible = myController.isToolbarVisible();
            if (myToolbar.isVisible() != visible) {
                myToolbar.setVisible(visible);
            }

            DesktopQtDataGridTableView table = aliveTable();
            if (table != null) {
                table.updateColumnMoving();
            }
        }
    }

    /**
     * A view of the controller for qt: the rows and columns are its view indices, and nothing is kept here. A cell is
     * handed to {@link DesktopQtTextItemDelegate} as the presentation the controller renders, so a null is painted in
     * the null style. The background of a pending change is the {@link Qt.ItemDataRole#BackgroundRole} of its cell, and of
     * the row header for the changes of its row; the text colour of a cell from the colour model of the controller is its
     * {@link Qt.ItemDataRole#ForegroundRole} - a selected cell is painted in the selection colour all the same.
     * <p/>
     * A column header shows the name of its column; the column header of the table paints the sort marker apart from it.
     * <p/>
     * A cell the controller edits is {@link Qt.ItemFlag#ItemIsEditable} - qt opens no editor for any other. {@link #setData} is not
     * implemented: the controller writes the value of an edit itself.
     */
    private final class GridTableModel extends QAbstractTableModel {
        GridTableModel(QObject parent) {
            super(parent);
        }

        @Override
        public int rowCount(QModelIndex parent) {
            return parent.isValid() ? 0 : myController.getViewRowCount();
        }

        @Override
        public int columnCount(QModelIndex parent) {
            return parent.isValid() ? 0 : myController.getViewColumnCount();
        }

        @Override
        public @Nullable Object data(QModelIndex index, int role) {
            if (!index.isValid()) {
                return null;
            }

            int row = index.row();
            int column = index.column();
            if (row >= myController.getViewRowCount() || column >= myController.getViewColumnCount()) {
                return null;
            }

            return switch (role) {
                case Qt.ItemDataRole.DisplayRole -> presentationAt(row, column).toString();
                case DesktopQtTextItemDelegate.PRESENTATION_ROLE -> presentationAt(row, column);
                case Qt.ItemDataRole.TextAlignmentRole -> toAlignment(myController.getColumnAlignment(column)).value();
                // the selection is painted over it, so a selected cell keeps the selection colour
                case Qt.ItemDataRole.BackgroundRole -> toBrush(myController.getCellBackground(row, column));
                // the delegate paints a selected cell in the selected text colour instead
                case Qt.ItemDataRole.ForegroundRole -> toBrush(myController.getCellForeground(row, column));
                default -> null;
            };
        }

        @Override
        public Qt.ItemFlags flags(QModelIndex index) {
            Qt.ItemFlags flags = super.flags(index);
            if (!index.isValid() || index.row() >= myController.getViewRowCount() || index.column() >= myController.getViewColumnCount()) {
                return flags;
            }

            return myController.isCellEditable(index.row(), index.column()) ? flags.combined(Qt.ItemFlag.ItemIsEditable) : flags;
        }

        @Override
        public @Nullable Object headerData(int section, Qt.Orientation orientation, int role) {
            if (section < 0) {
                return null;
            }

            if (orientation == Qt.Orientation.Horizontal) {
                if (section >= myController.getViewColumnCount()) {
                    return null;
                }

                return switch (role) {
                    case Qt.ItemDataRole.DisplayRole -> DesktopQtDataGridTableView.getColumnName(myController, section);
                    case Qt.ItemDataRole.ToolTipRole -> emptyToNull(myController.getColumnTooltip(section));
                    default -> null;
                };
            }

            if (section >= myController.getViewRowCount()) {
                return null;
            }

            return switch (role) {
                case Qt.ItemDataRole.DisplayRole -> myController.getRowNumberText(section);
                case Qt.ItemDataRole.TextAlignmentRole -> new Qt.Alignment(Qt.AlignmentFlag.AlignCenter).value();
                case Qt.ItemDataRole.ForegroundRole -> new QBrush(TargetQt.to(ComponentColors.DISABLED_TEXT));
                case Qt.ItemDataRole.BackgroundRole -> toBrush(myController.getRowHeaderBackground(section));
                default -> null;
            };
        }

        void reload() {
            beginResetModel();
            endResetModel();
        }

        void cellsChanged(int firstRow, int lastRow) {
            int rowCount = myController.getViewRowCount();
            int columnCount = myController.getViewColumnCount();

            int first = Math.max(0, firstRow);
            int last = Math.min(lastRow, rowCount - 1);
            if (columnCount == 0 || first > last) {
                return;
            }

            dataChanged.emit(index(first, 0), index(last, columnCount - 1));
            // the row headers show the pending changes of their rows
            headerDataChanged.emit(Qt.Orientation.Vertical, first, last);
        }

        void headersChanged() {
            int columnCount = myController.getViewColumnCount();
            if (columnCount == 0) {
                return;
            }

            headerDataChanged.emit(Qt.Orientation.Horizontal, 0, columnCount - 1);
        }

        @RequiredUIAccess
        private DesktopQtTextItemPresentation presentationAt(int row, int column) {
            DesktopQtTextItemPresentation presentation = new DesktopQtTextItemPresentation();
            // the background of a pending change is the BackgroundRole, which the style paints under the selection; the text colour
            // of the colour model is a fragment colour, which the delegate leaves out for a selected cell
            myController.renderCell(presentation, row, column, false);
            return presentation;
        }

        private static @Nullable String emptyToNull(String text) {
            return text.isEmpty() ? null : text;
        }

        /**
         * Resolved each time qt asks: a theme colour through the current style, so a repaint shows the colours of a style picked
         * meanwhile, any other colour from its own rgb.
         */
        private static @Nullable QBrush toBrush(@Nullable ColorValue color) {
            return color == null ? null : new QBrush(TargetQt.to(color));
        }
    }
}
