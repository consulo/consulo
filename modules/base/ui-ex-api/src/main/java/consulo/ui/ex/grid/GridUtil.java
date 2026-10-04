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

import consulo.application.Application;
import consulo.dataContext.UiDataProvider;
import consulo.localize.LocalizeValue;
import consulo.ui.MessageBoxes;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ContextMenuEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.grid.action.TableResultColumnHeaderPopupGroup;
import consulo.ui.ex.grid.action.TableResultPopupGroup;
import consulo.ui.ex.grid.editor.DefaultTextEditorFactory;
import consulo.ui.ex.grid.editor.GridCellEditorHelperImpl;
import consulo.ui.grid.BaseObjectFormatter;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.DataGridAppearance;
import consulo.ui.grid.DataGridRequestPlace;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridDataHookUp;
import consulo.ui.grid.GridHelper;
import consulo.ui.grid.GridHitArea;
import consulo.ui.grid.GridMutator;
import consulo.ui.grid.GridRequestSource;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.ModelIndex;
import consulo.ui.grid.ViewIndex;
import consulo.ui.grid.editor.GridCellEditorFactory;
import consulo.ui.grid.editor.GridCellEditorFactoryProvider;
import consulo.ui.grid.editor.GridCellEditorHelper;
import consulo.util.concurrent.ActionCallback;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static consulo.ui.grid.DataAccessType.DATA_WITH_MUTATIONS;

/**
 * Creating a data grid with its context menus, the configuration of a grid over CSV text, and the row and column helpers of the
 * grid actions. The helpers of the model are in {@link consulo.ui.grid.GridUtilCore}.
 */
public final class GridUtil {
    /**
     * A grid which has this flag clears the selected cells on the delete key, instead of deleting the selected rows.
     */
    public static final Key<Boolean> DELETE_CLEARS_CELLS = Key.create("DELETE_CLEARS_CELLS");

    private static final GridCellEditorFactory TEXT_EDITOR_FACTORY = new DefaultTextEditorFactory();

    private GridUtil() {
    }

    /**
     * Creates a grid which answers {@link DatabaseDataKeys#DATA_GRID_KEY} and the providers of {@link TableResultPanelUIDataRule} in
     * its data context, and shows a context menu: the column header group on a column header, the other group everywhere else.
     *
     * @param popupGroupId       the id of the action group of the cells, the row headers and the empty part of the grid
     * @param headerPopupGroupId the id of the action group of the column headers
     * @param configurator       runs before the view is built, as in {@link DataGrid#create(GridDataHookUp, BiConsumer)}
     */
    @RequiredUIAccess
    public static DataGrid createDataGrid(GridDataHookUp<GridRow, GridColumn> hookUp,
                                          String popupGroupId,
                                          String headerPopupGroupId,
                                          BiConsumer<DataGrid, DataGridAppearance> configurator) {
        DataGrid grid = DataGrid.create(hookUp, configurator);
        grid.putUserData(UiDataProvider.KEY, sink -> {
            sink.set(DatabaseDataKeys.DATA_GRID_KEY, grid);
            TableResultPanelUIDataRule.uiDataSnapshot(sink, grid);
        });
        grid.addContextMenuListener(event -> showPopup(grid, popupGroupId, headerPopupGroupId, event));
        return grid;
    }

    @RequiredUIAccess
    private static void showPopup(DataGrid grid, String popupGroupId, String headerPopupGroupId, ContextMenuEvent event) {
        if (!grid.stopEditing()) {
            grid.cancelEditing();
        }
        String groupId = grid.getContextArea() == GridHitArea.COLUMN_HEADER ? headerPopupGroupId : popupGroupId;
        ActionManager actionManager = ActionManager.getInstance();
        if (!(actionManager.getAction(groupId) instanceof ActionGroup group) || group == ActionGroup.EMPTY_GROUP) {
            return;
        }
        // a popup place, so that the column actions take the column the menu was opened on
        ActionPopupMenu menu = actionManager.createActionPopupMenu(ActionPlaces.getActionGroupPopupPlace(groupId), group);
        menu.setTargetComponent(grid);
        InputDetails details = event.getInputDetails();
        menu.show(event.getComponent(), details.getX(), details.getY());
    }

    public static BiConsumer<DataGrid, DataGridAppearance> configureCsvTable() {
        return GridUtil::configureCsvTable;
    }

