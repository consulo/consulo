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
import consulo.ui.ex.grid.DatabaseDataKeys;
import consulo.ui.ex.grid.GridUtil;
import consulo.ui.grid.DataAccessType;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.GridHelper;
import org.jspecify.annotations.Nullable;

/**
 * Appends an empty row.
 *
 * @author Gregory.Shrago
 */
@ActionImpl(id = AddRowAction.ID, shortcutFrom = @ActionRef(id = "Generate"))
public class AddRowAction extends DumbAwareAction implements GridEditAction, AnActionWithSyncUpdate {
    public static final String ID = "Console.TableResult.AddRow";

    public AddRowAction() {
        super(LocalizeValue.localizeTODO("Add Row"), LocalizeValue.localizeTODO("Add row"), PlatformIconGroup.generalAdd());
    }

    @Override
    public void update(AnActionEvent e) {
        DataGrid grid = e.getData(DatabaseDataKeys.DATA_GRID_KEY);
        if (grid == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        boolean visible = grid.isEditable() || GridHelper.get(grid).hasTargetForEditing(grid);
        // while a cell editor is open the shortcut belongs to the editor, and the new row would drop the edit; the context menu
        // commits the edit before it is built
        boolean enabled = visible && !grid.isEditing() && canAddRow(grid);

        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setVisible(visible);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        DataGrid dataGrid = e.getData(DatabaseDataKeys.DATA_GRID_KEY);
        if (dataGrid == null) {
            return;
        }
        GridUtil.addRow(dataGrid);
        GridUtil.focusDataGrid(dataGrid);
    }

    public static boolean canAddRow(@Nullable DataGrid grid) {
        if (grid == null) {
            return false;
        }
        boolean canAddRow = grid.isEditable() &&
            grid.isReady() &&
            grid.getDataHookup().isForSingleSource() &&
            grid.getDataSupport().hasRowMutator() &&
            grid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS).getColumnCount() != 0;

        if (!canAddRow) {
            return false;
        }

        return GridHelper.get(grid).canAddRow(grid);
    }
}
