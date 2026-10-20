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
import consulo.ui.TreeExecutor;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.internal.PopupTree;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * The tree of a popup: the row under the pointer is selected, a click activates it and separator rows are never
 * selected. It scrolls itself, the way the popup trees of the other frontends do, since a popup puts it straight
 * into its content.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
class DesktopPopupTreeImpl<E> extends DesktopTreeBase<E, JScrollPane> implements PopupTree<E> {
    private boolean mySelectOnHover;
    private Predicate<@Nullable E> mySeparator = value -> false;
    private @Nullable BiConsumer<TreeNode<E>, InputDetails> myActivateListener;

    DesktopPopupTreeImpl(TreeModel<E> model) {
        super(null, model, TreeExecutor.uiThread());
    }

    @Override
    protected JScrollPane createComponent() {
        DesktopAWTTree tree = createTree(createAsyncTreeModel());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        installTree(tree);
        return ScrollPaneFactory.createScrollPane(tree, true);
    }

    @Override
    protected DesktopAWTTree getTree() {
        return (DesktopAWTTree) toAWTComponent().getViewport().getView();
    }

    @Override
    protected List<java.awt.Component> getInputComponents(JScrollPane component) {
        return List.of(component.getViewport().getView());
    }

    @Override
    public void focus() {
        getTree().requestFocus();
    }

    @Override
    public boolean isFocusable() {
        return getTree().isFocusable();
    }

    @Override
    public void setFocusable(boolean focusable) {
        getTree().setFocusable(focusable);
    }

    @RequiredUIAccess
    @Override
    public void setEnabled(boolean value) {
        super.setEnabled(value);

        getTree().setEnabled(value);
    }

    @Override
    void installTree(DesktopAWTTree tree) {
        super.installTree(tree);

        InputMap inputMap = tree.getInputMap(JComponent.WHEN_FOCUSED);
        inputMap.put(KeyStroke.getKeyStroke("UP"), "none");
        inputMap.put(KeyStroke.getKeyStroke("DOWN"), "none");
        inputMap.put(KeyStroke.getKeyStroke("KP_UP"), "none");
        inputMap.put(KeyStroke.getKeyStroke("KP_DOWN"), "none");

        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (!mySelectOnHover) {
                    return;
                }
                int row = rowAt(tree, e);
                if (row >= 0 && !isSeparator(tree.getPathForRow(row)) && !tree.isRowSelected(row)) {
                    tree.setSelectionRow(row);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e) || e.getClickCount() != 1) {
                    return;
                }
                int row = rowAt(tree, e);
                if (row < 0) {
                    return;
                }
                Rectangle bounds = tree.getRowBounds(row);
                if (bounds != null && e.getX() < bounds.x) {
                    return;
                }
                TreePath path = tree.getPathForRow(row);
                TreeNode<E> node = nodeOf(path);
                BiConsumer<TreeNode<E>, InputDetails> listener = myActivateListener;
                if (node != null && listener != null && !isSeparator(path)) {
                    listener.accept(node, DesktopAWTInputDetails.convert(toAWTComponent(), e));
                }
            }
        };
        tree.addMouseListener(adapter);
        tree.addMouseMotionListener(adapter);
    }

    @Override
    TreeCellRenderer createCellRenderer() {
        TreeCellRenderer renderer = super.createCellRenderer();
        JPanel separator = new JPanel(new BorderLayout());
        separator.setOpaque(false);
        separator.setBorder(JBUI.Borders.empty(3, 0));
        separator.add(new JSeparator(), BorderLayout.CENTER);
        return (tree, value, selected, expanded, leaf, row, hasFocus) -> {
            TreeNode<E> node = nodeOfComponent(value);
            if (node != null && mySeparator.test(node.getValue())) {
                separator.setPreferredSize(new Dimension(Math.max(tree.getWidth(), 1), JBUI.scale(9)));
                return separator;
            }
            return renderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        };
    }

    private static int rowAt(JTree tree, MouseEvent e) {
        int row = tree.getClosestRowForLocation(e.getX(), e.getY());
        Rectangle bounds = row < 0 ? null : tree.getRowBounds(row);
        return bounds != null && e.getY() >= bounds.y && e.getY() < bounds.y + bounds.height ? row : -1;
    }

    private boolean isSeparator(@Nullable TreePath path) {
        TreeNode<E> node = path == null ? null : nodeOf(path);
        return node == null || mySeparator.test(node.getValue());
    }

    private @Nullable TreePath pathOf(TreeNode<E> node) {
        DesktopAWTTree tree = getTree();
        for (int row = 0; row < tree.getRowCount(); row++) {
            TreePath path = tree.getPathForRow(row);
            if (nodeOf(path) == node) {
                return path;
            }
        }
        return null;
    }

    private void selectAdjacent(boolean forward) {
        DesktopAWTTree tree = getTree();
        int count = tree.getRowCount();
        int[] selected = tree.getSelectionRows();
        int current = selected == null || selected.length == 0 ? (forward ? -1 : count) : selected[0];
        for (int row = current + (forward ? 1 : -1); row >= 0 && row < count; row += forward ? 1 : -1) {
            if (!isSeparator(tree.getPathForRow(row))) {
                tree.setSelectionRow(row);
                tree.scrollRowToVisible(row);
                return;
            }
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
        selectAdjacent(true);
    }

    @Override
    public void selectPrevious() {
        selectAdjacent(false);
    }

    @Override
    public boolean isExpanded(TreeNode<E> node) {
        TreePath path = pathOf(node);
        return path != null && getTree().isExpanded(path);
    }

    @Override
    public void setExpanded(TreeNode<E> node, boolean expanded) {
        if (expanded) {
            expand(node);
            return;
        }
        TreePath path = pathOf(node);
        if (path != null) {
            getTree().collapsePath(path);
        }
    }
}
