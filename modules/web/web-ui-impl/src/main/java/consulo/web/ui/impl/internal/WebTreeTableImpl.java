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
package consulo.web.ui.impl.internal;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import consulo.localize.LocalizeValue;
import consulo.ui.TableColumn;
import consulo.ui.TreeExecutor;
import consulo.ui.TreeModel;
import consulo.ui.TreeTable;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.impl.tree.TreeNodeImpl;
import consulo.ui.impl.tree.TreeTableColumns;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class WebTreeTableImpl<E> extends WebTreeImpl<E> implements TreeTable<E> {
    private final TreeTableColumns<E> myColumns = new TreeTableColumns<>();
    private final List<WebTreeTableColumnImpl<E, ?>> myGridColumns = new ArrayList<>();

    private boolean myColumnWidthsPending = true;

    @RequiredUIAccess
    public WebTreeTableImpl(@Nullable E rootValue, TreeModel<E> model, TreeExecutor executor) {
        super(rootValue, model, executor);

        getController().setColumnValueFactory(myColumns::values);

        WebTreeImpl<E>.Vaadin vaadin = toVaadinComponent();
        vaadin.getTreeColumn().setHeader(LocalizeValue.empty().get());

        vaadin.asSingleSelect().addValueChangeListener(event -> {
            vaadin.refreshRow(event.getOldValue());
            vaadin.refreshRow(event.getValue());
        });

        vaadin.addSortListener(event -> onSort(event.getSortOrder()));

        vaadin.addDetachListener(event -> myColumnWidthsPending = true);
    }

    @Override
    public void setTreeColumnHeader(LocalizeValue header) {
        toVaadinComponent().getTreeColumn().setHeader(header.get());
    }

    @Override
    public <V> TableColumn<E, V> addColumn(LocalizeValue header, Function<E, V> valueProvider) {
        WebTreeTableColumnImpl<E, V> column = new WebTreeTableColumnImpl<>(this, myColumns, header, valueProvider);
        myGridColumns.add(column);
        column.applyState();

        CompletableFuture<?> values = getController().refreshColumnValues();
        if (getController().getRoot().isLoaded()) {
            values.whenComplete((ignored, error) -> recalculateColumnWidths());
        }
        return column.getState();
    }

    @Override
    public List<TableColumn<E, ?>> getColumns() {
        return myColumns.getTableColumns();
    }

    @Override
    void childrenApplied(TreeNodeImpl<E> node) {
        if (myColumnWidthsPending && node == getController().getRoot() && !node.getChildren().isEmpty()) {
            myColumnWidthsPending = false;
            toVaadinComponent().recalculateColumnWidths();
        }
    }

    private void recalculateColumnWidths() {
        UIAccess access = getUIAccess();
        if (access != null) {
            access.give(() -> toVaadinComponent().recalculateColumnWidths());
        }
    }

    boolean isSelected(WebTreeRow<E> row) {
        return toVaadinComponent().getSelectedItems().contains(row);
    }

    void refreshRows() {
        toVaadinComponent().getDataProvider().refreshAll();
    }

    void columnSortChanged(WebTreeTableColumnImpl<E, ?> column) {
        column.applySortable();

        if (myColumns.getSortColumn() != column.getIndex()) {
            return;
        }

        Comparator<TreeNodeImpl<E>> comparator = myColumns.getSortComparator();
        if (comparator == null) {
            toVaadinComponent().sort(List.of());
        }
        getController().setSortComparator(comparator);
    }

    private void onSort(List<GridSortOrder<WebTreeRow<E>>> order) {
        int index = -1;
        boolean ascending = true;
        if (!order.isEmpty()) {
            GridSortOrder<WebTreeRow<E>> first = order.get(0);
            WebTreeTableColumnImpl<E, ?> column = columnOf(first.getSorted());
            if (column != null) {
                index = column.getIndex();
                ascending = first.getDirection() == SortDirection.ASCENDING;
            }
        }

        if (index == myColumns.getSortColumn() && (index < 0 || ascending == myColumns.isSortAscending())) {
            return;
        }

        Comparator<TreeNodeImpl<E>> comparator = myColumns.sort(index, ascending);
        if (comparator == null && !order.isEmpty()) {
            toVaadinComponent().sort(List.of());
        }
        getController().setSortComparator(comparator);
    }

    private @Nullable WebTreeTableColumnImpl<E, ?> columnOf(Grid.Column<WebTreeRow<E>> gridColumn) {
        for (WebTreeTableColumnImpl<E, ?> column : myGridColumns) {
            if (column.getGridColumn() == gridColumn) {
                return column;
            }
        }
        return null;
    }
}
