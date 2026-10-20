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

import consulo.desktop.qt.ui.impl.image.DesktopQtCellImage;
import consulo.desktop.qt.ui.impl.image.DesktopQtIconOwner;
import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.localize.LocalizeValue;
import consulo.ui.ComponentItemRenderer;
import consulo.ui.Length;
import consulo.ui.RenderItem;
import consulo.ui.Table;
import consulo.ui.TableColumn;
import consulo.ui.TableItemEditor;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.event.TableDoubleClickEvent;
import consulo.ui.event.TableSelectEvent;
import consulo.ui.impl.table.TableColumnImpl;
import consulo.ui.impl.table.TableColumnOwner;
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
import io.qt.gui.QMouseEvent;
import io.qt.widgets.QAbstractItemView;
import io.qt.widgets.QTableView;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-08-23
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class DesktopQtTableImpl<Item> extends QtComponentDelegate<QTableView>
    implements Table<Item>, DesktopQtIconOwner, TableColumnOwner {
    private final class TableModel extends QAbstractTableModel {
        TableModel(QObject parent) {
            super(parent);
        }

        @Override
        public int rowCount(QModelIndex parent) {
            return parent.isValid() ? 0 : myOrder.size();
        }

        @Override
        public int columnCount(QModelIndex parent) {
            return parent.isValid() ? 0 : myColumns.size();
        }

        @Override
        public @Nullable Object data(QModelIndex index, int role) {
            if (!index.isValid() || index.row() >= myOrder.size() || index.column() >= myColumns.size()) {
                return null;
            }

            int row = index.row();
            TableColumnImpl<Item, ?> column = myColumns.get(index.column());
            return switch (role) {
                case Qt.ItemDataRole.DisplayRole -> presentation(row, column).toString();
                case DesktopQtTextItemDelegate.PRESENTATION_ROLE -> presentation(row, column);
                case Qt.ItemDataRole.DecorationRole -> DesktopQtImage.toQIcon(presentation(row, column).getImage());
                case Qt.ItemDataRole.TextAlignmentRole -> DesktopQtColumnSupport.toAlignment(column.getAlignment()).value();
                case Qt.ItemDataRole.BackgroundRole -> background(row, column);
                default -> null;
            };
        }

        @Override
        public Qt.ItemFlags flags(QModelIndex index) {
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

        QModelIndex index = tableModel.index(row, 0);
        component.selectionModel().setCurrentIndex(
            index,
            QItemSelectionModel.SelectionFlag.ClearAndSelect,
            QItemSelectionModel.SelectionFlag.Rows
        );
        component.scrollTo(index);
    }

    @Override
    protected QTableView createQt(QWidget parent) {
        QTableView table = new QTableView(parent);
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

        Item pendingSelection = myPendingSelection;
        myPendingSelection = null;
        if (pendingSelection != null) {
            select(pendingSelection);
        }
    }

    private void onModelChanged(FlatDataModelEvent event) {
        myOrder.modelChanged();
        dropCachedPresentation();

        TableModel tableModel = myTableModel;
        if (tableModel == null) {
            return;
        }

        boolean sorted = myOrder.isSorted();
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
                    tableModel.rowsChanged(0, myOrder.size() - 1);
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
    }

    private void resetKeepingSelection(TableModel tableModel) {
        List<Item> selection = mySelectionSnapshot;
        tableModel.reset();
        for (Item item : selection) {
            selectRow(item, true);
        }
    }

    private @Nullable Item itemAtRow(int row) {
        return row >= 0 && row < myOrder.size() ? myOrder.get(row) : null;
    }

    private <Value> DesktopQtTextItemPresentation presentation(int row, TableColumnImpl<Item, Value> column) {
        DesktopQtTextItemPresentation cached = myCachedPresentation;
        if (cached != null && myCachedRow == row && myCachedColumn == column.getIndex()) {
            return cached;
        }

        Item item = myOrder.get(row);
        DesktopQtTextItemPresentation presentation = new DesktopQtTextItemPresentation();
        column.getRenderer().render(presentation, RenderItem.of(column.getValueProvider().apply(item), isSelected(row)), item);

        myCachedRow = row;
        myCachedColumn = column.getIndex();
        myCachedPresentation = presentation;
        return presentation;
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
            tableModel.rowsChanged(0, myOrder.size() - 1);
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

        TableModel tableModel = myTableModel;
        if (tableModel != null) {
            resetKeepingSelection(tableModel);
        }
        updateRowHeights();
        updateCellWidgets();
    }

    private void updateRowHeights() {
        QTableView component = myComponent;
        Function<Item, Length> itemHeightGetter = myItemHeightGetter;
        if (component == null || itemHeightGetter == null) {
            return;
        }

        for (int row = 0; row < myOrder.size(); row++) {
            component.setRowHeight(row, DesktopQtLength.toPixels(component, itemHeightGetter.apply(myOrder.get(row))));
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
                QModelIndex index = tableModel.index(row, column.getIndex());
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
        return component != null && component.selectionModel() != null && component.selectionModel().isRowSelected(row);
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

        if (add) {
            component.selectionModel().select(
                tableModel.index(row, 0),
                new QItemSelectionModel.SelectionFlags(QItemSelectionModel.SelectionFlag.Select, QItemSelectionModel.SelectionFlag.Rows)
            );
        }
        else {
            component.selectRow(row);
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
            component.scrollTo(tableModel.index(row, 0));
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
