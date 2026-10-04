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

import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utilities of the grid model and the cell editing. None of them works on documents, PSI or JDBC errors, which are not visible
 * from {@code consulo.ui.api}.
 */
public final class GridUtilCore {
    public static final String FAILED_TO_LOAD_PREFIX = "<failed to load>";
    public static final String COLUMN_NAME_PREFIX = "column ";

    private GridUtilCore() {
    }

    /**
     * @return the data source of the grid if it is an instance of the class, else {@code null}
     */
    public static <R, C, T extends GridDataHookUp<R, C>> @Nullable T getHookUp(@Nullable CoreGrid<R, C> grid, Class<T> clazz) {
        GridDataHookUp<R, C> hookup = grid != null ? grid.getDataHookup() : null;
        return ObjectUtil.tryCast(hookup, clazz);
    }

    /**
     * Groups cell changes by row: one row change per row which still exists in the model, ordered by row number.
     */
    public static List<RowMutation> mergeAll(List<? extends CellMutation> mutations, GridModel<GridRow, GridColumn> model) {
        List<CellMutation> copy = new ArrayList<>(mutations);
        List<RowMutation> rowMutations = new ArrayList<>();
        while (!copy.isEmpty()) {
            RowMutation merged = merge(copy, model);
            if (merged != null) {
                rowMutations.add(merged);
            }
        }
        Collections.sort(rowMutations);
        return rowMutations;
    }

    /**
     * Takes the first change and every other change of its row out of the list, and merges them into one row change.
     */
    private static @Nullable RowMutation merge(List<CellMutation> mutations, GridModel<GridRow, GridColumn> model) {
        CellMutation item = ContainerUtil.getFirstItem(mutations);
        if (item == null) {
            throw new IllegalStateException("Shouldn't call merge() when there is no pending changes");
        }
        mutations.remove(item);
        List<CellMutation> toMerge = ContainerUtil.filter(mutations, item::canMergeByRowWith);
        mutations.removeAll(toMerge);
        RowMutation merged = item.createRowMutation(model);
        for (CellMutation mutation : toMerge) {
            if (merged == null) {
                merged = mutation.createRowMutation(model);
            }
            else {
                merged = merged.merge(mutation.createRowMutation(model));
            }
        }
        return merged;
    }

    /**
     * Whether the value stands for a value which failed to load. The row comparator of the model needs it too.
     */
    public static boolean isFailedToLoad(@Nullable Object value) {
        return value instanceof String s && s.startsWith(FAILED_TO_LOAD_PREFIX);
    }

    public static List<CellMutation> createMutations(ModelIndexSet<GridRow> rows,
                                                     ModelIndexSet<GridColumn> columns,
                                                     @Nullable Object value) {
        List<CellMutation> mutations = new ArrayList<>();
        for (ModelIndex<GridRow> rowIdx : rows.asIterable()) {
            for (ModelIndex<GridColumn> columnIdx : columns.asIterable()) {
                mutations.add(new CellMutation(rowIdx, columnIdx, value));
            }
        }
        return mutations;
    }

    /**
     * @return the first column of the name, or an index of {@code -1} when there is none
     */
    public static <R extends GridRow, C extends GridColumn> ModelIndex<C> findColumn(GridModel<R, C> gridModel,
                                                                                     @Nullable String name,
                                                                                     boolean caseSensitive) {
        for (ModelIndex<C> c : gridModel.getColumnIndices().asIterable()) {
            C column = gridModel.getColumn(c);
            if (column != null && Comparing.strEqual(name, column.getName(), caseSensitive)) {
                return c;
            }
        }
        return ModelIndex.forColumn(gridModel, -1);
    }

    /**
     * @return a name for a new column, {@code "column <n>"}, which no column of the model has yet (ignoring case)
     */
    public static String generateColumnName(GridModel<? extends GridRow, ? extends GridColumn> model) {
        int idx = model.getColumnCount() + 1;
        String name = COLUMN_NAME_PREFIX + idx;
        while (findColumn(model, name, false).asInteger() != -1) {
            idx++;
            name = COLUMN_NAME_PREFIX + idx;
        }
        return name;
    }

    public static boolean isRowId(@Nullable GridColumn column) {
        return column != null && column.getAttributes().contains(ColumnDescriptor.Attribute.ROW_ID);
    }

    public static boolean isVirtualColumn(@Nullable GridColumn column) {
        return column != null && column.getAttributes().contains(ColumnDescriptor.Attribute.VIRTUAL);
    }

    /**
     * The message of the error, without the SQL state and vendor code of a {@code java.sql.SQLException}: that prefix belongs to
     * the JDBC data source, which builds the message of its own errors.
     */
    public static String getLongMessage(Throwable e) {
        return getMessage(e);
    }

    public static String getMessage(Throwable t) {
        String m = t.getMessage();
        return StringUtil.isNotEmpty(m) ? m.trim() : t.getClass().getName();
    }

    public static boolean isPageSizeUnlimited(int pageSize) {
        return pageSize < 1;
    }
}
