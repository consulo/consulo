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
package consulo.ui.ex.grid.action;

import consulo.annotation.component.ActionImpl;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.grid.DatabaseDataKeys;
import consulo.ui.ex.grid.GridUtil;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridHelper;
import consulo.ui.grid.GridMutator;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.ModelIndex;
import consulo.ui.grid.ModelIndexSet;

import static consulo.ui.ex.grid.action.AddColumnAction.newInsertOrCloneColumnRequestSource;
import static consulo.ui.ex.grid.action.DeleteColumnsAction.getColumns;

/**
 * Appends a copy of the column a column header menu was opened on, or else of the selected column. The keystroke comes from the
 * keymap.
 */
@ActionImpl(id = CloneColumnAction.ID)
public class CloneColumnAction extends DumbAwareAction implements GridEditAction, AnActionWithSyncUpdate {
    public static final String ID = "Console.TableResult.CloneColumn";

    public CloneColumnAction() {
        super(LocalizeValue.localizeTODO("Clone Column"), LocalizeValue.empty(), PlatformIconGroup.actionsCopy());
    }

    @Override
    public void update(AnActionEvent e) {
        DataGrid dataGrid = e.getData(DatabaseDataKeys.DATA_GRID_KEY);
        if (dataGrid == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        boolean singleColumnSelected = getColumns(dataGrid, GridUtil.getContextColumn(dataGrid, e)).size() == 1;
        boolean canAddColumn = GridUtil.canMutateColumns(dataGrid);
        // while a cell editor is open its keys reach the editor, and the change of the columns does not drop the edit
        e.getPresentation().setEnabledAndVisible(
            singleColumnSelected && canAddColumn && !dataGrid.isEditing() && !GridHelper.get(dataGrid).isModifyColumnAcrossCollection()
        );
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        DataGrid dataGrid = e.getData(DatabaseDataKeys.DATA_GRID_KEY);
        ModelIndexSet<GridColumn> columns = dataGrid != null ? getColumns(dataGrid, GridUtil.getContextColumn(dataGrid, e)) : null;
        if (dataGrid == null || columns == null || columns.size() != 1) {
            return;
        }
        ModelIndex<GridColumn> selectedColumn = columns.asIterable().first();
        if (selectedColumn != null && selectedColumn.isValid(dataGrid)) {
            cloneColumn(dataGrid, selectedColumn);
        }
    }

    @RequiredUIAccess
    public static void cloneColumn(DataGrid grid, ModelIndex<GridColumn> selectedColumn) {
        GridMutator.ColumnsMutator<GridRow, GridColumn> mutator = GridUtil.getColumnsMutator(grid);
        if (mutator == null) {
            return;
        }

        if (mutator.isUpdateImmediately() && mutator.hasPendingChanges()) {
            grid.submit().doWhenDone(() -> mutator.cloneColumn(newInsertOrCloneColumnRequestSource(grid), selectedColumn));
            return;
        }
        mutator.cloneColumn(newInsertOrCloneColumnRequestSource(grid), selectedColumn);
    }
}
