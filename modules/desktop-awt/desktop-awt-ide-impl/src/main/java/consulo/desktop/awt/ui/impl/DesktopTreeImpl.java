/*
 * Copyright 2013-2021 consulo.io
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

import consulo.desktop.awt.facade.DesktopAWTTargetAWTImpl;
import consulo.desktop.awt.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.*;
import consulo.ui.event.TreeCollapseEvent;
import consulo.ui.event.TreeDoubleClickEvent;
import consulo.ui.event.TreeExpandEvent;
import consulo.ui.event.TreeSelectEvent;
import consulo.ui.ex.awt.dnd.DnDAwareTree;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.tree.AsyncTreeModel;
import consulo.ui.ex.awt.tree.NodeRenderer;
import consulo.ui.ex.awt.tree.StructureTreeModel;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awt.tree.TreeVisitor;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.ex.tree.PresentableNodeDescriptor;
import consulo.ui.ex.tree.PresentationData;
import consulo.ui.image.Image;
import consulo.util.concurrent.Promise;
import consulo.util.concurrent.Promises;
import consulo.util.lang.ObjectUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreePath;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2021-07-14
 */
public class DesktopTreeImpl<E> extends SwingComponentDelegate<DesktopTreeImpl.MyTree> implements Tree<E> {
    private static class MyTreeNodeImpl<K> implements TreeNode<K> {
        private boolean myLeaf;

        private final K myValue;
        private final Object myParentElement;
        private final MyStructureWrapper<K> myStructure;

        private BiConsumer<K, TextItemPresentation> myRenderer = (e, t) -> t.append(e == null ? "null" : e.toString());

        private MyTreeNodeImpl(K value, Object parentElement, MyStructureWrapper<K> structure) {
            myValue = value;
            myParentElement = parentElement;
            myStructure = structure;
        }

