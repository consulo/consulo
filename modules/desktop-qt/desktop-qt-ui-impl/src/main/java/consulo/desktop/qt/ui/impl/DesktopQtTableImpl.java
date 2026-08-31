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

import consulo.localize.LocalizeValue;
import consulo.ui.Length;
import consulo.ui.ComponentItemRender;
import consulo.ui.HorizontalAlignment;
import consulo.ui.RenderItem;
import consulo.ui.SelectionMode;
import consulo.ui.Table;
import consulo.ui.TableColumn;
import consulo.ui.TableItemEditor;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.TableDoubleClickEvent;
import consulo.ui.event.TableSelectEvent;
import consulo.ui.model.FlatDataModel;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.widgets.QAbstractItemView;
import io.qt.widgets.QHeaderView;
import io.qt.widgets.QTableWidget;
import io.qt.widgets.QTableWidgetItem;
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
public class DesktopQtTableImpl<Item> extends QtComponentDelegate<QTableWidget> implements Table<Item> {
    private final FlatDataModel<Item> myModel;

    private final List<DesktopQtTableColumnImpl<Item, ?>> myColumns = new ArrayList<>();

    private SelectionMode mySelectionMode = SelectionMode.SINGLE;
    private boolean myShowHeader = true;

    private @Nullable Function<Item, String> mySpeedSearchConverter;
    private @Nullable Function<Item, Length> myItemHeightGetter;

    /**
     * The rows the table shows, which is the model order until a sortable column is clicked - the model itself is
     * never reordered, so an item is still found by the index the caller knows it by.
     */
    private final List<Item> myRows = new ArrayList<>();

    public DesktopQtTableImpl(FlatDataModel<Item> model) {
        myModel = model;

        rebuildRows();
    }

    @Override
    protected QTableWidget createQt(QWidget parent) {
        return new QTableWidget(parent);
    }

    @Override
    protected void initialize(QTableWidget component) {
        super.initialize(component);

        component.setEditTriggers(QAbstractItemView.EditTrigger.NoEditTriggers);
        component.verticalHeader().setVisible(false);
        component.setSelectionBehavior(QAbstractItemView.SelectionBehavior.SelectRows);

        component.itemSelectionChanged.connect(() -> getListenerDispatcher(TableSelectEvent.class)
            .onEvent(new TableSelectEvent(this, getSelectedItems(), DesktopQtCurrentInput.current(component))));

        component.doubleClicked.connect(index -> {
            Item item = itemAtRow(index.row());
            if (item != null) {
                getListenerDispatcher(TableDoubleClickEvent.class)
                    .onEvent(new TableDoubleClickEvent(this, item, DesktopQtCurrentInput.current(component)));
            }
        });

        updateHeaders();
        updateColumnLayout();
        updateSortable();
        updateSelectionMode();
        updateRows();
    }

    private void rebuildRows() {
        myRows.clear();
        for (int i = 0; i < myModel.getSize(); i++) {
            myRows.add(myModel.get(i));
        }
    }

    private @Nullable Item itemAtRow(int row) {
        return row >= 0 && row < myRows.size() ? myRows.get(row) : null;
    }

    @Override
    public FlatDataModel<Item> getDataModel() {
        return myModel;
    }

    @Override
    public <Value> TableColumn<Item, Value> addColumn(LocalizeValue header, Function<Item, Value> valueProvider) {
        DesktopQtTableColumnImpl<Item, Value> column = new DesktopQtTableColumnImpl<>(this, myColumns.size(), header, valueProvider);
        myColumns.add(column);

        updateHeaders();
        updateRows();
        return column;
    }

    @Override
    public List<TableColumn<Item, ?>> getColumns() {
        return List.copyOf(myColumns);
    }

    void updateHeaders() {
        QTableWidget component = myComponent;
        if (component == null) {
            return;
        }

        component.setColumnCount(myColumns.size());

        List<String> labels = new ArrayList<>();
        for (DesktopQtTableColumnImpl<Item, ?> column : myColumns) {
            labels.add(column.getHeader().get());
        }
        component.setHorizontalHeaderLabels(labels);
        component.horizontalHeader().setVisible(myShowHeader);
    }

