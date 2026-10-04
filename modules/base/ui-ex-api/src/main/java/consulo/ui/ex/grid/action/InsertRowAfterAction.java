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
import consulo.ui.grid.GridRow;
import consulo.ui.grid.ModelIndex;

/**
 * Inserts an empty row after the lead selected row, or at the end when no row is selected, and selects it. A data source which
 * cannot insert in place appends the row.
 *
 * @since 2026-10-04
 */
@ActionImpl(id = InsertRowAfterAction.ID)
public class InsertRowAfterAction extends DumbAwareAction implements GridEditAction, AnActionWithSyncUpdate {
    public static final String ID = "Console.TableResult.InsertRowAfter";

    public InsertRowAfterAction() {
        super(LocalizeValue.localizeTODO("Insert Row After"), LocalizeValue.localizeTODO("Insert an empty row after the selected row"));
    }

    @Override
    public void update(AnActionEvent e) {
        DataGrid grid = e.getData(DatabaseDataKeys.DATA_GRID_KEY);
        boolean visible = InsertRowBeforeAction.isVisible(grid);
        e.getPresentation().setVisible(visible);
        e.getPresentation().setEnabled(visible && InsertRowBeforeAction.canInsertRow(grid));
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        DataGrid grid = e.getData(DatabaseDataKeys.DATA_GRID_KEY);
        if (grid == null || !InsertRowBeforeAction.canInsertRow(grid)) {
            return;
        }
        ModelIndex<GridRow> row = grid.getSelectionModel().getLeadSelectionRow();
        // after the last row there is no row to insert before - the row is appended
        GridUtil.insertRows(grid, row.isValid(grid) ? ModelIndex.forRow(grid, row.asInteger() + 1) : null, 1);
        GridUtil.focusDataGrid(grid);
    }
}
