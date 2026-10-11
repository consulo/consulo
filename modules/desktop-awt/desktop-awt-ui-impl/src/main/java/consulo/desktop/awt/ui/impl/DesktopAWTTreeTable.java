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
import consulo.desktop.awt.ui.impl.tree.JBTreeTable;
import consulo.ui.Component;
import consulo.ui.ex.awt.Splitter;
import consulo.ui.ex.awt.table.JBTable;
import consulo.ui.ex.awt.tree.table.TreeTableModel;

import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class DesktopAWTTreeTable extends JBTreeTable implements FromSwingComponentWrapper, Scrollable {
    private static final float COLUMN_PROPORTION = 0.1f;
    private static final float MAX_COLUMNS_PROPORTION = 0.7f;

    private final DesktopTreeTableImpl<?> myOwner;

    private boolean myFitScheduled;
    private boolean mySettingProportion;
    private boolean myProportionChosenByUser;

    public DesktopAWTTreeTable(TreeTableModel model, DesktopAWTTree tree, DesktopTreeTableImpl<?> owner) {
        super(model, tree);
        myOwner = owner;

        JBTable table = getTable();
        table.getModel().addTableModelListener(e -> requestFitColumns());
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                requestFitColumns();
            }
        });
        getSplitter().addPropertyChangeListener(Splitter.PROP_PROPORTION, e -> {
            if (!mySettingProportion) {
                myProportionChosenByUser = true;
            }
        });
        tree.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                updateTableSelectionColors(true);
            }

            @Override
            public void focusLost(FocusEvent e) {
                updateTableSelectionColors(table.hasFocus());
            }
        });
        table.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                tree.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                tree.repaint();
            }
        });
    }

    private void updateTableSelectionColors(boolean focused) {
        JBTable table = getTable();
        Color background = UIManager.getColor(focused ? "Table.selectionBackground" : "Table.selectionInactiveBackground");
        Color foreground = UIManager.getColor(focused ? "Table.selectionForeground" : "Table.selectionInactiveForeground");
        if (background != null) {
            table.setSelectionBackground(background);
        }
        if (foreground != null) {
            table.setSelectionForeground(foreground);
        }
    }

    public DesktopAWTTree getAWTTree() {
        return (DesktopAWTTree) getTree();
    }

    void updateColumnProportion() {
        setColumnProportion(COLUMN_PROPORTION);
        requestFitColumns();
    }

    @Override
    public void setColumnProportion(float columnProportion) {
        int columns = Math.max(1, getModel().getColumnCount() - 1);
        boolean settingProportion = mySettingProportion;
        mySettingProportion = true;
        try {
            super.setColumnProportion(Math.min(columnProportion, MAX_COLUMNS_PROPORTION / columns));
        }
        finally {
            mySettingProportion = settingProportion;
        }
    }

    void requestFitColumns() {
        if (myFitScheduled) {
            return;
        }

        myFitScheduled = true;
        SwingUtilities.invokeLater(this::fitColumns);
    }

    private void fitColumns() {
        myFitScheduled = false;

        JBTable table = getTable();
        TableColumnModel columnModel = table.getColumnModel();
        int count = columnModel.getColumnCount();
        int total = getWidth();
        if (myProportionChosenByUser || count == 0 || total <= 0) {
            return;
        }

        Insets insets = table.getInsets();
        int required = insets.left + insets.right;
        for (int index = 0; index < count; index++) {
            TableColumn column = columnModel.getColumn(index);
            if (column.getMinWidth() == column.getMaxWidth()) {
                required += column.getMaxWidth();
                continue;
            }

            int width = Math.max(column.getMinWidth(), contentWidth(table, column, index));
            column.setPreferredWidth(width);
            required += width;
        }

        float columnsShare = Math.min(MAX_COLUMNS_PROPORTION, count * COLUMN_PROPORTION);
        float share = Math.min(MAX_COLUMNS_PROPORTION, Math.max(columnsShare, (float) required / total));

        mySettingProportion = true;
        try {
            getSplitter().setProportion(1f - share);
        }
        finally {
            mySettingProportion = false;
        }
    }

    private static int contentWidth(JBTable table, TableColumn column, int index) {
        int width = 0;

        TableCellRenderer headerRenderer = column.getHeaderRenderer();
        if (headerRenderer == null && table.getTableHeader() != null) {
            headerRenderer = table.getTableHeader().getDefaultRenderer();
        }
        if (headerRenderer != null) {
            java.awt.Component header = headerRenderer.getTableCellRendererComponent(table, column.getHeaderValue(), false, false, -1, index);
            width = header.getPreferredSize().width;
        }

        Rectangle visible = table.getVisibleRect();
        int first = table.rowAtPoint(new Point(0, visible.y));
        int last = table.rowAtPoint(new Point(0, visible.y + visible.height - 1));
        if (first < 0) {
            return width;
        }
        if (last < 0) {
            last = table.getRowCount() - 1;
        }

        for (int row = first; row <= last; row++) {
            java.awt.Component cell = table.prepareRenderer(table.getCellRenderer(row, index), row, index);
            width = Math.max(width, cell.getPreferredSize().width + table.getIntercellSpacing().width);
        }
        return width;
    }

    @Override
    protected void onTableDoubleClick(MouseEvent e, int row) {
        if (row >= 0 && myOwner.onTableDoubleClick(e, row)) {
            super.onTableDoubleClick(e, row);
        }
    }

    @Override
    public Component toUIComponent() {
        return myOwner;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 1;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return true;
    }
}
