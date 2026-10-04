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
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.grid.DataAccessType;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.DataGridRequestPlace;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridDataSupport;
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

import static consulo.ui.ex.grid.GridUtil.getRowsMutator;
import static consulo.ui.ex.grid.GridUtil.showIgnoreUnsubmittedChangesYesNoDialog;

/**
 * Deletes the selected rows.
 *
 * @author Gregory.Shrago
 */
@ActionImpl(id = DeleteRowsAction.ID, shortcutFrom = @ActionRef(id = IdeActions.ACTION_EDITOR_DELETE_LINE))
public class DeleteRowsAction extends DeleteActionBase {
    public static final String ID = "Console.TableResult.DeleteRows";

    public DeleteRowsAction() {
        super(LocalizeValue.localizeTODO("Delete Selected Rows"), LocalizeValue.localizeTODO("Delete selected rows"));
    }

    @Override
    protected boolean isEnabled(DataGrid grid, ModelIndex<GridColumn> contextColumn) {
        GridDataSupport support = grid.getDataSupport();
        return grid.isReady()
            && !support.isDeletedRows(grid.getSelectionModel().getSelectedRows())
            && grid.isEditable()
            && grid.getDataHookup().isForSingleSource();
    }

    @RequiredUIAccess
    @Override
    protected void doDelete(DataGrid grid, ModelIndex<GridColumn> contextColumn) {
        deleteRows(grid, grid.getSelectionModel().getSelectedRows());
    }

    @RequiredUIAccess
    private static void deleteRows(DataGrid grid, ModelIndexSet<GridRow> rows) {
        GridMutator.RowsMutator<GridRow, GridColumn> mutator = getRowsMutator(grid);
        if (mutator == null) {
            return;
        }

        boolean areRowsInserted = true;
        for (ModelIndex<GridRow> index : rows.asIterable()) {
            areRowsInserted &= mutator.isInsertedRow(index);
        }
        if (!mutator.hasPendingChanges() || !mutator.isUpdateImmediately() || areRowsInserted) {
            deleteRows(grid, mutator, rows);
            return;
        }
        // the question is answered later
        showIgnoreUnsubmittedChangesYesNoDialog(grid, canDelete -> {
            if (canDelete) {
                deleteRows(grid, mutator, rows);
            }
        });
    }

    @RequiredUIAccess
    private static void deleteRows(DataGrid grid, GridMutator.RowsMutator<GridRow, GridColumn> mutator, ModelIndexSet<GridRow> rows) {
        GridSelection<GridRow, GridColumn> selection = grid.getSelectionModel().store();
        GridRequestSource source = new GridRequestSource(new DataGridRequestPlace(grid, rows, ModelIndexSet.forColumns(grid)));
        mutator.deleteRows(source, rows);
        source.getActionCallback().doWhenDone(() -> grid.getSelectionModel().restore(grid.getSelectionModel().fit(selection)));
    }

    @Override
    protected List<?> getTargets(DataGrid grid, ModelIndex<GridColumn> contextColumn) {
        return grid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS).getRows(grid.getSelectionModel().getSelectedRows());
    }

    @Override
    protected int itemsCount(DataGrid grid, ModelIndex<GridColumn> contextColumn) {
        return grid.getSelectionModel().getSelectedRowCount();
    }

    @Override
    protected String getItemName(DataGrid grid, ModelIndex<GridColumn> contextColumn) {
        GridRow row = grid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS).getRow(grid.getSelectionModel().getSelectedRow());
        return "#" + Objects.requireNonNull(row).getRowNum();
    }

    @Override
    protected ActionText text(DataGrid grid) {
        return new ActionText(
            LocalizeValue.localizeTODO("Delete Row"),
            LocalizeValue.localizeTODO("Delete Rows"),
            LocalizeValue.localizeTODO("Delete Row %s"),
            LocalizeValue.localizeTODO("Delete Selected Rows"),
            LocalizeValue.localizeTODO("One row selected.\nAre you sure you want to delete row %s?"),
            LocalizeValue.localizeTODO("%s rows selected.\nAre you sure you want to delete the selected rows?")
        );
    }

    @Override
    protected boolean isVisible(@Nullable DataGrid grid) {
        return grid != null && (grid.isEditable() || GridHelper.get(grid).hasTargetForEditing(grid));
    }
}
