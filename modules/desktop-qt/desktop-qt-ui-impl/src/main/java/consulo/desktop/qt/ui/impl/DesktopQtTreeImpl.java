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

import consulo.desktop.qt.ui.impl.font.DesktopQtFontSize;
import consulo.desktop.qt.ui.impl.clipboard.DesktopQtDragAndDropSupport;
import consulo.desktop.qt.ui.impl.clipboard.DesktopQtDrop;
import consulo.desktop.qt.ui.impl.clipboard.DesktopQtTransferTarget;
import consulo.desktop.qt.ui.impl.image.DesktopQtIconOwner;
import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.DragAndDropTransferHandler;
import consulo.ui.Length;
import consulo.ui.Point2D;
import consulo.ui.PopupOwner;
import consulo.ui.TextItemPresentation;
import consulo.ui.TransferHandler;
import consulo.ui.Tree;
import consulo.ui.TreeExecutor;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.TreeStyle;
import consulo.ui.UIAccess;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.impl.tree.TreeController;
import consulo.ui.impl.tree.TreeNodeImpl;
import consulo.ui.impl.tree.TreeWidget;
import io.qt.core.QPoint;
import io.qt.core.QRect;
import io.qt.core.QSize;
import io.qt.core.QTimer;
import io.qt.core.Qt;
import io.qt.gui.QDragEnterEvent;
import io.qt.gui.QDragLeaveEvent;
import io.qt.gui.QDragMoveEvent;
import io.qt.gui.QDropEvent;
import io.qt.gui.QFont;
import io.qt.gui.QKeyEvent;
import io.qt.gui.QMouseEvent;
import io.qt.gui.QPaintEvent;
import io.qt.widgets.QAbstractItemView;
import io.qt.widgets.QApplication;
import io.qt.widgets.QFrame;
import io.qt.widgets.QTreeWidget;
import io.qt.widgets.QTreeWidgetItem;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtTreeImpl<E> extends QtComponentDelegate<QTreeWidget>
    implements Tree<E>, PopupOwner, DesktopQtIconOwner, DesktopQtTransferTarget<TreeNode<E>> {
    private static final int ourDropExpandDelay = 2000;

    private @Nullable TransferHandler<TreeNode<E>> myTransferHandler;
    private final DesktopQtDragAndDropSupport<TreeNode<E>> myDragAndDrop = new DesktopQtDragAndDropSupport<>(this, this);
    private @Nullable QTreeWidgetItem myDropExpandItem;
    private int myDropExpandGeneration;
    private @Nullable Function<TreeNode<E>, String> mySpeedSearchConverter;
    private @Nullable Function<TreeNode<E>, Length> myItemHeightGetter;

    private final Disposable myDestroyHook = Disposable.newDisposable("Tree");

    protected final TreeController<E> myController;

    private final Map<QTreeWidgetItem, TreeNodeImpl<E>> myNodes = new HashMap<>();
    private final Map<TreeNodeImpl<E>, QTreeWidgetItem> myItems = new HashMap<>();
    private final Map<TreeNodeImpl<E>, QTreeWidgetItem> myLoadingItems = new HashMap<>();

    private boolean mySyncing;
    private boolean myAllowMultipleSelect;

    public DesktopQtTreeImpl(@Nullable E rootValue, TreeModel<E> model, TreeExecutor executor) {
        myController = new TreeController<>(this, rootValue, model, executor, new Binding());
        Disposer.register(myDestroyHook, myController);
    }

    @Override
    public Disposable destroyHook() {
        return myDestroyHook;
    }

    /**
     * A view emits its double click for whichever button was struck, while the api means the left one by it -
     * the right button opens the context menu, and answering it with the action of a double click as well would
     * run that action every time the menu is asked for.
     */
    private class QtTree extends QTreeWidget {
        private final DesktopQtDragAndDropSupport<?> myDragAndDrop;

        private QtTree(QWidget parent, DesktopQtDragAndDropSupport<?> dragAndDrop) {
            super(parent);
            myDragAndDrop = dragAndDrop;
        }

        @Override
        protected void mouseDoubleClickEvent(QMouseEvent event) {
            if (event.button() != Qt.MouseButton.LeftButton || isPressOnly(this, event)) {
                // the second click of a pair arrives as a double click rather than as a press, so it has to be
                // handled as the press it stands for - dropping it would swallow every other right click
                mousePressEvent(event);
                return;
            }

            super.mouseDoubleClickEvent(event);
        }

        @Override
        protected void keyPressEvent(QKeyEvent event) {
            if (consumeKeyPress(this, event)) {
                event.accept();
                return;
            }

            super.keyPressEvent(event);
        }

        @Override
        protected void startDrag(Qt.DropActions supportedActions) {
            myDragAndDrop.startDrag(this);
        }

        @Override
        protected void dragEnterEvent(QDragEnterEvent event) {
            myDragAndDrop.dragEnter(this, event);
        }

        @Override
        protected void dragMoveEvent(QDragMoveEvent event) {
            myDragAndDrop.dragMove(this, event);
            if (myDragAndDrop.shouldAutoScroll(this, event.position().toPoint())) {
                startAutoScroll();
            }
        }

        @Override
        protected void dragLeaveEvent(QDragLeaveEvent event) {
            stopAutoScroll();
            myDragAndDrop.dragLeave(this);
        }

        @Override
        protected void dropEvent(QDropEvent event) {
            stopAutoScroll();
            myDragAndDrop.drop(this, event);
        }

        @Override
        protected void paintEvent(QPaintEvent event) {
            super.paintEvent(event);
            myDragAndDrop.paintIndicator(this);
        }

        @Override
        protected void scrollContentsBy(int dx, int dy) {
            super.scrollContentsBy(dx, dy);
            myDragAndDrop.contentsScrolled(this);
        }
    }

    @Override
    protected QTreeWidget createQt(QWidget parent) {
        QTreeWidget tree = new QtTree(parent, myDragAndDrop);
        tree.setColumnCount(1);
        tree.setHeaderHidden(true);
        tree.setFrameShape(QFrame.Shape.NoFrame);
        tree.setSelectionMode(selectionMode());
        tree.setItemDelegate(new DesktopQtTextItemDelegate(tree));

        // a view opens and closes the row of a double click by itself, and here that is the answer of the model -
        // leaving it to both means the two undo each other and the row never moves
        tree.setExpandsOnDoubleClick(false);

        return tree;
    }

    @Override
    protected void initialize(QTreeWidget tree) {
        myNodes.clear();
        myItems.clear();
        myLoadingItems.clear();
        myDropExpandItem = null;

        myDragAndDrop.install(tree);

        tree.itemSelectionChanged.connect(() -> {
            if (mySyncing) {
                return;
            }

            if (myAllowMultipleSelect) {
                myController.onSelectionChanged(leadNode(tree), selectedNodes(tree), DesktopQtCurrentInput.current(tree));
            }
            else {
                myController.onSelected(selectedNode(tree), DesktopQtCurrentInput.current(tree));
            }
        });

        tree.itemDoubleClicked.connect((item, column) -> {
            // the handler is queued past the dispatch of the click, so the details are taken while it still runs
            InputDetails inputDetails = DesktopQtCurrentInput.current(tree);
            UIAccess.current().give(() -> {
                TreeNodeImpl<E> selectedNode = myController.getSelected();
                if (selectedNode != null) {
                    myController.onDoubleClick(selectedNode, inputDetails);
                }
            });
        });

        tree.itemExpanded.connect(item -> {
            TreeNodeImpl<E> node = myNodes.get(item);
            if (!mySyncing && node != null) {
                myController.onExpanded(node, DesktopQtCurrentInput.current(tree));
            }
        });

        tree.itemCollapsed.connect(item -> {
            TreeNodeImpl<E> node = myNodes.get(item);
            if (!mySyncing && node != null) {
                myController.onCollapsed(node, DesktopQtCurrentInput.current(tree));
            }
        });

        myController.bind();
    }

    private @Nullable TreeNodeImpl<E> selectedNode(QTreeWidget tree) {
        List<QTreeWidgetItem> selection = tree.selectedItems();
        return selection.size() == 1 ? myNodes.get(selection.get(0)) : null;
    }

    private @Nullable TreeNodeImpl<E> leadNode(QTreeWidget tree) {
        QTreeWidgetItem current = tree.currentItem();
        return current != null && current.isSelected() ? myNodes.get(current) : null;
    }

    private List<TreeNodeImpl<E>> selectedNodes(QTreeWidget tree) {
        List<TreeNodeImpl<E>> nodes = new ArrayList<>();
        for (QTreeWidgetItem item : tree.selectedItems()) {
            TreeNodeImpl<E> node = myNodes.get(item);
            if (node != null) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    private QAbstractItemView.SelectionMode selectionMode() {
        return myAllowMultipleSelect ? QAbstractItemView.SelectionMode.ExtendedSelection : QAbstractItemView.SelectionMode.SingleSelection;
    }

    private class Binding implements TreeWidget<E> {
        @Override
        public UIAccess getUIAccess() {
            return DesktopQtUIAccess.INSTANCE;
        }

        @Override
        public TextItemPresentation createPresentation() {
            return new DesktopQtTextItemPresentation();
        }

        @Override
        public void showLoading(TreeNodeImpl<E> node) {
            QTreeWidgetItem parent = rowsOf(node);
            if (parent == null || myLoadingItems.containsKey(node)) {
                return;
            }

            QTreeWidgetItem loading = new QTreeWidgetItem();
            loading.setText(0, UILocalize.treenodeLoading().get());
            loading.setFlags(Qt.ItemFlag.ItemIsEnabled);
            loading.setChildIndicatorPolicy(QTreeWidgetItem.ChildIndicatorPolicy.DontShowIndicator);

            sync(() -> parent.addChild(loading));
            myLoadingItems.put(node, loading);
        }

        @Override
        public void hideLoading(TreeNodeImpl<E> node) {
            QTreeWidgetItem loading = myLoadingItems.remove(node);
            if (loading == null || loading.isDisposed()) {
                return;
            }

            QTreeWidgetItem parent = rowsOf(node);
            int index = parent == null ? -1 : parent.indexOfChild(loading);
            if (index >= 0) {
                sync(() -> parent.takeChild(index));
            }

            loading.dispose();
        }

        @Override
        public void setChildren(TreeNodeImpl<E> node, List<TreeNodeImpl<E>> children) {
            QTreeWidgetItem parent = rowsOf(node);
            if (parent == null) {
                return;
            }

            sync(() -> {
                List<QTreeWidgetItem> reordered = reorder(node, parent, children);
                if (reordered != null) {
                    for (int index = 0; index < children.size(); index++) {
                        render(children.get(index), reordered.get(index));
                    }
                }
                else {
                    reconcile(node, parent, children);
                }

                restoreSelection();
            });
        }

        private void reconcile(TreeNodeImpl<E> node, QTreeWidgetItem parent, List<TreeNodeImpl<E>> children) {
            Set<QTreeWidgetItem> wanted = new HashSet<>();
            for (TreeNodeImpl<E> child : children) {
                QTreeWidgetItem item = liveItem(child);
                if (item != null) {
                    wanted.add(item);
                }
            }

            QTreeWidgetItem loading = myLoadingItems.get(node);
            for (int i = parent.childCount() - 1; i >= 0; i--) {
                QTreeWidgetItem item = parent.child(i);
                if (item != loading && !wanted.contains(item)) {
                    parent.takeChild(i);
                    forget(item);
                    item.dispose();
                }
            }

            for (int index = 0; index < children.size(); index++) {
                TreeNodeImpl<E> child = children.get(index);
                QTreeWidgetItem item = liveItem(child);
                int current = item == null ? -1 : parent.indexOfChild(item);
                if (item == null || current < 0) {
                    item = new QTreeWidgetItem();
                    myItems.put(child, item);
                    myNodes.put(item, child);
                    parent.insertChild(index, item);
                }
                else if (current != index) {
                    parent.takeChild(current);
                    parent.insertChild(index, item);
                    restoreExpanded(child, item);
                }

                render(child, item);
            }
        }

        private @Nullable List<QTreeWidgetItem> reorder(TreeNodeImpl<E> node, QTreeWidgetItem parent, List<TreeNodeImpl<E>> children) {
            int count = parent.childCount();
            if (count < 2 || count != children.size() || myLoadingItems.containsKey(node)) {
                return null;
            }

            List<QTreeWidgetItem> current = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                current.add(parent.child(i));
            }

            Set<QTreeWidgetItem> present = new HashSet<>(current);
            List<QTreeWidgetItem> ordered = new ArrayList<>(count);
            boolean moved = false;
            for (TreeNodeImpl<E> child : children) {
                QTreeWidgetItem item = liveItem(child);
                if (item == null || !present.remove(item)) {
                    return null;
                }

                if (item != current.get(ordered.size())) {
                    moved = true;
                }
                ordered.add(item);
            }

            if (moved) {
                parent.takeChildren();
                parent.addChildren(ordered);

                for (int index = 0; index < children.size(); index++) {
                    restoreExpanded(children.get(index), ordered.get(index));
                }
            }
            return ordered;
        }

        @Override
        public void setExpanded(TreeNodeImpl<E> node, boolean expanded) {
            QTreeWidgetItem item = liveItem(node);
            if (item != null && item.isExpanded() != expanded) {
                sync(() -> item.setExpanded(expanded));
            }
        }

        @Override
        public void setSelected(@Nullable TreeNodeImpl<E> node) {
            QTreeWidget tree = myComponent;
            if (tree == null || tree.isDisposed()) {
                return;
            }

            if (node == null) {
                sync(tree::clearSelection);
                return;
            }

            QTreeWidgetItem item = liveItem(node);
            if (item != null) {
                sync(() -> {
                    tree.setCurrentItem(item);
                    tree.scrollToItem(item);
                });
            }
        }

        @Override
        public void update(TreeNodeImpl<E> node) {
            QTreeWidgetItem item = liveItem(node);
            if (item != null) {
                render(node, item);
            }
        }
    }

    private @Nullable QTreeWidgetItem rowsOf(TreeNodeImpl<E> node) {
        QTreeWidget tree = myComponent;
        if (tree == null || tree.isDisposed()) {
            return null;
        }

        return node == myController.getRoot() ? tree.invisibleRootItem() : liveItem(node);
    }

    private @Nullable QTreeWidgetItem liveItem(TreeNodeImpl<E> node) {
        QTreeWidgetItem item = myItems.get(node);
        return item == null || item.isDisposed() ? null : item;
    }

    private void forget(QTreeWidgetItem item) {
        for (int i = 0; i < item.childCount(); i++) {
            forget(item.child(i));
        }

        TreeNodeImpl<E> node = myNodes.remove(item);
        if (node != null) {
            myItems.remove(node);
            myLoadingItems.remove(node);
        }
    }

    private void restoreExpanded(TreeNodeImpl<E> node, QTreeWidgetItem item) {
        if (node.isExpanded()) {
            item.setExpanded(true);
        }

        for (TreeNodeImpl<E> child : node.getChildren()) {
            QTreeWidgetItem childItem = liveItem(child);
            if (childItem != null) {
                restoreExpanded(child, childItem);
            }
        }
    }

    private void restoreSelection() {
        QTreeWidget tree = myComponent;
        TreeNodeImpl<E> selected = myController.getSelected();
        QTreeWidgetItem item = selected == null ? null : liveItem(selected);
        if (tree != null && item != null && !item.isSelected()) {
            tree.setCurrentItem(item);
        }

        if (tree != null && myAllowMultipleSelect) {
            for (TreeNode<E> node : myController.getSelection()) {
                QTreeWidgetItem selectedItem = node instanceof TreeNodeImpl<E> impl ? liveItem(impl) : null;
                if (selectedItem != null && !selectedItem.isSelected()) {
                    selectedItem.setSelected(true);
                }
            }
        }
    }

    protected void render(TreeNodeImpl<E> node, QTreeWidgetItem item) {
        if (node.getPresentation() instanceof DesktopQtTextItemPresentation presentation) {
            item.setText(0, presentation.toString());

            DesktopQtTextItemDelegate.bind(item, 0, presentation);
            item.setBackground(0, DesktopQtTextItemDelegate.toBrush(presentation.getBackgroundColor()));

            item.setIcon(0, DesktopQtImage.toQIcon(presentation.getImage()));
        }

        applyItemHeight(item, node);

        if (node.isLeaf()) {
            item.setChildIndicatorPolicy(QTreeWidgetItem.ChildIndicatorPolicy.DontShowIndicator);
        }
        else if (node.isLoaded()) {
            item.setChildIndicatorPolicy(QTreeWidgetItem.ChildIndicatorPolicy.DontShowIndicatorWhenChildless);
        }
        else {
            item.setChildIndicatorPolicy(QTreeWidgetItem.ChildIndicatorPolicy.ShowIndicator);
        }
    }

    protected boolean isPressOnly(QTreeWidget tree, QMouseEvent event) {
        return false;
    }

    protected boolean consumeKeyPress(QTreeWidget tree, QKeyEvent event) {
        return false;
    }

    protected final @Nullable TreeNodeImpl<E> nodeOf(QTreeWidgetItem item) {
        return myNodes.get(item);
    }

    protected final void forEachItem(BiConsumer<TreeNodeImpl<E>, QTreeWidgetItem> action) {
        for (Map.Entry<TreeNodeImpl<E>, QTreeWidgetItem> entry : List.copyOf(myItems.entrySet())) {
            QTreeWidgetItem item = entry.getValue();
            if (!item.isDisposed()) {
                action.accept(entry.getKey(), item);
            }
        }
    }

    protected final boolean isSyncing() {
        return mySyncing;
    }

    protected final void sync(Runnable action) {
        boolean syncing = mySyncing;
        mySyncing = true;
        try {
            action.run();
        }
        finally {
            mySyncing = syncing;
        }
    }

    @Override
    public @Nullable TreeNode<E> getSelectedNode() {
        return myController.getSelected();
    }

    @Override
    public List<TreeNode<E>> getSelectedNodes() {
        return myController.getSelection();
    }

    @Override
    public void setAllowMultipleSelect(boolean allow) {
        myAllowMultipleSelect = allow;

        QTreeWidget tree = myComponent;
        if (tree != null && !tree.isDisposed()) {
            tree.setSelectionMode(selectionMode());
        }

        myController.setAllowMultipleSelect(allow);
    }

    @Override
    public CompletableFuture<?> expand(TreeNode<E> node, int depth) {
        return myController.expand(node, depth);
    }

    @Override
    public boolean isExpandCollapseAllSupported() {
        return true;
    }

    @Override
    public CompletableFuture<?> expandAll() {
        return myController.expandAll();
    }

    @Override
    public CompletableFuture<?> expandAll(int depth) {
        return myController.expandAll(depth);
    }

    @Override
    public CompletableFuture<?> collapseAll() {
        return myController.collapseAll();
    }

    @Override
    public TreeNode<E> getRootNode() {
        return myController.getRoot();
    }

    @Override
    public List<List<TreeNode<E>>> getExpandedPaths() {
        return myController.getExpandedPaths();
    }

    @Override
    public List<TreeNode<E>> getSelectedPath() {
        return myController.getSelectedPath();
    }

    @Override
    public void select(TreeNode<E> node) {
        myController.select(node);
    }

    @Override
    public void refreshItem(TreeNode<E> node, boolean refreshChildren) {
        myController.refreshItem(node, refreshChildren);
    }

    @Override
    public CompletableFuture<?> refreshAll() {
        return myController.refreshAll();
    }

    /**
     * Where a popup raised over the tree hangs off - the bottom left of the row which is current, which is what the
     * awt trees anchor to. The rectangle of a row is measured against the viewport, while a popup is placed against
     * the component, so the two are the same point only once the frame and the header are counted in.
     */
    @Override
    public @Nullable Point2D getBestPopupPosition() {
        QTreeWidget tree = myComponent;
        if (tree == null || tree.isDisposed()) {
            return null;
        }

        QTreeWidgetItem item = tree.currentItem();
        if (item == null) {
            return null;
        }

        QRect rect = tree.visualItemRect(item);
        if (rect.isEmpty()) {
            return null;
        }

        QPoint bottomLeft = tree.viewport().mapTo(tree, new QPoint(rect.x(), rect.bottom()));

        return new Point2D(bottomLeft.x() + 2, bottomLeft.y() - 1);
    }

    @Override
    public void refreshIcons() {
        renderAll();
    }

    protected void renderAll() {
        for (Map.Entry<TreeNodeImpl<E>, QTreeWidgetItem> entry : List.copyOf(myItems.entrySet())) {
            QTreeWidgetItem item = entry.getValue();
            if (!item.isDisposed()) {
                render(entry.getKey(), item);
            }
        }
    }

    @Override
    public void setItemHeightGetter(@Nullable Function<TreeNode<E>, Length> getter) {
        myItemHeightGetter = getter;
    }

    private void applyItemHeight(QTreeWidgetItem item, TreeNode<E> node) {
        Function<TreeNode<E>, Length> getter = myItemHeightGetter;
        if (getter == null) {
            return;
        }

        // the width is -1 until something sets one, and a negative width makes the whole hint invalid
        int height = DesktopQtLength.toPixels(toQtComponent(), getter.apply(node));
        item.setSizeHint(0, new QSize(Math.max(0, item.sizeHint(0).width()), height));
    }

    @Override
    public void addStyle(TreeStyle style) {
        switch (style) {
            case TRANSPARENT_BACKGROUND -> whenBound(widget -> {
                widget.setAutoFillBackground(false);
                ((QTreeWidget) widget).viewport().setAutoFillBackground(false);
            });
            default -> applyFontSize(style.getFontPointDelta());
        }
    }

    private void applyFontSize(int pointDelta) {
        whenBound(widget -> widget.setFont(DesktopQtFontSize.withPointDelta(widget.font(), pointDelta)));
    }

    @Override
    public void setTransferHandler(@Nullable TransferHandler<TreeNode<E>> handler) {
        myTransferHandler = handler;
        myDragAndDrop.setHandler(handler, myComponent);
    }

    @Override
    public @Nullable TransferHandler<TreeNode<E>> getTransferHandler() {
        return myTransferHandler;
    }

    @Override
    public List<TreeNode<E>> getTransferItems() {
        QTreeWidget tree = myComponent;
        if (tree == null || tree.isDisposed()) {
            return List.of();
        }
        return new ArrayList<>(selectedNodes(tree));
    }

    @Override
    public @Nullable DesktopQtDrop<TreeNode<E>> resolveDrop(QPoint position) {
        QTreeWidget tree = myComponent;
        if (tree == null || tree.isDisposed()) {
            return null;
        }

        QTreeWidgetItem item = tree.itemAt(position);
        if (item == null) {
            QTreeWidgetItem root = tree.invisibleRootItem();
            int count = root.childCount();
            QTreeWidgetItem last = count == 0 ? null : root.child(count - 1);
            TreeNodeImpl<E> node = last == null ? null : myNodes.get(last);
            if (last == null || node == null) {
                return null;
            }
            return new DesktopQtDrop<>(node, DragAndDropTransferHandler.DropPosition.BELOW, lineBelow(tree, last));
        }

        TreeNodeImpl<E> node = myNodes.get(item);
        if (node == null) {
            return null;
        }

        QRect rect = tree.visualItemRect(item);
        DragAndDropTransferHandler.DropPosition dropPosition = DesktopQtDragAndDropSupport.positionOf(rect, position);
        return switch (dropPosition) {
            case ABOVE -> new DesktopQtDrop<>(node, dropPosition, DesktopQtDragAndDropSupport.lineAbove(rect));
            case BELOW -> new DesktopQtDrop<>(node, dropPosition, lineBelow(tree, item));
            case INTO -> new DesktopQtDrop<>(node, dropPosition, rect);
        };
    }

    private static QRect lineBelow(QTreeWidget tree, QTreeWidgetItem item) {
        QTreeWidgetItem last = item;
        while (last.isExpanded() && last.childCount() > 0) {
            last = last.child(last.childCount() - 1);
        }

        QRect rect = tree.visualItemRect(item);
        return new QRect(rect.left(), tree.visualItemRect(last).bottom(), rect.width(), 0);
    }

    @Override
    public void dragHovered(@Nullable QPoint position) {
        QTreeWidget tree = myComponent;
        QTreeWidgetItem item = tree == null || tree.isDisposed() || position == null ? null : tree.itemAt(position);
        if (item == myDropExpandItem) {
            return;
        }

        myDropExpandItem = item;
        myDropExpandGeneration++;

        TreeNodeImpl<E> node = item == null ? null : myNodes.get(item);
        if (tree != null && item != null && node != null && !node.isLeaf() && !item.isExpanded()) {
            expandOnDropHover(tree, item, myDropExpandGeneration);
        }
    }

    private void expandOnDropHover(QTreeWidget tree, QTreeWidgetItem item, int generation) {
        QTimer.singleShot(ourDropExpandDelay, tree, () -> {
            if (generation == myDropExpandGeneration && !tree.isDisposed() && !item.isDisposed() && !item.isExpanded()) {
                item.setExpanded(true);
            }
        });
    }

    @Override
    public void setSpeedSearchConverter(@Nullable Function<TreeNode<E>, String> converter) {
        mySpeedSearchConverter = converter;
    }

    @Override
    public @Nullable String getSpeedSearchText() {
        return null;
    }
}
