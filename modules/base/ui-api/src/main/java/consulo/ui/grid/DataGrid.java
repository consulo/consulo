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
package consulo.ui.grid;

import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.grid.color.GridColorModel;
import consulo.ui.internal.UIInternal;
import consulo.util.concurrent.ActionCallback;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A grid showing the data of a {@link GridDataHookUp}, on every frontend.
 * <p/>
 * It carries the members which work on the grid model. What depends on a widget toolkit or the IDE - the panel, the result
 * view, content language, find - and the features not supported yet - presentation modes, transposition, filtering, value
 * editor - stay out of the contract. Colors come as {@link consulo.ui.color.ColorValue} values through {@link #getColorModel()} - a
 * theme color, such as a {@link consulo.ui.style.ComponentColors} entry, follows the current theme - and column widths are counted
 * in characters of the grid font, so that they mean the same on every frontend.
 * <p/>
 * The grid owns its scrolling and its paging bar, so it is not wrapped in a {@code ScrollableLayout}. It loads the first
 * page of its data source itself, unless the source already holds rows.
 */
public interface DataGrid extends Component, CoreGrid<GridRow, GridColumn> {
    @RequiredUIAccess
    static DataGrid create(GridDataHookUp<GridRow, GridColumn> hookUp) {
        return create(hookUp, (grid, appearance) -> {
        });
    }

    /**
     * @param configurator runs before the view is built: it fills in the appearance, and installs what the grid needs, such as its
     *                     cell editors
     */
    @RequiredUIAccess
    static DataGrid create(GridDataHookUp<GridRow, GridColumn> hookUp, BiConsumer<DataGrid, DataGridAppearance> configurator) {
        return UIInternal.get()._DataGrid_create(hookUp, configurator);
    }

    boolean isEmpty();

    void fireContentChanged(GridRequestSource.@Nullable RequestPlace place);

    RowSortOrder.Type getSortOrder(ModelIndex<GridColumn> column);

    /**
     * @return the priority of the column among the sorted ones, starting from 1, or 0 when the column is not sorted
     */
    int getThenBySortOrder(ModelIndex<GridColumn> column);

    @RequiredUIAccess
    void toggleSortColumns(List<ModelIndex<GridColumn>> columns, boolean additive);

    @RequiredUIAccess
    void sortColumns(List<ModelIndex<GridColumn>> columns, RowSortOrder.Type order, boolean additive);

    int countSortedColumns();

    @Nullable
    Comparator<GridRow> getComparator(ModelIndex<GridColumn> column);

    int getVisibleColumnCount();

    String getName(GridColumn column);

    ObjectFormatter getObjectFormatter();

    void setObjectFormatterProvider(Function<DataGrid, ObjectFormatter> provider);

    void addDataGridListener(DataGridListener listener, Disposable disposable);

    DataGridAppearance getAppearance();

    /**
     * The column the last context menu of the grid was opened on: the column under the pointer, for a cell or a column header.
     *
     * @return the column, or an index of {@code -1} when the menu was opened on a row header or on the empty part of the grid, or
     * when no menu was opened yet
     */
    default ModelIndex<GridColumn> getContextColumn() {
        throw new UnsupportedOperationException("The context column is tracked by the grid controller");
    }

    /**
     * @return the part of the grid the last context menu was opened on, {@link GridHitArea#EMPTY} when no menu was opened yet
     */
    default GridHitArea getContextArea() {
        throw new UnsupportedOperationException("The context area is tracked by the grid controller");
    }

    /**
     * @return the width of the column in characters of the grid font, or {@code 0} when the grid has no width for it - the column
     * is not valid, or it was not laid out yet
     */
    default int getColumnWidth(ModelIndex<GridColumn> column) {
        throw new UnsupportedOperationException("Column widths are kept by the grid controller");
    }

    /**
     * Sets the width of the column, as the user resizing it would. The width is kept when the columns of the data source change,
     * as long as a column of the same name, or else at the same index, remains.
     *
     * @param chars the width in characters of the grid font, at least {@code 1}
     */
    @RequiredUIAccess
    default void setColumnWidth(ModelIndex<GridColumn> column, int chars) {
        throw new UnsupportedOperationException("Column widths are kept by the grid controller");
    }

    /**
     * Sets the width of every column to its content: the longest line of the header and of the loaded values.
     *
     * @param maxChars the most characters of the grid font a column gets, or {@code 0} for no limit
     */
    @RequiredUIAccess
    default void fitColumnWidths(int maxChars) {
        throw new UnsupportedOperationException("Column widths are kept by the grid controller");
    }

    /**
     * The colors of the cells and row headers. It is a {@link consulo.ui.grid.color.GridColorModelImpl} whose first layer shows the
     * pending changes; a data source adds its own layers to it, usually from the configurator of the grid.
     */
    default GridColorModel getColorModel() {
        throw new UnsupportedOperationException("The color model is owned by the grid controller");
    }

    /**
     * Commits the open cell editor, then submits the pending changes of the data source.
     *
     * @return a callback which is done once the changes are submitted, at once when there are none, and rejected when the edit
     * could not be committed or the submit failed
     */
    @RequiredUIAccess
    default ActionCallback submit() {
        throw new UnsupportedOperationException("Submitting is done by the grid controller");
    }

    /**
     * Gives the keyboard focus to the cells of the grid, so the keys of the grid act on its selection - for one, after an action
     * run from elsewhere selected a new row.
     */
    @RequiredUIAccess
    void focus();
}
