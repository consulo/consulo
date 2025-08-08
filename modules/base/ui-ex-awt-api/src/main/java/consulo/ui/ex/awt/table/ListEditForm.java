/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ui.ex.awt.table;

import consulo.application.ui.wm.IdeFocusManager;
import consulo.ui.UIAccess;
import consulo.ui.ex.awt.ToolbarDecorator;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.List;

public class ListEditForm {
    JPanel contentPanel;
    ListTable table;

    public ListEditForm(String title, List<String> stringList) {
        table = new ListTable(new ListWrappingTableModel(stringList, title));

        contentPanel = ToolbarDecorator.createDecorator(table).setAddAction(button -> {
            ListWrappingTableModel tableModel = table.getModel();
            tableModel.addRow();

            UIAccess uiAccess = UIAccess.current();

            uiAccess.give(() -> {
                int lastRowIndex = tableModel.getRowCount() - 1;
                Rectangle rectangle = table.getCellRect(lastRowIndex, 0, true);
                table.scrollRectToVisible(rectangle);
                table.editCellAt(lastRowIndex, 0);
                ListSelectionModel selectionModel = table.getSelectionModel();
                selectionModel.setSelectionInterval(lastRowIndex, lastRowIndex);
                TableCellEditor editor = table.getCellEditor();
                Component component = editor.getTableCellEditorComponent(table, null, true, lastRowIndex, 0);
                IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
                    IdeFocusManager.getGlobalInstance().requestFocus(component, true);
                });
            });
        }).setRemoveAction(button -> {
            ListSelectionModel selectionModel = table.getSelectionModel();
            int minIndex = selectionModel.getMinSelectionIndex();
            int maxIndex = selectionModel.getMaxSelectionIndex();
            if (minIndex == -1 || maxIndex == -1) {
                return;
            }
            ListWrappingTableModel tableModel = table.getModel();
            for (int i = minIndex; i <= maxIndex; i++) {
                if (selectionModel.isSelectedIndex(i)) {
                    tableModel.removeRow(i);
                }
            }
            int count = tableModel.getRowCount();
            if (count <= minIndex) {
                selectionModel.setSelectionInterval(count - 1, count - 1);
            }
            else if (minIndex <= 0) {
                if (count > 0) {
                    selectionModel.setSelectionInterval(0, 0);
                }
            }
            else {
                selectionModel.setSelectionInterval(minIndex - 1, minIndex - 1);
            }
        }).disableUpDownActions().createPanel();
    }

    public JComponent getContentPanel() {
        return contentPanel;
    }
}
