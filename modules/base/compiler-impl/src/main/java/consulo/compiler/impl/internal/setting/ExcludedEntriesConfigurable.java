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
package consulo.compiler.impl.internal.setting;

import consulo.application.ui.wm.IdeFocusManager;
import consulo.compiler.localize.CompilerLocalize;
import consulo.compiler.setting.ExcludeEntryDescription;
import consulo.compiler.setting.ExcludedEntriesConfiguration;
import consulo.configurable.UnnamedConfigurable;
import consulo.disposer.Disposer;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.ToolbarDecorator;
import consulo.ui.ex.awt.table.JBTable;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatusManager;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class ExcludedEntriesConfigurable implements UnnamedConfigurable {
    private final Project myProject;
    private final ArrayList<ExcludeEntryDescription> myExcludeEntryDescriptions = new ArrayList<>();
    private final FileChooserDescriptor myDescriptor;
    private final ExcludedEntriesConfiguration myConfiguration;
    private ExcludedEntriesPanel myExcludedEntriesPanel;

    public ExcludedEntriesConfigurable(Project project, FileChooserDescriptor descriptor, ExcludedEntriesConfiguration configuration) {
        myDescriptor = descriptor;
        myConfiguration = configuration;
        myProject = project;
    }

    @Override
    @RequiredUIAccess
    public void reset() {
        ExcludeEntryDescription[] descriptions = myConfiguration.getExcludeEntryDescriptions();
        disposeMyDescriptions();
        for (ExcludeEntryDescription description : descriptions) {
            myExcludeEntryDescriptions.add(description.copy(myProject));
        }
        ((AbstractTableModel) myExcludedEntriesPanel.myExcludedTable.getModel()).fireTableDataChanged();
    }

    public void addEntry(ExcludeEntryDescription description) {
        myExcludeEntryDescriptions.add(description);
        ((AbstractTableModel) myExcludedEntriesPanel.myExcludedTable.getModel()).fireTableDataChanged();
    }

    private void disposeMyDescriptions() {
        for (ExcludeEntryDescription description : myExcludeEntryDescriptions) {
            Disposer.dispose(description);
        }
        myExcludeEntryDescriptions.clear();
    }

    @Override
    @RequiredUIAccess
    public void apply() {
        myConfiguration.removeAllExcludeEntryDescriptions();
        for (ExcludeEntryDescription description : myExcludeEntryDescriptions) {
            myConfiguration.addExcludeEntryDescription(description.copy(myProject));
        }
        FileStatusManager.getInstance(myProject).fileStatusesChanged(); // refresh exclude from compile status
    }

    @Override
    @RequiredUIAccess
    public boolean isModified() {
        ExcludeEntryDescription[] excludeEntryDescriptions = myConfiguration.getExcludeEntryDescriptions();
        if (excludeEntryDescriptions.length != myExcludeEntryDescriptions.size()) {
            return true;
        }
        for (int i = 0; i < excludeEntryDescriptions.length; i++) {
            ExcludeEntryDescription description = excludeEntryDescriptions[i];
            if (!Objects.equals(description, myExcludeEntryDescriptions.get(i))) {
                return true;
            }
        }
        return false;
    }

    @Override
    @RequiredUIAccess
    public JComponent createComponent() {
        if (myExcludedEntriesPanel == null) {
            myExcludedEntriesPanel = new ExcludedEntriesPanel();
        }
        return myExcludedEntriesPanel;
    }

    @Override
    @RequiredUIAccess
    public void disposeUIResources() {
        myExcludedEntriesPanel = null;
    }

    private class ExcludedEntriesPanel extends JPanel {
        private JBTable myExcludedTable;

        public ExcludedEntriesPanel() {
            super(new BorderLayout());

            add(createMainComponent(), BorderLayout.CENTER);
        }

        @RequiredUIAccess
        private void addPath(FileChooserDescriptor descriptor) {
            FileChooser.chooseFiles(descriptor, myProject, null).whenComplete((chosen, error) -> {
                if (error != null) {
                    return;
                }

                int selected = -1 /*myExcludedTable.getSelectedRow() + 1*/;
                if (selected < 0) {
                    selected = myExcludeEntryDescriptions.size();
                }
                int savedSelected = selected;

                for (VirtualFile chosenFile : chosen) {
                    if (isFileExcluded(chosenFile)) {
                        continue;
                    }
                    ExcludeEntryDescription description;
                    if (chosenFile.isDirectory()) {
                        description = new ExcludeEntryDescription(chosenFile.getUrl(), true, false, myProject);
                    }
                    else {
                        description = new ExcludeEntryDescription(chosenFile.getUrl(), false, true, myProject);
                    }
                    myExcludeEntryDescriptions.add(selected, description);
                    selected++;
                }
                if (selected > savedSelected) { // actually added something
                    AbstractTableModel model = (AbstractTableModel) myExcludedTable.getModel();
                    model.fireTableRowsInserted(savedSelected, selected - 1);
                    myExcludedTable.setRowSelectionInterval(savedSelected, selected - 1);
                }
            });
        }

        private boolean isFileExcluded(VirtualFile file) {
            for (ExcludeEntryDescription description : myExcludeEntryDescriptions) {
                if (file.getUrl().equals(description.getUrl())) {
                    return true;
                }
            }
            return false;
        }

        private void removePaths() {
            int[] selected = myExcludedTable.getSelectedRows();
            if (selected == null || selected.length <= 0) {
                return;
            }
            if (myExcludedTable.isEditing()) {
                TableCellEditor editor = myExcludedTable.getCellEditor();
                if (editor != null) {
                    editor.stopCellEditing();
                }
            }
            AbstractTableModel model = (AbstractTableModel) myExcludedTable.getModel();
            Arrays.sort(selected);
            int indexToSelect = selected[selected.length - 1];
            int removedCount = 0;
            for (int indexToRemove : selected) {
                int row = indexToRemove - removedCount;
                ExcludeEntryDescription description = myExcludeEntryDescriptions.get(row);
                Disposer.dispose(description);
                myExcludeEntryDescriptions.remove(row);
                model.fireTableRowsDeleted(row, row);
                removedCount += 1;
            }
            if (indexToSelect >= myExcludeEntryDescriptions.size()) {
                indexToSelect = myExcludeEntryDescriptions.size() - 1;
            }
            if (indexToSelect >= 0) {
                myExcludedTable.setRowSelectionInterval(indexToSelect, indexToSelect);
            }
            IdeFocusManager.getGlobalInstance()
                .doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(myExcludedTable, true));
        }

        private JComponent createMainComponent() {
            final LocalizeValue[] names = {
                CompilerLocalize.excludeFromCompileTablePathColumnName(),
                CompilerLocalize.excludeFromCompileTableRecursivelyColumnName()
            };
            // Create a model of the data.
            TableModel dataModel = new AbstractTableModel() {
                @Override
                public int getColumnCount() {
                    return names.length;
                }

                @Override
                public int getRowCount() {
                    return myExcludeEntryDescriptions.size();
                }

                @Override
                public Object getValueAt(int row, int col) {
                    ExcludeEntryDescription description = myExcludeEntryDescriptions.get(row);
                    if (col == 0) {
                        return description.getPresentableUrl();
                    }
                    if (col == 1) {
                        if (!description.isFile()) {
                            return description.isIncludeSubdirectories() ? Boolean.TRUE : Boolean.FALSE;
                        }
                        else {
                            return null;
                        }
                    }
                    return null;
                }

                @Override
                public String getColumnName(int column) {
                    return names[column].get();
                }

                @Override
                public Class getColumnClass(int c) {
                    if (c == 0) {
                        return String.class;
                    }
                    if (c == 1) {
                        return Boolean.class;
                    }
                    return null;
                }

                @Override
                public boolean isCellEditable(int row, int col) {
                    if (col == 1) {
                        ExcludeEntryDescription description = myExcludeEntryDescriptions.get(row);
                        return !description.isFile();
                    }
                    return true;
                }

                @Override
                public void setValueAt(Object aValue, int row, int col) {
                    ExcludeEntryDescription description = myExcludeEntryDescriptions.get(row);
                    if (col == 1) {
                        description.setIncludeSubdirectories(aValue.equals(Boolean.TRUE));
                    }
                    else {
                        String path = (String) aValue;
                        description.setPresentableUrl(path);
                    }
                }
            };

            myExcludedTable = new JBTable(dataModel);
            myExcludedTable.setEnableAntialiasing(true);

            myExcludedTable.getEmptyText().setText(CompilerLocalize.noExcludes());
            myExcludedTable.setPreferredScrollableViewportSize(new Dimension(300, myExcludedTable.getRowHeight() * 6));
            myExcludedTable.setDefaultRenderer(Boolean.class, new BooleanRenderer());
            myExcludedTable.setDefaultRenderer(Object.class, new MyObjectRenderer());
            myExcludedTable.getColumn(names[0].get()).setPreferredWidth(350);
            int cbWidth = 15 + myExcludedTable.getTableHeader()
                .getFontMetrics(myExcludedTable.getTableHeader().getFont())
                .stringWidth(names[1].get());
            TableColumn cbColumn = myExcludedTable.getColumn(names[1].get());
            cbColumn.setPreferredWidth(cbWidth);
            cbColumn.setMaxWidth(cbWidth);
            myExcludedTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            if (myExcludedTable.getDefaultEditor(String.class) instanceof DefaultCellEditor defaultCellEditor) {
                defaultCellEditor.setClickCountToStart(1);
            }

            return ToolbarDecorator.createDecorator(myExcludedTable)
                .disableUpAction()
                .disableDownAction()
                .setAddAction(anActionButton -> addPath(myDescriptor))
                .setRemoveAction(anActionButton -> removePaths()).createPanel();
        }
    }

    private static class BooleanRenderer extends JCheckBox implements TableCellRenderer {
        private final JPanel myPanel = new JPanel();

        public BooleanRenderer() {
            setHorizontalAlignment(CENTER);
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
            if (value == null) {
                myPanel.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                return myPanel;
            }
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                super.setBackground(table.getSelectionBackground());
            }
            else {
                setForeground(table.getForeground());
                setBackground(table.getBackground());
            }
            setSelected((Boolean) value);
            return this;
        }
    }

    private class MyObjectRenderer extends DefaultTableCellRenderer {
        public MyObjectRenderer() {
            setUI(new RightAlignedLabelUI());
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
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ExcludeEntryDescription description = myExcludeEntryDescriptions.get(row);
            component.setForeground(!description.isValid() ? JBColor.RED : isSelected ? table.getSelectionForeground() : table.getForeground());
            component.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return component;
        }
    }
}
