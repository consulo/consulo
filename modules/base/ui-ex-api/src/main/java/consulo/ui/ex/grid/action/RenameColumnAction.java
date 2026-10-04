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
import consulo.ui.InputBoxBuilder;
import consulo.ui.TextBox;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.grid.GridUtil;
import consulo.ui.ex.grid.csv.CsvFormatHookUp;
import consulo.ui.grid.DataAccessType;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.DataGridRequestPlace;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridDataHookUp;
import consulo.ui.grid.GridMutator;
import consulo.ui.grid.GridRequestSource;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.ModelIndex;
import consulo.ui.grid.ModelIndexSet;
import consulo.util.lang.StringUtil;

/**
 * Renames a column: a column inserted and not submitted yet, or any column of a data source whose records are parsed with a CSV
 * format, where the name is the header record.
 */
@ActionImpl(id = RenameColumnAction.ID)
public class RenameColumnAction extends SingleColumnHeaderAction {
    public static final String ID = "Console.TableResult.RenameColumn";

    public RenameColumnAction() {
        super(LocalizeValue.localizeTODO("Rename…"), true);
    }

    @Override
    protected void update(AnActionEvent e, DataGrid grid, ModelIndexSet<GridColumn> columnIdxs) {
        GridDataHookUp<?, ?> hookUp = grid.getDataHookup();
        e.getPresentation().setEnabledAndVisible(columnIdxs.size() == 1 && columnIdxs.first().isValid(grid) &&
            (grid.getDataSupport().isInsertedColumn(columnIdxs.first()) ||
                hookUp instanceof CsvFormatHookUp));
    }

    @RequiredUIAccess
    @Override
    protected void actionPerformed(AnActionEvent e, DataGrid grid, ModelIndex<GridColumn> columnIdx) {
        GridColumn column = grid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS).getColumn(columnIdx);
        if (column == null) {
            return;
        }
        String name = column.getName();
        UIAccess uiAccess = UIAccess.current();
        InputBoxBuilder.text()
            .title(LocalizeValue.localizeTODO("Rename Column"))
            .text(LocalizeValue.localizeTODO("Name:"))
            .value(name)
            .setupComponent(TextBox::selectAll)
            .validator(AddColumnAction::validateColumnName)
            .showAsync(grid)
            .whenComplete((newName, error) -> {
                if (error != null || StringUtil.isEmptyOrSpaces(newName)) {
                    return;
                }
                uiAccess.giveIfNeed(() -> {
                    // Consulo: another column may hold the index once the box is closed - the columns of a parse landed meanwhile
                    if (grid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS).getColumn(columnIdx) == column) {
                        renameColumn(grid, columnIdx, newName);
                    }
                });
            });
    }

    @RequiredUIAccess
    private static void renameColumn(DataGrid grid, ModelIndex<GridColumn> idx, String name) {
        GridMutator.ColumnsMutator<GridRow, GridColumn> mutator = GridUtil.getColumnsMutator(grid);
        if (mutator == null) {
            return;
        }
        GridRequestSource source = new GridRequestSource(new DataGridRequestPlace(grid));
        if (mutator.isUpdateImmediately() && mutator.hasPendingChanges()) {
            grid.submit().doWhenDone(() -> mutator.renameColumn(source, idx, name));
            return;
        }
        mutator.renameColumn(source, idx, name);
    }
}
