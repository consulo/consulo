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

import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.desktop.awt.ui.impl.clipboard.DesktopAWTTransferHandlerAdapter;
import consulo.desktop.awt.ui.impl.clipboard.DesktopAWTTransferTarget;
import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.desktop.awt.ui.impl.tree.DesktopAsyncTreeModel;
import consulo.desktop.awt.ui.impl.tree.DesktopStructureTreeModel;
import consulo.disposer.Disposable;
import consulo.ui.DragAndDropTransferHandler;
import consulo.ui.Length;
import consulo.ui.Point2D;
import consulo.ui.PopupOwner;
import consulo.ui.TransferHandler;
import consulo.ui.Tree;
import consulo.ui.TreeExecutor;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.TreeStyle;
import consulo.ui.event.TreeCollapseEvent;
import consulo.ui.event.TreeDoubleClickEvent;
import consulo.ui.event.TreeExpandEvent;
import consulo.ui.event.TreeSelectEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.MorphColor;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.speedSearch.SpeedSearchSupply;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.tree.NodeRenderer;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awt.tree.TreeVisitor;
import consulo.util.concurrent.Promise;
import consulo.util.concurrent.Promises;
import org.jspecify.annotations.Nullable;

import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public abstract class DesktopTreeBase<E, T extends java.awt.Component> extends SwingComponentDelegate<T>
    implements Tree<E>, PopupOwner, DesktopAWTTransferTarget<TreeNode<E>> {

    private final TreeModel<E> myModel;
    private final DesktopTreeStructure<E> myStructure;
    private final DesktopStructureTreeModel<DesktopTreeStructure<E>> myStructureTreeModel;
    private final TreeExecutor myExecutor;

    private final Disposable myDestroyHook = Disposable.newDisposable("Tree");

    private @Nullable TransferHandler<TreeNode<E>> myTransferHandler;
    private @Nullable Function<TreeNode<E>, Length> myItemHeightGetter;
    private @Nullable Function<TreeNode<E>, String> mySpeedSearchConverter;

    protected DesktopTreeBase(@Nullable E rootValue, TreeModel<E> model, TreeExecutor executor) {
        myModel = model;
        myExecutor = executor;
        myStructure = new DesktopTreeStructure<>(rootValue, model, executor, this);
        myStructureTreeModel = new DesktopStructureTreeModel<>(myStructure, null, this, executor, myDestroyHook);
    }

    protected abstract DesktopAWTTree getTree();

    @Override
    public Disposable destroyHook() {
        return myDestroyHook;
    }

    DesktopTreeStructure<E> getStructure() {
        return myStructure;
    }

    DesktopStructureTreeModel<DesktopTreeStructure<E>> getStructureTreeModel() {
        return myStructureTreeModel;
    }

    DesktopAsyncTreeModel createAsyncTreeModel() {
        return new DesktopAsyncTreeModel(myStructureTreeModel, this, myExecutor, myDestroyHook);
    }

    DesktopAWTTree createTree(javax.swing.tree.TreeModel model) {
        return new DesktopAWTTree(model, this);
    }

    boolean isItemHeightSupported() {
        return true;
    }

    @Override
    public @Nullable Point2D getBestPopupPosition() {
        DesktopAWTTree tree = getTree();

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
                Point point = SwingUtilities.convertPoint(tree, rowBounds.x + 2, rowBounds.y + rowBounds.height - 1, toAWTComponent());
                return new Point2D(point.x, point.y);
            }
        }
        return null;
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

    void installTree(DesktopAWTTree tree) {
        tree.setRootVisible(false);
        // the root is hidden, so without this the rows which stand for its children - the top level of the
        // tree - are drawn with no expand control at all, see BasicTreeUI#shouldPaintExpandControl
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(wrapWithItemHeight(new NodeRenderer()));
        if (myItemHeightGetter != null && isItemHeightSupported()) {
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

                // the model answers whether the node should open, which is what the tree does on its own -
                // the event is consumed only when the model took the click for an action of its own
                return !fireDoubleClick(node, DesktopAWTInputDetails.convert(toAWTComponent(), event));
            }
        }.installOn(tree);

        tree.addTreeSelectionListener(e -> {
            TreePath path = TreeUtil.getSelectedPathIfOne(tree);
            if (path == null) {
                return;
            }

            TreeNode<E> node = nodeOf(path);
            if (node != null) {
                getListenerDispatcher(TreeSelectEvent.class).onEvent(new TreeSelectEvent<>(
                    this,
                    node,
                    DesktopAWTInputDetails.currentEvent(toAWTComponent())
                ));
            }
        });

        // fires for the nodes the user toggles and for those a call on the tree opens alike
        tree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                TreeNode<E> node = nodeOf(event.getPath());
                if (node != null) {
                    getListenerDispatcher(TreeExpandEvent.class).onEvent(
                        new TreeExpandEvent<>(DesktopTreeBase.this, node, DesktopAWTInputDetails.currentEvent(toAWTComponent()))
                    );
                }
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                TreeNode<E> node = nodeOf(event.getPath());
                if (node != null) {
                    getListenerDispatcher(TreeCollapseEvent.class).onEvent(
                        new TreeCollapseEvent<>(DesktopTreeBase.this, node, DesktopAWTInputDetails.currentEvent(toAWTComponent()))
                    );
                }
            }
        });
    }

    boolean fireDoubleClick(TreeNode<E> node, InputDetails inputDetails) {
        getListenerDispatcher(TreeDoubleClickEvent.class).onEvent(new TreeDoubleClickEvent<>(this, node, inputDetails));

        return myModel.onDoubleClick(this, node, inputDetails);
    }

    @Nullable
    TreeNode<E> nodeOf(TreePath path) {
        TreeNode<E> node = nodeOfComponent(TreeUtil.getLastUserObject(path));
        return node == myStructure.getRootNode() ? null : node;
    }

    @Nullable
    @SuppressWarnings({"unchecked", "rawtypes"})
    TreeNode<E> nodeOfComponent(@Nullable Object component) {
        Object userObject = component instanceof DesktopTreeNodeDescriptor ? component : TreeUtil.getUserObject(component);

        if (userObject instanceof DesktopTreeNodeDescriptor<?> descriptor
            && descriptor.getElement() instanceof DesktopTreeNodeImpl element) {
            return element;
        }
        return null;
    }

    @Override
    public @Nullable TreeNode<E> getSelectedNode() {
        TreePath path = TreeUtil.getSelectedPathIfOne(getTree());
        return path == null ? null : nodeOf(path);
    }

    @Override
    public TreeNode<E> getRootNode() {
        return myStructure.getRootNode();
    }

    @Override
    public void select(TreeNode<E> node) {
        DesktopAWTTree tree = getTree();

        myStructureTreeModel.promiseVisitor(node).onSuccess(visitor -> TreeUtil.promiseSelect(tree, visitor));
    }

    @Override
    public List<TreeNode<E>> getSelectedPath() {
        TreePath path = TreeUtil.getSelectedPathIfOne(getTree());
        return path == null ? List.of() : nodesOf(path);
    }

    @Override
    public List<List<TreeNode<E>>> getExpandedPaths() {
        List<List<TreeNode<E>>> paths = new ArrayList<>();

        for (TreePath path : TreeUtil.collectExpandedPaths(getTree())) {
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
        DesktopAWTTree tree = getTree();

        Promise<?> expanded = myStructureTreeModel.promiseVisitor(node)
            .thenAsync(visitor -> TreeUtil.promiseExpand(tree, visitor))
            .thenAsync(basePath -> depth <= 1 ? Promises.resolvedPromise(basePath) : expandBelow(tree, basePath, depth));

        return toFuture(expanded);
    }

    /**
     * A promise which found nothing to open is cancelled rather than rejected, and {@link Promise#onProcessed}
     * is the one handler that runs whichever way it ends - a future left hanging would take its caller with it.
     */
    static CompletableFuture<?> toFuture(Promise<?> promise) {
        CompletableFuture<@Nullable Object> future = new CompletableFuture<>();
        promise.onProcessed(ignored -> future.complete(null));
        return future;
    }

    /**
     * {@link TreeUtil#expand(JTree, int)} counts from the root and opens every branch down to that depth, so a
     * walk which stays inside one subtree is done here. Opening the visited path is what the walk of the
     * platform does - see {@code TreeUtil#promiseMakeVisible} - and the visitor runs on EDT.
     */
    private Promise<TreePath> expandBelow(DesktopAWTTree tree, TreePath basePath, int depth) {
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
        return toFuture(TreeUtil.promiseExpand(getTree(), depth == Integer.MAX_VALUE ? depth : depth + 1));
    }

    @Override
    public CompletableFuture<?> collapseAll() {
        // what DefaultTreeExpander - the collapse all of the platform trees - does
        TreeUtil.collapseAll(getTree(), true, 1);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * The nodes are held between calls, so the model is asked for the level again only once the cached one is
     * marked - otherwise a refresh would show what the tree already had.
     */
    @Override
    public void refreshItem(TreeNode<E> node, boolean refreshChildren) {
        if (refreshChildren && node instanceof DesktopTreeNodeImpl<E> impl) {
            impl.outdateChildren();
        }

        myStructureTreeModel.invalidate(node, refreshChildren);
    }

    @Override
    public CompletableFuture<?> refreshAll() {
        return rebuildKeepingState(() -> {
            myStructure.getRootNode().outdateChildren();

            return myStructureTreeModel.invalidate();
        });
    }

    CompletableFuture<?> rebuildKeepingState(Supplier<? extends Promise<?>> action) {
        DesktopAWTTree tree = getTree();

        List<TreeNode<E>> expanded = expandedNodes(tree);
        TreeNode<E> selected = getSelectedNode();

        return toFuture(action.get())
            .thenCompose(ignored -> restoreExpanded(tree, expanded))
            .thenCompose(ignored -> restoreSelected(tree, selected));
    }

    CompletableFuture<?> refreshLevelsKeepingState(Set<DesktopTreeNodeImpl<E>> levels) {
        DesktopAWTTree tree = getTree();

        List<TreeNode<E>> expanded = new ArrayList<>();
        for (TreeNode<E> node : expandedNodes(tree)) {
            if (isBelow(node, levels)) {
                expanded.add(node);
            }
        }

        TreeNode<E> selected = getSelectedNode();
        TreeNode<E> movable = selected != null && isBelow(selected, levels) ? selected : null;

        List<CompletableFuture<?>> invalidated = new ArrayList<>();
        for (DesktopTreeNodeImpl<E> level : levels) {
            invalidated.add(toFuture(myStructureTreeModel.invalidate(level, true)));
        }

        return CompletableFuture.allOf(invalidated.toArray(new CompletableFuture<?>[0]))
            .thenCompose(ignored -> restoreExpanded(tree, expanded))
            .thenCompose(ignored -> restoreLostSelection(tree, movable));
    }

    private static <E> boolean isBelow(TreeNode<E> node, Set<DesktopTreeNodeImpl<E>> levels) {
        DesktopTreeNodeImpl<E> parent = node instanceof DesktopTreeNodeImpl<E> impl ? impl.getParent() : null;
        while (parent != null) {
            if (levels.contains(parent)) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    private List<TreeNode<E>> expandedNodes(DesktopAWTTree tree) {
        List<TreeNode<E>> nodes = new ArrayList<>();

        for (TreePath path : TreeUtil.collectExpandedPaths(tree)) {
            TreeNode<E> node = nodeOf(path);
            if (node != null) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    private CompletableFuture<?> restoreExpanded(DesktopAWTTree tree, List<TreeNode<E>> nodes) {
        if (nodes.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        List<Promise<TreeVisitor>> visitors = new ArrayList<>();
        for (TreeNode<E> node : nodes) {
            visitors.add(myStructureTreeModel.promiseVisitor(node));
        }

        return toFuture(Promises.collectResults(visitors, true).thenAsync(list -> TreeUtil.promiseExpand(tree, list.stream())));
    }

    private CompletableFuture<?> restoreSelected(DesktopAWTTree tree, @Nullable TreeNode<E> node) {
        if (node == null) {
            return CompletableFuture.completedFuture(null);
        }

        return toFuture(myStructureTreeModel.promiseVisitor(node).thenAsync(visitor -> TreeUtil.promiseSelect(tree, visitor)));
    }

    private CompletableFuture<?> restoreLostSelection(DesktopAWTTree tree, @Nullable TreeNode<E> node) {
        if (node == null) {
            return CompletableFuture.completedFuture(null);
        }

        Promise<TreePath> found = myStructureTreeModel.promiseVisitor(node).thenAsync(visitor -> TreeUtil.promiseVisit(tree, visitor));
        return toFuture(found.onSuccess(path -> UIUtil.invokeLaterIfNeeded(() -> {
            if (path != null && tree.isSelectionEmpty() && tree.isVisible(path)) {
                tree.setSelectionPath(path);
            }
        })));
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
            if (getter != null && component != null && node != null && isItemHeightSupported()) {
                Dimension size = component.getPreferredSize();
                component.setPreferredSize(new Dimension(size.width, DesktopLength.toPixels(tree, getter.apply(node))));
            }

            return component;
        };
    }

    @Override
    public void setItemHeightGetter(@Nullable Function<TreeNode<E>, Length> getter) {
        myItemHeightGetter = getter;

        if (isInitialized() && isItemHeightSupported()) {
            getTree().setRowHeight(getter == null ? UIManager.getInt("Tree.rowHeight") : 0);
        }
    }

    @Override
    public void setSpeedSearchConverter(@Nullable Function<TreeNode<E>, String> converter) {
        mySpeedSearchConverter = converter;

        if (isInitialized()) {
            applySpeedSearch(getTree());
        }
    }

    @Override
    public @Nullable String getSpeedSearchText() {
        if (!isInitialized()) {
            return null;
        }
        SpeedSearchSupply supply = SpeedSearchSupply.getSupply(getTree());
        return supply == null ? null : supply.getEnteredPrefix();
    }

    private void applySpeedSearch(DesktopAWTTree tree) {
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
        applyStyle(getTree(), style);
    }

    static void applyStyle(JComponent component, TreeStyle style) {
        if (style == TreeStyle.TRANSPARENT_BACKGROUND) {
            component.setOpaque(false);
            component.setBackground(MorphColor.of(UIUtil::getPanelBackground));
            return;
        }

        Font font = component.getFont();
        if (font == null) {
            font = UIUtil.getLabelFont();
        }

        component.setFont(font.deriveFont(font.getSize2D() + JBUI.scale(fontDelta(style))));
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

        DesktopAWTTree tree = getTree();
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
        TreePath[] paths = getTree().getSelectionPaths();
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

        javax.swing.tree.TreeModel model = getTree().getModel();
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
