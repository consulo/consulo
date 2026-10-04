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

/**
 * The part of a grid a pointer gesture hit, such as the one which opened a context menu. Actions read it from
 * {@link DataGrid#getContextArea()} to decide whether they act on cells, on whole columns or on whole rows.
 *
 * @since 2026-10-04
 */
public enum GridHitArea {
    /**
     * A data cell.
     */
    CELL,
    /**
     * The header of a column.
     */
    COLUMN_HEADER,
    /**
     * The header of a row - the row number gutter.
     */
    ROW_HEADER,
    /**
     * The part of the grid below the last row or right of the last column, or no gesture happened yet.
     */
    EMPTY
}
