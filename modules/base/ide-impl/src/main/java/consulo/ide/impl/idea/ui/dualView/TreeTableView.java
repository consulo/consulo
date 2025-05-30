/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui.dualView;

import consulo.ide.impl.idea.ui.HighlightableCellRenderer;
import consulo.ide.impl.idea.ui.treeStructure.treetable.ListTreeTableModelOnColumns;
import consulo.logging.Logger;
import consulo.ui.ex.awt.ColumnInfo;
import consulo.ui.ex.awt.SortableColumnModel;
import consulo.ui.ex.awt.table.ItemsProvider;
import consulo.ui.ex.awt.table.SelectionProvider;
import consulo.ui.ex.awt.tree.table.TreeTable;
import consulo.ui.ex.awt.tree.table.TreeTableCellRenderer;
import consulo.ui.ex.awt.tree.table.TreeTableModel;
import consulo.ui.ex.awt.tree.table.TreeTableTree;
import consulo.util.collection.ContainerUtil;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TreeTableView extends TreeTable implements ItemsProvider, SelectionProvider {
    private static final Logger LOG = Logger.getInstance(TreeTableView.class);

    public TreeTableView(ListTreeTableModelOnColumns treeTableModel) {
        super(treeTableModel);
        setRootVisible(false);

        setTreeCellRenderer(new TreeCellRenderer() {
            private final TreeCellRenderer myBaseRenderer = new HighlightableCellRenderer();

            @Override
            public Component getTreeCellRendererComponent(
                JTree tree1,
                Object value,
                boolean selected,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus
            ) {
                JComponent result =
                    (JComponent)myBaseRenderer.getTreeCellRendererComponent(tree1, value, selected, expanded, leaf, row, hasFocus);
                result.setOpaque(!selected);
                return result;
            }
        });

        setSizes();
    }

    @Override
    public void setTableModel(TreeTableModel treeTableModel) {
        super.setTableModel(treeTableModel);
        LOG.assertTrue(treeTableModel instanceof SortableColumnModel);
    }

    private void setSizes() {
        ColumnInfo[] columns = ((ListTreeTableModelOnColumns)getTableModel()).getColumns();
        for (int i = 0; i < columns.length; i++) {
            ColumnInfo columnInfo = columns[i];
            TableColumn column = getColumnModel().getColumn(i);
            if (columnInfo.getWidth(this) > 0) {
                int width = columnInfo.getWidth(this);
                column.setMaxWidth(width);
                column.setMinWidth(width);
            }
            else {
                String preferredValue = columnInfo.getPreferredStringValue();
                if (preferredValue != null) {
                    int width = getFontMetrics(getFont()).stringWidth(preferredValue) + columnInfo.getAdditionalWidth();
                    column.setPreferredWidth(width);
                }
            }
        }
    }

    @Override
    public TableCellEditor getCellEditor(int row, int column) {
        TableCellEditor editor = getColumnInfo(column).getEditor(getRowElement(row));
        return editor == null ? super.getCellEditor(row, column) : editor;
    }

    @Override
    public TreeTableCellRenderer createTableRenderer(TreeTableModel treeTableModel) {
        return new TreeTableCellRenderer(TreeTableView.this, getTree()) {
            @Override
            public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
            ) {
                JComponent component = (JComponent)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                component.setOpaque(isSelected);
                return component;
            }
        };
    }


    ListTreeTableModelOnColumns getTreeViewModel() {
        return (ListTreeTableModelOnColumns)getTableModel();
    }

    public List<DualTreeElement> getFlattenItems() {
        List<DualTreeElement> items = getTreeViewModel().getItems();
        return ContainerUtil.findAll(items, DualTreeElement::shouldBeInTheFlatView);
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        TableCellRenderer renderer = getColumnInfo(column).getRenderer(getRowElement(row));
        TableCellRenderer baseRenderer = renderer == null ? super.getCellRenderer(row, column) : renderer;
        return new CellRendererWrapper(baseRenderer);
    }

    protected Object getRowElement(int row) {
        return getTree().getPathForRow(row).getLastPathComponent();
    }

    protected final ColumnInfo<Object, ?> getColumnInfo(int column) {
        return getTreeViewModel().getColumnInfos()[convertColumnIndexToModel(column)];
    }

    @Override
    public List getItems() {
        return getTreeViewModel().getItems();
    }

    @Override
    public List getSelection() {
        TreeTableTree tree = getTree();
        if (tree == null) {
            return Collections.emptyList();
        }
        int[] rows = getSelectedRows();
        ArrayList result = new ArrayList();
        for (int row : rows) {
            TreePath pathForRow = tree.getPathForRow(row);
            if (pathForRow != null) {
                result.add(pathForRow.getLastPathComponent());
            }
        }
        return result;
    }

    @Override
    public void addSelection(Object item) {
        getTree().setExpandsSelectedPaths(true);
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)item;
        addSelectedPath(new TreePath(treeNode.getPath()));
    }

    public static class CellRendererWrapper implements TableCellRenderer {
        private final TableCellRenderer myBaseRenderer;

        public CellRendererWrapper(TableCellRenderer baseRenderer) {
            myBaseRenderer = baseRenderer;
        }

        public TableCellRenderer getBaseRenderer() {
            return myBaseRenderer;
        }

        @Override
        public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column
        ) {
            JComponent rendererComponent = (JComponent)myBaseRenderer.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
            if (isSelected) {
                rendererComponent.setBackground(table.getSelectionBackground());
                rendererComponent.setForeground(table.getSelectionForeground());
            }
            else {
                Color bg = table.getBackground();
                rendererComponent.setBackground(bg);
                rendererComponent.setForeground(table.getForeground());
            }
            rendererComponent.setOpaque(isSelected);
            return rendererComponent;
        }
    }
}
