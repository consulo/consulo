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
package consulo.ui.grid;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.util.collection.JBIterable;
import consulo.util.collection.Lists;
import consulo.util.lang.ObjectUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The listeners are kept in a plain lock-free list.
 * <p/>
 * The column and row lists hold {@code null} where an index has no column or row behind it.
 */
public class GridMutationModel implements GridModel<GridRow, GridColumn> {
    private final GridModel<GridRow, GridColumn> myModel;
    private final Map<ModelIndex<GridRow>, GridRow> myCache;
    private final GridDataHookUp<GridRow, GridColumn> myHookUp;
    private final List<Listener<GridRow, GridColumn>> myListeners = Lists.newLockFreeCopyOnWriteList();

    public GridMutationModel(GridDataHookUp<GridRow, GridColumn> hookUp) {
        myHookUp = hookUp;
        myModel = myHookUp.getDataModel();
        myCache = new ConcurrentHashMap<>();
    }

    @Override
    @SuppressWarnings("NullAway")
    public List<GridColumn> getColumns() {
        return getColumnIndicesInner().map(this::wrapColumn).toList();
    }

    @Override
    @SuppressWarnings("NullAway")
    public JBIterable<GridColumn> getColumnsAsIterable() {
        return getColumnIndicesInner().map(this::wrapColumn);
    }

    @Override
    @SuppressWarnings("NullAway")
    public List<GridColumn> getColumns(ModelIndexSet<GridColumn> columnsIdxs) {
        return columnsIdxs.asIterable().filter(this::isValidColumnIdx).map(this::wrapColumn).toList(); // todo remove
    }

    @Override
    @SuppressWarnings("NullAway")
    public JBIterable<GridColumn> getColumnsAsIterable(ModelIndexSet<GridColumn> columns) {
        return columns.asIterable().filter(this::isValidColumnIdx).map(this::wrapColumn);
    }

    @Override
    public @Nullable GridColumn getColumn(ModelIndex<GridColumn> columnIdx) {
        return !isValidColumnIdx(columnIdx) ? null : wrapColumn(columnIdx);
    }

    @Override
    @SuppressWarnings("NullAway")
    public List<GridRow> getRows(ModelIndexSet<GridRow> rows) {
        return rows.asIterable().filter(this::isValidRowIdx).map(this::wrapRow).toList();
    }

    @Override
    public @Nullable Object getValueAt(ModelIndex<GridRow> row, ModelIndex<GridColumn> column) {
        return getValueAt(row, column, myHookUp.getMutator(), myModel);
    }

    @SuppressWarnings("unchecked")
    public static @Nullable Object getValueAt(ModelIndex<GridRow> row, ModelIndex<GridColumn> column,
                                              @Nullable GridMutator<GridRow, GridColumn> mutator,
                                              GridModel<GridRow, GridColumn> model) {
        GridMutator.DatabaseMutator<GridRow, GridColumn> databaseMutator = ObjectUtil.tryCast(mutator, GridMutator.DatabaseMutator.class);
        GridMutator.ColumnsMutator<GridRow, GridColumn> columnsMutator = ObjectUtil.tryCast(mutator, GridMutator.ColumnsMutator.class);
        MutationData value = databaseMutator == null ? null : databaseMutator.getMutation(row, column);
        return value != null
            ? value.getValue()
            : row.isValid(model) && column.isValid(model)
            ? model.getValueAt(row, column)
            : columnsMutator != null && (columnsMutator.isDeletedColumn(column) || columnsMutator.isInsertedColumn(column))
            ? ReservedCellValue.UNSET
            : null
            ;
    }

    @Override
    public boolean allValuesEqualTo(ModelIndexSet<GridRow> rowIndices,
                                    ModelIndexSet<GridColumn> columnIndices,
                                    @Nullable Object what) {
        GridMutator.DatabaseMutator<GridRow, GridColumn> databaseMutator = getDatabaseMutator();
        return myModel.allValuesEqualTo(rowIndices, columnIndices, what) &&
            (databaseMutator == null || !databaseMutator.hasMutatedRows(rowIndices, columnIndices));
    }

    @Override
    public @Nullable GridRow getRow(ModelIndex<GridRow> row) {
        return !isValidRowIdx(row) ? null : wrapRow(row);
    }

    @Override
    @SuppressWarnings("NullAway")
    public List<GridRow> getRows() {
        return getRowIndicesInner().map(this::wrapRow).toList();
    }

    private @Nullable GridRow wrapRow(ModelIndex<GridRow> rowIdx) {
        GridMutator.DatabaseMutator<GridRow, GridColumn> mutator = getDatabaseMutator();
        MutationType type = mutator == null ? null : mutator.getMutationType(rowIdx);
        return mutator != null && (type == MutationType.MODIFY || type == MutationType.INSERT)
            ? myCache.computeIfAbsent(rowIdx, r -> new MutationRow(rowIdx, new Object[getColumnCount()], mutator, myModel))
            : myModel.getRow(rowIdx);
    }

    private @Nullable GridColumn wrapColumn(ModelIndex<GridColumn> columnIdx) {
        GridMutator.ColumnsMutator<GridRow, GridColumn> mutator = getColumnsMutator();
        GridColumn column = mutator == null ? null : mutator.getInsertedColumn(columnIdx);
        return column != null ? column : myModel.getColumn(columnIdx);
    }

