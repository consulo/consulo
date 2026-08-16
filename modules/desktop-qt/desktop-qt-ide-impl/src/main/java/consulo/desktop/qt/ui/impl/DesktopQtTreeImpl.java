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
package consulo.desktop.qt.ui.impl;

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.component.ProcessCanceledException;
import consulo.desktop.qt.ui.impl.image.DesktopQtIconOwner;
import consulo.disposer.Disposable;
import consulo.ui.TransferHandler;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.TreeStyle;
import consulo.ui.UIAccess;
import consulo.ui.event.TreeCollapseEvent;
import consulo.ui.event.TreeDoubleClickEvent;
import consulo.ui.event.TreeExpandEvent;
import consulo.ui.event.TreeSelectEvent;
import consulo.ui.ex.localize.UILocalize;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.gui.QFont;
import io.qt.widgets.QAbstractItemView;
import io.qt.widgets.QApplication;
import io.qt.widgets.QTreeWidget;
import io.qt.widgets.QTreeWidgetItem;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.ToIntFunction;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class DesktopQtTreeImpl<E> extends QtComponentDelegate<QTreeWidget> implements Tree<E>, DesktopQtIconOwner {
    /**
     * Levels of rows an expand all opens, counting the top level ones.
     */
    private static final int EXPAND_ALL_DEPTH = 4;

    private @Nullable TransferHandler<TreeNode<E>> myTransferHandler;

    private final E myRootValue;
    private final TreeModel<E> myModel;

    /**
     * Children are built off the ui thread, so the widget and this map are only ever touched from the qt
     * thread while the map itself can be read from the loading chain.
     */
    private final Map<QTreeWidgetItem, DesktopQtTreeNode<E>> myNodes = new ConcurrentHashMap<>();

    private final ExecutorService myExecutor;

    /**
     * The node the tree was built on. It has no row of its own - the tree starts at its children - and it is
     * where a walk down a stored path, or down to a file being revealed, begins.
     */
    private volatile DesktopQtTreeNode<E> myRootNode;

    private @Nullable ToIntFunction<TreeNode<E>> myItemHeightGetter;

    public DesktopQtTreeImpl(E rootValue, TreeModel<E> model, Disposable disposable) {
        myRootValue = rootValue;
        myModel = model;
        myExecutor = AppExecutorUtil.createBoundedApplicationPoolExecutor(
            "DesktopQtTree Loader",
            AppExecutorUtil.getAppExecutorService(),
            1,
            disposable
        );

        myRootNode = createRootNode();
    }

    private DesktopQtTreeNode<E> createRootNode() {
        DesktopQtTreeNode<E> root = new DesktopQtTreeNode<>(null, myRootValue);
        root.setLoader(this::buildAsync);
        return root;
    }

    @Override
    protected QTreeWidget createQt(QWidget parent) {
        QTreeWidget tree = new QTreeWidget(parent);
        tree.setColumnCount(1);
        tree.setHeaderHidden(true);
        tree.setSelectionMode(QAbstractItemView.SelectionMode.SingleSelection);
        return tree;
    }

    @Override
    protected void initialize(QTreeWidget tree) {
        // hiding a tool window disposes the widget, and showing it again binds a fresh empty one - the nodes
        // of the previous widget are gone with it, so the level below the root has to be built once more
        myNodes.clear();
        myRootNode = createRootNode();

        tree.itemSelectionChanged.connect(() ->
            getListenerDispatcher(TreeSelectEvent.class).onEvent(new TreeSelectEvent(DesktopQtTreeImpl.this, getSelectedNode()))
        );

        tree.itemDoubleClicked.connect((item, column) -> UIAccess.current().give(() -> {
            TreeNode<E> selectedNode = getSelectedNode();
            if (selectedNode != null) {
                getListenerDispatcher(TreeDoubleClickEvent.class).onEvent(new TreeDoubleClickEvent<>(DesktopQtTreeImpl.this, selectedNode));

                myModel.onDoubleClick(DesktopQtTreeImpl.this, selectedNode);
            }
        }));

        tree.itemExpanded.connect(item -> {
            DesktopQtTreeNode<E> node = myNodes.get(item);
            if (node != null) {
                node.setExpanded(true);

                node.loadChildren();
            }

            fireExpand(item);
        });

        tree.itemCollapsed.connect(item -> {
            DesktopQtTreeNode<E> node = myNodes.get(item);
            if (node != null) {
                node.setExpanded(false);
            }

            fireCollapse(item);
        });

        myRootNode.loadChildren();
    }

    private CompletableFuture<List<DesktopQtTreeNode<E>>> buildAsync(DesktopQtTreeNode<E> node) {
        QTreeWidgetItem parent = node.getTreeItem();

        return DesktopQtUIAccess.INSTANCE.giveAsync(() -> showLoading(parent))
            .thenCompose(loading -> CompletableFuture.supplyAsync(() -> fetchChildren(node), myExecutor)
                .thenCompose(children -> DesktopQtUIAccess.INSTANCE.giveAsync(() -> {
                    hideLoading(loading);

                    attach(node, parent, children);
                    return children;
                })));
    }

    /**
     * The row standing in for the level while it is being fetched, so a level that takes a while to build does
     * not read as an empty one - what the web tree shows as its placeholder node.
     */
    private @Nullable QTreeWidgetItem showLoading(@Nullable QTreeWidgetItem parent) {
        if (myComponent == null || myComponent.isDisposed() || parent != null && parent.isDisposed()) {
            return null;
        }

        QTreeWidgetItem loading = parent == null ? new QTreeWidgetItem(myComponent) : new QTreeWidgetItem(parent);
        loading.setText(0, UILocalize.treenodeLoading().get());
        loading.setFlags(Qt.ItemFlag.ItemIsEnabled);
        loading.setChildIndicatorPolicy(QTreeWidgetItem.ChildIndicatorPolicy.DontShowIndicator);
        return loading;
    }

    private void hideLoading(@Nullable QTreeWidgetItem loading) {
        if (loading == null || loading.isDisposed()) {
            return;
        }

        QTreeWidgetItem parent = loading.parent();
        if (parent != null) {
            int index = parent.indexOfChild(loading);
            if (index < 0) {
                return;
            }

            parent.takeChild(index);
        }
        else if (myComponent != null && !myComponent.isDisposed()) {
            int index = myComponent.indexOfTopLevelItem(loading);
            if (index < 0) {
                return;
            }

            myComponent.takeTopLevelItem(index);
        }
        else {
            return;
        }

        loading.dispose();
    }

    private List<DesktopQtTreeNode<E>> fetchChildren(DesktopQtTreeNode<E> parent) {
        List<DesktopQtTreeNode<E>> list = new ArrayList<>();

        try {
            myModel.buildChildren(e -> {
                DesktopQtTreeNode<E> node = new DesktopQtTreeNode<>(parent, e);
                node.setLoader(this::buildAsync);
                list.add(node);
                return node;
            }, parent.getValue());
        }
        catch (ProcessCanceledException ignored) {
            return List.of();
        }

        Comparator<TreeNode<E>> comparator = myModel.getNodeComparator();
        if (comparator != null) {
            list.sort(comparator);
        }

        return list;
    }

    private void attach(DesktopQtTreeNode<E> parentNode, @Nullable QTreeWidgetItem parent, List<DesktopQtTreeNode<E>> children) {
        if (myComponent == null || myComponent.isDisposed() || parent != null && parent.isDisposed()) {
            return;
        }

        for (DesktopQtTreeNode<E> node : children) {
            QTreeWidgetItem item = parent == null ? new QTreeWidgetItem(myComponent) : new QTreeWidgetItem(parent);

            myNodes.put(item, node);

            node.setTreeItem(item);
            node.render();

            applyItemHeight(item, node);

            item.setChildIndicatorPolicy(node.isLeaf()
                ? QTreeWidgetItem.ChildIndicatorPolicy.DontShowIndicator
                : QTreeWidgetItem.ChildIndicatorPolicy.ShowIndicator);
        }

        parentNode.setChildren(children);

        if (children.isEmpty()) {
            // nothing came back, so the node has nothing below it - a walk which reaches it stops here rather
            // than asking the model over and over
            parentNode.setLeaf(true);
        }

        if (parent != null) {
            parent.setChildIndicatorPolicy(QTreeWidgetItem.ChildIndicatorPolicy.DontShowIndicatorWhenChildless);
        }
    }

    /**
     * Opens the row of the node, which is a no-op for the root - it has none.
     */
    private CompletableFuture<?> expandSelfAsync(DesktopQtTreeNode<E> node) {
        QTreeWidgetItem item = node.getTreeItem();
        if (item == null) {
            return CompletableFuture.completedFuture(null);
        }

        return DesktopQtUIAccess.INSTANCE.giveAsync(() -> {
            if (!item.isDisposed() && item.childCount() != 0) {
                if (!item.isExpanded()) {
                    item.setExpanded(true);
                }

                // the signal only reports a row which changed, so a row that was already open says nothing
                node.setExpanded(true);
            }

            return null;
        });
    }

    /**
     * Opens the node and the levels below it, building each level before it is opened - a level which is not
     * built yet has no rows to open, so it has to be waited for rather than walked over.
     */
    private CompletableFuture<?> expandDeepAsync(DesktopQtTreeNode<E> node, int depth) {
        if (depth <= 0 || node.isLeaf()) {
            return CompletableFuture.completedFuture(null);
        }

        return node.loadChildren()
            .thenCompose(children -> expandSelfAsync(node).thenCompose(v -> CompletableFuture.allOf(
                children.stream().map(child -> expandDeepAsync(child, depth - 1)).toArray(CompletableFuture[]::new)
            )));
    }

    /**
     * Opens everything above the node, so that its row exists and is on screen. The levels are opened one after
     * another - a node is only built once the one above it is.
     */
    private CompletableFuture<?> revealAsync(DesktopQtTreeNode<E> node) {
        List<DesktopQtTreeNode<E>> path = pathTo(node);

        CompletableFuture<?> reveal = CompletableFuture.completedFuture(null);
        for (int i = 0; i < path.size() - 1; i++) {
            DesktopQtTreeNode<E> ancestor = path.get(i);
            reveal = reveal.thenCompose(ignored -> ancestor.loadChildren().thenCompose(children -> expandSelfAsync(ancestor)));
        }
        return reveal;
    }

    private List<DesktopQtTreeNode<E>> pathTo(DesktopQtTreeNode<E> node) {
        LinkedList<DesktopQtTreeNode<E>> path = new LinkedList<>();
        for (DesktopQtTreeNode<E> current = node; current != null; current = current.getParent()) {
            path.addFirst(current);
        }
        return path;
    }

    private void collapseItem(QTreeWidgetItem item) {
        for (int i = 0; i < item.childCount(); i++) {
            collapseItem(item.child(i));
        }

        if (item.isExpanded()) {
            item.setExpanded(false);
        }
    }

    private void fireExpand(QTreeWidgetItem item) {
        DesktopQtTreeNode<E> node = myNodes.get(item);
        if (node != null) {
            getListenerDispatcher(TreeExpandEvent.class).onEvent(new TreeExpandEvent(this, node));
        }
    }

    private void fireCollapse(QTreeWidgetItem item) {
        DesktopQtTreeNode<E> node = myNodes.get(item);
        if (node != null) {
            getListenerDispatcher(TreeCollapseEvent.class).onEvent(new TreeCollapseEvent(this, node));
        }
    }

    private void forget(QTreeWidgetItem item) {
        for (int i = 0; i < item.childCount(); i++) {
            forget(item.child(i));
        }

        myNodes.remove(item);
    }

    @Override
    public @Nullable TreeNode<E> getSelectedNode() {
        if (myComponent == null || myComponent.isDisposed()) {
            return null;
        }

        List<QTreeWidgetItem> selection = myComponent.selectedItems();
        if (selection.size() != 1) {
            return null;
        }
        return myNodes.get(selection.get(0));
    }

    @Override
    public CompletableFuture<?> expand(TreeNode<E> node, int depth) {
        if (!(node instanceof DesktopQtTreeNode<E> qtNode)) {
            return CompletableFuture.completedFuture(null);
        }

        return revealAsync(qtNode).thenCompose(v -> expandDeepAsync(qtNode, depth));
    }

    @Override
    public boolean isExpandCollapseAllSupported() {
        return true;
    }

    @Override
    public CompletableFuture<?> expandAll() {
        // the children of a node are fetched when it is first opened, so an unbounded expand all would build a
        // project view down to every file - the depth is capped the way the web tree caps it
        return expandAll(EXPAND_ALL_DEPTH);
    }

    /**
     * The root carries no row of its own, so a depth of one has to reach the level below it.
     */
    @Override
    public CompletableFuture<?> expandAll(int depth) {
        return expandDeepAsync(myRootNode, depth == Integer.MAX_VALUE ? depth : depth + 1);
    }

    @Override
    public CompletableFuture<?> collapseAll() {
        return DesktopQtUIAccess.INSTANCE.giveAsync(() -> {
            if (myComponent == null || myComponent.isDisposed()) {
                return null;
            }

            for (int i = 0; i < myComponent.topLevelItemCount(); i++) {
                collapseItem(myComponent.topLevelItem(i));
            }
            return null;
        });
    }

    @Override
    public TreeNode<E> getRootNode() {
        return myRootNode;
    }

    @Override
    public List<List<TreeNode<E>>> getExpandedPaths() {
        List<List<TreeNode<E>>> paths = new ArrayList<>();
        collectExpandedPaths(myRootNode, paths);
        return paths;
    }

    private void collectExpandedPaths(DesktopQtTreeNode<E> node, List<List<TreeNode<E>>> paths) {
        for (DesktopQtTreeNode<E> child : node.getChildren()) {
            if (child.isExpanded()) {
                paths.add(List.copyOf(pathTo(child)));

                collectExpandedPaths(child, paths);
            }
        }
    }

    @Override
    public List<TreeNode<E>> getSelectedPath() {
        return getSelectedNode() instanceof DesktopQtTreeNode<E> node ? List.copyOf(pathTo(node)) : List.of();
    }

    @Override
    public void select(TreeNode<E> node) {
        if (!(node instanceof DesktopQtTreeNode<E> qtNode)) {
            return;
        }

        revealAsync(qtNode).thenCompose(v -> DesktopQtUIAccess.INSTANCE.giveAsync(() -> {
            QTreeWidgetItem item = qtNode.getTreeItem();
            if (item == null || item.isDisposed() || myComponent == null || myComponent.isDisposed()) {
                return null;
            }

            myComponent.setCurrentItem(item);
            myComponent.scrollToItem(item);
            return null;
        }));
    }

    @Override
    public void refreshItem(TreeNode<E> node, boolean refreshChildren) {
        if (!(node instanceof DesktopQtTreeNode<E> qtNode)) {
            return;
        }

        QTreeWidgetItem item = qtNode.getTreeItem();
        if (item == null || item.isDisposed()) {
            return;
        }

        qtNode.render();

        if (refreshChildren) {
            for (QTreeWidgetItem child : item.takeChildren()) {
                forget(child);

                child.dispose();
            }

            qtNode.resetChildren();

            qtNode.loadChildren();
        }
    }

    @Override
    public void refreshIcons() {
        for (DesktopQtTreeNode<E> node : myNodes.values()) {
            node.render();
        }
    }

    @Override
    public CompletableFuture<?> refreshAll() {
        return DesktopQtUIAccess.INSTANCE.<Void>giveAsync(() -> {
            if (myComponent != null && !myComponent.isDisposed()) {
                myComponent.clear();
            }

            myNodes.clear();
            myRootNode = createRootNode();
            return null;
        }).thenCompose(v -> myRootNode.loadChildren());
    }

    @Override
    public void setItemHeightGetter(@Nullable ToIntFunction<TreeNode<E>> getter) {
        myItemHeightGetter = getter;
    }

    private void applyItemHeight(QTreeWidgetItem item, DesktopQtTreeNode<E> node) {
        ToIntFunction<TreeNode<E>> getter = myItemHeightGetter;
        if (getter == null) {
            return;
        }

        item.setSizeHint(0, new QSize(item.sizeHint(0).width(), getter.applyAsInt(node)));
    }

    @Override
    public void addStyle(TreeStyle style) {
        switch (style) {
            case TRANSPARENT_BACKGROUND -> whenBound(widget -> {
                widget.setAutoFillBackground(false);
                ((QTreeWidget) widget).viewport().setAutoFillBackground(false);
            });
            case FONT_XX_SMALL -> scaleFont(0.6f);
            case FONT_X_SMALL -> scaleFont(0.75f);
            case FONT_SMALL -> scaleFont(0.9f);
            case FONT_MEDIUM -> scaleFont(1.0f);
            case FONT_LARGE -> scaleFont(1.2f);
            case FONT_X_LARGE -> scaleFont(1.5f);
            case FONT_XX_LARGE -> scaleFont(2.0f);
        }
    }

    private void scaleFont(float scale) {
        whenBound(widget -> {
            QFont font = new QFont(widget.font());
            font.setPointSizeF(Math.max(1.0, QApplication.font().pointSizeF() * scale));
            widget.setFont(font);
        });
    }

    @Override
    public void setTransferHandler(@Nullable TransferHandler<TreeNode<E>> handler) {
        myTransferHandler = handler;
    }

    @Override
    public @Nullable TransferHandler<TreeNode<E>> getTransferHandler() {
        return myTransferHandler;
    }
}
