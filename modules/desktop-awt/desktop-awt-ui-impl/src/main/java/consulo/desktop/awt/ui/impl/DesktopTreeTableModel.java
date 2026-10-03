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
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.TreeNode;
import consulo.ui.ex.awt.tree.AbstractTreeModel;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awt.tree.TreeVisitor;
import consulo.ui.ex.awt.tree.table.TreeTableModel;
import consulo.util.concurrent.Promise;
import org.jspecify.annotations.Nullable;

import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
class DesktopTreeTableModel<E> extends AbstractTreeModel implements TreeTableModel, TreeModelListener, TreeVisitor.Acceptor {
    private static final String EMPTY_HEADER = " ";

    private final DesktopAsyncTreeModel myAsyncModel;
    private final DesktopTreeTableImpl<E> myOwner;

    DesktopTreeTableModel(DesktopAsyncTreeModel asyncModel, DesktopTreeTableImpl<E> owner, Disposable parent) {
        myAsyncModel = asyncModel;
        myOwner = owner;
        myAsyncModel.addTreeModelListener(this);
        Disposer.register(parent, this);
    }

    @Override
    public void dispose() {
        myAsyncModel.removeTreeModelListener(this);
        super.dispose();
    }

    @Override
    public Object getRoot() {
        return myAsyncModel.getRoot();
    }

    @Override
    public Object getChild(Object parent, int index) {
        return myAsyncModel.getChild(parent, index);
    }

    @Override
    public int getChildCount(Object parent) {
        return myAsyncModel.getChildCount(parent);
    }

    @Override
    public boolean isLeaf(Object node) {
        return myAsyncModel.isLeaf(node);
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        return myAsyncModel.getIndexOfChild(parent, child);
    }

    @Override
    public void valueForPathChanged(TreePath path, Object value) {
        myAsyncModel.valueForPathChanged(path, value);
    }

    @Override
    public Promise<TreePath> accept(TreeVisitor visitor) {
        return myAsyncModel.accept(visitor);
    }

    @Override
    public int getColumnCount() {
        return 1 + myOwner.getColumnImpls().size();
    }

    @Override
    public String getColumnName(int column) {
        String name;
        if (column == 0) {
            name = myOwner.getTreeColumnHeader().get();
        }
        else {
            DesktopTableColumnImpl<E, ?> columnImpl = columnAt(column);
            name = columnImpl == null ? "" : columnImpl.getName().get();
        }
        return name.isEmpty() ? EMPTY_HEADER : name;
    }

    @Override
    public Class getColumnClass(int column) {
        return column == 0 ? TreeTableModel.class : Object.class;
    }

    @Override
    public @Nullable Object getValueAt(Object node, int column) {
        if (column == 0) {
            return node;
        }

        Object userObject = TreeUtil.getUserObject(node);
        return userObject instanceof DesktopTreeNodeDescriptor<?> descriptor ? descriptor.getColumnValue(column - 1) : null;
    }

    @Override
    public boolean isCellEditable(Object node, int column) {
        DesktopTableColumnImpl<E, ?> columnImpl = columnAt(column);
        E item = itemOf(node);
        return columnImpl != null && item != null && columnImpl.isCellEditable(item);
    }

    @Override
    public void setValueAt(@Nullable Object value, Object node, int column) {
        DesktopTableColumnImpl<E, ?> columnImpl = columnAt(column);
        TreeNode<E> treeNode = myOwner.nodeOfComponent(node);
        E item = treeNode == null ? null : treeNode.getValue();
        if (columnImpl == null || treeNode == null || item == null) {
            return;
        }

        commit(columnImpl, item, value);
        myOwner.refreshItem(treeNode);
    }

    @Override
    public void setTree(JTree tree) {
    }

    private @Nullable DesktopTableColumnImpl<E, ?> columnAt(int column) {
        List<DesktopTableColumnImpl<E, ?>> columns = myOwner.getColumnImpls();
        int index = column - 1;
        return index < 0 || index >= columns.size() ? null : columns.get(index);
    }

    private @Nullable E itemOf(Object node) {
        TreeNode<E> treeNode = myOwner.nodeOfComponent(node);
        return treeNode == null ? null : treeNode.getValue();
    }

    @SuppressWarnings("unchecked")
    private static <T, V> void commit(DesktopTableColumnImpl<T, V> column, T item, @Nullable Object value) {
        column.setValue(item, (V) value);
    }

    @Override
    public void treeNodesChanged(TreeModelEvent e) {
        treeNodesChanged(e.getTreePath(), e.getChildIndices(), e.getChildren());
    }

    @Override
    public void treeNodesInserted(TreeModelEvent e) {
        treeNodesInserted(e.getTreePath(), e.getChildIndices(), e.getChildren());
    }

    @Override
    public void treeNodesRemoved(TreeModelEvent e) {
        treeNodesRemoved(e.getTreePath(), e.getChildIndices(), e.getChildren());
    }

    @Override
    public void treeStructureChanged(TreeModelEvent e) {
        treeStructureChanged(e.getTreePath(), e.getChildIndices(), e.getChildren());
    }
}
