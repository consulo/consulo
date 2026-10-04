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
package consulo.ui.internal;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.grid.*;
import consulo.ui.grid.color.GridColorModel;
import consulo.util.concurrent.ActionCallback;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * A {@link DataGrid} whose grid logic is its {@link DataGridController}: every member of the contract which is not about
 * the component itself is answered by the controller, so a frontend only builds the native view.
 *
 * @since 2026-10-03
 */
public interface DataGridControllerOwner extends DataGrid {
    DataGridController getController();

    @Override
    default GridModel<GridRow, GridColumn> getDataModel(DataAccessType reason) {
        return reason.getModel(getController().getHookUp());
    }

    @Override
    default GridDataHookUp<GridRow, GridColumn> getDataHookup() {
        return getController().getHookUp();
    }

    @Override
    default SelectionModel<GridRow, GridColumn> getSelectionModel() {
        return getController().getSelectionModel();
    }

    @Override
    default GridDataSupport getDataSupport() {
        return getController().getDataSupport();
    }

    @Override
    default RawIndexConverter getRawIndexConverter() {
        return getController().getRawIndexConverter();
    }

    @Override
    default String getFilterText() {
        return "";
    }

    @Override
    default void setFilterText(String filter, int caretPosition) {
    }

    @Override
    default boolean isReady() {
        return !getDataModel(DataAccessType.DATABASE_DATA).isUpdatingNow();
    }

    @Override
    default CoreResultView getResultView() {
        return getController().getResultView();
    }

    @Override
    default ModelIndexSet<GridColumn> getVisibleColumns() {
        return getController().getVisibleColumns();
    }

    @Override
    default ModelIndexSet<GridRow> getVisibleRows() {
        return getController().getVisibleRows();
    }

    @Override
    default int getVisibleRowsCount() {
        return getController().getViewRowCount();
    }

    @Override
    default boolean isViewModified() {
        return getController().isViewModified();
    }

    @Override
    @RequiredUIAccess
    default void resetView() {
        getController().resetView();
    }

    @Override
    @RequiredUIAccess
    default void showCell(int absoluteRowIdx, ModelIndex<GridColumn> column) {
        getController().showCell(absoluteRowIdx, column);
    }

    @Override
    default boolean isColumnEnabled(ModelIndex<GridColumn> column) {
        return getController().isColumnEnabled(column);
    }

    @Override
    @RequiredUIAccess
    default void setColumnEnabled(ModelIndex<GridColumn> column, boolean state) {
        getController().setColumnEnabled(column, state);
    }

    @Override
    default void setRowEnabled(ModelIndex<GridRow> rowIdx, boolean state) {
        // hiding rows comes with the local filter
    }

    @Override
    default String getDisplayName() {
        return "";
    }

    @Override
    default boolean isEditable() {
        return getController().isEditable();
    }

    @Override
    default boolean isCellEditingAllowed() {
        return getController().isCellEditingAllowed();
    }

    @Override
    @RequiredUIAccess
    default void setCells(ModelIndexSet<GridRow> rows, ModelIndexSet<GridColumn> columns, @Nullable Object value) {
        getController().setCells(rows, columns, value);
    }

    @Override
    default boolean isEditing() {
        return getController().isEditing();
    }

    @Override
    @RequiredUIAccess
    default boolean stopEditing() {
        return getController().stopEditing();
    }

    @Override
    @RequiredUIAccess
    default void cancelEditing() {
        getController().cancelEditing();
    }

    @Override
    @RequiredUIAccess
    default void editSelectedCell() {
        getController().editSelectedCell();
    }

    @Override
    @RequiredUIAccess
    default void editSelectedCellWithValue(@Nullable Object value) {
        getController().editSelectedCellWithValue(value, true);
    }

    @Override
    default String getUnambiguousColumnName(ModelIndex<GridColumn> column) {
        GridColumn gridColumn = getDataModel(DataAccessType.DATA_WITH_MUTATIONS).getColumn(column);
        return gridColumn == null ? "" : getName(gridColumn);
    }

    @Override
    default boolean isEmpty() {
        return getDataModel(DataAccessType.DATA_WITH_MUTATIONS).getRowCount() == 0;
    }

    @Override
    default void fireContentChanged(GridRequestSource.@Nullable RequestPlace place) {
        getController().fireContentChanged(place);
    }

    @Override
    default RowSortOrder.Type getSortOrder(ModelIndex<GridColumn> column) {
        return getController().getSortOrder(column);
    }

    @Override
    default int getThenBySortOrder(ModelIndex<GridColumn> column) {
        return getController().getThenBySortOrder(column);
    }

    @Override
    @RequiredUIAccess
    default void toggleSortColumns(List<ModelIndex<GridColumn>> columns, boolean additive) {
        getController().toggleSortColumns(columns, additive);
    }

    @Override
    @RequiredUIAccess
    default void sortColumns(List<ModelIndex<GridColumn>> columns, RowSortOrder.Type order, boolean additive) {
        getController().sortColumns(columns, order, additive);
    }

    @Override
    default int countSortedColumns() {
        return getController().countSortedColumns();
    }

    @Override
    default @Nullable Comparator<GridRow> getComparator(ModelIndex<GridColumn> column) {
        GridColumn gridColumn = getDataModel(DataAccessType.DATA_WITH_MUTATIONS).getColumn(column);
        return gridColumn == null ? null : GridRowComparator.create(gridColumn);
    }

    @Override
    default int getVisibleColumnCount() {
        return getController().getViewColumnCount();
    }

    @Override
    default String getName(GridColumn column) {
        return getController().getName(column);
    }

    @Override
    default ObjectFormatter getObjectFormatter() {
        return getController().getObjectFormatter();
    }

    @Override
    @RequiredUIAccess
    default void setObjectFormatterProvider(Function<DataGrid, ObjectFormatter> provider) {
        getController().setObjectFormatterProvider(provider);
    }

    @Override
    default void addDataGridListener(DataGridListener listener, Disposable disposable) {
        getController().addDataGridListener(listener, disposable);
    }

    @Override
    default DataGridAppearance getAppearance() {
        return getController().getAppearance();
    }

    @Override
    default ModelIndex<GridColumn> getContextColumn() {
        return getController().getContextColumn();
    }

    @Override
    default GridHitArea getContextArea() {
        return getController().getContextArea();
    }

    @Override
    default int getColumnWidth(ModelIndex<GridColumn> column) {
        return getController().getColumnWidth(column);
    }

    @Override
    @RequiredUIAccess
    default void setColumnWidth(ModelIndex<GridColumn> column, int chars) {
        getController().setColumnWidth(column, chars);
    }

    @Override
    @RequiredUIAccess
    default void fitColumnWidths(int maxChars) {
        getController().fitColumnWidths(maxChars);
    }

    @Override
    default GridColorModel getColorModel() {
        return getController().getColorModel();
    }

    @Override
    @RequiredUIAccess
    default ActionCallback submit() {
        return getController().submit();
    }

    @Override
    default void dispose() {
        Disposer.dispose(getController());
    }
}
