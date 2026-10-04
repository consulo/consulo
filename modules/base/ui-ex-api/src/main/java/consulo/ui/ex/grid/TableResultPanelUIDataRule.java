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
package consulo.ui.ex.grid;

import consulo.dataContext.DataSink;
import consulo.ui.ex.CopyProvider;
import consulo.ui.ex.DeleteProvider;
import consulo.ui.ex.grid.action.ClearCellsAction;
import consulo.ui.ex.grid.action.DeleteRowsAction;
import consulo.ui.grid.DataGrid;

/**
 * The copy and delete providers of a grid.
 * <p/>
 * It is not an application-wide data rule: the data provider which {@link GridUtil#createDataGrid} installs on the grid applies
 * it, so the providers answer only where the grid itself is the nearest component of the data context - not for a component next
 * to the grid which answers {@link DatabaseDataKeys#DATA_GRID_KEY} as well. While a cell editor is open, the grid gives no
 * providers, so copy and delete reach the text of the editor.
 * <p/>
 * The paste provider is not given yet.
 */
public final class TableResultPanelUIDataRule {
    private TableResultPanelUIDataRule() {
    }

    public static void uiDataSnapshot(DataSink sink, DataGrid grid) {
        if (grid.isEditing()) {
            return;
        }
        sink.set(CopyProvider.KEY, new GridCopyProvider(grid));
        // a grid whose cells are cleared by the delete key - a CSV table - asks for it with a flag
        boolean clearsCells = Boolean.TRUE.equals(grid.getUserData(GridUtil.DELETE_CLEARS_CELLS));
        sink.set(DeleteProvider.KEY, clearsCells ? new ClearCellsAction() : new DeleteRowsAction());
    }
}
