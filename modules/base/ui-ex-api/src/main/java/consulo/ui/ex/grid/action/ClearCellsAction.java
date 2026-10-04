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
import consulo.dataContext.DataContext;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.DeleteProvider;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.grid.DatabaseDataKeys;
import consulo.ui.ex.grid.GridUtil;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.SelectionModel;

/**
 * Writes an empty text into the selected cells. It is the delete provider of a grid which has {@link GridUtil#DELETE_CLEARS_CELLS},
 * so the delete key clears the cells there instead of deleting the rows.
 *
 * @since 2026-10-04
 */
@ActionImpl(id = ClearCellsAction.ID)
public class ClearCellsAction extends DumbAwareAction implements DeleteProvider, GridEditAction, AnActionWithSyncUpdate {
    public static final String ID = "Console.TableResult.ClearCells";

    public ClearCellsAction() {
        super(LocalizeValue.localizeTODO("Clear Cells"), LocalizeValue.localizeTODO("Clear the values of the selected cells"));
    }

    @Override
    public void update(AnActionEvent e) {
        DataGrid grid = e.getData(DatabaseDataKeys.DATA_GRID_KEY);
        e.getPresentation().setVisible(InsertRowBeforeAction.isVisible(grid));
        e.getPresentation().setEnabled(grid != null && canClearCells(grid));
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        DataGrid grid = e.getData(DatabaseDataKeys.DATA_GRID_KEY);
        if (grid != null && canClearCells(grid)) {
            clearCells(grid);
        }
    }

    @Override
    public boolean canDeleteElement(DataContext dataContext) {
        DataGrid grid = dataContext.getData(DatabaseDataKeys.DATA_GRID_KEY);
        return grid != null && canClearCells(grid);
    }

    @RequiredUIAccess
    @Override
    public void deleteElement(DataContext dataContext) {
        DataGrid grid = dataContext.getData(DatabaseDataKeys.DATA_GRID_KEY);
        if (grid != null && canClearCells(grid)) {
            clearCells(grid);
        }
    }

    /**
     * Cells are cleared in an editable grid which is loaded, while no cell editor is open and some cells are selected.
     */
    public static boolean canClearCells(DataGrid grid) {
        return grid.isEditable() && grid.isReady() && !grid.isEditing() && !grid.getSelectionModel().isSelectionEmpty();
    }

    /**
     * Clears the selected cells, after the user agreed to lose the unsubmitted changes, if the data source asks for that.
     */
    @RequiredUIAccess
    public static void clearCells(DataGrid grid) {
        SelectionModel<GridRow, GridColumn> selection = grid.getSelectionModel();
        grid.setCells(selection.getSelectedRows(), selection.getSelectedColumns(), "");
    }
}
