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
package consulo.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.internal.UIInternal;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * A {@link Tree} whose rows carry columns next to the tree column. The tree column comes first and shows the
 * node presentation, as a plain tree does; the columns added by {@link #addColumn} follow it in the order they
 * were added, under a header row which is always shown.
 * <p/>
 * The value of a column is computed by its value provider through the {@link TreeExecutor} of the tree, together
 * with the node presentation - when a level is built and when a node is refreshed - and is kept with the node.
 * A value provider is never called for a {@code null} node value, nor on the UI thread unless the executor is
 * {@link TreeExecutor#uiThread()}. The renders of a column only paint that kept value, on the UI thread. A
 * component render or an editor which a cell of the tree table cannot host falls back to the default text of the
 * value.
 * <p/>
 * A column with a comparator (see {@link TableColumn#setSortable}) is sorted from its header, which cycles
 * through ascending, descending and back to the order of the model. Sorting reorders siblings among themselves
 * at every level, levels built later included; values which compare equal keep the order of the model, and
 * {@code null} values come first in ascending order. The tree column is not sortable. Refreshing keeps the
 * expanded nodes, the selection and the active sort.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public interface TreeTable<E> extends Tree<E> {
    static <E> TreeTable<E> create(TreeModel<E> model) {
        return create(null, model);
    }

    static <E> TreeTable<E> create(@Nullable E rootValue, TreeModel<E> model) {
        return create(rootValue, model, TreeExecutor.uiThread());
    }

    static <E> TreeTable<E> create(TreeModel<E> model, TreeExecutor executor) {
        return create(null, model, executor);
    }

    /**
     * @param executor where the model and the column value providers run - {@link TreeExecutor#uiThread()} is
     *                 only right for a model that computes nothing, see {@link TreeExecutor}
     */
    static <E> TreeTable<E> create(@Nullable E rootValue, TreeModel<E> model, TreeExecutor executor) {
        return UIInternal.get()._Components_treeTable(rootValue, model, executor);
    }

    /**
     * Header of the tree column, empty by default.
     */
    void setTreeColumnHeader(LocalizeValue header);

    /**
     * Adds a column after the existing ones. A column may be added once the tree table is shown: its header
     * appears and its values are computed for the nodes already built.
     */
    <V> TableColumn<E, V> addColumn(LocalizeValue header, Function<E, V> valueProvider);

    /**
     * The added columns in their order, without the tree column.
     */
    List<TableColumn<E, ?>> getColumns();
}