        /**
         * The structure builds the level below on demand, so the children exist by the time they are walked.
         */
        @Override
        @SuppressWarnings("unchecked")
        public CompletableFuture<TreeNode<K>> findChild(Predicate<K> predicate) {
            for (Object element : myStructure.getChildElements(this)) {
                MyTreeNodeImpl<K> child = (MyTreeNodeImpl<K>) element;
                if (predicate.test(child.getValue())) {
                    return CompletableFuture.completedFuture(child);
                }
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        @SuppressWarnings("unchecked")
        public CompletableFuture<TreeNode<K>> findChildDeep(Predicate<K> predicate) {
            for (Object element : myStructure.getChildElements(this)) {
                MyTreeNodeImpl<K> child = (MyTreeNodeImpl<K>) element;
                if (predicate.test(child.getValue())) {
                    return CompletableFuture.completedFuture(child);
                }

                TreeNode<K> found = child.findChildDeep(predicate).join();
                if (found != null) {
                    return CompletableFuture.completedFuture(found);
                }
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void setRenderer(BiConsumer<K, TextItemPresentation> renderer) {
            myRenderer = renderer;
        }

        @Override
        public void setLeaf(boolean leaf) {
            myLeaf = leaf;
        }

        @Override
        public boolean isLeaf() {
            return myLeaf;
        }

        @Override
        public @Nullable K getValue() {
            return myValue;
        }
    }

    private static class MyNodeDescriptor<K> extends PresentableNodeDescriptor {
        private final Object myRootElement;
        private final Object myElement;

        protected MyNodeDescriptor(Object rootElement, Object element, @Nullable NodeDescriptor parentDescriptor) {
            super(parentDescriptor);
            myRootElement = rootElement;
            myElement = element;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void update(PresentationData presentation) {
            if (myElement == myRootElement) {
                return;
            }

            MyTreeNodeImpl<K> node = (MyTreeNodeImpl) myElement;

            BiConsumer<K, TextItemPresentation> render = node.myRenderer;

            render.accept(node.myValue, new TextItemPresentation() {
                @Override
                public void clearText() {
                    presentation.clearText();
                }

                
                @Override
                public TextItemPresentation withIcon(@Nullable Image image) {
                    presentation.setIcon(image);
                    return this;
                }

                @Override
                public void append(LocalizeValue text, TextAttribute textAttribute) {
                    presentation.addText(text.getValue(), DesktopAWTTargetAWTImpl.from(textAttribute));
                }
            });
        }

        @Override
        public boolean isWasDeclaredAlwaysLeaf() {
            if (myElement instanceof MyTreeNodeImpl k) {
                return k.isLeaf();
            }
            return false;
        }

        @Override
        public Object getElement() {
            return myElement;
        }
    }

    public static class MyStructureWrapper<K> extends AbstractTreeStructure {
        private final Object myRootValue;
        private final TreeModel<K> myModel;

        public MyStructureWrapper(K rootValue, TreeModel<K> model) {
            myModel = model;
            myRootValue = rootValue == null ? ObjectUtil.NULL : rootValue;
        }

        
        @Override
        public Object getRootElement() {
            return myRootValue;
        }

        
        @Override
        @SuppressWarnings("unchecked")
        public Object[] getChildElements(Object element) {
            K targetParent = null;
            if (element == myRootValue) {
                // the root stands for a value of the model, and a model built over a tree structure asks that
                // structure for the children of it - only a tree created without a root value has none to give
                targetParent = myRootValue == ObjectUtil.NULL ? null : (K) myRootValue;
            }
            else if (element instanceof MyTreeNodeImpl node) {
                targetParent = (K) node.getValue();
            }

            List<MyTreeNodeImpl<K>> nodes = new ArrayList<>();
            myModel.buildChildren(k -> {
                MyTreeNodeImpl<K> node = new MyTreeNodeImpl<>(k, element, this);
                nodes.add(node);
                return node;
            }, targetParent);

            Comparator<TreeNode<K>> comparator = myModel.getNodeComparator();
            if (comparator != null) {
                nodes.sort(comparator);
            }
            return nodes.toArray(MyTreeNodeImpl[]::new);
        }

        /**
         * {@link StructureTreeModel#invalidate(Object, boolean)} walks up from the element to the root to find
         * the node it stands for, so a tree that cannot answer this can only ever be invalidated whole.
         */
        @Override
        public @Nullable Object getParentElement(Object element) {
            return element instanceof MyTreeNodeImpl node ? node.myParentElement : null;
        }

        
        @Override
        @SuppressWarnings("unchecked")
        public NodeDescriptor createDescriptor(Object element, @Nullable NodeDescriptor parentDescriptor) {
            return new MyNodeDescriptor(myRootValue, element, parentDescriptor);
        }

        @Override
        public void commit() {

        }

        @Override
        public boolean hasSomethingToCommit() {
            return false;
        }
    }

    public class MyTree extends DnDAwareTree implements FromSwingComponentWrapper {
        public MyTree(StructureTreeModel<MyStructureWrapper<E>> structureTreeModel, Disposable disposable) {
            super(new AsyncTreeModel(structureTreeModel, disposable));
        }

        
        @Override
        public Component toUIComponent() {
            return DesktopTreeImpl.this;
        }
    }

    private final TreeModel<E> myModel;
    private final Disposable myDisposable;
    private final StructureTreeModel<MyStructureWrapper<E>> myStructureTreeModel;

    public DesktopTreeImpl(E rootValue, TreeModel<E> model, Disposable disposable) {
        myModel = model;
        myDisposable = disposable;
        myStructureTreeModel = new StructureTreeModel<>(new MyStructureWrapper<>(rootValue, model), disposable);
    }

    @Override
    protected MyTree createComponent() {
        MyTree tree = new MyTree(myStructureTreeModel, myDisposable);
        tree.setRootVisible(false);
        tree.setCellRenderer(new NodeRenderer());

        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(MouseEvent event) {
                TreeNode<E> node = getSelectedNode();
                if (node == null) {
                    return false;
                }

                getListenerDispatcher(TreeDoubleClickEvent.class).onEvent(new TreeDoubleClickEvent<>(DesktopTreeImpl.this, node));

                // the model answers whether the node should open, which is what the tree does on its own -
                // the event is consumed only when the model took the click for an action of its own
                return !myModel.onDoubleClick(DesktopTreeImpl.this, node);
            }
        }.installOn(tree);

        tree.addTreeSelectionListener(e -> {
            TreePath path = TreeUtil.getSelectedPathIfOne(tree);
            if (path == null) {
                return;
            }

            TreeNode<E> node = nodeOf(path);
            if (node != null) {
                getListenerDispatcher(TreeSelectEvent.class).onEvent(new TreeSelectEvent<>(this, node));
            }
        });

        // fires for the nodes the user toggles and for those a call on the tree opens alike
        tree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                TreeNode<E> node = nodeOf(event.getPath());
                if (node != null) {
                    getListenerDispatcher(TreeExpandEvent.class).onEvent(new TreeExpandEvent<>(DesktopTreeImpl.this, node));
                }
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                TreeNode<E> node = nodeOf(event.getPath());
                if (node != null) {
                    getListenerDispatcher(TreeCollapseEvent.class).onEvent(new TreeCollapseEvent<>(DesktopTreeImpl.this, node));
                }
            }
        });
        return tree;
    }