    @Override
    public ModelIndexSet<GridColumn> getColumnIndices() {
        return ModelIndexSet.forColumns(myModel, getColumnIndicesInner());
    }

    @Override
    public ModelIndexSet<GridRow> getRowIndices() {
        return ModelIndexSet.forRows(myModel, getRowIndicesInner());
    }

    private JBIterable<ModelIndex<GridRow>> getRowIndicesInner() {
        GridMutator.RowsMutator<GridRow, GridColumn> mutator = getRowsMutator();
        return myModel.getRowIndices().asIterable().append(mutator == null ? Collections.emptyList() : mutator.getInsertedRows());
    }

    private JBIterable<ModelIndex<GridColumn>> getColumnIndicesInner() {
        GridMutator.ColumnsMutator<GridRow, GridColumn> mutator = getColumnsMutator();
        return myModel.getColumnIndices().asIterable().append(mutator == null ? Collections.emptyList() : mutator.getInsertedColumns());
    }

    @Override
    public int getColumnCount() {
        GridMutator.ColumnsMutator<GridRow, GridColumn> mutator = getColumnsMutator();
        return myModel.getColumnCount() + (mutator == null ? 0 : mutator.getInsertedColumnsCount());
    }

    @Override
    public int getRowCount() {
        GridMutator.RowsMutator<GridRow, GridColumn> mutator = getRowsMutator();
        return myModel.getRowCount() + (mutator == null ? 0 : mutator.getInsertedRowsCount());
    }

    @Override
    public boolean isValidRowIdx(ModelIndex<GridRow> rowIdx) {
        GridMutator.RowsMutator<GridRow, GridColumn> mutator = getRowsMutator();
        return myModel.isValidRowIdx(rowIdx) || mutator != null && mutator.isInsertedRow(rowIdx);
    }

    @Override
    public boolean isValidColumnIdx(ModelIndex<GridColumn> columnIdx) {
        GridMutator.ColumnsMutator<GridRow, GridColumn> mutator = getColumnsMutator();
        return myModel.isValidColumnIdx(columnIdx) || mutator != null && mutator.isInsertedColumn(columnIdx);
    }

    @Override
    public boolean isUpdatingNow() {
        return myModel.isUpdatingNow();
    }

    @Override
    public void addListener(Listener<GridRow, GridColumn> l, Disposable disposable) {
        myListeners.add(l);
        Disposer.register(disposable, () -> myListeners.remove(l));
    }

    @Override
    public boolean hasListeners() {
        return !myListeners.isEmpty();
    }

    @Override
    public List<GridColumn> getAllColumnsForExtraction(int... selection) {
        return myModel.getAllColumnsForExtraction(selection);
    }

    @SuppressWarnings("unchecked")
    private GridMutator.@Nullable DatabaseMutator<GridRow, GridColumn> getDatabaseMutator() {
        return ObjectUtil.tryCast(myHookUp.getMutator(), GridMutator.DatabaseMutator.class);
    }

    @SuppressWarnings("unchecked")
    private GridMutator.@Nullable RowsMutator<GridRow, GridColumn> getRowsMutator() {
        return ObjectUtil.tryCast(myHookUp.getMutator(), GridMutator.RowsMutator.class);
    }

    @SuppressWarnings("unchecked")
    private GridMutator.@Nullable ColumnsMutator<GridRow, GridColumn> getColumnsMutator() {
        return ObjectUtil.tryCast(myHookUp.getMutator(), GridMutator.ColumnsMutator.class);
    }

    public void afterLastRowAdded() {
        for (Listener<GridRow, GridColumn> listener : myListeners) {
            listener.afterLastRowAdded();
        }
    }

    public void notifyCellsUpdated(ModelIndexSet<GridRow> rows,
                                   ModelIndexSet<GridColumn> columns,
                                   GridRequestSource.@Nullable RequestPlace place) {
        if (rows.size() == 0 || columns.size() == 0) {
            return;
        }
        for (Listener<GridRow, GridColumn> listener : myListeners) {
            listener.cellsUpdated(rows, columns, place);
        }
    }

    public void notifyRowsAdded(ModelIndexSet<GridRow> rows) {
        if (rows.size() == 0) {
            return;
        }
        for (Listener<GridRow, GridColumn> listener : myListeners) {
            listener.rowsAdded(rows);
        }
    }

    public void notifyRowsRemoved(ModelIndexSet<GridRow> rows) {
        if (rows.size() == 0) {
            return;
        }
        for (Listener<GridRow, GridColumn> listener : myListeners) {
            listener.rowsRemoved(rows);
        }
    }

    public void notifyColumnsAdded(ModelIndexSet<GridColumn> columns) {
        if (columns.size() == 0) {
            return;
        }
        for (Listener<GridRow, GridColumn> listener : myListeners) {
            listener.columnsAdded(columns);
        }
    }

    public void notifyColumnsRemoved(ModelIndexSet<GridColumn> columns) {
        if (columns.size() == 0) {
            return;
        }
        for (Listener<GridRow, GridColumn> listener : myListeners) {
            listener.columnsRemoved(columns);
        }
    }
}
