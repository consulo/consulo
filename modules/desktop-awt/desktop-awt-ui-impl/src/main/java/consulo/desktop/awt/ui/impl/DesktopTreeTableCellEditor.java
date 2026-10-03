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

import consulo.ui.ex.awt.AbstractTableCellEditor;
import org.jspecify.annotations.Nullable;

import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import java.awt.Component;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
class DesktopTreeTableCellEditor<E> extends AbstractTableCellEditor {
    private final DesktopTreeTableImpl<E> myOwner;
    private final DesktopTableColumnImpl<E, ?> myColumn;

    private final CellEditorListener myBridge = new CellEditorListener() {
        @Override
        public void editingStopped(ChangeEvent e) {
            fireEditingStopped();
        }

        @Override
        public void editingCanceled(ChangeEvent e) {
            fireEditingCanceled();
        }
    };

    private @Nullable TableCellEditor myDelegate;

    DesktopTreeTableCellEditor(DesktopTreeTableImpl<E> owner, DesktopTableColumnImpl<E, ?> column) {
        myOwner = owner;
        myColumn = column;
    }

    @Override
    public @Nullable Component getTableCellEditorComponent(JTable table, @Nullable Object value, boolean isSelected, int row, int column) {
        detach();

        E item = myOwner.valueAtRow(row);
        TableCellEditor delegate = item == null ? null : myColumn.getEditor(item);
        if (delegate == null) {
            return null;
        }

        myDelegate = delegate;
        delegate.addCellEditorListener(myBridge);
        return delegate.getTableCellEditorComponent(table, value, isSelected, row, column);
    }

    @Override
    public @Nullable Object getCellEditorValue() {
        TableCellEditor delegate = myDelegate;
        return delegate == null ? null : delegate.getCellEditorValue();
    }

    @Override
    public boolean stopCellEditing() {
        TableCellEditor delegate = myDelegate;
        if (delegate == null) {
            fireEditingStopped();
            return true;
        }
        return delegate.stopCellEditing();
    }

    @Override
    public void cancelCellEditing() {
        TableCellEditor delegate = myDelegate;
        if (delegate == null) {
            fireEditingCanceled();
            return;
        }
        delegate.cancelCellEditing();
    }

    private void detach() {
        TableCellEditor delegate = myDelegate;
        if (delegate != null) {
            delegate.removeCellEditorListener(myBridge);
            myDelegate = null;
        }
    }
}