    /**
     * The root is not a node of the model - it stands for the value the tree was built on - so a path pointing
     * at it answers null and is left out of the events.
     */
    @SuppressWarnings("unchecked")
    private @Nullable TreeNode<E> nodeOf(TreePath path) {
        Object object = TreeUtil.getLastUserObject(path);

        if (object instanceof MyNodeDescriptor node && node.getElement() instanceof MyTreeNodeImpl element) {
            return element;
        }

        return null;
    }

    @Override
    public @Nullable TreeNode<E> getSelectedNode() {
        TreePath path = TreeUtil.getSelectedPathIfOne(toAWTComponent());
        return path == null ? null : nodeOf(path);
    }

    @Override
    public CompletableFuture<?> expand(TreeNode<E> node, int depth) {
        MyTree tree = toAWTComponent();

        Promise<?> expanded = myStructureTreeModel.promiseVisitor(node)
            .thenAsync(visitor -> TreeUtil.promiseExpand(tree, visitor))
            .thenAsync(basePath -> depth <= 1 ? Promises.resolvedPromise(basePath) : expandBelow(tree, basePath, depth));

        return toFuture(expanded);
    }

    /**
     * A promise which found nothing to open is cancelled rather than rejected, and {@link Promise#onProcessed}
     * is the one handler that runs whichever way it ends - a future left hanging would take its caller with it.
     */
    private static CompletableFuture<Void> toFuture(Promise<?> promise) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        promise.onProcessed(ignored -> future.complete(null));
        return future;
    }

    /**
     * {@link TreeUtil#expand(JTree, int)} counts from the root and opens every branch down to that depth, so a
     * walk which stays inside one subtree is done here. Opening the visited path is what the walk of the
     * platform does - see {@code TreeUtil#promiseMakeVisible} - and the visitor runs on EDT.
     */
    private Promise<TreePath> expandBelow(MyTree tree, TreePath basePath, int depth) {
        int base = basePath.getPathCount();

        return TreeUtil.promiseVisit(tree, path -> {
            // the walk starts at the root, and the nodes above the base one are already open - only the way
            // down to it is followed, the rest of the tree is left alone
            if (path.getPathCount() <= base) {
                return path.isDescendant(basePath) ? TreeVisitor.Action.CONTINUE : TreeVisitor.Action.SKIP_CHILDREN;
            }

            if (!basePath.isDescendant(path) || path.getPathCount() >= base + depth) {
                return TreeVisitor.Action.SKIP_CHILDREN;
            }

            tree.expandPath(path);
            return TreeVisitor.Action.CONTINUE;
        });
    }

    @Override
    public boolean isExpandCollapseAllSupported() {
        return true;
    }

    @Override
    public CompletableFuture<?> expandAll(int depth) {
        // the root is hidden, so a top level row is at a path count of two
        return toFuture(TreeUtil.promiseExpand(toAWTComponent(), depth == Integer.MAX_VALUE ? depth : depth + 1));
    }

    @Override
    public CompletableFuture<?> collapseAll() {
        // what DefaultTreeExpander - the collapse all of the platform trees - does
        TreeUtil.collapseAll(toAWTComponent(), true, 1);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void refreshItem(TreeNode<E> node, boolean refreshChildren) {
        myStructureTreeModel.invalidate(node, refreshChildren);
    }

    @Override
    public CompletableFuture<?> refreshAll() {
        myStructureTreeModel.invalidate();
        return CompletableFuture.completedFuture(null);
    }
}
