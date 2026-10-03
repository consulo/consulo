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
import consulo.ui.Component;
import consulo.ui.ex.awt.table.JBTable;
import consulo.desktop.awt.ui.impl.tree.JBTreeTable;
import consulo.ui.ex.awt.tree.table.TreeTableModel;

import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
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

    public DesktopAWTTreeTable(TreeTableModel model, DesktopAWTTree tree, DesktopTreeTableImpl<?> owner) {
        super(model, tree);
        myOwner = owner;

        JBTable table = getTable();
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
    }

    @Override
    public void setColumnProportion(float columnProportion) {
        int columns = Math.max(1, getModel().getColumnCount() - 1);
        super.setColumnProportion(Math.min(columnProportion, MAX_COLUMNS_PROPORTION / columns));
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
