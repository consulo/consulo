/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.language.editor.todo.impl.internal.configurable;

import consulo.ide.localize.IdeLocalize;
import consulo.language.psi.search.TodoPattern;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.awt.ItemRemovable;
import consulo.ui.image.Image;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.List;

final class PatternsTableModel extends AbstractTableModel implements ItemRemovable {
    private static final LocalizeValue[] OUR_COLUMN_NAMES = new LocalizeValue[]{
        IdeLocalize.columnTodoPatternsIcon(),
        IdeLocalize.columnTodoPatternsCaseSensitive(),
        IdeLocalize.columnTodoPatternsPattern()
    };
    private static final Class[] OUR_COLUMN_CLASSES = new Class[]{Icon.class, Boolean.class, String.class};

    private final List<TodoPattern> myPatterns;

    public PatternsTableModel(List<TodoPattern> patterns) {
        myPatterns = patterns;
    }

    @Override
    public String getColumnName(int column) {
        return column < OUR_COLUMN_NAMES.length ? OUR_COLUMN_NAMES[column].get() : null;
    }

    @Override
    public Class getColumnClass(int column) {
        return OUR_COLUMN_CLASSES[column];
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public int getRowCount() {
        return myPatterns.size();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 1;
    }

    @Override
    public Object getValueAt(int row, int column) {
        TodoPattern pattern = myPatterns.get(row);
        return switch (column) {
            // "Icon" column
            case 0 -> pattern.getAttributes().getIcon();
            // "Case Sensitive" column
            case 1 -> pattern.isCaseSensitive() ? Boolean.TRUE : Boolean.FALSE;
            // "Pattern" column
            case 2 -> pattern.getPatternString();
            default -> throw new IllegalArgumentException();
        };
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        TodoPattern pattern = myPatterns.get(row);
        switch (column) {
            case 0 -> pattern.getAttributes().setIcon((Image) value);
            case 1 -> pattern.setCaseSensitive((Boolean) value);
            case 2 -> pattern.setPatternString(((String) value).trim());
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void removeRow(int index) {
        myPatterns.remove(index);
        fireTableRowsDeleted(index, index);
    }
}
