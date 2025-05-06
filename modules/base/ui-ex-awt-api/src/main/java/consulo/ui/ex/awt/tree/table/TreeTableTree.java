/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ui.ex.awt.tree.table;

import consulo.ui.ex.awt.JBCurrentTheme;
import consulo.ui.ex.awt.tree.Tree;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * author: lesya
 */
public class TreeTableTree extends Tree {
    public static final String TREE_TABLE_TREE_KEY = "TreeTableTree";

    private final TreeTable myTreeTable;
    private int myVisibleRow;

    public TreeTableTree(TreeModel model, TreeTable treeTable) {
        super(model);
        myTreeTable = treeTable;
        putClientProperty(TREE_TABLE_TREE_KEY, treeTable);
        putClientProperty("JTree.paintSelection", false);
    }

    public TreeTable getTreeTable() {
        return myTreeTable;
    }

    @Override
    public void setRowHeight(int rowHeight) {
        if (rowHeight > 0) {
            super.setRowHeight(rowHeight);
            if (myTreeTable != null && myTreeTable.getRowHeight() != rowHeight) {
                myTreeTable.setRowHeight(getRowHeight());
            }
        }
    }

    @Override
    public void setBounds(int x, int y, int w, int h) {
        super.setBounds(x, 0, w, myTreeTable.getHeight());
    }

    @Override
    public void paint(Graphics g) {
        putClientProperty("JTree.lineStyle", "None");
        Graphics g1 = g.create();
        g1.translate(0, -myVisibleRow * getRowHeight());
        super.paint(g1);
        g1.dispose();
    }

    public void setVisibleRow(int row) {
        myVisibleRow = row;
        final Rectangle rowBounds = getRowBounds(myVisibleRow);
        final int indent = rowBounds.x - getVisibleRect().x - getTreeColumnOffsetX();
        setPreferredSize(new Dimension(getRowBounds(myVisibleRow).width + indent, getPreferredSize().height));
    }

    public void _processKeyEvent(KeyEvent e) {
        super.processKeyEvent(e);
    }

    @Nullable
    @Override
    public Rectangle getPathBounds(TreePath path) {
        Rectangle bounds = super.getPathBounds(path);
        if (bounds == null) {
            return null;
        }
        bounds.x += getTreeColumnOffsetX();
        return bounds;
    }

    protected int getTreeColumnOffsetX() {
        int offsetX = 0;
        for (int i = 0; i < myTreeTable.getColumnCount(); i++) {
            if (myTreeTable.isTreeColumn(i)) {
                break;
            }
            offsetX += myTreeTable.getColumnModel().getColumn(i).getWidth();
        }
        return offsetX;
    }
}
