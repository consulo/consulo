// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

import org.jspecify.annotations.Nullable;

import java.util.function.IntUnaryOperator;

public abstract class ModelIndex<S> extends Index {

    public static <RowType> ModelIndex<RowType> forRow(CoreGrid<RowType, ?> grid, int row) {
        return forRow(grid.getDataModel(DataAccessType.DATABASE_DATA), row);
    }

    public static <RowType> ModelIndex<RowType> forRow(@Nullable GridModel<RowType, ?> model, int row) {
        return new Row<>(row);
    }

    public static <ColumnType> ModelIndex<ColumnType> forColumn(CoreGrid<?, ColumnType> grid, int column) {
        return forColumn(grid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS), column);
    }

    public static <ColumnType> ModelIndex<ColumnType> forColumn(@Nullable GridModel<?, ColumnType> model, int column) {
        return new Column<>(column);
    }

    private ModelIndex(int value) {
        super(value);
    }

    public abstract ViewIndex<S> toView(CoreGrid<?, ?> grid);

    public abstract boolean isValid(GridModel<?, ?> model);

    public boolean isValid(CoreGrid<?, ?> grid) {
        return isValid(grid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS));
    }

    @Override
    public String toString() {
        return "Model" + getClass().getSimpleName() + "{" + value + "}";
    }


    static IntUnaryOperator row2View(CoreGrid<?, ?> grid) {
        return grid.getRawIndexConverter().row2View();
    }

    static IntUnaryOperator col2View(CoreGrid<?, ?> grid) {
        return grid.getRawIndexConverter().column2View();
    }


    private static class Column<ColumnType> extends ModelIndex<ColumnType> {
        Column(int value) {
            super(value);
        }

        @Override
        @SuppressWarnings("unchecked")
        public ViewIndex<ColumnType> toView(CoreGrid<?, ?> grid) {
            return ViewIndex.forColumn((CoreGrid<?, ColumnType>) grid, col2View(grid).applyAsInt(value));
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean isValid(GridModel<?, ?> model) {
            return ((GridModel<?, ColumnType>) model).isValidColumnIdx(this);
        }
    }

    private static class Row<RowType> extends ModelIndex<RowType> {
        Row(int value) {
            super(value);
        }

        @Override
        @SuppressWarnings("unchecked")
        public ViewIndex<RowType> toView(CoreGrid<?, ?> grid) {
            return ViewIndex.forRow((CoreGrid<RowType, ?>) grid, row2View(grid).applyAsInt(value));
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean isValid(GridModel<?, ?> model) {
            return ((GridModel<RowType, ?>) model).isValidRowIdx(this);
        }
    }
}
