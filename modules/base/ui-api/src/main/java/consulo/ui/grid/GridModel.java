// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

import consulo.disposer.Disposable;
import consulo.util.collection.JBIterable;
import org.jspecify.annotations.Nullable;

import java.util.EventListener;
import java.util.List;

public interface GridModel<Row, Column> {
    boolean isValidRowIdx(ModelIndex<Row> rowIdx);

    boolean isValidColumnIdx(ModelIndex<Column> columnIdx);

    @Nullable
    Object getValueAt(ModelIndex<Row> row, ModelIndex<Column> column);

    boolean allValuesEqualTo(ModelIndexSet<Row> rowIndices,
                             ModelIndexSet<Column> columnIndices,
                             @Nullable Object what);

    @Nullable
    Row getRow(ModelIndex<Row> row);

    @Nullable
    Column getColumn(ModelIndex<Column> column);

    List<Row> getRows(ModelIndexSet<Row> rows);

    List<Column> getColumns(ModelIndexSet<Column> columns);

    List<Column> getColumns();

    JBIterable<Column> getColumnsAsIterable();

    JBIterable<Column> getColumnsAsIterable(ModelIndexSet<Column> columns);

    List<Row> getRows();

    ModelIndexSet<Column> getColumnIndices();

    ModelIndexSet<Row> getRowIndices();

    int getColumnCount();

    int getRowCount();

    boolean isUpdatingNow();

    void addListener(Listener<Row, Column> l, Disposable disposable);

    boolean hasListeners();

    default List<Column> getAllColumnsForExtraction(int... selectedColumns) {
        return getColumns();
    }

    interface Listener<Row, Column> extends EventListener {

        void columnsAdded(ModelIndexSet<Column> columns);

        void columnsRemoved(ModelIndexSet<Column> columns);

        void rowsAdded(ModelIndexSet<Row> rows);

        void rowsRemoved(ModelIndexSet<Row> rows);

        void cellsUpdated(ModelIndexSet<Row> rows, ModelIndexSet<Column> columns, GridRequestSource.@Nullable RequestPlace place);

        default void afterLastRowAdded() {
        }
    }
}
