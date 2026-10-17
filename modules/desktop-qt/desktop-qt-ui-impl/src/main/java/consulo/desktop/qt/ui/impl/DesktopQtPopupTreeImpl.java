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

import consulo.ui.TreeExecutor;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.internal.PopupTree;
import consulo.ui.impl.tree.TreeNodeImpl;
import io.qt.core.Qt;
import io.qt.widgets.QFrame;
import io.qt.widgets.QTreeWidget;
import io.qt.widgets.QTreeWidgetItem;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
class DesktopQtPopupTreeImpl<E> extends DesktopQtTreeImpl<E> implements PopupTree<E> {
    private boolean mySelectOnHover;
    private Predicate<@Nullable E> mySeparator = value -> false;
    private @Nullable BiConsumer<TreeNode<E>, InputDetails> myActivateListener;

    DesktopQtPopupTreeImpl(TreeModel<E> model) {
        super(null, model, TreeExecutor.uiThread());
    }

    @Override
    protected void initialize(QTreeWidget tree) {
        super.initialize(tree);

        tree.setMouseTracking(true);

        tree.itemEntered.connect((item, column) -> {
            TreeNodeImpl<E> node = nodeOf(item);
            if (mySelectOnHover && node != null && !mySeparator.test(node.getValue())) {
                tree.setCurrentItem(item);
            }
        });

        tree.itemClicked.connect((item, column) -> {
            TreeNodeImpl<E> node = nodeOf(item);
            BiConsumer<TreeNode<E>, InputDetails> listener = myActivateListener;
            if (node != null && listener != null && !mySeparator.test(node.getValue())) {
                listener.accept(node, DesktopQtCurrentInput.current(tree));
            }
        });
    }

    @Override
    protected void render(TreeNodeImpl<E> node, QTreeWidgetItem item) {
        super.render(node, item);

        if (!mySeparator.test(node.getValue())) {
            return;
        }

        item.setFlags(Qt.ItemFlag.NoItemFlags);

        QTreeWidget tree = myComponent;
        if (tree != null && !tree.isDisposed() && tree.itemWidget(item, 0) == null) {
            QFrame line = new QFrame();
            line.setFrameShape(QFrame.Shape.HLine);
            line.setFrameShadow(QFrame.Shadow.Sunken);
            tree.setItemWidget(item, 0, line);
        }
    }

    @Override
    public void setSelectOnHover(boolean selectOnHover) {
        mySelectOnHover = selectOnHover;
    }

    @Override
    public void setSeparator(Predicate<E> separator) {
        mySeparator = separator::test;
    }

    @Override
    public void setActivateListener(BiConsumer<TreeNode<E>, InputDetails> listener) {
        myActivateListener = listener;
    }

    @Override
    public void selectNext() {
        myController.selectAdjacent(true, mySeparator);
    }

    @Override
    public void selectPrevious() {
        myController.selectAdjacent(false, mySeparator);
    }

    @Override
    public boolean isExpanded(TreeNode<E> node) {
        return myController.isExpanded(node);
    }

    @Override
    public void setExpanded(TreeNode<E> node, boolean expanded) {
        if (expanded) {
            expand(node);
        }
        else {
            myController.collapse(node);
        }
    }
}
