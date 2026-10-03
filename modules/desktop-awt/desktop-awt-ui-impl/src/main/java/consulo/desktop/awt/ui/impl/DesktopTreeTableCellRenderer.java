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

import org.jspecify.annotations.Nullable;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
class DesktopTreeTableCellRenderer<E> implements TableCellRenderer {
    private final DesktopTreeTableImpl<E> myOwner;
    private final DesktopTableColumnImpl<E, ?> myColumn;

    DesktopTreeTableCellRenderer(DesktopTreeTableImpl<E> owner, DesktopTableColumnImpl<E, ?> column) {
        myOwner = owner;
        myColumn = column;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table,
                                                   @Nullable Object value,
                                                   boolean selected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
        E item = myOwner.valueAtRow(row);
        TableCellRenderer renderer = item == null ? null : myColumn.getRenderer(item);
        if (renderer == null) {
            return table.getDefaultRenderer(Object.class).getTableCellRendererComponent(table, null, selected, hasFocus, row, column);
        }
        return renderer.getTableCellRendererComponent(table, value, selected, hasFocus, row, column);
    }
}
