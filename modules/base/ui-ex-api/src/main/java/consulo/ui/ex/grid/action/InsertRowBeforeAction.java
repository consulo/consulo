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
package consulo.ui.ex.grid.action;

import consulo.annotation.component.ActionImpl;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.grid.DatabaseDataKeys;
import consulo.ui.ex.grid.GridUtil;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.GridHelper;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.ModelIndex;
import org.jspecify.annotations.Nullable;

/**
 * Inserts an empty row before the lead selected row, or at the top when no row is selected, and selects it. A data source which
 * cannot insert in place appends the row.
 *
 * @since 2026-10-04
 */
@ActionImpl(id = InsertRowBeforeAction.ID)
public class InsertRowBeforeAction extends DumbAwareAction implements GridEditAction, AnActionWithSyncUpdate {
    public static final String ID = "Console.TableResult.InsertRowBefore";

    public InsertRowBeforeAction() {
        super(LocalizeValue.localizeTODO("Insert Row Before"), LocalizeValue.localizeTODO("Insert an empty row before the selected row"));
    }

    @Override
    public void update(AnActionEvent e) {
        DataGrid grid = e.getData(DatabaseDataKeys.DATA_GRID_KEY);
        boolean visible = isVisible(grid);
        e.getPresentation().setVisible(visible);
        e.getPresentation().setEnabled(visible && canInsertRow(grid));
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        DataGrid grid = e.getData(DatabaseDataKeys.DATA_GRID_KEY);
        if (grid == null || !canInsertRow(grid)) {
            return;
        }
        ModelIndex<GridRow> row = grid.getSelectionModel().getLeadSelectionRow();
        GridUtil.insertRows(grid, row.isValid(grid) ? row : ModelIndex.forRow(grid, 0), 1);
        GridUtil.focusDataGrid(grid);
    }

    static boolean isVisible(@Nullable DataGrid grid) {
        return grid != null && (grid.isEditable() || GridHelper.get(grid).hasTargetForEditing(grid));
    }

    /**
     * A row is inserted where {@link AddRowAction} may add one, while no cell editor is open: the rows below the editor would move.
     */
    static boolean canInsertRow(@Nullable DataGrid grid) {
        return grid != null && !grid.isEditing() && AddRowAction.canAddRow(grid);
    }
}
