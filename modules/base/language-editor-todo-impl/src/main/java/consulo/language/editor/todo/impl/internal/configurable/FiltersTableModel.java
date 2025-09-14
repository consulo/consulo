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
import consulo.language.editor.todo.TodoFilter;
import consulo.language.psi.search.TodoPattern;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.awt.ItemRemovable;

import javax.swing.table.AbstractTableModel;
import java.util.Iterator;
import java.util.List;

final class FiltersTableModel extends AbstractTableModel implements ItemRemovable {
    private static final LocalizeValue[] OUR_COLUMN_NAMES = new LocalizeValue[]{
        IdeLocalize.columnTodoFiltersName(),
        IdeLocalize.columnTodoFilterPatterns()
    };
    private static final Class[] OUR_COLUMN_CLASSES = new Class[]{String.class, String.class};

    private final List<TodoFilter> myFilters;

    public FiltersTableModel(List<TodoFilter> filters) {
        myFilters = filters;
    }

    @Override
    public String getColumnName(int column) {
        return column <= OUR_COLUMN_NAMES.length ? OUR_COLUMN_NAMES[column].get() : null;
    }

    @Override
    public Class getColumnClass(int column) {
        return OUR_COLUMN_CLASSES[column];
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public int getRowCount() {
        return myFilters.size();
    }

    @Override
    public Object getValueAt(int row, int column) {
        TodoFilter filter = myFilters.get(row);
        switch (column) {
            case 0: { // "Name" column
                return filter.getName();
            }
            case 1: {
                StringBuilder sb = new StringBuilder();
                for (Iterator i = filter.iterator(); i.hasNext(); ) {
                    TodoPattern pattern = (TodoPattern) i.next();
                    sb.append(pattern.getPatternString());
                    if (i.hasNext()) {
                        sb.append(" | ");
                    }
                }
                return sb.toString();
            }
            default: {
                throw new IllegalArgumentException();
            }
        }
    }

    @Override
    public void removeRow(int index) {
        myFilters.remove(index);
        fireTableRowsDeleted(index, index);
    }
}
