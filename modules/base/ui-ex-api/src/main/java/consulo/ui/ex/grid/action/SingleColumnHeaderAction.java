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
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.ModelIndex;
import consulo.ui.grid.ModelIndexSet;

/**
 * A column header action which works on one column only.
 */
public abstract class SingleColumnHeaderAction extends ColumnHeaderActionBase {
    public SingleColumnHeaderAction(LocalizeValue text, boolean invokeOnGrid) {
        super(text, invokeOnGrid);
    }

    @RequiredUIAccess
    @Override
    protected void actionPerformed(AnActionEvent e, DataGrid grid, ModelIndexSet<GridColumn> columnIdxs) {
        actionPerformed(e, grid, columnIdxs.first());
    }

    @RequiredUIAccess
    protected abstract void actionPerformed(AnActionEvent e, DataGrid grid, ModelIndex<GridColumn> columnIdx);

    @Override
    protected void update(AnActionEvent e, DataGrid grid, ModelIndexSet<GridColumn> columnIdxs) {
        e.getPresentation().setEnabledAndVisible(columnIdxs.size() == 1 && columnIdxs.first().isValid(grid));
    }
}
