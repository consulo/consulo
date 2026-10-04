// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

import org.jspecify.annotations.Nullable;

import java.util.function.IntUnaryOperator;

public abstract class ViewIndex<S> extends Index {

    static IntUnaryOperator row2Model(CoreGrid<?, ?> grid) {
        return grid.getRawIndexConverter().row2Model();
    }

    static IntUnaryOperator col2Model(CoreGrid<?, ?> grid) {
        return grid.getRawIndexConverter().column2Model();
    }


    public static <Row> ViewIndex<Row> forRow(@Nullable CoreGrid<Row, ?> grid, int row) {
        return new ViewIndex<>(row) {
            @Override
            @SuppressWarnings("unchecked")
            public ModelIndex<Row> toModel(CoreGrid<?, ?> grid) {
                return ModelIndex.forRow((CoreGrid<Row, ?>) grid, row2Model(grid).applyAsInt(value));
            }

            @Override
            public boolean isValid(CoreGrid<?, ?> grid) {
                return grid.getRawIndexConverter().isValidViewRowIdx(asInteger());
            }
        };
    }

    public static <Column> ViewIndex<Column> forColumn(@Nullable CoreGrid<?, Column> grid, int column) {
        return new ViewIndex<>(column) {
            @Override
            @SuppressWarnings("unchecked")
            public ModelIndex<Column> toModel(CoreGrid<?, ?> grid) {
                return ModelIndex.forColumn((CoreGrid<?, Column>) grid, col2Model(grid).applyAsInt(value));
            }

            @Override
            public boolean isValid(CoreGrid<?, ?> grid) {
                return grid.getRawIndexConverter().isValidViewColumnIdx(asInteger());
            }
        };
    }


    ViewIndex(int value) {
        super(value);
    }

    public abstract ModelIndex<S> toModel(CoreGrid<?, ?> grid);

    public abstract boolean isValid(CoreGrid<?, ?> grid);

    @Override
    public String toString() {
        return "ViewIndex{" + value + "}";
    }
}
