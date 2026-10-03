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
package consulo.desktop.awt.ui.impl;

import consulo.desktop.awt.ui.impl.tree.DesktopAsyncTreeModel;
import consulo.desktop.awt.ui.impl.tree.DesktopStructureTreeModel;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.ui.TreeExecutor;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.LeafState;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.impl.TreeNodeSupport;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
class DesktopTreeStructure<K> extends AbstractTreeStructure {
    private static final Logger LOG = Logger.getInstance(DesktopTreeStructure.class);

    private final TreeModel<K> myModel;
    private final TreeExecutor myExecutor;
    private final DesktopTreeBase<K, ?> myTree;

    private final DesktopTreeNodeImpl<K> myRootNode;

    private volatile @Nullable Function<@Nullable K, @Nullable Object[]> myColumnValueFactory;

    DesktopTreeStructure(@Nullable K rootValue, TreeModel<K> model, TreeExecutor executor, DesktopTreeBase<K, ?> tree) {
        myModel = model;
        myExecutor = executor;
        myTree = tree;
        myRootNode = new DesktopTreeNodeImpl<>(rootValue, null, this);
    }

    DesktopTreeNodeImpl<K> getRootNode() {
        return myRootNode;
    }

    void setColumnValueFactory(@Nullable Function<@Nullable K, @Nullable Object[]> factory) {
        myColumnValueFactory = factory;
    }

    @Nullable Object @Nullable [] computeColumnValues(@Nullable K value) {
        Function<@Nullable K, @Nullable Object[]> factory = myColumnValueFactory;
        return factory == null ? null : factory.apply(value);
    }

    @Override
    public Object getRootElement() {
        return myRootNode;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object[] getChildElements(Object element) {
        if (!(element instanceof DesktopTreeNodeImpl node)) {
            return new Object[0];
        }
        return ((DesktopTreeNodeImpl<K>) node).getChildren().toArray();
    }

    /**
     * {@link DesktopStructureTreeModel} takes {@link LeafState#DEFAULT} for "build the level and see whether
     * it came out empty", and {@link DesktopAsyncTreeModel} asks this of every sibling of a node it opens - so a
     * structure which cannot answer without building lists the whole tree to walk one path down it. The
     * node carries what the model said of it when it was built, and only a model which asks for the level
     * to be built first is given the answer that costs one.
     */
    @Override
    @SuppressWarnings("unchecked")
    public LeafState getLeafState(Object element) {
        if (!(element instanceof DesktopTreeNodeImpl)) {
            return super.getLeafState(element);
        }

        DesktopTreeNodeImpl<K> node = (DesktopTreeNodeImpl<K>) element;
        if (myModel.isNeedBuildChildrenBeforeOpen(node)) {
            return LeafState.DEFAULT;
        }
        return node.isLeaf() ? LeafState.ALWAYS : LeafState.NEVER;
    }

    CompletableFuture<List<DesktopTreeNodeImpl<K>>> loadChildren(DesktopTreeNodeImpl<K> node) {
        if (Disposer.isDisposed(myTree.destroyHook())) {
            return CompletableFuture.completedFuture(List.of());
        }

        List<DesktopTreeNodeImpl<K>> known = node.getKnownChildren();
        if (known != null) {
            return CompletableFuture.completedFuture(known);
        }

        CompletableFuture<List<DesktopTreeNodeImpl<K>>> result = new CompletableFuture<>();
        myExecutor.execute(myTree, node::getChildren).whenComplete((children, error) -> myTree.getUIAccess().giveIfNeed(() -> {
            if (children != null) {
                result.complete(children);
                return;
            }

            if (!Disposer.isDisposed(myTree.destroyHook())) {
                TreeNodeSupport.logBuildError(LOG, error);
            }
            result.complete(node.getBuiltChildren());
        }));
        return result;
    }

    List<DesktopTreeNodeImpl<K>> buildChildren(DesktopTreeNodeImpl<K> parent) {
        List<DesktopTreeNodeImpl<K>> nodes = new ArrayList<>();
        myModel.buildChildren(
            k -> {
                DesktopTreeNodeImpl<K> node = new DesktopTreeNodeImpl<>(k, parent, this);
                nodes.add(node);
                return node;
            },
            parent.getValue()
        );

        Comparator<TreeNode<K>> comparator = myModel.getNodeComparator();
        if (comparator != null) {
            nodes.sort(comparator);
        }
        return nodes;
    }

    /**
     * {@link DesktopStructureTreeModel#invalidate(Object, boolean)} walks up from the element to the root to find
     * the node it stands for, so a tree that cannot answer this can only ever be invalidated whole.
     */
    @Override
    public @Nullable Object getParentElement(Object element) {
        return element instanceof DesktopTreeNodeImpl<?> node ? node.getParent() : null;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public NodeDescriptor createDescriptor(Object element, @Nullable NodeDescriptor parentDescriptor) {
        return new DesktopTreeNodeDescriptor<>(this, element, parentDescriptor);
    }

    @Override
    public void commit() {
    }

    @Override
    public boolean hasSomethingToCommit() {
        return false;
    }
}
