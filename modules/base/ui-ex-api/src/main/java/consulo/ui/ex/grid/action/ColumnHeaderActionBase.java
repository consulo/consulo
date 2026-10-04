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
import consulo.ui.grid.ModelIndexSet;

/**
 * An action on the column a column header menu was opened on. An action which is invoked on the grid as well - with a shortcut, from
 * the menu of a cell - works on the selected columns there.
 */
public abstract class ColumnHeaderActionBase extends DumbAwareAction implements GridAction, AnActionWithSyncUpdate {
    private final boolean myInvokeOnGrid;

    protected ColumnHeaderActionBase(LocalizeValue text) {
        this(text, false);
    }

    protected ColumnHeaderActionBase(LocalizeValue text, boolean invokeOnGrid) {
        super(text);
        myInvokeOnGrid = invokeOnGrid;
    }

    @Override
    public void update(AnActionEvent e) {
        DataGrid grid = e.getData(DatabaseDataKeys.DATA_GRID_KEY);
        e.getPresentation().setEnabledAndVisible(grid != null);
        if (grid == null) {
            return;
        }
        update(e, grid, getColumns(e, grid));
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        DataGrid grid = e.getData(DatabaseDataKeys.DATA_GRID_KEY);
        if (grid != null) {
            actionPerformed(e, grid, getColumns(e, grid));
        }
    }

    protected void update(AnActionEvent e, DataGrid grid, ModelIndexSet<GridColumn> columnIdxs) {
        e.getPresentation().setEnabledAndVisible(columnIdxs.size() > 0 && isValid(columnIdxs, grid));
    }

    protected ModelIndexSet<GridColumn> getColumns(AnActionEvent e, DataGrid grid) {
        ModelIndex<GridColumn> column = GridUtil.getContextColumn(grid, e);
        return column.value != -1 || !myInvokeOnGrid
            ? ModelIndexSet.forColumns(grid, column.value)
            : grid.getSelectionModel().getSelectedColumns();
    }

    private static boolean isValid(ModelIndexSet<GridColumn> idxs, DataGrid grid) {
        return idxs.asIterable().find(i -> !i.isValid(grid)) == null;
    }

    @RequiredUIAccess
    protected abstract void actionPerformed(AnActionEvent e, DataGrid grid, ModelIndexSet<GridColumn> columnIdxs);
}
