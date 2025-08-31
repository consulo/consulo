/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.errorTreeView;

import consulo.ide.impl.idea.ui.CustomizeColoredTreeCellRenderer;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.tree.LoadingNode;
import consulo.ui.ex.awt.tree.Tree;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.EventObject;

/**
 * @author Vladislav.Soroka
 * @since 2014-03-25
 */
public class NewErrorTreeEditor extends AbstractCellEditor implements TreeCellEditor, MouseMotionListener {

    public static void install(Tree tree) {
        NewErrorTreeEditor treeEditor = new NewErrorTreeEditor(tree);
        tree.setCellEditor(treeEditor);
        tree.addMouseMotionListener(treeEditor);
        tree.setEditable(true);
    }

    private final MyWrapperEditor myWrapperEditor;
    private final CallingBackColoredTreeCellRenderer myColoredTreeCellRenderer;
    private final CellEditorDelegate myRightCellRenderer;
    private final JTree myTree;

    private NewErrorTreeEditor(JTree tree) {
        myTree = tree;
        myRightCellRenderer = new CellEditorDelegate();
        myColoredTreeCellRenderer = new CallingBackColoredTreeCellRenderer();
        myWrapperEditor = new MyWrapperEditor(myColoredTreeCellRenderer, myRightCellRenderer);
    }

    @Override
    public boolean isCellEditable(EventObject e) {
        Object node;
        if (e instanceof MouseEvent) {
            Point point = ((MouseEvent) e).getPoint();
            TreePath location = myTree.getClosestPathForLocation(point.x, point.y);
            node = location.getLastPathComponent();
        }
        else {
            node = myTree.getLastSelectedPathComponent();
        }
        ErrorTreeElement element = getElement(node);
        return element instanceof EditableMessageElement;
    }

    @Override
    public Component getTreeCellEditorComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row) {
        ErrorTreeElement element = getElement(value);
        if (element instanceof EditableMessageElement) {
            EditableMessageElement editableMessageElement = (EditableMessageElement) element;
            CustomizeColoredTreeCellRenderer leftSelfRenderer = editableMessageElement.getLeftSelfRenderer();
            TreeCellEditor rightSelfEditor = editableMessageElement.getRightSelfEditor();
            myColoredTreeCellRenderer.setCurrentCallback(leftSelfRenderer);
            myRightCellRenderer.setCurrentCallback(rightSelfEditor);
            return myWrapperEditor.getTreeCellEditorComponent(tree, value, selected, expanded, leaf, row);
        }
        return myTree.getCellRenderer().getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, true);
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        JTree tree = (JTree) e.getSource();
        int selRow = tree.getRowForLocation(e.getX(), e.getY());
        if (selRow != -1) {
            TreePath treePath = tree.getPathForRow(selRow);
            if (treePath != null && treePath != tree.getEditingPath()) {
                ErrorTreeElement element = getElement(treePath.getLastPathComponent());
                if (element instanceof EditableMessageElement && ((EditableMessageElement) element).startEditingOnMouseMove()) {
                    if (!tree.isRowSelected(selRow)) {
                        tree.setSelectionRow(selRow);
                    }
                    tree.startEditingAtPath(treePath);
                }
            }
        }
    }

    @Nullable
    private static ErrorTreeElement getElement(@Nullable Object value) {
        if (!(value instanceof DefaultMutableTreeNode)) {
            return null;
        }
        Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
        if (!(userObject instanceof ErrorTreeNodeDescriptor)) {
            return null;
        }
        return ((ErrorTreeNodeDescriptor) userObject).getElement();
    }

    private static class MyWrapperEditor extends AbstractCellEditor implements TreeCellEditor {
        private final TreeCellRenderer myLeft;
        private final TreeCellEditor myRight;
        private final JPanel myPanel;

        public TreeCellRenderer getLeft() {
            return myLeft;
        }

        public TreeCellEditor getRight() {
            return myRight;
        }

        public MyWrapperEditor(TreeCellRenderer left, TreeCellEditor right) {
            myLeft = left;
            myRight = right;
            myPanel = new JPanel(new BorderLayout());
        }

        @Override
        public Component getTreeCellEditorComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row) {
            myPanel.removeAll();
            myPanel.add(myLeft.getTreeCellRendererComponent(tree, value, false, expanded, leaf, row, true), BorderLayout.WEST);
            myPanel.add(myRight.getTreeCellEditorComponent(tree, value, selected, expanded, leaf, row), BorderLayout.EAST);

            if (selected) {
                myPanel.setBackground(UIUtil.getTreeSelectionBackground());
            }

            if (value instanceof LoadingNode) {
                myPanel.setForeground(JBColor.GRAY);
            }
            else {
                myPanel.setForeground(tree.getForeground());
            }

            myPanel.setOpaque(false);
            return myPanel;
        }

        @Override
        public Object getCellEditorValue() {
            return null;
        }
    }


    private static class CellEditorDelegate extends AbstractCellEditor implements TreeCellEditor {
        private TreeCellEditor myCurrentCallback;

        public Component getTreeCellEditorComponent(JTree tree,
                                                    Object value,
                                                    boolean selected,
                                                    boolean expanded,
                                                    boolean leaf,
                                                    int row) {
            return myCurrentCallback.getTreeCellEditorComponent(tree, value, selected, expanded, leaf, row);
        }

        public void setCurrentCallback(TreeCellEditor currentCallback) {
            myCurrentCallback = currentCallback;
        }

        @Override
        public Object getCellEditorValue() {
            return null;
        }
    }
}
