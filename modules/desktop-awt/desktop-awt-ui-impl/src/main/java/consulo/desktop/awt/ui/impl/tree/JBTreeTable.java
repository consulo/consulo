// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.desktop.awt.ui.impl.tree;

import consulo.platform.Platform;
import consulo.ui.ex.awt.Divider;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.OnePixelDivider;
import consulo.ui.ex.awt.OnePixelSplitter;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.SideBorder;
import consulo.ui.ex.awt.accessibility.AccessibleContextDelegate;
import consulo.ui.ex.awt.table.JBTable;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.tree.table.TreeTable;
import consulo.ui.ex.awt.tree.table.TreeTableModel;
import consulo.ui.ex.awt.tree.table.TreeTableModelAdapter;
import org.jspecify.annotations.Nullable;

import javax.accessibility.AccessibleContext;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Predicate;

/**
 * The tree-table view supports horizontal scrolling on a tree column only.
 *
 * Unlike {@link TreeTable} this view holds
 * a tree and a table separately and transfer changes from the one to the other.
 *
 * Use {@link TreeTableModel} as an internal model. Please, do NOT use separate models to the tree or the table. It is considered as error.
 * Tree column of the tree-table has to be first and should return <code>TreeTableModel.class</code>
 * from {@link TreeTableModel#getColumnClass(int)}.
 *
 * Cell renderers could be set separately or by calling {@link #setDefaultRenderer(Class, TableCellRenderer)}.
 */
public class JBTreeTable extends JComponent implements TreePathBackgroundSupplier {
    private static final String FOCUS_OWNER = "JComponent.focusOwner";

    private final Tree myTree;
    private final Table myTable;
    private final OnePixelSplitter split;

    private TreeTableModel myModel;

    private JTableHeader myTreeTableHeader;
    private float myColumnProportion = 0.1f;

    public JBTreeTable(TreeTableModel model) {
        this(model, null);
    }