    void updateColumnLayout() {
        QTableWidget component = myComponent;
        if (component == null) {
            return;
        }

        QHeaderView header = component.horizontalHeader();
        for (DesktopQtTableColumnImpl<Item, ?> column : myColumns) {
            int index = column.getIndex();

            if (column.getWidth() > 0) {
                component.setColumnWidth(index, column.getWidth());
            }

            header.setSectionResizeMode(
                index,
                column.isResizable() ? QHeaderView.ResizeMode.Interactive : QHeaderView.ResizeMode.Fixed
            );
        }
    }

    void updateSortable() {
        QTableWidget component = myComponent;
        if (component == null) {
            return;
        }

        boolean sortable = myColumns.stream().anyMatch(column -> column.getComparator() != null);
        component.setSortingEnabled(false);

        if (sortable) {
            QHeaderView header = component.horizontalHeader();
            header.setSectionsClickable(true);
            header.sectionClicked.connect(this::sortByColumn);
        }
    }

    private void sortByColumn(int index) {
        DesktopQtTableColumnImpl<Item, ?> column = myColumns.get(index);

        Comparator comparator = column.getComparator();
        if (comparator == null) {
            return;
        }

        Function valueProvider = column.getValueProvider();
        myRows.sort((left, right) -> comparator.compare(valueProvider.apply(left), valueProvider.apply(right)));

        updateRows();
    }

    void updateRows() {
        QTableWidget component = myComponent;
        if (component == null) {
            return;
        }

        component.setRowCount(myRows.size());

        for (int row = 0; row < myRows.size(); row++) {
            Item item = myRows.get(row);

            if (myItemHeightGetter != null) {
                component.setRowHeight(row, DesktopQtLength.toPixels(component, myItemHeightGetter.apply(item)));
            }

            for (DesktopQtTableColumnImpl<Item, ?> column : myColumns) {
                updateCell(component, row, item, column);
            }
        }
    }

    private <Value> void updateCell(QTableWidget component, int row, Item item, DesktopQtTableColumnImpl<Item, Value> column) {
        int index = column.getIndex();
        Value value = column.getValueProvider().apply(item);

        QWidget widget = createCellWidget(row, item, column, value);
        if (widget != null) {
            component.setCellWidget(row, index, widget);
            return;
        }

        // a cell keeps whatever widget was put in it, so one left from an earlier render would sit on top of the text
        component.removeCellWidget(row, index);

        DesktopQtTextItemPresentation presentation = new DesktopQtTextItemPresentation();
        column.getTextRender().render(presentation, RenderItem.of(value, isSelected(row)));

        QTableWidgetItem cell = new QTableWidgetItem(presentation.toString());
        cell.setFlags(Qt.ItemFlag.ItemIsEnabled, Qt.ItemFlag.ItemIsSelectable);
        cell.setTextAlignment(toAlignment(column.getAlignment()));

        component.setItem(row, index, cell);
    }

    /**
     * A cell widget is live from the moment it is placed, so an editable column puts its editor straight in the cell
     * rather than a read only render - there is no separate edit mode to enter, which is why the edit triggers are off,
     * and a value listener is the only point at which the change can be committed.
     *
     * @return the widget the cell shows, or {@code null} when the column falls back to text
     */
    private <Value> @Nullable QWidget createCellWidget(int row,
                                                       Item item,
                                                       DesktopQtTableColumnImpl<Item, Value> column,
                                                       Value value) {
        TableItemEditor<Item, Value> editor = column.getEditor();
        if (editor != null && editor.isEditable(item)) {
            ValueComponent<Value> valueComponent = editor.createComponent(item);
            valueComponent.setValue(value, false);
            valueComponent.addValueListener(event -> editor.commit(item, event.getValue()));
            return toQtWidget(valueComponent);
        }

        ComponentItemRender<Value> componentRender = column.getComponentRender();
        if (componentRender != null) {
            return toQtWidget(componentRender.render(RenderItem.of(value, isSelected(row))));
        }

        return null;
    }

