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

import consulo.ui.CheckableTree;
import consulo.ui.TreeNode;
import consulo.ui.impl.tree.CheckBoxTreeState;
import consulo.ui.impl.tree.TreeNodeImpl;
import consulo.util.lang.ThreeState;
import io.qt.core.QModelIndex;
import io.qt.core.QPoint;
import io.qt.core.Qt;
import io.qt.gui.QKeyEvent;
import io.qt.gui.QMouseEvent;
import io.qt.widgets.QAbstractItemDelegate;
import io.qt.widgets.QTreeWidget;
import io.qt.widgets.QTreeWidgetItem;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
final class DesktopQtTreeCheckBoxes<E> {
    private final DesktopQtTreeImpl<E> myOwner;
    private final CheckBoxTreeState<E> myState;

    private @Nullable DesktopQtCheckBoxTreeItemDelegate myDelegate;

    DesktopQtTreeCheckBoxes(DesktopQtTreeImpl<E> owner, CheckableTree<E> tree) {
        myOwner = owner;
        myState = CheckBoxTreeState.attach(tree, owner.myController, this::applyChecks);
    }

    CheckBoxTreeState<E> getState() {
        return myState;
    }

    void installDelegate(QTreeWidget tree) {
        QAbstractItemDelegate previous = tree.itemDelegate();
        DesktopQtCheckBoxTreeItemDelegate delegate = new DesktopQtCheckBoxTreeItemDelegate(tree);
        tree.setItemDelegate(delegate);
        myDelegate = delegate;
        if (previous != null) {
            previous.dispose();
        }
    }

    void connect(QTreeWidget tree) {
        tree.itemChanged.connect((item, column) -> {
            if (!myOwner.isSyncing() && column == 0) {
                onItemChanged(tree, item);
            }
        });
    }

    private void onItemChanged(QTreeWidget tree, QTreeWidgetItem item) {
        TreeNodeImpl<E> node = myOwner.nodeOf(item);
        ThreeState shown = node == null ? null : myState.getShownState(node);
        if (node == null || shown == null || item.checkState(0) == toQt(shown)) {
            return;
        }

        List<TreeNode<E>> selection = item.isSelected() ? myOwner.getSelectedNodes() : List.of();
        if (!myState.toggle(node, selection, DesktopQtCurrentInput.current(tree))) {
            applyCheck(node, item);
        }
    }

    boolean isCheckBoxPress(QTreeWidget tree, QMouseEvent event) {
        DesktopQtCheckBoxTreeItemDelegate delegate = myDelegate;
        QPoint position = event.position().toPoint();
        QModelIndex index = tree.indexAt(position);
        if (delegate == null || !index.isValid() || index.column() != 0) {
            return false;
        }
        return delegate.checkBoxRect(tree, tree.visualRect(index), index).contains(position);
    }

    void toggle(QTreeWidget tree, TreeNodeImpl<E> node) {
        if (myState.getShownState(node) != null && myState.isCheckEnabled(node.getValue())) {
            myState.toggle(node, List.of(), DesktopQtCurrentInput.current(tree));
        }
    }

    boolean toggleByKey(QTreeWidget tree, QKeyEvent event) {
        if (event.key() != Qt.Key.Key_Space.value() || event.modifiers().value() != 0) {
            return false;
        }

        TreeNode<E> lead = myOwner.getSelectedNode();
        return lead != null && myState.toggle(lead, myOwner.getSelectedNodes(), DesktopQtCurrentInput.current(tree));
    }

    void applyChecks() {
        myOwner.forEachItem(this::applyCheck);
    }

    void applyCheck(TreeNodeImpl<E> node, QTreeWidgetItem item) {
        ThreeState shown = myState.getShownState(node);
        boolean enabled = shown != null && myState.isCheckEnabled(node.getValue());

        myOwner.sync(() -> {
            Qt.ItemFlags flags = item.flags();
            if (flags.testFlag(Qt.ItemFlag.ItemIsUserCheckable) != enabled) {
                flags.setFlag(Qt.ItemFlag.ItemIsUserCheckable, enabled);
                item.setFlags(flags);
            }

            if (shown == null) {
                if (item.data(0, Qt.ItemDataRole.CheckStateRole) != null) {
                    item.setData(0, Qt.ItemDataRole.CheckStateRole, null);
                }
            }
            else if (item.data(0, Qt.ItemDataRole.CheckStateRole) == null || item.checkState(0) != toQt(shown)) {
                item.setCheckState(0, toQt(shown));
            }
        });
    }

    private static Qt.CheckState toQt(ThreeState state) {
        return switch (state) {
            case YES -> Qt.CheckState.Checked;
            case NO -> Qt.CheckState.Unchecked;
            case UNSURE -> Qt.CheckState.PartiallyChecked;
        };
    }
}
