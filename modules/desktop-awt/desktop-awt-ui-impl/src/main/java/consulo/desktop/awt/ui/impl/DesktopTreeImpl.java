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

import consulo.ui.Length;
import consulo.ui.TransferHandler;
import consulo.ui.DragAndDropTransferHandler;
import consulo.desktop.awt.ui.impl.clipboard.DesktopAWTTransferHandlerAdapter;
import consulo.desktop.awt.ui.impl.clipboard.DesktopAWTTransferTarget;
import consulo.desktop.awt.ui.impl.facade.DesktopAWTTargetAWTImpl;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.ui.*;
import consulo.ui.event.TreeCollapseEvent;
import consulo.ui.event.TreeDoubleClickEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.event.TreeExpandEvent;
import consulo.ui.event.TreeSelectEvent;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.MorphColor;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.dnd.DnDAwareTree;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.speedSearch.SpeedSearchSupply;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.desktop.awt.ui.impl.tree.DesktopAsyncTreeModel;
import consulo.desktop.awt.ui.impl.tree.DesktopStructureTreeModel;
import consulo.ui.ex.awt.tree.NodeRenderer;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awt.tree.TreeVisitor;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.LeafState;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.ex.tree.PresentableNodeDescriptor;
import consulo.ui.ex.tree.PresentationData;
import consulo.ui.image.Image;
import consulo.util.concurrent.Promise;
import consulo.util.concurrent.Promises;
import org.jspecify.annotations.Nullable;

import javax.swing.DropMode;
import consulo.ui.Point2D;
import consulo.ui.PopupOwner;

import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.util.Arrays;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2021-07-14
 */
