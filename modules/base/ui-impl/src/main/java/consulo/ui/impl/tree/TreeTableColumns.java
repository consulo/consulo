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
package consulo.ui.impl.tree;

import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.RenderItem;
import consulo.ui.TableColumn;
import consulo.ui.TextItemPresentation;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.impl.table.TableColumnImpl;
import consulo.ui.impl.table.TableColumnOwner;
import consulo.util.lang.ControlFlowException;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class TreeTableColumns<E> {
    private static final Logger LOG = Logger.getInstance(TreeTableColumns.class);

    private final List<TableColumnImpl<E, ?>> myColumns = new CopyOnWriteArrayList<>();

    private volatile int mySortColumn = -1;
    private volatile boolean mySortAscending = true;

    public <V> TableColumnImpl<E, V> add(TableColumnOwner owner, LocalizeValue header, Function<E, V> valueProvider) {
        TableColumnImpl<E, V> column = new TableColumnImpl<>(owner, myColumns.size(), header, valueProvider);
        myColumns.add(column);
        return column;
    }

    public List<TableColumnImpl<E, ?>> getColumns() {
        return List.copyOf(myColumns);
    }

    public List<TableColumn<E, ?>> getTableColumns() {
        return List.copyOf(myColumns);
    }

    public int size() {
        return myColumns.size();
    }

    public TableColumnImpl<E, ?> get(int index) {
        return myColumns.get(index);
    }

    public List<@Nullable Object> values(@Nullable E value) {
        if (value == null) {
            return List.of();
        }

        List<@Nullable Object> values = new ArrayList<>(myColumns.size());
        for (TableColumnImpl<E, ?> column : myColumns) {
            values.add(computeValue(column, value));
        }
        return Collections.unmodifiableList(values);
    }

    private static <T> @Nullable Object computeValue(TableColumnImpl<T, ?> column, T item) {
        try {
            return column.getValue(item);
        }
        catch (RuntimeException e) {
            if (e instanceof ControlFlowException || e instanceof CancellationException) {
                throw e;
            }

            LOG.error("Column " + column.getIndex() + " failed to compute the value of " + item, e);
            return null;
        }
    }

    public @Nullable Comparator<TreeNodeImpl<E>> comparator(int columnIndex, boolean ascending) {
        if (columnIndex < 0 || columnIndex >= myColumns.size()) {
            return null;
        }
        return comparator(myColumns.get(columnIndex), columnIndex, ascending);
    }

    private static <T, V> @Nullable Comparator<TreeNodeImpl<T>> comparator(TableColumnImpl<T, V> column,
                                                                          int columnIndex,
                                                                          boolean ascending) {
        Comparator<V> valueComparator = column.getComparator();
        if (valueComparator == null) {
            return null;
        }

        Comparator<@Nullable V> nullsFirst = Comparator.nullsFirst(valueComparator);
        Comparator<TreeNodeImpl<T>> byValue = (left, right) -> nullsFirst.compare(
            TreeTableColumns.<V>cast(left.getColumnValue(columnIndex)),
            TreeTableColumns.<V>cast(right.getColumnValue(columnIndex))
        );
        return ascending ? byValue : byValue.reversed();
    }

    public int getSortColumn() {
        return mySortColumn;
    }

    public boolean isSortAscending() {
        return mySortAscending;
    }

    public @Nullable Comparator<TreeNodeImpl<E>> sort(int columnIndex, boolean ascending) {
        Comparator<TreeNodeImpl<E>> comparator = comparator(columnIndex, ascending);
        if (comparator == null) {
            mySortColumn = -1;
            mySortAscending = true;
            return null;
        }

        mySortColumn = columnIndex;
        mySortAscending = ascending;
        return comparator;
    }

    public @Nullable Comparator<TreeNodeImpl<E>> cycleSort(int columnIndex) {
        if (comparator(columnIndex, true) == null) {
            return getSortComparator();
        }

        if (mySortColumn != columnIndex) {
            return sort(columnIndex, true);
        }

        if (mySortAscending) {
            return sort(columnIndex, false);
        }

        return sort(-1, true);
    }

    public @Nullable Comparator<TreeNodeImpl<E>> getSortComparator() {
        int column = mySortColumn;
        return column < 0 ? null : sort(column, mySortAscending);
    }

    @RequiredUIAccess
    public void render(int columnIndex, TextItemPresentation presentation, TreeNodeImpl<E> node, boolean selected) {
        E value = node.getValue();
        if (value == null || columnIndex < 0 || columnIndex >= myColumns.size()) {
            return;
        }

        render(myColumns.get(columnIndex), presentation, node.getColumnValue(columnIndex), value, selected);
    }

    @RequiredUIAccess
    private static <T, V> void render(TableColumnImpl<T, V> column,
                                      TextItemPresentation presentation,
                                      @Nullable Object cached,
                                      T item,
                                      boolean selected) {
        RenderItem<V> renderItem = RenderItem.of(TreeTableColumns.<V>cast(cached), selected);
        column.getTextRender().render(presentation, renderItem, item);
    }

    @SuppressWarnings("unchecked")
    private static <V> @Nullable V cast(@Nullable Object value) {
        return (V) value;
    }
}
