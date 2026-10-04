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
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.ModelIndex;
import org.jspecify.annotations.Nullable;

/**
 * Inserts a column before the column a column header menu was opened on, else before the lead selected column, else as the first
 * column, and selects it. The user names it first, as for {@link AddColumnAction}. A data source which cannot insert in place
 * appends the column.
 *
 * @since 2026-10-04
 */
@ActionImpl(id = InsertColumnBeforeAction.ID)
public class InsertColumnBeforeAction extends DumbAwareAction implements GridEditAction, AnActionWithSyncUpdate {
    public static final String ID = "Console.TableResult.InsertColumnBefore";

    public InsertColumnBeforeAction() {
        super(
            LocalizeValue.localizeTODO("Insert Column Before"),
            LocalizeValue.localizeTODO("Insert a column before the selected column")
        );
    }

    @Override
    public void update(AnActionEvent e) {
        DataGrid grid = e.getData(DatabaseDataKeys.DATA_GRID_KEY);
        boolean visible = GridUtil.canMutateColumns(grid);
        e.getPresentation().setVisible(visible);
        e.getPresentation().setEnabled(visible && canInsertColumn(grid));
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        DataGrid grid = e.getData(DatabaseDataKeys.DATA_GRID_KEY);
        if (grid == null || !canInsertColumn(grid)) {
            return;
        }
        ModelIndex<GridColumn> column = getReferenceColumn(grid, e);
        AddColumnAction.insertColumnBefore(grid, column.isValid(grid) ? column : ModelIndex.forColumn(grid, 0));
    }

    /**
     * The column a column header menu was opened on, for an action run from that menu, else the lead selected column.
     */
    static ModelIndex<GridColumn> getReferenceColumn(DataGrid grid, AnActionEvent e) {
        ModelIndex<GridColumn> column = GridUtil.getContextColumn(grid, e);
        return column.asInteger() != -1 ? column : grid.getSelectionModel().getLeadSelectionColumn();
    }

    /**
     * A column is inserted while no cell editor is open: the columns right of the editor would move.
     */
    static boolean canInsertColumn(@Nullable DataGrid grid) {
        return grid != null && !grid.isEditing() && GridUtil.canMutateColumns(grid);
    }
}