public class DesktopTreeImpl<E> extends SwingComponentDelegate<DesktopTreeImpl.MyTree>
    implements Tree<E>, PopupOwner, DesktopAWTTransferTarget<TreeNode<E>> {

    @Override
    public @Nullable Point2D getBestPopupPosition() {
        MyTree tree = toAWTComponent();

        int[] selectionRows = tree.getSelectionRows();
        if (selectionRows == null || selectionRows.length == 0) {
            return null;
        }

        Rectangle visibleRect = tree.getVisibleRect();

        int[] sorted = selectionRows.clone();
        Arrays.sort(sorted);

        for (int row : sorted) {
            Rectangle rowBounds = tree.getRowBounds(row);
            if (visibleRect.contains(rowBounds)) {
                // the same point the swing popup factory anchors at - the bottom left of the selected row
                return new Point2D(rowBounds.x + 2, rowBounds.y + rowBounds.height - 1);
            }
        }
        return null;
    }

    private @Nullable TransferHandler<TreeNode<E>> myTransferHandler;
    private @Nullable Function<TreeNode<E>, Length> myItemHeightGetter;
    private @Nullable Function<TreeNode<E>, String> mySpeedSearchConverter;

    private static class MyTreeNodeImpl<K> implements TreeNode<K> {
        private boolean myLeaf;

        private K myValue;
        private final MyTreeNodeImpl<K> myParent;
        private final MyStructureWrapper<K> myStructure;

        private @Nullable List<MyTreeNodeImpl<K>> myChildren;
        private boolean myChildrenOutdated;

        private BiConsumer<K, TextItemPresentation> myRenderer = (e, t) -> t.append(e == null ? "null" : e.toString());

        private MyTreeNodeImpl(K value, MyTreeNodeImpl<K> parent, MyStructureWrapper<K> structure) {
            myValue = value;
            myParent = parent;
            myStructure = structure;
        }

        /**
         * A node stands for a place in the tree, so the same one is handed out for the same place every time -
         * a level built anew on each call would give a caller nodes the tree does not hold, and selecting or
         * opening one of those finds nothing.
         */
        private synchronized List<MyTreeNodeImpl<K>> getChildren() {
            List<MyTreeNodeImpl<K>> children = myChildren;
            if (children == null) {
                children = myStructure.buildChildren(this);
            }
            else if (myChildrenOutdated) {
                children = reuse(children, myStructure.buildChildren(this));
            }
            else {
                return children;
            }

            myChildren = children;
            myChildrenOutdated = false;
            return children;
        }

        /**
         * A node the model built again for the same value keeps the place the old one had - what
         * {@link DesktopStructureTreeModel} does with its own nodes - so what was open below it stays open. The value
         * itself is the one just built, since a refresh is there to show what changed about it.
         */
        private List<MyTreeNodeImpl<K>> reuse(List<MyTreeNodeImpl<K>> oldChildren, List<MyTreeNodeImpl<K>> newChildren) {
            Map<K, MyTreeNodeImpl<K>> byValue = new HashMap<>();
            for (MyTreeNodeImpl<K> child : oldChildren) {
                if (child.myValue != null) {
                    byValue.put(child.myValue, child);
                }
            }

            List<MyTreeNodeImpl<K>> children = new ArrayList<>(newChildren.size());
            for (MyTreeNodeImpl<K> child : newChildren) {
                MyTreeNodeImpl<K> old = child.myValue == null ? null : byValue.get(child.myValue);
                if (old != null) {
                    old.myValue = child.myValue;
                    old.myLeaf = child.myLeaf;
                    old.myRenderer = child.myRenderer;
                    children.add(old);
                }
                else {
                    children.add(child);
                }
            }
            return children;
        }

        /**
         * The level is marked rather than dropped, so a refresh of a branch nobody opened costs nothing and the
         * nodes which are on screen live until the model asks for them again.
         */
        private synchronized void outdateChildren() {
            List<MyTreeNodeImpl<K>> children = myChildren;
            if (children == null) {
                return;
            }

            myChildrenOutdated = true;
            for (MyTreeNodeImpl<K> child : children) {
                child.outdateChildren();
            }
        }

        /**
         * The structure builds the level below on demand, so the children exist by the time they are walked.
         */
        @Override
        public CompletableFuture<TreeNode<K>> findChild(Predicate<K> predicate) {
            for (MyTreeNodeImpl<K> child : getChildren()) {
                if (predicate.test(child.getValue())) {
                    return CompletableFuture.completedFuture(child);
                }
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<TreeNode<K>> findChildDeep(Predicate<K> predicate) {
            for (MyTreeNodeImpl<K> child : getChildren()) {
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
        @SuppressWarnings({"unchecked", "rawtypes"})
        public synchronized List<TreeNode<K>> getLoadedChildren() {
            List<MyTreeNodeImpl<K>> children = myChildren;
            return children == null ? List.of() : List.copyOf((List) children);
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

        @Override
        public boolean equals(Object obj) {
            return obj instanceof MyTreeNodeImpl<?> node && Objects.equals(myValue, node.myValue);
        }

        @Override
        public int hashCode() {
            return myValue == null ? 0 : myValue.hashCode();
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
        public Object getElement() {
            return myElement;
        }
    }

    public static class MyStructureWrapper<K> extends AbstractTreeStructure {
        private final TreeModel<K> myModel;

        private final MyTreeNodeImpl<K> myRootNode;

        public MyStructureWrapper(K rootValue, TreeModel<K> model) {
            myModel = model;
            myRootNode = new MyTreeNodeImpl<>(rootValue, null, this);
        }

        public MyTreeNodeImpl<K> getRootNode() {
            return myRootNode;
        }

        @Override
        public Object getRootElement() {
            return myRootNode;
        }


        @Override
        @SuppressWarnings("unchecked")
        public Object[] getChildElements(Object element) {
            if (!(element instanceof MyTreeNodeImpl node)) {
                return new Object[0];
            }
            return ((MyTreeNodeImpl<K>) node).getChildren().toArray();
        }

        /**
         * {@link DesktopStructureTreeModel.Node} takes {@link LeafState#DEFAULT} for "build the level and see whether
         * it came out empty", and {@link DesktopAsyncTreeModel} asks this of every sibling of a node it opens - so a
         * structure which cannot answer without building lists the whole tree to walk one path down it. The
         * node carries what the model said of it when it was built, and only a model which asks for the level
         * to be built first is given the answer that costs one.
         */
        @Override
        @SuppressWarnings("unchecked")
        public LeafState getLeafState(Object element) {
            if (!(element instanceof MyTreeNodeImpl)) {
                return super.getLeafState(element);
            }

            MyTreeNodeImpl<K> node = (MyTreeNodeImpl<K>) element;
            if (myModel.isNeedBuildChildrenBeforeOpen(node)) {
                return LeafState.DEFAULT;
            }
            return node.isLeaf() ? LeafState.ALWAYS : LeafState.NEVER;
        }

        private List<MyTreeNodeImpl<K>> buildChildren(MyTreeNodeImpl<K> parent) {
            List<MyTreeNodeImpl<K>> nodes = new ArrayList<>();
            myModel.buildChildren(k -> {
                MyTreeNodeImpl<K> node = new MyTreeNodeImpl<>(k, parent, this);
                nodes.add(node);
                return node;
            }, parent.getValue());

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
            return element instanceof MyTreeNodeImpl node ? node.myParent : null;
        }

        
        @Override
        @SuppressWarnings("unchecked")
        public NodeDescriptor createDescriptor(Object element, @Nullable NodeDescriptor parentDescriptor) {
            return new MyNodeDescriptor(myRootNode, element, parentDescriptor);
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
        public MyTree(DesktopStructureTreeModel<MyStructureWrapper<E>> structureTreeModel, TreeExecutor executor) {
            super(new DesktopAsyncTreeModel(structureTreeModel, DesktopTreeImpl.this, executor, myDestroyHook));
        }

        
        @Override
        public Component toUIComponent() {
            return DesktopTreeImpl.this;
        }
    }

    private final TreeModel<E> myModel;
    private final MyStructureWrapper<E> myStructure;
    private final DesktopStructureTreeModel<MyStructureWrapper<E>> myStructureTreeModel;
    private final TreeExecutor myExecutor;


    private final Disposable myDestroyHook = Disposable.newDisposable("Tree");

    @Override
    public Disposable destroyHook() {
        return myDestroyHook;
    }

    public DesktopTreeImpl(E rootValue, TreeModel<E> model, TreeExecutor executor) {
        myModel = model;
        myExecutor = executor;
        myStructure = new MyStructureWrapper<>(rootValue, model);
        myStructureTreeModel = new DesktopStructureTreeModel<>(myStructure, null, this, executor, myDestroyHook);
    }

    /**
     * The awt trees move the selection to the row under the pointer before a popup is shown - see
     * {@code PopupHandler#installFollowingSelectionTreePopup} - and a {@link consulo.ui.event.ContextMenuEvent}
     * listener reads the selection. The web tree already does this in its {@code installSelectOnRightClick}.
     */
    private static void selectRowUnderPopupTrigger(JTree tree, MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }

        TreePath path = tree.getPathForLocation(e.getX(), e.getY());
        if (path != null && !tree.isPathSelected(path)) {
            tree.setSelectionPath(path);
        }
    }

    @Override
    protected MyTree createComponent() {
        MyTree tree = new MyTree(myStructureTreeModel, myExecutor);
        tree.setRootVisible(false);
        // the root is hidden, so without this the rows which stand for its children - the top level of the
        // tree - are drawn with no expand control at all, see BasicTreeUI#shouldPaintExpandControl
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(wrapWithItemHeight(new NodeRenderer()));
        if (myItemHeightGetter != null) {
            tree.setRowHeight(0);
        }
        applySpeedSearch(tree);

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                selectRowUnderPopupTrigger(tree, e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                selectRowUnderPopupTrigger(tree, e);
            }
        });

        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(MouseEvent event) {
                TreeNode<E> node = getSelectedNode();
                if (node == null) {
                    return false;
                }

                InputDetails inputDetails = DesktopAWTInputDetails.convert(event.getComponent(), event);

                getListenerDispatcher(TreeDoubleClickEvent.class)
                    .onEvent(new TreeDoubleClickEvent<>(DesktopTreeImpl.this, node, inputDetails));

                // the model answers whether the node should open, which is what the tree does on its own -
                // the event is consumed only when the model took the click for an action of its own
                return !myModel.onDoubleClick(DesktopTreeImpl.this, node, inputDetails);
            }
        }.installOn(tree);

        tree.addTreeSelectionListener(e -> {
            TreePath path = TreeUtil.getSelectedPathIfOne(tree);
            if (path == null) {
                return;
            }

            TreeNode<E> node = nodeOf(path);
            if (node != null) {
                getListenerDispatcher(TreeSelectEvent.class).onEvent(new TreeSelectEvent<>(this, node, DesktopAWTInputDetails.currentEvent(tree)));
            }
        });

        // fires for the nodes the user toggles and for those a call on the tree opens alike
        tree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                TreeNode<E> node = nodeOf(event.getPath());
                if (node != null) {
                    getListenerDispatcher(TreeExpandEvent.class)
                        .onEvent(new TreeExpandEvent<>(DesktopTreeImpl.this, node, DesktopAWTInputDetails.currentEvent(tree)));
                }
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                TreeNode<E> node = nodeOf(event.getPath());
                if (node != null) {
                    getListenerDispatcher(TreeCollapseEvent.class)
                        .onEvent(new TreeCollapseEvent<>(DesktopTreeImpl.this, node, DesktopAWTInputDetails.currentEvent(tree)));
                }
            }
        });
        return tree;
    }

    private @Nullable TreeNode<E> nodeOf(TreePath path) {
        TreeNode<E> node = nodeOfComponent(TreeUtil.getLastUserObject(path));
        return node == myStructure.getRootNode() ? null : node;
    }

    @SuppressWarnings("unchecked")
    private @Nullable TreeNode<E> nodeOfComponent(@Nullable Object component) {
        Object userObject = component instanceof MyNodeDescriptor ? component : TreeUtil.getUserObject(component);

        if (userObject instanceof MyNodeDescriptor descriptor && descriptor.getElement() instanceof MyTreeNodeImpl element) {
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
    public TreeNode<E> getRootNode() {
        return myStructure.getRootNode();
    }

    @Override
    public void select(TreeNode<E> node) {
        MyTree tree = toAWTComponent();

        myStructureTreeModel.promiseVisitor(node).onSuccess(visitor -> TreeUtil.promiseSelect(tree, visitor));
    }

    @Override
    public List<TreeNode<E>> getSelectedPath() {
        TreePath path = TreeUtil.getSelectedPathIfOne(toAWTComponent());
        return path == null ? List.of() : nodesOf(path);
    }

    @Override
    public List<List<TreeNode<E>>> getExpandedPaths() {
        List<List<TreeNode<E>>> paths = new ArrayList<>();

        for (TreePath path : TreeUtil.collectExpandedPaths(toAWTComponent())) {
            List<TreeNode<E>> nodes = nodesOf(path);
            if (!nodes.isEmpty()) {
                paths.add(nodes);
            }
        }
        return paths;
    }

    private List<TreeNode<E>> nodesOf(TreePath path) {
        List<TreeNode<E>> nodes = new ArrayList<>();

        for (Object component : path.getPath()) {
            TreeNode<E> node = nodeOfComponent(component);
            if (node != null) {
                nodes.add(node);
            }
        }
        return nodes;
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

    /**
     * The nodes are held between calls, so the model is asked for the level again only once the cached one is
     * marked - otherwise a refresh would show what the tree already had.
     */
    @Override
    public void refreshItem(TreeNode<E> node, boolean refreshChildren) {
        if (refreshChildren && node instanceof MyTreeNodeImpl<E> impl) {
            impl.outdateChildren();
        }

        myStructureTreeModel.invalidate(node, refreshChildren);
    }

    @Override
    public CompletableFuture<?> refreshAll() {
        MyTree tree = toAWTComponent();

        List<TreeNode<E>> expanded = expandedNodes(tree);
        TreeNode<E> selected = getSelectedNode();

        myStructure.getRootNode().outdateChildren();

        return toFuture(myStructureTreeModel.invalidate())
            .thenCompose(ignored -> restoreExpanded(tree, expanded))
            .thenCompose(ignored -> restoreSelected(tree, selected));
    }

    private List<TreeNode<E>> expandedNodes(MyTree tree) {
        List<TreeNode<E>> nodes = new ArrayList<>();

        for (TreePath path : TreeUtil.collectExpandedPaths(tree)) {
            TreeNode<E> node = nodeOf(path);
            if (node != null) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    private CompletableFuture<?> restoreExpanded(MyTree tree, List<TreeNode<E>> nodes) {
        if (nodes.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        List<Promise<TreeVisitor>> visitors = new ArrayList<>();
        for (TreeNode<E> node : nodes) {
            visitors.add(myStructureTreeModel.promiseVisitor(node));
        }

        return toFuture(Promises.collectResults(visitors, true).thenAsync(list -> TreeUtil.promiseExpand(tree, list.stream())));
    }

    private CompletableFuture<?> restoreSelected(MyTree tree, @Nullable TreeNode<E> node) {
        if (node == null) {
            return CompletableFuture.completedFuture(null);
        }

        return toFuture(myStructureTreeModel.promiseVisitor(node).thenAsync(visitor -> TreeUtil.promiseSelect(tree, visitor)));
    }

    /**
     * JTree asks the renderer for the height of each row only while its own row height is zero, so a getter
     * turns the fixed height off and gives the rendered component the height it answered.
     */
    private TreeCellRenderer wrapWithItemHeight(TreeCellRenderer delegate) {
        return (tree, value, selected, expanded, leaf, row, hasFocus) -> {
            java.awt.Component component =
                delegate.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            Function<TreeNode<E>, Length> getter = myItemHeightGetter;
            TreeNode<E> node = nodeOfComponent(value);
            if (getter != null && component != null && node != null) {
                Dimension size = component.getPreferredSize();
                component.setPreferredSize(new Dimension(size.width, DesktopLength.toPixels(tree, getter.apply(node))));
            }

            return component;
        };
    }

    @Override
    public void setItemHeightGetter(@Nullable Function<TreeNode<E>, Length> getter) {
        myItemHeightGetter = getter;

        if (isInitialized()) {
            toAWTComponent().setRowHeight(getter == null ? UIManager.getInt("Tree.rowHeight") : 0);
        }
    }

    @Override
    public void setSpeedSearchConverter(@Nullable Function<TreeNode<E>, String> converter) {
        mySpeedSearchConverter = converter;

        if (isInitialized()) {
            applySpeedSearch(toAWTComponent());
        }
    }

    @Override
    public @Nullable String getSpeedSearchText() {
        if (!isInitialized()) {
            return null;
        }
        SpeedSearchSupply supply = SpeedSearchSupply.getSupply(toAWTComponent());
        return supply == null ? null : supply.getEnteredPrefix();
    }

    private void applySpeedSearch(MyTree tree) {
        Function<TreeNode<E>, String> converter = mySpeedSearchConverter;
        if (converter == null) {
            return;
        }

        TreeSpeedSearch.installOn(tree, false, path -> {
            TreeNode<E> node = nodeOf(path);
            return node == null ? "" : converter.apply(node);
        });
    }

    @Override
    public void addStyle(TreeStyle style) {
        MyTree tree = toAWTComponent();

        if (style == TreeStyle.TRANSPARENT_BACKGROUND) {
            tree.setOpaque(false);
            tree.setBackground(MorphColor.of(UIUtil::getPanelBackground));
            return;
        }

        Font font = tree.getFont();
        if (font == null) {
            font = UIUtil.getLabelFont();
        }

        tree.setFont(font.deriveFont(font.getSize2D() + JBUI.scale(fontDelta(style))));
    }

    /**
     * The ladder {@link consulo.ui.ex.awt.JBFont} gives a label - h4/h3/h2 are one, three and five points
     * above it, and medium and small one and two below.
     */
    private static float fontDelta(TreeStyle style) {
        return switch (style) {
            case FONT_XX_SMALL -> -3f;
            case FONT_X_SMALL -> -2f;
            case FONT_SMALL -> -1f;
            case FONT_LARGE -> 1f;
            case FONT_X_LARGE -> 3f;
            case FONT_XX_LARGE -> 5f;
            default -> 0f;
        };
    }

    @Override
    public void setTransferHandler(@Nullable TransferHandler<TreeNode<E>> handler) {
        myTransferHandler = handler;

        MyTree tree = toAWTComponent();
        if (handler == null) {
            tree.setTransferHandler(null);
            tree.setDragEnabled(false);
            return;
        }

        DesktopAWTTransferHandlerAdapter<TreeNode<E>> adapter = new DesktopAWTTransferHandlerAdapter<>(this, handler, this);
        tree.setTransferHandler(adapter);
        tree.setDragEnabled(adapter.isDragAndDropSupported());
        if (adapter.isDragAndDropSupported()) {
            tree.setDropMode(DropMode.ON_OR_INSERT);
        }
    }

    @Override
    public @Nullable TransferHandler<TreeNode<E>> getTransferHandler() {
        return myTransferHandler;
    }

    @Override
    public List<TreeNode<E>> getTransferItems() {
        TreePath[] paths = toAWTComponent().getSelectionPaths();
        if (paths == null) {
            return List.of();
        }

        List<TreeNode<E>> nodes = new ArrayList<>(paths.length);
        for (TreePath path : paths) {
            TreeNode<E> node = nodeOf(path);
            if (node != null) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    /**
     * The toolkit states a drop as a parent path plus the index it would be inserted at, so an
     * insertion is turned back into the sibling it lands next to. An insertion into a parent with no
     * children left to name lands on the parent itself.
     */
    @Override
    public @Nullable Drop<TreeNode<E>> resolveDrop(javax.swing.TransferHandler.TransferSupport support) {
        if (!(support.getDropLocation() instanceof JTree.DropLocation location)) {
            return null;
        }

        TreePath path = location.getPath();
        if (path == null) {
            return null;
        }

        int childIndex = location.getChildIndex();
        if (childIndex == -1) {
            TreeNode<E> node = nodeOf(path);
            return node == null ? null : new Drop<>(node, DragAndDropTransferHandler.DropPosition.INTO);
        }

        javax.swing.tree.TreeModel model = toAWTComponent().getModel();
        Object parent = path.getLastPathComponent();
        int childCount = model.getChildCount(parent);
        if (childCount == 0) {
            TreeNode<E> node = nodeOf(path);
            return node == null ? null : new Drop<>(node, DragAndDropTransferHandler.DropPosition.INTO);
        }

        boolean above = childIndex < childCount;
        TreeNode<E> sibling = nodeOfComponent(model.getChild(parent, above ? childIndex : childCount - 1));
        if (sibling == null) {
            return null;
        }
        return new Drop<>(sibling, above ? DragAndDropTransferHandler.DropPosition.ABOVE : DragAndDropTransferHandler.DropPosition.BELOW);
    }
}