    public JBTreeTable(TreeTableModel model, @Nullable Tree tree) {
        setLayout(new BorderLayout());

        myTree = tree == null ? new MyTree() : tree;
        myTable = new Table();
        myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        myTree.setRootVisible(false);
        myTree.setBorder(JBUI.Borders.empty());
        myTable.setShowGrid(false);
        myTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        myTable.setColumnSelectionAllowed(false);
        myTable.getTableHeader().setReorderingAllowed(false);

        split = new OnePixelSplitter() {
            @Override
            protected Divider createDivider() {
                return new OnePixelDivider(isVertical(), this) {
                    @Override
                    public void paint(Graphics g) {
                        Rectangle bounds = g.getClipBounds();
                        g.setColor(myTable.getShowVerticalLines() ? myTable.getGridColor() : myTable.getBackground());
                        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

                        JTableHeader header = myTreeTableHeader;
                        Rectangle rect = header.getHeaderRect(0);
                        g.setClip(rect.x, rect.y, 2, rect.height);
                        g.translate(-rect.width + 1, 0);
                        header.paint(g);
                    }
                };
            }
        };

        add(split);

        JScrollPane treePane = ScrollPaneFactory.createScrollPane(myTree, SideBorder.NONE);
        split.setFirstComponent(treePane);

        JScrollPane tablePane = ScrollPaneFactory.createScrollPane(myTable, SideBorder.NONE);
        split.setSecondComponent(tablePane);

        SelectionSupport selection = new SelectionSupport();

        treePane.setColumnHeaderView(myTreeTableHeader);
        treePane.getHorizontalScrollBar().addComponentListener(new ComponentAdapter() {

            {
                if (tablePane.isVisible()) {
                    tablePane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
                }
            }

            @Override
            public void componentShown(ComponentEvent e) {
                tablePane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                tablePane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            }
        });
        int scrollMode = !Platform.current().os().isMac() ? JViewport.SIMPLE_SCROLL_MODE : JViewport.BLIT_SCROLL_MODE;
        treePane.getViewport().setScrollMode(scrollMode);
        tablePane.setVerticalScrollBar(treePane.getVerticalScrollBar());
        tablePane.getViewport().setScrollMode(scrollMode);

        myTree.getSelectionModel().addTreeSelectionListener(selection);
        myTable.getSelectionModel().addListSelectionListener(selection);
        myTable.setRowMargin(0);
        myTable.addMouseListener(selection);
        myTree.addPropertyChangeListener(JTree.ROW_HEIGHT_PROPERTY, evt -> {
            int treeRowHeight = myTree.getRowHeight();
            if (treeRowHeight < 1 || treeRowHeight == myTable.getRowHeight()) {
                return;
            }
            myTable.setRowHeight(treeRowHeight);
        });

        myTree.setCellRenderer(new TreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree,
                                                          Object value,
                                                          boolean selected,
                                                          boolean expanded,
                                                          boolean leaf,
                                                          int row,
                                                          boolean hasFocus) {
                TreeColumnModel cm = (TreeColumnModel) myTreeTableHeader.getColumnModel();
                TableCellRenderer renderer = getDefaultRenderer(TreeTableModel.class);
                return renderer.getTableCellRendererComponent(myTable, value, selected, hasFocus, row, cm.treeColumnIndex);
            }
        });
        Predicate<JComponent> focusOwner = component -> myTree.hasFocus() || myTable.hasFocus();
        myTree.putClientProperty(FOCUS_OWNER, focusOwner);
        myTable.putClientProperty(FOCUS_OWNER, focusOwner);

        setModel(model);
    }

    public Tree getTree() {
        return myTree;
    }

    public JBTable getTable() {
        return myTable;
    }

    public final OnePixelSplitter getSplitter() {
        return split;
    }

    public void setDefaultRenderer(Class<?> columnClass, TableCellRenderer renderer) {
        myTable.setDefaultRenderer(columnClass, renderer);
    }

    public TableCellRenderer getDefaultRenderer(Class<?> columnClass) {
        return myTable.getDefaultRenderer(columnClass);
    }

    public void setModel(TreeTableModel model) {
        myModel = model;

        myTree.setModel(model);
        myTable.setModel(new TreeTableModelAdapter(model, myTree, myTable));

        TreeColumnModel tcm = (TreeColumnModel) myTreeTableHeader.getColumnModel();
        if (tcm.treeColumnIndex >= 0) {
            myTable.removeColumn(myTable.getColumnModel().getColumn(tcm.treeColumnIndex));
        }

        if (myTree.getRowHeight() < 1) {
            myTable.setRowHeight(JBUIScale.scale(18));
        }
        else {
            myTable.setRowHeight(myTree.getRowHeight());
        }

        setColumnProportion(myColumnProportion);
    }

    public void setColumnProportion(float columnProportion) {
        myColumnProportion = columnProportion;
        split.setProportion(1f - ((myModel.getColumnCount() - 1) * columnProportion));
    }

    @SuppressWarnings("unused")
    public float getColumnProportion() {
        return myColumnProportion;
    }

    public TreeTableModel getModel() {
        return myModel;
    }

    /**
     * Enable sorting by columns in this tree-table.
     * Use this method only if this RowSorter or your model support tree sorting.
     * For example, com.intellij.ui.tree.StructureTreeModel supports sorting via setComparator method,
     * so in this case a RowSorter may just redirect sorting requests to the StructureTreeModel.
     */
    public void setRowSorter(RowSorter<? extends TableModel> sorter) {
        myTable.setRowSorter(sorter);

        JTable ref = myTreeTableHeader.getTable();
        ref.setModel(myTable.getModel());
        ref.setRowSorter(sorter);
    }

    @Override
    public @Nullable Color getPathBackground(TreePath path, int row) {
        return null;
    }

    @Override
    public boolean hasFocus() {
        return myTree.hasFocus() || myTable.hasFocus();
    }

    protected void onTableDoubleClick(MouseEvent e, int row) {
        if (myTree.isCollapsed(row)) {
            myTree.expandRow(row);
        }
        else {
            myTree.collapseRow(row);
        }
    }

    @SuppressWarnings("unused")
    private boolean addTreeTableRowDirtyRegion(JComponent component, long tm, int x, int y, int width, int height) {
        // checks if repaint manager should mark row of tree or table
        // and mark we need to repaint both components together,
        // otherwise super repaint
        boolean isNeedToRepaintRow = component.getWidth() == width;
        if (isNeedToRepaintRow) {
            repaint(tm, 0, y, this.getWidth(), height);
        }
        return isNeedToRepaintRow;
    }

    private final class SelectionSupport extends MouseAdapter implements TreeSelectionListener, ListSelectionListener {

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2 && e.getSource() == myTable) {
                onTableDoubleClick(e, myTable.getSelectedRow());
            }
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (e.getSource() == myTable.getSelectionModel()) {
                int row = myTable.getSelectedRow();
                if (row >= 0) {
                    myTree.setSelectionRow(row);
                }
            }
        }

        @Override
        public void valueChanged(TreeSelectionEvent e) {
            if (e.getSource() == myTree.getSelectionModel()) {
                int row = myTree.getRowForPath(myTree.getSelectionPath());
                if (row >= 0) {
                    myTable.setRowSelectionInterval(row, row);
                }
                else {
                    myTable.clearSelection();
                }
            }
        }
    }

    private final class Table extends JBTable {
        final JBTable ref = new JBTable();

        private Table() {
            myTreeTableHeader = new JBTableHeader() {
                @Override
                public AccessibleContext getAccessibleContext() {
                    return new MyAccessibleContext();
                }

                @Override
                public int getWidth() {
                    return super.getWidth() + 1;
                }
            };
            ref.setTableHeader(myTreeTableHeader); // <- we steal table header and need to provide any JTable to handle right ui painting
            myTreeTableHeader.setColumnModel(new TreeColumnModel());
            myTreeTableHeader.setReorderingAllowed(false);
            myTreeTableHeader.setResizingAllowed(false);
        }

        @Override
        public void setRowHeight(int rowHeight) {
            super.setRowHeight(rowHeight);
            if (myTree != null && myTree.getRowHeight() < rowHeight) {
                myTree.setRowHeight(getRowHeight());
            }
        }

        @Override
        public void updateUI() {
            super.updateUI();
            // dynamically update ui for stolen header
            if (ref != null) {
                ref.updateUI();
            }
        }

        // Insets support below
        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            Insets insets = getInsets();
            return new Dimension(size.width + insets.left + insets.right, size.height + insets.top + insets.bottom);
        }

        @Override
        public Rectangle getCellRect(int row, int column, boolean includeSpacing) {
            Rectangle rect = super.getCellRect(row, column, includeSpacing);
            Insets insets = getInsets();
            return new Rectangle(rect.x + insets.left, rect.y + insets.top, rect.width, rect.height);
        }

        @Override
        public int columnAtPoint(Point point) {
            Insets insets = getInsets();
            return super.columnAtPoint(new Point(point.x - insets.left, point.y - insets.top));
        }

        @Override
        public int rowAtPoint(Point point) {
            Insets insets = getInsets();
            return super.rowAtPoint(new Point(point.x - insets.left, point.y - insets.top));
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            int increment = super.getScrollableUnitIncrement(visibleRect, orientation, direction);
            if (increment == 0 && orientation == SwingConstants.VERTICAL && direction < 0) {
                return visibleRect.y; // To support insets
            }
            return increment;
        }

        private final class MyAccessibleContext extends AccessibleContextDelegate {

            MyAccessibleContext() {
                super(myTable.getAccessibleContext());
            }

            @Override
            protected Container getDelegateParent() {
                return JBTreeTable.this;
            }
        }
    }

    private final class TreeColumnModel extends DefaultTableColumnModel {

        private int treeColumnIndex = -1;

        private TreeColumnModel() {
            addColumn(new TableColumn(0) {

                @Override
                public int getWidth() {
                    return getTotalColumnWidth();
                }

                @Override
                public Object getHeaderValue() {
                    return treeColumnIndex < 0 ? " " : myModel.getColumnName(treeColumnIndex);
                }
            });
            addColumn(new TableColumn(1, 0));
            myTree.addPropertyChangeListener(JTree.TREE_MODEL_PROPERTY, evt -> {
                TreeTableModel model = myModel;
                treeColumnIndex = -1;
                for (int i = 0; i < model.getColumnCount(); i++) {
                    if (TreeTableModel.class.isAssignableFrom(model.getColumnClass(i))) {
                        if (i != 0) {
                            throw new IllegalArgumentException("Tree column must be first");
                        }
                        treeColumnIndex = i;
                        break;
                    }
                }
            });
        }

        @Override
        public int getTotalColumnWidth() {
            return myTree.getVisibleRect().width + 1;
        }
    }

    private final class MyTree extends Tree implements PlainSelectionTree, TreePathBackgroundSupplier {
        @Override
        public void repaint(long tm, int x, int y, int width, int height) {
            if (!addTreeTableRowDirtyRegion(this, tm, x, y, width, height)) {
                super.repaint(tm, x, y, width, height);
            }
        }

        @Override
        public void treeDidChange() {
            super.treeDidChange();
            if (myTable != null) {
                myTable.revalidate();
                myTable.repaint();
            }
        }

        @Override
        public @Nullable Color getPathBackground(TreePath path, int row) {
            return JBTreeTable.this.getPathBackground(path, row);
        }
    }
}
