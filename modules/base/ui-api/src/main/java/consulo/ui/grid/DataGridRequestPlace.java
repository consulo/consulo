// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

public class DataGridRequestPlace implements GridRequestSource.GridRequestPlace<GridRow, GridColumn> {
    private final CoreGrid<GridRow, GridColumn> myGrid;
    private final ModelIndexSet<GridRow> myRows;
    private final ModelIndexSet<GridColumn> myColumns;

    public DataGridRequestPlace(CoreGrid<GridRow, GridColumn> grid) {
        this(grid, ModelIndexSet.forRows(grid), ModelIndexSet.forColumns(grid));
    }

    public DataGridRequestPlace(CoreGrid<GridRow, GridColumn> grid, ModelIndexSet<GridRow> rows, ModelIndexSet<GridColumn> columns) {
        myGrid = grid;
        myRows = rows;
        myColumns = columns;
    }

    public ModelIndexSet<GridRow> getRows() {
        return myRows;
    }

    public ModelIndexSet<GridColumn> getColumns() {
        return myColumns;
    }

    @Override
    public CoreGrid<GridRow, GridColumn> getGrid() {
        return myGrid;
    }
}
