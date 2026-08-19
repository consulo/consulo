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
import consulo.ui.event.TreeCollapseEvent;
import consulo.ui.event.TreeDoubleClickEvent;
import consulo.ui.event.TreeExpandEvent;
import consulo.ui.event.TreeSelectEvent;
import consulo.ui.internal.UIInternal;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2017-09-12
 */
public interface Tree<E> extends Component, HasTransferHandler<TreeNode<E>>, HasComponentStyle<TreeStyle>, HasItemSize<TreeNode<E>>,
    HasSpeedSearch<TreeNode<E>> {
    static <E> Tree<E> create(TreeModel<E> model, Disposable disposable) {
        return create(null, model, disposable);
    }

    static <E> Tree<E> create(@Nullable E rootValue, TreeModel<E> model, Disposable disposable) {
        return UIInternal.get()._Components_tree(rootValue, model, disposable);
    }

    @Nullable
    TreeNode<E> getSelectedNode();

    /**
     * Opens the node, building its children first when they were not built yet.
     *
     * @return a future which is done once the node is open
     */
    default CompletableFuture<?> expand(TreeNode<E> node) {
        return expand(node, 1);
    }

    /**
     * Opens the node and the levels below it, where a depth of 1 opens the node alone. The children of a node
     * are built when it is first opened, so the depth is what keeps this from walking a whole project view.
     *
     * @return a future which is done once every level asked for is open
     */
    default CompletableFuture<?> expand(TreeNode<E> node, int depth) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Counterpart of the expand all / collapse all actions of the platform, which need a
     * {@code TreeExpander} to bind to. A tree which cannot walk its own nodes leaves them disabled.
     */
    default boolean isExpandCollapseAllSupported() {
        return false;
    }

    /**
     * Opens the tree down to the given depth, where a depth of 1 opens the top level rows. The root is not
     * shown, so it is not counted.
     *
     * @return a future which is done once every level asked for is open
     */
    default CompletableFuture<?> expandAll(int depth) {
        return CompletableFuture.completedFuture(null);
    }

    default CompletableFuture<?> expandAll() {
        return expandAll(Integer.MAX_VALUE);
    }

    default CompletableFuture<?> collapseAll() {
        return CompletableFuture.completedFuture(null);
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
    CompletableFuture<?> refreshAll();

    @SuppressWarnings("unchecked")
    default Disposable addSelectListener(ComponentEventListener<Tree<E>, TreeSelectEvent<E>> listener) {
        return addListener((Class) TreeSelectEvent.class, listener);
    }

    @SuppressWarnings("unchecked")
    default Disposable addDoubleClickListener(ComponentEventListener<Tree<E>, TreeDoubleClickEvent<E>> listener) {
        return addListener((Class) TreeDoubleClickEvent.class, listener);
    }

    /**
     * Sent for every node that is opened, whether the user opened it or a call on the tree did.
     */
    @SuppressWarnings("unchecked")
    default Disposable addExpandListener(ComponentEventListener<Tree<E>, TreeExpandEvent<E>> listener) {
        return addListener((Class) TreeExpandEvent.class, listener);
    }

    /**
     * Sent for every node that is closed, whether the user closed it or a call on the tree did.
     */
    @SuppressWarnings("unchecked")
    default Disposable addCollapseListener(ComponentEventListener<Tree<E>, TreeCollapseEvent<E>> listener) {
        return addListener((Class) TreeCollapseEvent.class, listener);
    }
}
