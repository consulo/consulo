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

import consulo.ui.TreeExecutor;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.TreeNodeEditor;
import org.jspecify.annotations.Nullable;

import javax.swing.JTree;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.MouseEvent;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2021-07-14
 */
public class DesktopTreeImpl<E> extends DesktopTreeBase<E, DesktopAWTTree> {
    private @Nullable TreeNodeEditor<E> myNodeEditor;

    public DesktopTreeImpl(@Nullable E rootValue, TreeModel<E> model, TreeExecutor executor) {
        super(rootValue, model, executor);
    }

    @Override
    protected DesktopAWTTree createComponent() {
        DesktopAWTTree tree = createTree(createAsyncTreeModel());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        installTree(tree);
        return tree;
    }

    @Override
    protected DesktopAWTTree getTree() {
        return toAWTComponent();
    }

    int editorLeftInset(JTree tree, int row) {
        return 0;
    }

    void editorPrefixPressed(TreePath path, MouseEvent event) {
    }

    @Override
    public void setNodeEditor(@Nullable TreeNodeEditor<E> editor) {
        DesktopAWTTree tree = getTree();
        if (tree.isEditing()) {
            tree.cancelEditing();
        }

        myNodeEditor = editor;
        tree.setInvokesStopCellEditing(true);
        tree.setCellEditor(editor == null ? null : new DesktopTreeNodeCellEditor<>(this, editor));
        tree.setEditable(editor != null);
        if (editor != null) {
            tree.setRowHeight(0);
        }
    }

    @Override
    public boolean startEditing(TreeNode<E> node) {
        DesktopAWTTree tree = getTree();
        TreeNodeEditor<E> editor = myNodeEditor;
        E value = node.getValue();
        if (editor == null || value == null || !editor.isEditable(value)) {
            return false;
        }

        for (int row = 0; row < tree.getRowCount(); row++) {
            TreePath path = tree.getPathForRow(row);
            TreeNode<E> candidate = nodeOf(path);
            if (candidate == node || candidate != null && Objects.equals(candidate.getValue(), value)) {
                tree.setSelectionPath(path);
                tree.startEditingAtPath(path);
                return tree.isEditing();
            }
        }
        return false;
    }

    @Override
    public boolean stopEditing() {
        DesktopAWTTree tree = getTree();
        return !tree.isEditing() || tree.stopEditing();
    }

    @Override
    public void cancelEditing() {
        DesktopAWTTree tree = getTree();
        if (tree.isEditing()) {
            tree.cancelEditing();
        }
    }

    @Override
    public boolean isEditing() {
        return getTree().isEditing();
    }
}