    /**
     * Configures a grid over CSV text: every column is edited as text, values are shown as they are, rows are numbered. The column
     * actions follow the default {@link GridHelper}, which allows them on an editable grid of a data source with a column mutator.
     */
    public static void configureCsvTable(DataGrid grid, DataGridAppearance appearance) {
        GridCellEditorHelper.set(grid, new GridCellEditorHelperImpl());
        // a CSV value is text, whatever type its column is guessed to be
        GridCellEditorFactoryProvider.set(grid, request -> TEXT_EDITOR_FACTORY);
        BaseObjectFormatter formatter = new BaseObjectFormatter();
        grid.setObjectFormatterProvider(dataGrid -> formatter);
        appearance.setResultViewShowRowNumbers(true);
    }

    @RequiredUIAccess
    public static void focusDataGrid(@Nullable DataGrid grid) {
        if (grid != null) {
            grid.focus();
        }
    }

    /**
     * Asks whether to lose the unsubmitted changes. The answer comes later, on the UI thread; it is {@code false} when the question
     * was dismissed.
     */
    @RequiredUIAccess
    public static void showIgnoreUnsubmittedChangesYesNoDialog(DataGrid grid, @RequiredUIAccess Consumer<Boolean> answer) {
        if (Application.get().isUnitTestMode()) {
            answer.accept(true);
            return;
        }

        UIAccess uiAccess = UIAccess.current();
        MessageBoxes.yesNo()
            .asWarning()
            .title(LocalizeValue.localizeTODO("Ignore Unsubmitted Changes"))
            .text(LocalizeValue.localizeTODO("Changes are not submitted. Data will be lost. Continue?"))
            .showAsync(grid)
            .whenComplete((yes, error) -> uiAccess.giveIfNeed(() -> answer.accept(Boolean.TRUE.equals(yes))));
    }

    public static GridMutator.@Nullable RowsMutator<GridRow, GridColumn> getRowsMutator(@Nullable DataGrid grid) {
        GridMutator<GridRow, GridColumn> mutator = grid == null ? null : grid.getDataHookup().getMutator();
        return mutator instanceof GridMutator.RowsMutator<GridRow, GridColumn> rowsMutator ? rowsMutator : null;
    }

    public static GridMutator.@Nullable ColumnsMutator<GridRow, GridColumn> getColumnsMutator(@Nullable DataGrid grid) {
        GridMutator<GridRow, GridColumn> mutator = grid == null ? null : grid.getDataHookup().getMutator();
        return mutator instanceof GridMutator.ColumnsMutator<GridRow, GridColumn> columnsMutator ? columnsMutator : null;
    }

    @RequiredUIAccess
    public static void scrollToLocally(DataGrid grid, ViewIndex<GridRow> row, ViewIndex<GridColumn> column) {
        Pair<Integer, Integer> rowAndColumn = grid.getRawIndexConverter().rowAndColumn2Model().fun(row.asInteger(), column.asInteger());
        grid.getSelectionModel().setSelection(ModelIndex.forRow(grid, rowAndColumn.first), ModelIndex.forColumn(grid, rowAndColumn.second));
    }

    @RequiredUIAccess
    public static void scrollToLocally(DataGrid grid, ViewIndex<GridRow> row) {
        grid.getSelectionModel().setRowSelection(row.toModel(grid), true);
    }

    @RequiredUIAccess
    public static @Nullable ActionCallback addRows(DataGrid grid, int amount) {
        GridMutator.RowsMutator<GridRow, GridColumn> mutator = getRowsMutator(grid);
        if (amount == 0 || mutator == null) {
            return null;
        }
        GridRequestSource source = newInsertOrCloneRowRequestSource(grid);
        if (mutator.isUpdateImmediately() && mutator.hasPendingChanges()) {
            grid.submit().doWhenDone(() -> mutator.insertRows(source, amount));
        }
        else {
            mutator.insertRows(source, amount);
        }
        return source.getActionCallback();
    }

    @RequiredUIAccess
    public static @Nullable ActionCallback addRow(DataGrid grid) {
        return addRows(grid, 1);
    }

