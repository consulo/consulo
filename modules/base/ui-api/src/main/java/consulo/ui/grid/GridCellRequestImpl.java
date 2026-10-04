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

import org.jspecify.annotations.Nullable;

/**
 * The request {@link GridCellRequest#request} makes: it reads the value from the grid model. It is a class of its own because a
 * Java interface cannot have private member classes.
 */
final class GridCellRequestImpl<Row, Column> implements ActualGridCellRequest<Row, Column> {
    private final CoreGrid<Row, Column> myGrid;
    private final ModelIndex<Row> myRowIdx;
    private final ModelIndex<Column> myColumnIdx;

    GridCellRequestImpl(CoreGrid<Row, Column> grid, ModelIndex<Row> rowIdx, ModelIndex<Column> columnIdx) {
        myGrid = grid;
        myRowIdx = rowIdx;
        myColumnIdx = columnIdx;
    }

    @Override
    public CoreGrid<Row, Column> getGrid() {
        return myGrid;
    }

    @Override
    public ModelIndex<Row> getRowIdx() {
        return myRowIdx;
    }

    @Override
    public ModelIndex<Column> getColumnIdx() {
        return myColumnIdx;
    }

    @Override
    public @Nullable Column getColumn() {
        return myGrid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS).getColumn(myColumnIdx);
    }

    @Override
    public @Nullable Object getValue() {
        return myGrid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS).getValueAt(myRowIdx, myColumnIdx);
    }
}
