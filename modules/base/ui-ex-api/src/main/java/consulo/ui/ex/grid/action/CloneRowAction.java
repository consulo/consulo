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
import consulo.annotation.component.ActionRef;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.grid.DatabaseDataKeys;
import consulo.ui.ex.grid.GridUtil;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridMutator;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.ModelIndex;

import static consulo.ui.ex.grid.GridUtil.newInsertOrCloneRowRequestSource;

/**
 * Inserts a copy of the selected row. The grid model has no truncated values, so every row can be cloned.
 */
@ActionImpl(id = CloneRowAction.ID, shortcutFrom = @ActionRef(id = IdeActions.ACTION_EDITOR_DUPLICATE))
public class CloneRowAction extends DumbAwareAction implements GridEditAction, AnActionWithSyncUpdate {
    public static final String ID = "Console.TableResult.CloneRow";

    public CloneRowAction() {
        super(LocalizeValue.localizeTODO("Clone Row"), LocalizeValue.empty(), PlatformIconGroup.actionsCopy());
    }

    @Override
    public void update(AnActionEvent e) {
        DataGrid dataGrid = e.getData(DatabaseDataKeys.DATA_GRID_KEY);
        boolean available = dataGrid != null && dataGrid.getSelectionModel().getSelectedRowCount() == 1 &&
            AddRowAction.canAddRow(dataGrid) && !dataGrid.isEditing();
        e.getPresentation().setEnabledAndVisible(available);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        DataGrid dataGrid = e.getData(DatabaseDataKeys.DATA_GRID_KEY);
        if (dataGrid == null) {
            return;
        }
        ModelIndex<GridRow> selectedRow = dataGrid.getSelectionModel().getSelectedRow();
        if (selectedRow.isValid(dataGrid)) {
            cloneRow(dataGrid, selectedRow);
        }
    }

    @RequiredUIAccess
    private static void cloneRow(DataGrid grid, ModelIndex<GridRow> rowToClone) {
        GridMutator.RowsMutator<GridRow, GridColumn> mutator = GridUtil.getRowsMutator(grid);
        if (mutator == null) {
            return;
        }
        if (mutator.isUpdateImmediately() && mutator.hasPendingChanges()) {
            grid.submit().doWhenDone(() -> mutator.cloneRow(newInsertOrCloneRowRequestSource(grid), rowToClone));
            return;
        }
        mutator.cloneRow(newInsertOrCloneRowRequestSource(grid), rowToClone);
    }
}