    /**
     * Inserts empty rows before a row, and selects the first of them once they are loaded.
     *
     * @param before the row the new rows go before, or {@code null} to append them
     * @return the callback of the request, or {@code null} when nothing is inserted
     */
    @RequiredUIAccess
    public static @Nullable ActionCallback insertRows(DataGrid grid, @Nullable ModelIndex<GridRow> before, int amount) {
        GridMutator.RowsMutator<GridRow, GridColumn> mutator = getRowsMutator(grid);
        if (amount == 0 || mutator == null) {
            return null;
        }
        ModelIndex<GridRow> target = before != null && before.isValid(grid) ? before : null;
        int rowIndex = target != null ? target.asInteger() : grid.getDataModel(DATA_WITH_MUTATIONS).getRowCount();
        GridRequestSource source = newInsertRowRequestSource(grid, rowIndex);
        if (mutator.isUpdateImmediately() && mutator.hasPendingChanges()) {
            grid.submit().doWhenDone(() -> mutator.insertRows(source, target, amount));
        }
        else {
            mutator.insertRows(source, target, amount);
        }
        return source.getActionCallback();
    }

    public static GridRequestSource newInsertOrCloneRowRequestSource(DataGrid grid) {
        GridRequestSource source = new GridRequestSource(new DataGridRequestPlace(grid));
        source.getActionCallback().doWhenDone(() -> {
            if (grid.isEditing()) {
                return;
            }
            GridMutator.RowsMutator<GridRow, GridColumn> mutator = getRowsMutator(grid);
            ModelIndex<GridRow> row = mutator != null ? mutator.getLastInsertedRow() : ModelIndex.forRow(grid, -1);
            row = row != null && row.isValid(grid)
                ? row
                : ModelIndex.forRow(grid, grid.getDataModel(DATA_WITH_MUTATIONS).getRowCount() - 1);
            scrollToLocally(grid, row.toView(grid));
        });
        return source;
    }

    /**
     * A request source which selects the inserted row once the request is done: the last inserted row of the mutator when it tracks
     * one, else the row at {@code rowIndex}, else the last row.
     */
    public static GridRequestSource newInsertRowRequestSource(DataGrid grid, int rowIndex) {
        GridRequestSource source = new GridRequestSource(new DataGridRequestPlace(grid));
        source.getActionCallback().doWhenDone(() -> {
            if (grid.isEditing()) {
                return;
            }
            GridMutator.RowsMutator<GridRow, GridColumn> mutator = getRowsMutator(grid);
            ModelIndex<GridRow> inserted = mutator != null ? mutator.getLastInsertedRow() : null;
            ModelIndex<GridRow> row = inserted != null && inserted.isValid(grid) ? inserted : ModelIndex.forRow(grid, rowIndex);
            if (!row.isValid(grid)) {
                row = ModelIndex.forRow(grid, grid.getDataModel(DATA_WITH_MUTATIONS).getRowCount() - 1);
            }
            scrollToLocally(grid, row.toView(grid));
        });
        return source;
    }

    public static boolean canMutateColumns(@Nullable DataGrid grid) {
        return grid != null && GridHelper.get(grid).canMutateColumns(grid);
    }

    /**
     * The column a column header menu of the grid was opened on, for an action run from that menu. An action run in any other way
     * - with a shortcut, from the menu of a cell - gets an index of {@code -1}, and works on the selection: the column of an earlier
     * menu does not apply to it.
     */
    public static ModelIndex<GridColumn> getContextColumn(DataGrid grid, AnActionEvent e) {
        return getContextColumn(grid, e.getPlace());
    }

    /**
     * @see #getContextColumn(DataGrid, AnActionEvent)
     */
    public static ModelIndex<GridColumn> getContextColumn(DataGrid grid, @Nullable String place) {
        if (place != null && ActionPlaces.isPopupPlace(place) && grid.getContextArea() == GridHitArea.COLUMN_HEADER) {
            return grid.getContextColumn();
        }
        return ModelIndex.forColumn(grid, -1);
    }

    public static ActionGroup getGridColumnHeaderPopupActions() {
        return ActionManager.getInstance().getAction(TableResultColumnHeaderPopupGroup.ID) instanceof ActionGroup group
            ? group
            : ActionGroup.EMPTY_GROUP;
    }

    public static ActionGroup getGridPopupActions() {
        return ActionManager.getInstance().getAction(TableResultPopupGroup.ID) instanceof ActionGroup group
            ? group
            : ActionGroup.EMPTY_GROUP;
    }
}
