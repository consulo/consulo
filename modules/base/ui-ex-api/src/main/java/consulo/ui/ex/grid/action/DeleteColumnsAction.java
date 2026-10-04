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
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.grid.GridUtil;
import consulo.ui.grid.DataAccessType;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.DataGridRequestPlace;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridHelper;
import consulo.ui.grid.GridMutator;
import consulo.ui.grid.GridRequestSource;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.GridSelection;
import consulo.ui.grid.ModelIndex;
import consulo.ui.grid.ModelIndexSet;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Deletes the column a column header menu was opened on, or else the selected columns. The keystroke comes from the keymap.
 */
@ActionImpl(id = DeleteColumnsAction.ID)
public class DeleteColumnsAction extends DeleteActionBase {
    public static final String ID = "Console.TableResult.DeleteColumns";

    public DeleteColumnsAction() {
        super(LocalizeValue.localizeTODO("Delete Selected Columns"), LocalizeValue.localizeTODO("Delete selected columns"));
    }

    @Override
    protected boolean isVisible(@Nullable DataGrid grid) {
        return GridUtil.canMutateColumns(grid);
    }

    @Override
    protected boolean isEnabled(DataGrid grid, ModelIndex<GridColumn> contextColumn) {
        return GridUtil.canMutateColumns(grid) && getColumns(grid, contextColumn).size() != 0;
    }

    /**
     * @param contextColumn the column a column header menu was opened on, see {@link GridUtil#getContextColumn}
     */
    public static ModelIndexSet<GridColumn> getColumns(DataGrid grid, ModelIndex<GridColumn> contextColumn) {
        return contextColumn.value != -1
            ? ModelIndexSet.forColumns(grid, contextColumn.value)
            : grid.getSelectionModel().getSelectedColumns();
    }

    @RequiredUIAccess
    @Override
    protected void doDelete(DataGrid grid, ModelIndex<GridColumn> contextColumn) {
        deleteColumns(grid, getColumns(grid, contextColumn));
    }

    @RequiredUIAccess
    public static void deleteColumns(DataGrid grid, ModelIndexSet<GridColumn> columns) {
        GridMutator.ColumnsMutator<GridRow, GridColumn> mutator = GridUtil.getColumnsMutator(grid);
        if (mutator != null) {
            GridSelection<GridRow, GridColumn> selection = grid.getSelectionModel().store();
            GridRequestSource source = new GridRequestSource(new DataGridRequestPlace(grid, ModelIndexSet.forRows(grid), columns));
            mutator.deleteColumns(source, columns);
            source.getActionCallback().doWhenDone(() -> grid.getSelectionModel().restore(grid.getSelectionModel().fit(selection)));
        }
    }

    @Override
    protected List<?> getTargets(DataGrid grid, ModelIndex<GridColumn> contextColumn) {
        return grid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS).getColumns(getColumns(grid, contextColumn));
    }

    @Override
    protected int itemsCount(DataGrid grid, ModelIndex<GridColumn> contextColumn) {
        return contextColumn.asInteger() != -1 ? 1 : grid.getSelectionModel().getSelectedColumnCount();
    }

    @Override
    protected String getItemName(DataGrid grid, ModelIndex<GridColumn> contextColumn) {
        ModelIndex<GridColumn> column = contextColumn.asInteger() == -1 ? grid.getSelectionModel().getSelectedColumn() : contextColumn;
        return Objects.requireNonNull(grid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS).getColumn(column)).getName();
    }

    @Override
    protected ActionText text(DataGrid grid) {
        String acrossCollection = GridHelper.get(grid).isModifyColumnAcrossCollection() ? " Across Collection" : "";

        return new ActionText(
            LocalizeValue.localizeTODO("Delete Column" + acrossCollection),
            LocalizeValue.localizeTODO("Delete Columns" + acrossCollection),
            LocalizeValue.localizeTODO("Delete Column %s" + acrossCollection),
            LocalizeValue.localizeTODO("Delete Selected Columns" + acrossCollection),
            LocalizeValue.localizeTODO("One column selected.\nAre you sure you want to delete column %s?"),
            LocalizeValue.localizeTODO("%s columns selected.\nAre you sure you want to delete the selected columns?")
        );
    }
}
