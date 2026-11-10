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

import consulo.desktop.qt.ui.impl.clipboard.DesktopQtDragAndDropSupport;
import consulo.desktop.qt.ui.impl.clipboard.DesktopQtDrop;
import consulo.desktop.qt.ui.impl.clipboard.DesktopQtTransferTarget;
import consulo.desktop.qt.ui.impl.font.DesktopQtFontImpl;
import consulo.desktop.qt.ui.impl.image.DesktopQtCellImage;
import consulo.desktop.qt.ui.impl.image.DesktopQtIconOwner;
import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.localize.LocalizeValue;
import consulo.ui.ComponentItemRenderer;
import consulo.ui.DragAndDropTransferHandler;
import consulo.ui.Length;
import consulo.ui.RenderItem;
import consulo.ui.Table;
import consulo.ui.TableColumn;
import consulo.ui.TableItemEditor;
import consulo.ui.TransferHandler;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.event.TableDoubleClickEvent;
import consulo.ui.event.TableSelectEvent;
import consulo.ui.impl.table.TableColumnImpl;
import consulo.ui.impl.table.TableColumnOwner;
import consulo.ui.impl.table.TableDetails;
import consulo.ui.impl.table.TableRowOrder;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.FlatDataModelEvent;
import io.qt.core.QAbstractTableModel;
import io.qt.core.QEvent;
import io.qt.core.QItemSelectionModel;
import io.qt.core.QModelIndex;
import io.qt.core.QObject;
import io.qt.core.QPoint;
import io.qt.core.QRect;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.gui.QBrush;
import io.qt.gui.QDragEnterEvent;
import io.qt.gui.QDragLeaveEvent;
import io.qt.gui.QDragMoveEvent;
import io.qt.gui.QDropEvent;
import io.qt.gui.QFont;
import io.qt.gui.QMouseEvent;
import io.qt.gui.QPaintEvent;
import io.qt.widgets.QAbstractItemView;
import io.qt.widgets.QTableView;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-08-23
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class DesktopQtTableImpl<Item> extends QtComponentDelegate<QTableView>
    implements Table<Item>, DesktopQtIconOwner, TableColumnOwner, DesktopQtTransferTarget<Item> {
    private static final class QtTable extends QTableView {
        private final DesktopQtDragAndDropSupport<?> myDragAndDrop;
        private @Nullable Runnable myGeometryListener;

        private QtTable(QWidget parent, DesktopQtDragAndDropSupport<?> dragAndDrop) {
            super(parent);
            myDragAndDrop = dragAndDrop;
        }

        @Override
        protected void startDrag(Qt.DropActions supportedActions) {
            myDragAndDrop.startDrag(this);
        }

        @Override
        protected void dragEnterEvent(QDragEnterEvent event) {
            myDragAndDrop.dragEnter(this, event);
        }

        @Override
        protected void dragMoveEvent(QDragMoveEvent event) {
            myDragAndDrop.dragMove(this, event);
            if (myDragAndDrop.shouldAutoScroll(this, event.position().toPoint())) {
                startAutoScroll();
            }
        }

        @Override
        protected void dragLeaveEvent(QDragLeaveEvent event) {
            stopAutoScroll();
            myDragAndDrop.dragLeave(this);
        }

        @Override
        protected void dropEvent(QDropEvent event) {
            stopAutoScroll();
            myDragAndDrop.drop(this, event);
        }

        @Override
        protected void paintEvent(QPaintEvent event) {
            super.paintEvent(event);
            myDragAndDrop.paintIndicator(this);
        }

        @Override
        protected void scrollContentsBy(int dx, int dy) {
            super.scrollContentsBy(dx, dy);
            myDragAndDrop.contentsScrolled(this);
            geometryChanged();
        }

        @Override
        protected void updateGeometries() {
            super.updateGeometries();
            geometryChanged();
        }

        private void geometryChanged() {
            Runnable listener = myGeometryListener;
            if (listener != null) {
                listener.run();
            }
        }
    }

    private final class TableModel extends QAbstractTableModel {
        TableModel(QObject parent) {
            super(parent);
        }

        @Override
        public int rowCount(QModelIndex parent) {
            return parent.isValid() ? 0 : viewRowCount();
        }

        @Override
        public int columnCount(QModelIndex parent) {
            return parent.isValid() ? 0 : myColumns.size();
        }

        @Override
        public @Nullable Object data(QModelIndex index, int role) {
            int row = index.isValid() ? orderRowAt(index.row()) : -1;
            if (row < 0 || index.column() >= myColumns.size()) {
                return null;
            }

            TableColumnImpl<Item, ?> column = myColumns.get(index.column());
            return switch (role) {
                case Qt.ItemDataRole.DisplayRole -> presentation(row, column).toString();
                case DesktopQtTextItemDelegate.PRESENTATION_ROLE -> presentation(row, column);
                case Qt.ItemDataRole.DecorationRole -> DesktopQtImage.toQIcon(presentation(row, column).getImage());
                case Qt.ItemDataRole.TextAlignmentRole -> DesktopQtColumnSupport.toAlignment(column.getAlignment()).value();
                case Qt.ItemDataRole.BackgroundRole -> background(row, column);
                case Qt.ItemDataRole.FontRole -> columnFont(column);
                default -> null;
            };
        }

        @Override
        public Qt.ItemFlags flags(QModelIndex index) {
            // a details row is neither selectable nor enabled, so the selection and the cursor keys pass it by
            if (detailsOrderRowAt(index.row()) >= 0) {
                return new Qt.ItemFlags(Qt.ItemFlag.NoItemFlags);
            }
            // a view starts a drag only from a row its model calls draggable
            if (myTransferHandler instanceof DragAndDropTransferHandler) {
                return new Qt.ItemFlags(Qt.ItemFlag.ItemIsEnabled, Qt.ItemFlag.ItemIsSelectable, Qt.ItemFlag.ItemIsDragEnabled);
            }
            return new Qt.ItemFlags(Qt.ItemFlag.ItemIsEnabled, Qt.ItemFlag.ItemIsSelectable);
        }

        @Override
        public @Nullable Object headerData(int section, Qt.Orientation orientation, int role) {
            if (orientation != Qt.Orientation.Horizontal || role != Qt.ItemDataRole.DisplayRole || section < 0 || section >= myColumns.size()) {
                return null;
            }
            return myColumns.get(section).getHeader().get();
        }

        void reset() {
            beginResetModel();
            endResetModel();
        }

        void rowsInserted(int first, int last) {
            beginInsertRows(new QModelIndex(), first, last);
            endInsertRows();
        }

        void rowsRemoved(int first, int last) {
            beginRemoveRows(new QModelIndex(), first, last);
            endRemoveRows();
        }

        void insertRow(int row, Runnable change) {
            myRowsChanging = true;
            try {
                beginInsertRows(new QModelIndex(), row, row);
                change.run();
                endInsertRows();
            }
            finally {
                myRowsChanging = false;
            }
            runPendingDetailsChanges();
        }

        void removeRow(int row, Runnable change) {
            myRowsChanging = true;
            try {
                beginRemoveRows(new QModelIndex(), row, row);
                change.run();
                endRemoveRows();
            }
            finally {
                myRowsChanging = false;
            }
            runPendingDetailsChanges();
        }

        void rowsChanged(int first, int last) {
            if (myColumns.isEmpty() || first > last) {
                return;
            }
            dataChanged.emit(index(first, 0), index(last, myColumns.size() - 1));
        }

        void insertColumn(TableColumnImpl<Item, ?> column) {
            beginInsertColumns(new QModelIndex(), myColumns.size(), myColumns.size());
            myColumns.add(column);
            endInsertColumns();
        }

        void columnsChanged() {
            headerDataChanged.emit(Qt.Orientation.Horizontal, 0, Math.max(0, myColumns.size() - 1));
        }
    }

    private final TableRowOrder<Item> myOrder;
    private @Nullable Function<Item, ColorValue> myRowBackgroundGetter;

    private final List<TableColumnImpl<Item, ?>> myColumns = new ArrayList<>();

    private @Nullable TransferHandler<Item> myTransferHandler;
    private final DesktopQtDragAndDropSupport<Item> myDragAndDrop = new DesktopQtDragAndDropSupport<>(this, this);

    private boolean myAllowMultipleSelect;
    private boolean myShowHeader = true;

    private @Nullable Item myPendingSelection;

    private final DesktopQtSpeedSearch<Item> mySpeedSearch;
    private @Nullable Function<Item, Length> myItemHeightGetter;

    private @Nullable TableModel myTableModel;
    private @Nullable DesktopQtTextItemDelegate myDelegate;
    private @Nullable DesktopQtCellImage myHoveredCellImage;
    private long myHoveredCellKey = -1;
    private List<Item> mySelectionSnapshot = List.of();

    private final TableDetails<Item> myDetails = new TableDetails<>();
    private final Map<consulo.ui.Component, QWidget> myDetailsWidgets = new HashMap<>();
    private int @Nullable [] myViewRows;
    private int @Nullable [] myOrderToView;
    private boolean myRowsChanging;
    private final List<Runnable> myPendingDetailsChanges = new ArrayList<>();

    private int myCachedRow = -1;
    private int myCachedColumn = -1;
    private @Nullable DesktopQtTextItemPresentation myCachedPresentation;

    public DesktopQtTableImpl(FlatDataModel<Item> model) {
        myOrder = new TableRowOrder<>(model);
        mySpeedSearch = new DesktopQtSpeedSearch<>(false, this::rows, this::leadItem, this::selectFound);

        model.addListener(this::onModelChanged);
    }

    private List<Item> rows() {
        return myOrder.fetch(0, myOrder.size());
    }

    private @Nullable Item leadItem() {
        QTableView component = myComponent;
        if (component == null || component.isDisposed() || component.selectionModel() == null) {
            return null;
        }

        QModelIndex current = component.currentIndex();
        if (current.isValid() && component.selectionModel().isRowSelected(current.row(), current.parent())) {
            return itemAtRow(current.row());
        }
        return getSelectedItem();
    }

    private void selectFound(Item item) {
        QTableView component = myComponent;
        TableModel tableModel = myTableModel;
        int row = myOrder.rowOf(item);
        if (component == null || component.isDisposed() || tableModel == null || row < 0) {
            return;
        }

        QModelIndex index = tableModel.index(viewRowOf(row), 0);
        component.selectionModel().setCurrentIndex(
            index,
            QItemSelectionModel.SelectionFlag.ClearAndSelect,
            QItemSelectionModel.SelectionFlag.Rows
        );
        component.scrollTo(index);
    }

    @Override
    protected QTableView createQt(QWidget parent) {
        QtTable table = new QtTable(parent, myDragAndDrop);
        table.myGeometryListener = () -> layoutDetails(table);
        TableModel tableModel = new TableModel(table);
        myTableModel = tableModel;
        table.setModel(tableModel);
        DesktopQtTextItemDelegate delegate = new DesktopQtTextItemDelegate(table);
        myDelegate = delegate;
        table.setItemDelegate(delegate);
        return table;
    }

    @Override
    protected void initialize(QTableView component) {
        super.initialize(component);

        component.setEditTriggers(QAbstractItemView.EditTrigger.NoEditTriggers);
        component.verticalHeader().setVisible(false);
        component.setSelectionBehavior(QAbstractItemView.SelectionBehavior.SelectRows);
        component.setWordWrap(false);

        component.selectionModel().selectionChanged.connect((selected, deselected) -> {
            dropCachedPresentation();
            component.viewport().update();
            List<Item> selection = getSelectedItems();
            mySelectionSnapshot = selection;
            getListenerDispatcher(TableSelectEvent.class)
                .onEvent(new TableSelectEvent(this, selection, DesktopQtCurrentInput.current(component)));
        });

        component.horizontalHeader().sectionClicked.connect(this::sortByColumn);

        mySpeedSearch.install(component);
        myDragAndDrop.install(component);

        component.viewport().setMouseTracking(true);
        component.viewport().installEventFilter(new QObject(component) {
            @Override
            public boolean eventFilter(QObject watched, QEvent event) {
                QEvent.Type type = event.type();
                if (type == QEvent.Type.MouseMove && event instanceof QMouseEvent mouse) {
                    onCellImageMouse(component, mouse.position().toPoint(), false);
                }
                else if (type == QEvent.Type.MouseButtonRelease
                    && event instanceof QMouseEvent mouse
                    && mouse.button() == Qt.MouseButton.LeftButton) {
                    onCellImageMouse(component, mouse.position().toPoint(), true);
                }
                else if (type == QEvent.Type.Leave) {
                    exitHoveredCellImage();
                }
                return false;
            }
        });

        component.doubleClicked.connect(index -> {
            Item item = itemAtRow(index.row());
            if (item != null) {
                getListenerDispatcher(TableDoubleClickEvent.class)
                    .onEvent(new TableDoubleClickEvent(this, item, DesktopQtCurrentInput.current(component)));
            }
        });

        component.horizontalHeader().setVisible(myShowHeader);
        updateColumnLayout();
        updateSortable();
        updateSelectionMode();
        updateRowHeights();
        updateCellWidgets();
        updateDetails();

        Item pendingSelection = myPendingSelection;
        myPendingSelection = null;
        if (pendingSelection != null) {
            select(pendingSelection);
        }
    }

    private void onModelChanged(FlatDataModelEvent event) {
        myOrder.modelChanged();
        dropCachedPresentation();

        boolean mapped = myViewRows != null;
        for (consulo.ui.Component details : myDetails.retain(item -> myOrder.getModel().indexOf(item) >= 0)) {
            disposeDetails(details);
        }
        rebuildViewRows();

        TableModel tableModel = myTableModel;
        if (tableModel == null || tableModel.isDisposed()) {
            return;
        }

        // the rows of an event are model rows, which say nothing about where details rows went
        boolean sorted = myOrder.isSorted() || mapped || myViewRows != null;
        switch (event.getType()) {
            case ADDED -> {
                if (sorted) {
                    resetKeepingSelection(tableModel);
                }
                else {
                    tableModel.rowsInserted(event.getFromIndex(), event.getToIndex());
                }
            }
            case REMOVED -> {
                if (sorted) {
                    resetKeepingSelection(tableModel);
                }
                else {
                    tableModel.rowsRemoved(event.getFromIndex(), event.getToIndex());
                }
            }
            case UPDATED -> {
                if (sorted) {
                    tableModel.rowsChanged(0, viewRowCount() - 1);
                }
                else {
                    tableModel.rowsChanged(event.getFromIndex(), event.getToIndex());
                }
            }
            case RESET -> {
                tableModel.reset();
                if (!mySelectionSnapshot.isEmpty()) {
                    mySelectionSnapshot = List.of();
                    getListenerDispatcher(TableSelectEvent.class).onEvent(new TableSelectEvent(this, List.of()));
                }
            }
        }

        updateRowHeights();
        updateCellWidgets();
        updateDetails();
    }

    private void resetKeepingSelection(TableModel tableModel) {
        List<Item> selection = mySelectionSnapshot;
        tableModel.reset();
        for (Item item : selection) {
            selectRow(item, true);
        }
    }

    private @Nullable Item itemAtRow(int viewRow) {
        int row = orderRowAt(viewRow);
        return row >= 0 ? myOrder.get(row) : null;
    }

    private void rebuildViewRows() {
        List<Item> items = myDetails.getItems();
        if (items.isEmpty()) {
            myViewRows = null;
            myOrderToView = null;
            return;
        }

        int size = myOrder.size();
        boolean[] withDetails = new boolean[size];
        int count = 0;
        for (Item item : items) {
            int row = myOrder.rowOf(item);
            if (row >= 0 && !withDetails[row]) {
                withDetails[row] = true;
                count++;
            }
        }

        int[] viewRows = new int[size + count];
        int[] orderToView = new int[size];
        int viewRow = 0;
        for (int row = 0; row < size; row++) {
            orderToView[row] = viewRow;
            viewRows[viewRow++] = row;
            if (withDetails[row]) {
                viewRows[viewRow++] = -(row + 1);
            }
        }
        myViewRows = viewRows;
        myOrderToView = orderToView;
    }

    private int viewRowCount() {
        int[] viewRows = myViewRows;
        return viewRows == null ? myOrder.size() : viewRows.length;
    }

    private int orderRowAt(int viewRow) {
        int[] viewRows = myViewRows;
        if (viewRow < 0) {
            return -1;
        }
        if (viewRows == null) {
            return viewRow < myOrder.size() ? viewRow : -1;
        }
        return viewRow < viewRows.length && viewRows[viewRow] >= 0 ? viewRows[viewRow] : -1;
    }

    private int detailsOrderRowAt(int viewRow) {
        int[] viewRows = myViewRows;
        return viewRows != null && viewRow >= 0 && viewRow < viewRows.length && viewRows[viewRow] < 0 ? -(viewRows[viewRow] + 1) : -1;
    }

    private int viewRowOf(int row) {
        int[] orderToView = myOrderToView;
        if (row < 0) {
            return -1;
        }
        if (orderToView == null) {
            return row;
        }
        return row < orderToView.length ? orderToView[row] : -1;
    }

    private <Value> DesktopQtTextItemPresentation presentation(int row, TableColumnImpl<Item, Value> column) {
        DesktopQtTextItemPresentation cached = myCachedPresentation;
        if (cached != null && myCachedRow == row && myCachedColumn == column.getIndex()) {
            return cached;
        }

        Item item = myOrder.get(row);
        DesktopQtTextItemPresentation presentation = new DesktopQtTextItemPresentation();
        if (!hasCellWidget(item, column)) {
            column.getRenderer().render(presentation, RenderItem.of(column.getValueProvider().apply(item), isSelected(row)), item);
        }

        myCachedRow = row;
        myCachedColumn = column.getIndex();
        myCachedPresentation = presentation;
        return presentation;
    }

    private <Value> boolean hasCellWidget(Item item, TableColumnImpl<Item, Value> column) {
        TableItemEditor<Item, Value> editor = column.getEditor();
        return column.getComponentRenderer() != null || editor != null && editor.isEditable(item);
    }

    private @Nullable QFont columnFont(TableColumnImpl<Item, ?> column) {
        QTableView view = toQtComponent();
        if (view == null || !column.hasFontBuilder()) {
            return null;
        }
        return DesktopQtTextItemDelegate.toQFont(column.getFont(DesktopQtFontImpl.of(view.font())));
    }

    private void dropCachedPresentation() {
        myCachedPresentation = null;
        myCachedRow = -1;
        myCachedColumn = -1;
    }

    private @Nullable QBrush background(int row, TableColumnImpl<Item, ?> column) {
        ColorValue cellBackground = presentation(row, column).getBackgroundColor();
        if (cellBackground != null) {
            return DesktopQtTextItemDelegate.toBrush(cellBackground);
        }

        // the band belongs to the row, so it goes under every column; a selected row keeps the selection fill
        Function<Item, ColorValue> rowBackgroundGetter = myRowBackgroundGetter;
        if (rowBackgroundGetter == null || isSelected(row)) {
            return null;
        }

        ColorValue rowBackground = rowBackgroundGetter.apply(myOrder.get(row));
        return rowBackground == null ? null : new QBrush(TargetQt.to(rowBackground));
    }

    @Override
    public FlatDataModel<Item> getDataModel() {
        return myOrder.getModel();
    }

    @Override
    public void setRowBackgroundGetter(@Nullable Function<Item, ColorValue> getter) {
        myRowBackgroundGetter = getter;
        refreshAllRows();
    }

    @Override
    public <Value> TableColumn<Item, Value> addColumn(LocalizeValue header, Function<Item, Value> valueProvider) {
        TableColumnImpl<Item, Value> column = new TableColumnImpl<>(this, myColumns.size(), header, valueProvider);

        TableModel tableModel = myTableModel;
        if (tableModel != null) {
            tableModel.insertColumn(column);
        }
        else {
            myColumns.add(column);
        }

        updateColumnLayout();
        updateCellWidgets();
        return column;
    }

    @Override
    public List<TableColumn<Item, ?>> getColumns() {
        return List.copyOf(myColumns);
    }

    @Override
    public void headerChanged() {
        TableModel tableModel = myTableModel;
        if (tableModel != null) {
            tableModel.columnsChanged();
        }
    }

    @Override
    public void rendererChanged() {
        refreshAllRows();
        updateCellWidgets();
    }

    @Override
    public void layoutChanged() {
        updateColumnLayout();
    }

    @Override
    public void sortChanged() {
        updateSortable();
    }

    private void refreshAllRows() {
        dropCachedPresentation();

        TableModel tableModel = myTableModel;
        if (tableModel != null) {
            tableModel.rowsChanged(0, viewRowCount() - 1);
        }
    }

    private void updateColumnLayout() {
        QTableView component = myComponent;
        if (component == null) {
            return;
        }

        DesktopQtColumnSupport.applyLayout(component.horizontalHeader(), myColumns);
    }

    private void updateSortable() {
        QTableView component = myComponent;
        if (component == null) {
            return;
        }

        component.setSortingEnabled(false);

        if (myColumns.stream().anyMatch(TableColumnImpl::isSortable)) {
            component.horizontalHeader().setSectionsClickable(true);
        }
    }

    private void sortByColumn(int index) {
        if (index < 0 || index >= myColumns.size()) {
            return;
        }

        TableColumnImpl<Item, ?> column = myColumns.get(index);

        Comparator comparator = column.getComparator();
        if (comparator == null) {
            return;
        }

        Function valueProvider = column.getValueProvider();
        dropCachedPresentation();
        myOrder.sort((left, right) -> comparator.compare(valueProvider.apply(left), valueProvider.apply(right)));
        rebuildViewRows();

        TableModel tableModel = myTableModel;
        if (tableModel != null) {
            resetKeepingSelection(tableModel);
        }
        updateRowHeights();
        updateCellWidgets();
        updateDetails();
    }

    private void updateRowHeights() {
        QTableView component = myComponent;
        Function<Item, Length> itemHeightGetter = myItemHeightGetter;
        if (component == null || itemHeightGetter == null) {
            return;
        }

        for (int row = 0; row < myOrder.size(); row++) {
            component.setRowHeight(viewRowOf(row), DesktopQtLength.toPixels(component, itemHeightGetter.apply(myOrder.get(row))));
        }
    }

    /**
     * A cell widget is live from the moment it is placed, so an editable column puts its editor straight in the cell
     * rather than a read only renderer - there is no separate edit mode to enter, which is why the edit triggers are off,
     * and a value listener is the only point at which the change can be committed.
     */
    private void updateCellWidgets() {
        QTableView component = myComponent;
        TableModel tableModel = myTableModel;
        if (component == null || tableModel == null) {
            return;
        }

        for (TableColumnImpl<Item, ?> column : myColumns) {
            if (column.getEditor() == null && column.getComponentRenderer() == null) {
                continue;
            }

            for (int row = 0; row < myOrder.size(); row++) {
                QModelIndex index = tableModel.index(viewRowOf(row), column.getIndex());
                QWidget widget = createCellWidget(component, row, myOrder.get(row), column);
                component.setIndexWidget(index, widget);
            }
        }
    }

    @RequiredUIAccess
    private void onCellImageMouse(QTableView component, QPoint position, boolean click) {
        QModelIndex index = component.indexAt(position);
        DesktopQtCellImage image = null;
        QRect rect = null;
        DesktopQtTextItemDelegate delegate = myDelegate;
        if (index.isValid() && delegate != null) {
            QRect painted = delegate.getCellImageRect(index);
            if (painted != null
                && painted.contains(position)
                && index.data(DesktopQtTextItemDelegate.PRESENTATION_ROLE) instanceof DesktopQtTextItemPresentation presentation
                && presentation.getImage() instanceof DesktopQtCellImage cellImage) {
                image = cellImage;
                rect = painted;
            }
        }

        long key = image == null ? -1 : ((long) index.row() << 32) | (index.column() & 0xffffffffL);
        if (key != myHoveredCellKey) {
            exitHoveredCellImage();
        }
        if (image == null || rect == null) {
            return;
        }

        myHoveredCellKey = key;
        myHoveredCellImage = image;
        int x = position.x() - rect.left();
        int y = position.y() - rect.top();
        if (click) {
            image.mouseClicked(x, y, rect.height());
        }
        else {
            image.mouseMoved(x, y, rect.height());
        }
    }

    @RequiredUIAccess
    private void exitHoveredCellImage() {
        DesktopQtCellImage hovered = myHoveredCellImage;
        myHoveredCellImage = null;
        myHoveredCellKey = -1;
        if (hovered != null) {
            hovered.mouseExited();
        }
    }

    private <Value> @Nullable QWidget createCellWidget(QTableView table, int row, Item item, TableColumnImpl<Item, Value> column) {
        Value value = column.getValueProvider().apply(item);

        TableItemEditor<Item, Value> editor = column.getEditor();
        if (editor != null && editor.isEditable(item)) {
            ValueComponent<Value> valueComponent = editor.createComponent(item);
            valueComponent.setValue(value, false);
            valueComponent.addValueListener(event -> editor.commit(item, event.getValue()));
            return toQtWidget(table, valueComponent);
        }

        ComponentItemRenderer<Value> componentRenderer = column.getComponentRenderer();
        if (componentRenderer != null) {
            return toQtWidget(table, componentRenderer.render(RenderItem.of(value, isSelected(row))));
        }

        return null;
    }

    private @Nullable QWidget toQtWidget(QTableView table, consulo.ui.@Nullable Component component) {
        if (!(component instanceof QtComponentDelegate<?> delegate)) {
            return null;
        }

        delegate.setParent(this);
        delegate.bind(table.viewport(), null);
        return delegate.toQtComponent();
    }

    @RequiredUIAccess
    @Override
    public void refreshIcons() {
        QTableView component = myComponent;
        if (component == null || component.isDisposed()) {
            return;
        }

        refreshAllRows();
    }

    private boolean isSelected(int row) {
        QTableView component = myComponent;
        return component != null && component.selectionModel() != null && component.selectionModel().isRowSelected(viewRowOf(row));
    }

    @Override
    public void setAllowMultipleSelect(boolean allow) {
        myAllowMultipleSelect = allow;

        updateSelectionMode();
    }

    private void updateSelectionMode() {
        QTableView component = myComponent;
        if (component == null) {
            return;
        }

        component.setSelectionMode(
            myAllowMultipleSelect ? QAbstractItemView.SelectionMode.ExtendedSelection : QAbstractItemView.SelectionMode.SingleSelection
        );
    }

    @Override
    public @Nullable Item getSelectedItem() {
        List<Item> items = getSelectedItems();
        return items.isEmpty() ? null : items.get(0);
    }

    @Override
    public List<Item> getSelectedItems() {
        QTableView component = myComponent;
        if (component == null || component.selectionModel() == null) {
            return List.of();
        }

        List<Item> items = new ArrayList<>();
        for (QModelIndex index : component.selectionModel().selectedRows()) {
            Item item = itemAtRow(index.row());
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    @RequiredUIAccess
    @Override
    public void select(Item item) {
        if (myComponent == null) {
            myPendingSelection = item;
            return;
        }

        selectRow(item, false);
    }

    private void selectRow(Item item, boolean add) {
        QTableView component = myComponent;
        TableModel tableModel = myTableModel;
        if (component == null || tableModel == null) {
            return;
        }

        int row = myOrder.rowOf(item);
        if (row < 0) {
            return;
        }

        int viewRow = viewRowOf(row);
        if (add) {
            component.selectionModel().select(
                tableModel.index(viewRow, 0),
                new QItemSelectionModel.SelectionFlags(QItemSelectionModel.SelectionFlag.Select, QItemSelectionModel.SelectionFlag.Rows)
            );
        }
        else {
            component.selectRow(viewRow);
        }
    }

    @RequiredUIAccess
    @Override
    public void deselectAll() {
        myPendingSelection = null;

        QTableView component = myComponent;
        if (component != null) {
            component.clearSelection();
        }
    }

    @Override
    public void setShowHeader(boolean show) {
        myShowHeader = show;

        QTableView component = myComponent;
        if (component != null) {
            component.horizontalHeader().setVisible(show);
        }
    }

    @RequiredUIAccess
    @Override
    public void scrollTo(Item item) {
        QTableView component = myComponent;
        TableModel tableModel = myTableModel;
        if (component == null || tableModel == null) {
            return;
        }

        int row = myOrder.rowOf(item);
        if (row >= 0) {
            component.scrollTo(tableModel.index(viewRowOf(row), 0));
        }
    }

    @Override
    public void setDetailsRenderer(@Nullable Function<Item, consulo.ui.Component> renderer) {
        boolean mapped = myViewRows != null;
        for (consulo.ui.Component details : myDetails.setRenderer(renderer)) {
            disposeDetails(details);
        }
        rebuildViewRows();

        TableModel tableModel = myTableModel;
        if (mapped && tableModel != null && !tableModel.isDisposed()) {
            dropCachedPresentation();
            resetKeepingSelection(tableModel);
            updateRowHeights();
            updateCellWidgets();
            updateDetails();
        }
    }

    @RequiredUIAccess
    @Override
    public void setDetailsVisible(Item item, boolean visible) {
        // a row change moves the selection, and a select listener showing details would come back in before the view
        // has taken the change
        if (myRowsChanging) {
            myPendingDetailsChanges.add(() -> setDetailsVisible(item, visible));
            return;
        }

        TableModel tableModel = myTableModel;
        boolean live = tableModel != null && !tableModel.isDisposed();
        if (visible) {
            if (myDetails.isShown(item) || myDetails.show(item) == null) {
                return;
            }

            int row = myOrder.rowOf(item);
            if (live && row >= 0) {
                tableModel.insertRow(viewRowOf(row) + 1, this::rebuildViewRows);
            }
            else {
                rebuildViewRows();
            }
        }
        else {
            int row = myOrder.rowOf(item);
            int detailsRow = row >= 0 && myDetails.isShown(item) ? viewRowOf(row) + 1 : -1;
            consulo.ui.Component details = myDetails.hide(item);
            if (details == null) {
                return;
            }

            disposeDetails(details);
            if (live && detailsRow >= 0) {
                tableModel.removeRow(detailsRow, this::rebuildViewRows);
            }
            else {
                rebuildViewRows();
            }
        }

        dropCachedPresentation();
        updateDetails();
    }

    @Override
    public boolean isDetailsVisible(Item item) {
        return myDetails.isShown(item);
    }

    @RequiredUIAccess
    private void runPendingDetailsChanges() {
        while (!myRowsChanging && !myPendingDetailsChanges.isEmpty()) {
            myPendingDetailsChanges.remove(0).run();
        }
    }

    private void disposeDetails(consulo.ui.Component details) {
        myDetailsWidgets.remove(details);
        if (details instanceof QtComponentDelegate<?> delegate) {
            delegate.disposeQt();
        }
    }

    /**
     * A details row spans every column, and its component is a child of the viewport laid over that row rather than
     * an index widget - the view deletes index widgets whenever its model resets, and the details would lose their
     * state with them.
     */
    private void updateDetails() {
        QTableView component = myComponent;
        if (component == null || component.isDisposed()) {
            return;
        }

        component.clearSpans();
        int[] viewRows = myViewRows;
        if (viewRows == null) {
            return;
        }

        int columns = myColumns.size();
        for (int viewRow = 0; viewRow < viewRows.length; viewRow++) {
            int row = detailsOrderRowAt(viewRow);
            if (row < 0) {
                continue;
            }

            if (columns > 1) {
                component.setSpan(viewRow, 0, 1, columns);
            }

            consulo.ui.Component details = myDetails.get(myOrder.get(row));
            if (details instanceof QtComponentDelegate<?> delegate) {
                delegate.setParent(this);
                delegate.bind(component.viewport(), null);
                QWidget widget = delegate.toQtComponent();
                if (widget != null && myDetailsWidgets.get(details) != widget) {
                    myDetailsWidgets.put(details, widget);
                    widget.installEventFilter(new QObject(widget) {
                        @Override
                        public boolean eventFilter(QObject watched, QEvent event) {
                            if (event.type() == QEvent.Type.LayoutRequest) {
                                layoutDetails(component);
                            }
                            return false;
                        }
                    });
                }
                if (widget != null) {
                    widget.show();
                }
            }
        }

        layoutDetails(component);
    }

    /**
     * The heights go in first and the bounds after, since a row grown above moves every row under it.
     */
    private void layoutDetails(QTableView component) {
        int[] viewRows = myViewRows;
        // between the begin and the end of a row change the rows of the view and the sections of its header disagree
        if (viewRows == null || myRowsChanging || component.isDisposed()) {
            return;
        }

        int width = component.viewport().width();
        List<Integer> detailsRows = new ArrayList<>();
        for (int viewRow = 0; viewRow < viewRows.length; viewRow++) {
            int row = detailsOrderRowAt(viewRow);
            QWidget widget = row < 0 ? null : myDetailsWidgets.get(myDetails.get(myOrder.get(row)));
            if (widget == null || widget.isDisposed()) {
                continue;
            }

            int height = widget.hasHeightForWidth() ? widget.heightForWidth(width) : widget.sizeHint().height();
            if (component.rowHeight(viewRow) != height) {
                component.setRowHeight(viewRow, height);
            }
            detailsRows.add(viewRow);
        }

        for (int viewRow : detailsRows) {
            QWidget widget = myDetailsWidgets.get(myDetails.get(myOrder.get(detailsOrderRowAt(viewRow))));
            if (widget != null) {
                widget.setGeometry(0, component.rowViewportPosition(viewRow), width, component.rowHeight(viewRow));
            }
        }
    }

    @Override
    public void setSpeedSearchConverter(@Nullable Function<Item, String> converter) {
        mySpeedSearch.setConverter(converter);
    }

    @Override
    public @Nullable String getSpeedSearchText() {
        return mySpeedSearch.getText();
    }

    @Override
    public void setItemHeightGetter(@Nullable Function<Item, Length> getter) {
        myItemHeightGetter = getter;

        updateRowHeights();
    }

    @Override
    public void setTransferHandler(@Nullable TransferHandler<Item> handler) {
        myTransferHandler = handler;
        myDragAndDrop.setHandler(handler, myComponent);
    }

    @Override
    public @Nullable TransferHandler<Item> getTransferHandler() {
        return myTransferHandler;
    }

    @Override
    public List<Item> getTransferItems() {
        return getSelectedItems();
    }

    @Override
    public @Nullable DesktopQtDrop<Item> resolveDrop(QPoint position) {
        QTableView component = myComponent;
        if (component == null || component.isDisposed() || myOrder.size() == 0) {
            return null;
        }

        QModelIndex index = component.indexAt(position);
        if (!index.isValid()) {
            // past the last row the drop lands below it, like a list
            int last = myOrder.size() - 1;
            return new DesktopQtDrop<>(myOrder.get(last), DragAndDropTransferHandler.DropPosition.BELOW,
                DesktopQtDragAndDropSupport.lineBelow(rowRect(component, viewRowOf(last))));
        }

        Item item = itemAtRow(index.row());
        if (item == null) {
            return null;
        }

        QRect rect = rowRect(component, index.row());
        DragAndDropTransferHandler.DropPosition dropPosition = DesktopQtDragAndDropSupport.positionOf(rect, position);
        return switch (dropPosition) {
            case ABOVE -> new DesktopQtDrop<>(item, dropPosition, DesktopQtDragAndDropSupport.lineAbove(rect));
            case BELOW -> new DesktopQtDrop<>(item, dropPosition, DesktopQtDragAndDropSupport.lineBelow(rect));
            case INTO -> new DesktopQtDrop<>(item, dropPosition, rect);
        };
    }

    /**
     * The whole row in viewport coordinates - a cell rect would draw the indicator under one column only.
     */
    private static QRect rowRect(QTableView component, int row) {
        return new QRect(0, component.rowViewportPosition(row), component.viewport().width(), component.rowHeight(row));
    }

    @Override
    public void setVisibleRowCount(int count) {
        QTableView component = myComponent;
        if (component == null || count <= 0) {
            return;
        }

        int rowHeight = component.verticalHeader().defaultSectionSize();
        int headerHeight = myShowHeader ? component.horizontalHeader().height() : 0;

        component.setMinimumSize(new QSize(0, rowHeight * count + headerHeight));
    }
}
