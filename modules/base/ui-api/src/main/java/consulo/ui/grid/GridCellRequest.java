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
 * A cell of a grid, and the value it is asked for. The requests are made by the static methods of this interface:
 * {@link #request}, {@link #requestColumn}, {@link #selectedCellRequest}, {@link #overrideValue}, {@link #actual},
 * {@link #getDatabaseValue} and {@link #fixed}.
 */
public interface GridCellRequest<Row, Column> {
    CoreGrid<Row, Column> getGrid();

    ModelIndex<Row> getRowIdx();

    ModelIndex<Column> getColumnIdx();

    @Nullable
    Column getColumn();

    @Nullable
    Object getValue();

    default boolean isValid() {
        return isRowIdxValid() && isColumnIdxValid();
    }

    default boolean isColumnIdxValid() {
        return getColumnIdx().isValid(getGrid());
    }

    default boolean isRowIdxValid() {
        return getRowIdx().isValid(getGrid());
    }

    static <Row, Column> ActualGridCellRequest<Row, Column> request(CoreGrid<Row, Column> grid,
                                                                    ModelIndex<Row> rowIdx,
                                                                    ModelIndex<Column> columnIdx) {
        return new GridCellRequestImpl<>(grid, rowIdx, columnIdx);
    }

    static <Row, Column> FixedGridCellRequest<Row, Column> fixedValue(GridCellRequest<Row, Column> delegate,
                                                                      @Nullable Object valueOverride) {
        return new FixedValueGridCellRequest<>(delegate, valueOverride);
    }

    static <Row, Column> ActualGridCellRequest<Row, Column> requestColumn(CoreGrid<Row, Column> grid, ModelIndex<Column> columnIdx) {
        return request(grid, ModelIndex.forRow(grid, -1), columnIdx);
    }

    static <Row, Column> ActualGridCellRequest<Row, Column> selectedCellRequest(CoreGrid<Row, Column> grid) {
        SelectionModel<Row, Column> selectionModel = grid.getSelectionModel();
        return request(grid, selectionModel.getLeadSelectionRow(), selectionModel.getLeadSelectionColumn());
    }

    static <Row, Column> FixedGridCellRequest<Row, Column> overrideValue(GridCellRequest<Row, Column> request, @Nullable Object value) {
        return fixedValue(request, value);
    }

    static <Row, Column> ActualGridCellRequest<Row, Column> actual(GridCellRequest<Row, Column> request) {
        if (request instanceof ActualGridCellRequest<Row, Column> actual) {
            return actual;
        }
        return request(request.getGrid(), request.getRowIdx(), request.getColumnIdx());
    }

    /**
     * Pre-mutation DB value; {@link #actual} only strips an {@link #overrideValue} wrapper, this also bypasses the mutation overlay.
     */
    static <Row, Column> @Nullable Object getDatabaseValue(GridCellRequest<Row, Column> request) {
        return request.getGrid().getDataModel(DataAccessType.DATABASE_DATA).getValueAt(request.getRowIdx(), request.getColumnIdx());
    }

    static <Row, Column> FixedGridCellRequest<Row, Column> fixed(GridCellRequest<Row, Column> request) {
        if (request instanceof FixedGridCellRequest<Row, Column> fixed) {
            return fixed;
        }
        return overrideValue(request, request.getValue());
    }
}
