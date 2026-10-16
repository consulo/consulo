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

import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.disposer.Disposer;
import consulo.ui.CheckableTree;
import consulo.ui.TreeNode;
import consulo.ui.ex.awt.speedSearch.SpeedSearchSupply;
import consulo.ui.impl.tree.CheckBoxTreeState;
import consulo.util.lang.ThreeState;
import org.jspecify.annotations.Nullable;

import javax.swing.SwingUtilities;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
final class DesktopTreeCheckBoxes<E> {
    private final DesktopTreeBase<E, ?> myOwner;
    private final CheckBoxTreeState<E> myState;
    private final DesktopCheckBoxTreeCellRenderer myRenderer = new DesktopCheckBoxTreeCellRenderer(this);

    DesktopTreeCheckBoxes(DesktopTreeBase<E, ?> owner, CheckableTree<E> tree) {
        myOwner = owner;
        myState = new CheckBoxTreeState<>(tree, owner.getStructure().getRootNode(), DesktopTreeCheckBoxes::parentOf, this::repaint);
    }

    @SuppressWarnings("unchecked")
    private static <E> @Nullable TreeNode<E> parentOf(TreeNode<E> node) {
        return node instanceof DesktopTreeNodeImpl<?> impl ? (TreeNode<E>) impl.getParent() : null;
    }

    CheckBoxTreeState<E> getState() {
        return myState;
    }

    TreeCellRenderer getRenderer() {
        return myRenderer;
    }

    DesktopAWTTree createTree(javax.swing.tree.TreeModel model) {
        return new DesktopAWTCheckBoxTree(model, myOwner, this::toggleAt, this::isOverCheckBox);
    }

    void installToggleKey(DesktopAWTTree tree, java.awt.Component component) {
        component.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_SPACE || e.getModifiersEx() != 0 || SpeedSearchSupply.getSupply(tree) != null) {
                    return;
                }

                TreeNode<E> lead = myOwner.getSelectedNode();
                if (lead != null && myState.toggle(lead, myOwner.getSelectedNodes(), DesktopAWTInputDetails.convert(component, e))) {
                    e.consume();
                }
            }
        });
    }

    void childrenBuilt() {
        SwingUtilities.invokeLater(() -> {
            if (!Disposer.isDisposed(myOwner.destroyHook())) {
                myState.structureChanged();
            }
        });
    }

    private int checkBoxRowAt(int x, int y) {
        DesktopAWTTree tree = myOwner.getTree();
        int row = tree.getRowForLocation(x, y);
        TreePath path = row < 0 ? null : tree.getPathForRow(row);
        TreeNode<E> node = path == null ? null : myOwner.nodeOf(path);
        if (path == null || node == null || myState.getShownState(node) == null || !myState.isCheckEnabled(node.getValue())) {
            return -1;
        }

        Rectangle rowBounds = tree.getRowBounds(row);
        myRenderer.getTreeCellRendererComponent(
            tree,
            path.getLastPathComponent(),
            tree.isRowSelected(row),
            tree.isExpanded(row),
            tree.getModel().isLeaf(path.getLastPathComponent()),
            row,
            false
        );
        return myRenderer.getCheckBoxBounds(rowBounds).contains(x, y) ? row : -1;
    }

    private boolean isOverCheckBox(Point point) {
        return checkBoxRowAt(point.x, point.y) >= 0;
    }

    private boolean toggleAt(MouseEvent e) {
        int row = checkBoxRowAt(e.getX(), e.getY());
        if (row < 0) {
            return false;
        }

        DesktopAWTTree tree = myOwner.getTree();
        TreePath path = tree.getPathForRow(row);
        TreeNode<E> node = path == null ? null : myOwner.nodeOf(path);
        if (node == null) {
            return false;
        }

        tree.requestFocusInWindow();
        if (myState.toggle(node, List.of(), DesktopAWTInputDetails.convert(tree, e))) {
            tree.setSelectionRow(row);
        }
        return true;
    }

    @Nullable ThreeState checkStateOf(Object value) {
        TreeNode<E> node = myOwner.nodeOfComponent(value);
        return node == null ? null : myState.getShownState(node);
    }

    boolean isCheckEnabledOf(Object value) {
        TreeNode<E> node = myOwner.nodeOfComponent(value);
        return node != null && myState.isCheckEnabled(node.getValue());
    }

    private void repaint() {
        if (myOwner.isInitialized()) {
            myOwner.toAWTComponent().repaint();
        }
    }
}
