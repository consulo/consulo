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
import consulo.ui.InputBoxBuilder;
import consulo.ui.InputProblem;
import consulo.ui.TextBox;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.grid.DatabaseDataKeys;
import consulo.ui.ex.grid.GridUtil;
import consulo.ui.ex.grid.csv.CsvFormat;
import consulo.ui.ex.grid.csv.CsvFormatHookUp;
import consulo.ui.grid.DataAccessType;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.DataGridRequestPlace;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridDataHookUp;
import consulo.ui.grid.GridMutator;
import consulo.ui.grid.GridRequestSource;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.GridUtilCore;
import consulo.ui.grid.ModelIndex;
import consulo.ui.grid.ViewIndex;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Appends a column. The user names it first, unless the records of the data source have no header to hold the name.
 */
@ActionImpl(id = AddColumnAction.ID, shortcutFrom = @ActionRef(id = "EditorToggleColumnMode"))
public class AddColumnAction extends DumbAwareAction implements GridEditAction, AnActionWithSyncUpdate {
    public static final String ID = "Console.TableResult.AddColumn";

    public AddColumnAction() {
        super(LocalizeValue.localizeTODO("Add Column"), LocalizeValue.empty(), PlatformIconGroup.generalAdd());
    }

    @Override
    public void update(AnActionEvent e) {
        DataGrid grid = e.getData(DatabaseDataKeys.DATA_GRID_KEY);
        // while a cell editor is open its keys reach the editor, and the change of the columns does not drop the edit
        e.getPresentation().setEnabledAndVisible(grid != null && !grid.isEditing() && GridUtil.canMutateColumns(grid));
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        DataGrid dataGrid = e.getData(DatabaseDataKeys.DATA_GRID_KEY);
        if (dataGrid == null) {
            return;
        }
        insertColumn(dataGrid);
    }

    @RequiredUIAccess
    public static void insertColumn(DataGrid grid) {
        askColumnName(grid, name -> insertColumn(grid, name));
    }

    /**
     * Inserts a column before a column, once the user named it.
     *
     * @param before the column the new column goes before, or {@code null} to append it
     */
    @RequiredUIAccess
    public static void insertColumnBefore(DataGrid grid, @Nullable ModelIndex<GridColumn> before) {
        GridColumn beforeColumn = before != null ? grid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS).getColumn(before) : null;
        askColumnName(grid, name -> {
            // Consulo: another column may hold the index once the name is given - the columns of a parse landed meanwhile
            if (before == null || grid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS).getColumn(before) == beforeColumn) {
                insertColumnBefore(grid, before, name);
            }
        });
    }

    /**
     * Asks the name of a new column, and passes it on later, on the UI thread - nothing is passed when the user dismissed the box. A
     * data source whose records have no header gets no name at once, without a question.
     */
    @RequiredUIAccess
    private static void askColumnName(DataGrid grid, @RequiredUIAccess Consumer<@Nullable String> insert) {
        GridDataHookUp<?, ?> hookup = grid.getDataHookup();
        if (hookup instanceof CsvFormatHookUp csvHookUp) {
            CsvFormat format = csvHookUp.getFormat();
            if (format.headerRecord == null) {
                insert.accept(null);
                return;
            }
        }
        String defaultColumnName = GridUtilCore.generateColumnName(grid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS));
        UIAccess uiAccess = UIAccess.current();
        InputBoxBuilder.text()
            .title(LocalizeValue.localizeTODO("New Column Name"))
            .text(LocalizeValue.localizeTODO("Name:"))
            .value(defaultColumnName)
            .setupComponent(TextBox::selectAll)
            .validator(AddColumnAction::validateColumnName)
            .showAsync(grid)
            .whenComplete((name, error) -> {
                if (error != null || StringUtil.isEmptyOrSpaces(name)) {
                    return;
                }
                uiAccess.giveIfNeed(() -> insert.accept(name));
            });
    }

    /**
     * Refuses an empty name, which the user may not confirm.
     */
    @RequiredUIAccess
    static @Nullable InputProblem validateColumnName(String name) {
        return StringUtil.isEmptyOrSpaces(name) ? InputProblem.error(LocalizeValue.localizeTODO("The column name is empty")) : null;
    }

    @RequiredUIAccess
    public static void insertColumn(DataGrid grid, @Nullable String columnName) {
        GridMutator.ColumnsMutator<GridRow, GridColumn> mutator = GridUtil.getColumnsMutator(grid);
        if (mutator == null) {
            return;
        }
        if (mutator.isUpdateImmediately() && mutator.hasPendingChanges()) {
            grid.submit().doWhenDone(() -> mutator.insertColumn(newInsertOrCloneColumnRequestSource(grid), columnName));
            return;
        }
        mutator.insertColumn(newInsertOrCloneColumnRequestSource(grid), columnName);
    }

    /**
     * Inserts a column before a column, and selects it once it is loaded.
     *
     * @param before     the column the new column goes before, or {@code null} to append it
     * @param columnName the name of the new column, or {@code null} for a generated one
     */
    @RequiredUIAccess
    public static void insertColumnBefore(DataGrid grid, @Nullable ModelIndex<GridColumn> before, @Nullable String columnName) {
        GridMutator.ColumnsMutator<GridRow, GridColumn> mutator = GridUtil.getColumnsMutator(grid);
        if (mutator == null) {
            return;
        }
        ModelIndex<GridColumn> target = before != null && before.isValid(grid) ? before : null;
        int columnIndex = target != null ? target.asInteger() : grid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS).getColumnCount();
        if (mutator.isUpdateImmediately() && mutator.hasPendingChanges()) {
            grid.submit().doWhenDone(() -> mutator.insertColumn(newInsertColumnRequestSource(grid, columnIndex), target, columnName));
            return;
        }
        mutator.insertColumn(newInsertColumnRequestSource(grid, columnIndex), target, columnName);
    }

    public static GridRequestSource newInsertOrCloneColumnRequestSource(DataGrid grid) {
        GridRequestSource source = new GridRequestSource(new DataGridRequestPlace(grid));
        source.getActionCallback().doWhenDone(() -> {
            ModelIndex<GridColumn> column =
                ModelIndex.forColumn(grid, grid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS).getColumnCount() - 1);
            GridUtil.scrollToLocally(grid, ViewIndex.forRow(grid, 0), column.toView(grid));
        });
        return source;
    }

    /**
     * A request source which selects the column at {@code columnIndex} once the request is done, or the last column when there is
     * none at that index.
     */
    public static GridRequestSource newInsertColumnRequestSource(DataGrid grid, int columnIndex) {
        GridRequestSource source = new GridRequestSource(new DataGridRequestPlace(grid));
        source.getActionCallback().doWhenDone(() -> {
            ModelIndex<GridColumn> column = ModelIndex.forColumn(grid, columnIndex);
            if (!column.isValid(grid)) {
                column = ModelIndex.forColumn(grid, grid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS).getColumnCount() - 1);
            }
            GridUtil.scrollToLocally(grid, ViewIndex.forRow(grid, 0), column.toView(grid));
        });
        return source;
    }
}
