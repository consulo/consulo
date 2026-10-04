// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

import consulo.disposer.Disposable;
import consulo.util.collection.JBIterable;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class GridListModelBase<Row, Column> implements GridModel<Row, Column> {

    private List<Column> myColumns;
    private final List<Row> myRows;
    private boolean myUpdatingNow;

    public GridListModelBase() {
        this(new ArrayList<>(), new ArrayList<>());
    }

    public GridListModelBase(List<Column> columns, List<Row> rows) {
        myColumns = columns;
        myRows = rows;
    }

    @Override
    public boolean hasListeners() {
        return false;
    }

    @Override
    public @Nullable Object getValueAt(ModelIndex<Row> rowIdx, ModelIndex<Column> columnIdx) {
        Row row = getRow(rowIdx);
        Column column = getColumn(columnIdx);
        return row != null && column != null ? getValueAt(row, column) : null;
    }

    @Override
    public @Nullable Row getRow(ModelIndex<Row> row) {
        return getRow(row.asInteger());
    }

    @Override
    public @Nullable Column getColumn(ModelIndex<Column> column) {
        return getColumn(column.asInteger());
    }

    @Override
    @SuppressWarnings("NullAway")
    public List<Row> getRows(ModelIndexSet<Row> rows) {
        List<Row> result = new ArrayList<>(rows.size());
        for (int rowIndex : rows.asArray()) {
            result.add(getRow(rowIndex));
        }
        return result;
    }

    @Override
    public List<Column> getColumns(ModelIndexSet<Column> columns) {
        return getColumnsAsIterable(columns).toList();
    }

    @Override
    @SuppressWarnings("NullAway")
    public JBIterable<Column> getColumnsAsIterable(ModelIndexSet<Column> columns) {
        return columns.asIterable().transform(this::getColumn);
    }

    @Override
    public JBIterable<Column> getColumnsAsIterable() {
        return JBIterable.from(Collections.unmodifiableList(myColumns));
    }

    @Override
    public List<Column> getColumns() {
        return Collections.unmodifiableList(myColumns);
    }

    @Override
    public List<Row> getRows() {
        return Collections.unmodifiableList(myRows);
    }

    @Override
    public ModelIndexSet<Column> getColumnIndices() {
        return ModelIndexSet.forColumns(this, range(0, getColumnCount()));
    }

    @Override
    public ModelIndexSet<Row> getRowIndices() {
        return ModelIndexSet.forRows(this, range(0, getRowCount()));
    }

    @Override
    public int getColumnCount() {
        return myColumns.size();
    }

    @Override
    public int getRowCount() {
        return myRows.size();
    }

    @Override
    public boolean isUpdatingNow() {
        return myUpdatingNow;
    }

    @Override
    public boolean isValidRowIdx(ModelIndex<Row> rowIdx) {
        return isValidIdx(myRows, rowIdx.asInteger());
    }

    @Override
    public boolean isValidColumnIdx(ModelIndex<Column> columnIdx) {
        return isValidIdx(myColumns, columnIdx.asInteger());
    }

    @Override
    public void addListener(Listener<Row, Column> l, Disposable disposable) {
    }


    protected abstract @Nullable Object getValueAt(Row row, Column column);

    public abstract boolean allValuesEqualTo(List<CellMutation> mutations);

    public void setUpdatingNow(boolean updatingNow) {
        myUpdatingNow = updatingNow;
    }

    public void addRows(List<? extends Row> rows) {
        myRows.addAll(rows);
    }

    public void addRow(Row row) {
        myRows.add(row);
    }

    public void removeRows(int firstRowIndex, int rowCount) {
        removeRange(myRows, firstRowIndex, rowCount);
    }

    public void setColumns(List<? extends Column> columns) {
        myColumns.addAll(columns);
    }

    private @Nullable Row getRow(int rowIdx) {
        return isValidIdx(myRows, rowIdx) ? myRows.get(rowIdx) : null;
    }

    private @Nullable Column getColumn(int columnIdx) {
        return isValidIdx(myColumns, columnIdx) ? myColumns.get(columnIdx) : null;
    }

    public void clearColumns() {
        myColumns = new ArrayList<>();
    }

    public void set(int i, Row row) {
        myRows.set(i, row);
    }

    private static boolean isValidIdx(List<?> list, int idx) {
        return idx > -1 && idx < list.size();
    }

    @SuppressWarnings("SameParameterValue")
    private static int[] range(int first, int length) {
        int[] range = new int[length];
        for (int i = 0; i < length; i++) {
            range[i] = first + i;
        }
        return range;
    }

    private static void removeRange(List<?> list, int firstIdx, int count) {
        for (int i = 0; i < count; i++) {
            list.remove(firstIdx + count - i - 1);
        }
    }
}
