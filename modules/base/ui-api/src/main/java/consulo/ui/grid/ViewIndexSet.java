// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

import consulo.util.lang.StringUtil;

import static consulo.ui.grid.ViewIndex.col2Model;
import static consulo.ui.grid.ViewIndex.forColumn;
import static consulo.ui.grid.ViewIndex.forRow;
import static consulo.ui.grid.ViewIndex.row2Model;

public abstract class ViewIndexSet<S> extends IndexSet<ViewIndex<S>> {
    public static <Row> ViewIndexSet<Row> forRows(CoreGrid<Row, ?> grid, int... rows) {
        return new ViewIndexSet<>(rows) {
            @Override
            @SuppressWarnings("unchecked")
            public ModelIndexSet<Row> toModel(CoreGrid<?, ?> grid) {
                return ModelIndexSet.forRows((CoreGrid<Row, ?>) grid, convert(row2Model(grid), asArray()));
            }

            @Override
            protected ViewIndex<Row> forValue(int value) {
                return forRow(null, value);
            }
        };
    }

    public static <Column> ViewIndexSet<Column> forColumns(CoreGrid<?, Column> grid, int... columns) {
        return new ViewIndexSet<>(columns) {
            @Override
            @SuppressWarnings("unchecked")
            public ModelIndexSet<Column> toModel(CoreGrid<?, ?> grid) {
                return ModelIndexSet.forColumns((CoreGrid<?, Column>) grid, convert(col2Model(grid), asArray()));
            }

            @Override
            protected ViewIndex<Column> forValue(int value) {
                return forColumn(null, value);
            }
        };
    }

    ViewIndexSet(int... indices) {
        super(indices);
    }

    public abstract ModelIndexSet<S> toModel(CoreGrid<?, ?> grid);

    @Override
    public String toString() {
        return "ViewIndexSet{" + StringUtil.join(values, ",") + "}";
    }
}
