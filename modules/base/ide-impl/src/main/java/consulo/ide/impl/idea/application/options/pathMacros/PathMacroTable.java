/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.application.options.pathMacros;

import consulo.application.localize.ApplicationLocalize;
import consulo.application.macro.PathMacros;
import consulo.component.store.internal.PathMacrosService;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.table.JBTable;
import consulo.util.lang.Couple;
import consulo.util.lang.Pair;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.*;

/**
 * @author dsl
 */
public class PathMacroTable extends JBTable {
    private static final Logger LOG = Logger.getInstance(PathMacroTable.class);
    private final PathMacros myPathMacros = PathMacros.getInstance();
    private final MyTableModel myTableModel = new MyTableModel();
    private static final int NAME_COLUMN = 0;
    private static final int VALUE_COLUMN = 1;

    private final List<Pair<String, String>> myMacros = new ArrayList<>();
    private static final Comparator<Pair<String, String>> MACRO_COMPARATOR =
        (pair, pair1) -> pair.getFirst().compareTo(pair1.getFirst());

    private final Collection<String> myUndefinedMacroNames;

    public PathMacroTable() {
        this(null);
    }

    public PathMacroTable(Collection<String> undefinedMacroNames) {
        myUndefinedMacroNames = undefinedMacroNames;
        setModel(myTableModel);
        TableColumn column = getColumnModel().getColumn(NAME_COLUMN);
        column.setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
            ) {
                Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String macroValue = getMacroValueAt(row);
                component.setForeground(macroValue.length() == 0
                    ? JBColor.RED
                    : isSelected ? table.getSelectionForeground() : table.getForeground());
                return component;
            }
        });
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        //obtainData();

        getEmptyText().setText(ApplicationLocalize.textNoPathVariables());
    }

    public String getMacroValueAt(int row) {
        return (String) getValueAt(row, VALUE_COLUMN);
    }

    public String getMacroNameAt(int row) {
        return (String) getValueAt(row, NAME_COLUMN);
    }

    @RequiredUIAccess
    public void addMacro() {
        String title = ApplicationLocalize.titleAddVariable().get();
        PathMacroEditor macroEditor = new PathMacroEditor(title, "", "", new AddValidator(title));
        macroEditor.show();
        if (macroEditor.isOK()) {
            String name = macroEditor.getName();
            myMacros.add(Couple.of(name, macroEditor.getValue()));
            Collections.sort(myMacros, MACRO_COMPARATOR);
            int index = indexOfMacroWithName(name);
            LOG.assertTrue(index >= 0);
            myTableModel.fireTableDataChanged();
            setRowSelectionInterval(index, index);
        }
    }

    private boolean isValidRow(int selectedRow) {
        return selectedRow >= 0 && selectedRow < myMacros.size();
    }

    public void removeSelectedMacros() {
        int[] selectedRows = getSelectedRows();
        if (selectedRows.length == 0) {
            return;
        }
        Arrays.sort(selectedRows);
        int originalRow = selectedRows[0];
        for (int i = selectedRows.length - 1; i >= 0; i--) {
            int selectedRow = selectedRows[i];
            if (isValidRow(selectedRow)) {
                myMacros.remove(selectedRow);
            }
        }
        myTableModel.fireTableDataChanged();
        if (originalRow < getRowCount()) {
            setRowSelectionInterval(originalRow, originalRow);
        }
        else if (getRowCount() > 0) {
            int index = getRowCount() - 1;
            setRowSelectionInterval(index, index);
        }
    }

    public void commit() {
        myPathMacros.removeAllMacros();
        for (Pair<String, String> pair : myMacros) {
            String value = pair.getSecond();
            if (value != null && value.trim().length() > 0) {
                String path = value.replace(File.separatorChar, '/');
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                myPathMacros.setMacro(pair.getFirst(), path);
            }
        }
    }

    public void reset() {
        obtainData();
    }

    private boolean hasMacroWithName(String name) {
        if (PathMacros.getInstance().getSystemMacroNames().contains(name)) {
            return true;
        }

        for (Pair<String, String> macro : myMacros) {
            if (name.equals(macro.getFirst())) {
                return true;
            }
        }
        return false;
    }

    private int indexOfMacroWithName(String name) {
        for (int i = 0; i < myMacros.size(); i++) {
            Pair<String, String> pair = myMacros.get(i);
            if (name.equals(pair.getFirst())) {
                return i;
            }
        }
        return -1;
    }

    private void obtainData() {
        obtainMacroPairs(myMacros);
        myTableModel.fireTableDataChanged();
    }

    private void obtainMacroPairs(List<Pair<String, String>> macros) {
        macros.clear();
        Set<String> macroNames = myPathMacros.getUserMacroNames();
        for (String name : macroNames) {
            macros.add(Couple.of(name, myPathMacros.getValue(name).replace('/', File.separatorChar)));
        }

        if (myUndefinedMacroNames != null) {
            for (String undefinedMacroName : myUndefinedMacroNames) {
                macros.add(Couple.of(undefinedMacroName, ""));
            }
        }
        Collections.sort(macros, MACRO_COMPARATOR);
    }

    @RequiredUIAccess
    public void editMacro() {
        if (getSelectedRowCount() != 1) {
            return;
        }
        int selectedRow = getSelectedRow();
        Pair<String, String> pair = myMacros.get(selectedRow);
        String title = ApplicationLocalize.titleEditVariable().get();
        String macroName = pair.getFirst();
        PathMacroEditor macroEditor = new PathMacroEditor(title, macroName, pair.getSecond(), new EditValidator());
        macroEditor.show();
        if (macroEditor.isOK()) {
            myMacros.remove(selectedRow);
            myMacros.add(Couple.of(macroEditor.getName(), macroEditor.getValue()));
            Collections.sort(myMacros, MACRO_COMPARATOR);
            myTableModel.fireTableDataChanged();
        }
    }

    public boolean isModified() {
        List<Pair<String, String>> macros = new ArrayList<>();
        obtainMacroPairs(macros);
        return !macros.equals(myMacros);
    }

    private class MyTableModel extends AbstractTableModel {
        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public int getRowCount() {
            return myMacros.size();
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Pair<String, String> pair = myMacros.get(rowIndex);
            switch (columnIndex) {
                case NAME_COLUMN:
                    return pair.getFirst();
                case VALUE_COLUMN:
                    return pair.getSecond();
            }
            LOG.error("Wrong indices");
            return null;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        }

        @Override
        public String getColumnName(int columnIndex) {
            return switch (columnIndex) {
                case NAME_COLUMN -> ApplicationLocalize.columnName().get();
                case VALUE_COLUMN -> ApplicationLocalize.columnValue().get();
                default -> null;
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }

    private class AddValidator implements PathMacroEditor.Validator {
        private final String myTitle;

        public AddValidator(String title) {
            myTitle = title;
        }

        @Override
        public boolean checkName(String name) {
            return name.length() != 0 && PathMacrosService.MACRO_PATTERN.matcher("$" + name + "$").matches();
        }

        @Override
        @RequiredUIAccess
        public boolean isOK(String name, String value) {
            if (name.length() == 0) {
                return false;
            }
            if (hasMacroWithName(name)) {
                Messages.showErrorDialog(
                    PathMacroTable.this,
                    ApplicationLocalize.errorVariableAlreadyExists(name).get(),
                    myTitle
                );
                return false;
            }
            return true;
        }
    }

    private static class EditValidator implements PathMacroEditor.Validator {
        @Override
        public boolean checkName(String name) {
            if (name.length() == 0) {
                return false;
            }
            if (PathMacros.getInstance().getSystemMacroNames().contains(name)) {
                return false;
            }

            return PathMacrosService.MACRO_PATTERN.matcher("$" + name + "$").matches();
        }

        @Override
        public boolean isOK(String name, String value) {
            return checkName(name);
        }
    }
}
