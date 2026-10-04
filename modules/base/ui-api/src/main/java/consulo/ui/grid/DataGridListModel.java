// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;

public class DataGridListModel extends GridListModelBase<GridRow, GridColumn> implements GridModelWithInjections<GridRow, GridColumn> {
    private final BiFunction<@Nullable Object, @Nullable Object, Boolean> myValuesEquals;

    public DataGridListModel(BiFunction<@Nullable Object, @Nullable Object, Boolean> valuesEquals) {
        myValuesEquals = valuesEquals;
    }

    @Override
    protected @Nullable Object getValueAt(GridRow row, GridColumn column) {
        return column.getValue(row);
    }

    @Override
    public boolean allValuesEqualTo(ModelIndexSet<GridRow> rowIndices,
                                    ModelIndexSet<GridColumn> columnIndices,
                                    @Nullable Object what) {
        for (ModelIndex<GridRow> rowIdx : rowIndices.asIterable()) {
            for (ModelIndex<GridColumn> colIdx : columnIndices.asIterable()) {
                if (!GridUtilCore.isRowId(getColumn(colIdx)) &&
                    !myValuesEquals.apply(what, getValueAt(rowIdx, colIdx))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean allValuesEqualTo(List<CellMutation> mutations) {
        for (CellMutation mutation : mutations) {
            ModelIndex<GridRow> row = mutation.getRow();
            ModelIndex<GridColumn> column = mutation.getColumn();
            Object value = mutation.getValue();
            Object oldValue = getValueAt(row, column);
            if (!GridUtilCore.isRowId(getColumn(column)) && !myValuesEquals.apply(value, oldValue)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void injectValue(ModelIndex<GridRow> rowIndex, ModelIndex<GridColumn> columnIndex, Object value) {
        GridRow row = getRow(rowIndex);
        if (row != null) {
            row.setValue(columnIndex.value, value);
        }
    }
}
