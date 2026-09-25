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

import consulo.desktop.qt.ui.impl.image.DesktopQtIconOwner;
import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
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
import consulo.ui.image.Image;
import consulo.ui.impl.tree.TreeController;
import consulo.ui.impl.tree.TreeNodeImpl;
import consulo.ui.impl.tree.TreeWidget;
import io.qt.core.QPoint;
import io.qt.core.QRect;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.gui.QFont;
import io.qt.gui.QMouseEvent;
import io.qt.widgets.QAbstractItemView;
import io.qt.widgets.QApplication;
import io.qt.widgets.QTreeWidget;
import io.qt.widgets.QTreeWidgetItem;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtTreeImpl<E> extends QtComponentDelegate<QTreeWidget> implements Tree<E>, PopupOwner, DesktopQtIconOwner {
    private @Nullable TransferHandler<TreeNode<E>> myTransferHandler;
    private @Nullable Function<TreeNode<E>, String> mySpeedSearchConverter;
    private @Nullable Function<TreeNode<E>, Length> myItemHeightGetter;

    private final Disposable myDestroyHook = Disposable.newDisposable("Tree");

    private final TreeController<E> myController;

    private final Map<QTreeWidgetItem, TreeNodeImpl<E>> myNodes = new HashMap<>();
    private final Map<TreeNodeImpl<E>, QTreeWidgetItem> myItems = new HashMap<>();
    private final Map<TreeNodeImpl<E>, QTreeWidgetItem> myLoadingItems = new HashMap<>();

    private boolean mySyncing;

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
    private static class QtTree extends QTreeWidget {
        private QtTree(QWidget parent) {
            super(parent);
        }

        @Override
        protected void mouseDoubleClickEvent(QMouseEvent event) {
            if (event.button() != Qt.MouseButton.LeftButton) {
                // the second click of a pair arrives as a double click rather than as a press, so it has to be
                // handled as the press it stands for - dropping it would swallow every other right click
                mousePressEvent(event);
                return;
            }

            super.mouseDoubleClickEvent(event);
        }
    }

    @Override
    protected QTreeWidget createQt(QWidget parent) {
        QTreeWidget tree = new QtTree(parent);
        tree.setColumnCount(1);
        tree.setHeaderHidden(true);
        tree.setSelectionMode(QAbstractItemView.SelectionMode.SingleSelection);

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

        tree.itemSelectionChanged.connect(() -> {
            if (!mySyncing) {
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

                restoreSelection();
            });
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
    }

    private void render(TreeNodeImpl<E> node, QTreeWidgetItem item) {
        if (node.getPresentation() instanceof DesktopQtTextItemPresentation presentation) {
            item.setText(0, presentation.toString());

            Image image = presentation.getImage();
            if (image != null) {
                item.setIcon(0, DesktopQtImage.toQIcon(image));
            }
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

    private void sync(Runnable action) {
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
        item.setSizeHint(0, new QSize(Math.max(0, item.sizeHint(0).width()), DesktopQtLength.toPixels(toQtComponent(), getter.apply(node))));
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

    @Override
    public void setSpeedSearchConverter(@Nullable Function<TreeNode<E>, String> converter) {
        mySpeedSearchConverter = converter;
    }

    @Override
    public @Nullable String getSpeedSearchText() {
        return null;
    }
}
