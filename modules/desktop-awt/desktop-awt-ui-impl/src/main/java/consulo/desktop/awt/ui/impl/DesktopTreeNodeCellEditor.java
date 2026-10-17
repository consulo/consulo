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

import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.ui.Label;
import consulo.ui.TreeNode;
import consulo.ui.TreeNodeEditor;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.Layout;
import org.jspecify.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
final class DesktopTreeNodeCellEditor<E> extends AbstractCellEditor implements TreeCellEditor {
    private static final String STOP = "uiTreeStopEditing";
    private static final String CANCEL = "uiTreeCancelEditing";

    private final DesktopTreeImpl<E> myOwner;
    private final TreeNodeEditor<E> myEditor;

    private @Nullable E myValue;
    private consulo.ui.@Nullable Component myComponent;

    DesktopTreeNodeCellEditor(DesktopTreeImpl<E> owner, TreeNodeEditor<E> editor) {
        myOwner = owner;
        myEditor = editor;
    }

    @Override
    public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {
        TreeNode<E> node = myOwner.nodeOfComponent(value);
        E element = node == null ? null : node.getValue();
        if (element == null) {
            return new JPanel();
        }

        consulo.ui.Component component = myEditor.createEditor(element);
        myValue = element;
        myComponent = component;

        Rectangle rowBounds = tree.getRowBounds(row);
        int rowX = rowBounds == null ? 0 : rowBounds.x;
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                Insets insets = tree.getInsets();
                int available = tree.getVisibleRect().width - rowX - insets.right;
                return new Dimension(Math.max(size.width, available), size.height);
            }
        };
        panel.setOpaque(false);
        int prefix = myOwner.editorLeftInset(tree, row);
        if (prefix > 0) {
            panel.add(new DesktopTreeEditorRowPrefix(tree, prefix, myOwner::editorPrefixPressed), BorderLayout.WEST);
        }
        Component editorComponent = TargetAWT.to(component);
        clearBackground(editorComponent);
        panel.add(editorComponent, BorderLayout.CENTER);

        panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), STOP);
        panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), CANCEL);
        panel.getActionMap().put(STOP, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tree.stopEditing();
            }
        });
        panel.getActionMap().put(CANCEL, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tree.cancelEditing();
            }
        });

        SwingUtilities.invokeLater(() -> {
            if (myComponent != component) {
                return;
            }
            Component focusTarget = firstFocusable(panel);
            if (focusTarget != null) {
                focusTarget.requestFocusInWindow();
            }
            myEditor.editingStarted(element, component);
        });
        return panel;
    }

    private static void clearBackground(Component component) {
        consulo.ui.Component uiComponent = component instanceof FromSwingComponentWrapper wrapper ? wrapper.toUIComponent() : null;
        boolean container = uiComponent instanceof Layout || (uiComponent == null && component instanceof JPanel);
        if (container || uiComponent instanceof Label || (uiComponent == null && component instanceof JLabel)) {
            ((JComponent) component).setOpaque(false);
        }

        if (container) {
            for (Component child : ((Container) component).getComponents()) {
                clearBackground(child);
            }
        }
    }

    private static @Nullable Component firstFocusable(Container container) {
        for (Component child : container.getComponents()) {
            if (child.isFocusable() && child.isShowing() && child.isEnabled() && !(child instanceof JPanel)) {
                return child;
            }
            if (child instanceof Container nested) {
                Component found = firstFocusable(nested);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @Override
    public boolean isCellEditable(EventObject event) {
        JTree tree = myOwner.getTree();
        if (event instanceof MouseEvent mouseEvent) {
            if (mouseEvent.getClickCount() != 1 || !SwingUtilities.isLeftMouseButton(mouseEvent)) {
                return false;
            }
            TreePath path = tree.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());
            return path != null && tree.getSelectionCount() == 1 && tree.isPathSelected(path) && isEditable(path);
        }

        TreePath path = tree.getLeadSelectionPath();
        return path != null && isEditable(path);
    }

    private boolean isEditable(TreePath path) {
        TreeNode<E> node = myOwner.nodeOf(path);
        E value = node == null ? null : node.getValue();
        return value != null && myEditor.isEditable(value);
    }

    @Override
    public boolean stopCellEditing() {
        E value = myValue;
        consulo.ui.Component component = myComponent;
        if (value != null && component != null && !myEditor.stopEditing(value, component)) {
            return false;
        }

        clear();
        fireEditingCanceled();
        return true;
    }

    @Override
    public void cancelCellEditing() {
        E value = myValue;
        consulo.ui.Component component = myComponent;
        clear();
        if (value != null && component != null) {
            myEditor.cancelEditing(value, component);
        }
        fireEditingCanceled();
    }

    private void clear() {
        myValue = null;
        myComponent = null;
    }

    @Override
    public @Nullable Object getCellEditorValue() {
        return myValue;
    }
}
