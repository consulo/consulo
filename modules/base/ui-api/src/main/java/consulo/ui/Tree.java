/*
 * Copyright 2013-2017 consulo.io
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

import consulo.disposer.Disposable;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.TreeDoubleClickEvent;
import consulo.ui.event.TreeSelectEvent;
import consulo.ui.internal.UIInternal;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2017-09-12
 */
public interface Tree<E> extends Component {
    static <E> Tree<E> create(TreeModel<E> model, Disposable disposable) {
        return create(null, model, disposable);
    }

    static <E> Tree<E> create(@Nullable E rootValue, TreeModel<E> model, Disposable disposable) {
        return UIInternal.get()._Components_tree(rootValue, model, disposable);
    }

    @Nullable
    TreeNode<E> getSelectedNode();

    void expand(TreeNode<E> node);

    /**
     * Counterpart of the expand all / collapse all actions of the platform, which need a
     * {@code TreeExpander} to bind to. A tree which cannot walk its own nodes leaves them disabled.
     */
    default boolean isExpandCollapseAllSupported() {
        return false;
    }

    default void expandAll() {
    }

    default void collapseAll() {
    }

    /**
     * The node the tree was built on. It is not shown - the tree starts at its children - and it is where a
     * walk down a stored path begins.
     */
    default @Nullable TreeNode<E> getRootNode() {
        return null;
    }

    /**
     * Paths of the expanded nodes, each starting at the root node. A tree which cannot walk its own nodes
     * answers empty and is simply left unrestored, the way {@link #isExpandCollapseAllSupported()} is handled.
     */
    default List<List<TreeNode<E>>> getExpandedPaths() {
        return List.of();
    }

    /**
     * Path to the selected node, starting at the root node.
     */
    default List<TreeNode<E>> getSelectedPath() {
        return List.of();
    }

    /**
     * Opens the node, fetching its children first when they were not loaded yet.
     */
    default CompletableFuture<Void> expandAsync(TreeNode<E> node) {
        return CompletableFuture.completedFuture(null);
    }

    default void select(TreeNode<E> node) {
    }

    /**
     * Re-reads the node from the model - its presentation, and with {@code refreshChildren} its subtree.
     */
    void refreshItem(TreeNode<E> node, boolean refreshChildren);

    default void refreshItem(TreeNode<E> node) {
        refreshItem(node, false);
    }

    /**
     * Rebuilds the whole tree from the model, for a change no single node can be pointed at.
     */
    void refreshAll();

    @SuppressWarnings("unchecked")
    default Disposable addSelectListener(ComponentEventListener<Tree<E>, TreeSelectEvent<E>> listener) {
        return addListener((Class) TreeSelectEvent.class, listener);
    }

    @SuppressWarnings("unchecked")
    default Disposable addDoubleClickListener(ComponentEventListener<Tree<E>, TreeDoubleClickEvent<E>> listener) {
        return addListener((Class) TreeDoubleClickEvent.class, listener);
    }
}