    private static @Nullable QWidget toQtWidget(consulo.ui.@Nullable Component component) {
        return component instanceof QtComponentDelegate<?> delegate ? delegate.toQtComponent() : null;
    }

    private static Qt.Alignment toAlignment(HorizontalAlignment alignment) {
        return switch (alignment) {
            case LEFT -> new Qt.Alignment(Qt.AlignmentFlag.AlignLeft, Qt.AlignmentFlag.AlignVCenter);
            case CENTER -> new Qt.Alignment(Qt.AlignmentFlag.AlignHCenter, Qt.AlignmentFlag.AlignVCenter);
            case RIGHT -> new Qt.Alignment(Qt.AlignmentFlag.AlignRight, Qt.AlignmentFlag.AlignVCenter);
        };
    }

    private boolean isSelected(int row) {
        QTableWidget component = myComponent;
        return component != null && component.selectionModel().isRowSelected(row);
    }

    @Override
    public void setSelectionMode(SelectionMode mode) {
        mySelectionMode = mode;

        updateSelectionMode();
    }

    private void updateSelectionMode() {
        QTableWidget component = myComponent;
        if (component == null) {
            return;
        }

        component.setSelectionMode(switch (mySelectionMode) {
            case NONE -> QAbstractItemView.SelectionMode.NoSelection;
            case SINGLE -> QAbstractItemView.SelectionMode.SingleSelection;
            case MULTIPLE -> QAbstractItemView.SelectionMode.ExtendedSelection;
        });
    }

    @Override
    public @Nullable Item getSelectedItem() {
        List<Item> items = getSelectedItems();
        return items.isEmpty() ? null : items.get(0);
    }

    @Override
    public List<Item> getSelectedItems() {
        QTableWidget component = myComponent;
        if (component == null) {
            return List.of();
        }

        List<Item> items = new ArrayList<>();
        for (io.qt.core.QModelIndex index : component.selectionModel().selectedRows()) {
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
        QTableWidget component = myComponent;
        if (component == null) {
            return;
        }

        int row = myRows.indexOf(item);
        if (row >= 0) {
            component.selectRow(row);
        }
    }

    @RequiredUIAccess
    @Override
    public void deselectAll() {
        QTableWidget component = myComponent;
        if (component != null) {
            component.clearSelection();
        }
    }

    @Override
    public void setShowHeader(boolean show) {
        myShowHeader = show;

        QTableWidget component = myComponent;
        if (component != null) {
            component.horizontalHeader().setVisible(show);
        }
    }

    @RequiredUIAccess
    @Override
    public void scrollTo(Item item) {
        QTableWidget component = myComponent;
        if (component == null) {
            return;
        }

        int row = myRows.indexOf(item);
        if (row >= 0) {
            component.scrollToItem(component.item(row, 0));
        }
    }

    @Override
    public void setSpeedSearchConverter(@Nullable Function<Item, String> converter) {
        mySpeedSearchConverter = converter;

        QTableWidget component = myComponent;
        if (component != null) {
            // the built in search of an item view reads the display role, which is what the text render already wrote
            component.setProperty("showDropIndicator", false);
        }
    }

    @Override
    public @Nullable String getSpeedSearchText() {
        return null;
    }

    @Override
    public void setItemHeightGetter(@Nullable Function<Item, Length> getter) {
        myItemHeightGetter = getter;

        updateRows();
    }

    @Override
    public void setVisibleRowCount(int count) {
        QTableWidget component = myComponent;
        if (component == null || count <= 0) {
            return;
        }

        int rowHeight = component.verticalHeader().defaultSectionSize();
        int headerHeight = myShowHeader ? component.horizontalHeader().height() : 0;

        component.setMinimumSize(new QSize(0, rowHeight * count + headerHeight));
    }
}
