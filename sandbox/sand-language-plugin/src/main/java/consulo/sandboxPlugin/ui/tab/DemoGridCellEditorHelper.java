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
package consulo.sandboxPlugin.ui.tab;

import consulo.ui.ex.grid.editor.GridCellEditorHelperImpl;
import consulo.ui.grid.CoreGrid;
import consulo.ui.grid.GridCellRequest;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.GridTypeKind;
import consulo.ui.grid.ModelIndex;

/**
 * How the cells of {@link SampleGridData} are edited:
 * <ul>
 * <li>JSON and UUID columns are edited as text - there is no editor of their own. The text editor edits strings only, so the UUID
 * values open read-only;</li>
 * <li>numbers are parsed to the class of their kind: INTEGER to {@link Integer} ({@link Long} when it does not fit), FLOAT to
 * {@link Double} and DECIMAL to {@link java.math.BigDecimal}, which keeps every digit typed;</li>
 * <li>the key column {@code id} is not nullable, so Set NULL is disabled while it is selected.</li>
 * </ul>
 *
 * @since 2026-10-04
 */
final class DemoGridCellEditorHelper extends GridCellEditorHelperImpl {
    /**
     * The model index of {@code id}, the first column of {@link SampleGridData#COLUMNS}.
     */
    private static final int ID_COLUMN = 0;

    @Override
    public GridTypeKind guessTypeKindForEditing(GridCellRequest<GridRow, GridColumn> request) {
        return editedKind(super.guessTypeKindForEditing(request));
    }

    @Override
    public GridTypeKind guessTypeKindForColumn(GridCellRequest<GridRow, GridColumn> request) {
        return editedKind(super.guessTypeKindForColumn(request));
    }

    @Override
    public boolean useBigDecimalWithPriorityType(CoreGrid<GridRow, GridColumn> grid) {
        return true;
    }

    @Override
    public boolean isNullable(CoreGrid<GridRow, GridColumn> grid, ModelIndex<GridColumn> idx) {
        return idx.asInteger() != ID_COLUMN;
    }

    private static GridTypeKind editedKind(GridTypeKind kind) {
        return kind == GridTypeKind.JSON || kind == GridTypeKind.UUID ? GridTypeKind.TEXT : kind;
    }
}
