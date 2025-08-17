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

package consulo.ide.impl.idea.ui.tabs;

import consulo.language.editor.FileColorManager;
import consulo.project.Project;
import consulo.ui.ex.awt.EditableModel;
import consulo.ui.ex.awt.EmptyIcon;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.table.JBTable;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

/**
 * @author spleaner
 * @author Konstantin Bulenkov
 */
public abstract class FileColorSettingsTable extends JBTable {
    private static final int NAME_COLUMN = 0;
    private static final int COLOR_COLUMN = 1;

    private final List<FileColorConfiguration> myOriginal;
    @Nonnull
    private final Project myProject;
    @Nonnull
    private final FileColorManager myManager;

    public FileColorSettingsTable(@Nonnull Project project,
                                  @Nonnull FileColorManager manager,
                                  @Nonnull List<FileColorConfiguration> configurations) {
        super(new ModelAdapter(project, manager, copy(configurations)));
        myProject = project;
        myManager = manager;
        myOriginal = configurations;

        setAutoResizeMode(AUTO_RESIZE_LAST_COLUMN);

        TableColumnModel columnModel = getColumnModel();
        TableColumn nameColumn = columnModel.getColumn(NAME_COLUMN);
        nameColumn.setCellRenderer(new ScopeNameRenderer());

        TableColumn colorColumn = columnModel.getColumn(COLOR_COLUMN);
        colorColumn.setCellRenderer(new ColorCellRenderer(manager));
    }

    private static List<FileColorConfiguration> copy(@Nonnull List<FileColorConfiguration> configurations) {
        List<FileColorConfiguration> result = new ArrayList<>();
        for (FileColorConfiguration c : configurations) {
            try {
                result.add(c.clone());
            }
            catch (CloneNotSupportedException e) {
                assert false : "Should not happen!";
            }
        }

        return result;
    }

    protected abstract void apply(@Nonnull List<FileColorConfiguration> configurations);

    @Override
    public ModelAdapter getModel() {
        return (ModelAdapter) super.getModel();
    }

    @Override
    public boolean editCellAt(int row, int column, EventObject e) {
        if (e == null || (e instanceof MouseEvent && ((MouseEvent) e).getClickCount() == 1)) {
            return false;
        }
        Object at = getModel().getValueAt(row, column);
        FileColorConfigurationEditDialog dialog = new FileColorConfigurationEditDialog(myProject, myManager, ((FileColorConfiguration) at));
        dialog.getScopeComboBox().setEnabled(false);
        dialog.show();
        return false;
    }

    public boolean isModified() {
        List<FileColorConfiguration> current = getModel().getConfigurations();

        if (myOriginal.size() != current.size()) {
            return true;
        }

        for (int i = 0; i < current.size(); i++) {
            if (!myOriginal.get(i).equals(current.get(i))) {
                return true;
            }
        }

        return false;
    }

    public void reset() {
        getModel().setConfigurations(myOriginal);
    }

    public void performRemove() {
        int rowCount = getSelectedRowCount();
        if (rowCount > 0) {
            int[] rows = getSelectedRows();
            for (int i = rows.length - 1; i >= 0; i--) {
                removeConfiguration(rows[i]);
            }
        }
    }

    public void moveUp() {
        int rowCount = getSelectedRowCount();
        if (rowCount == 1) {
            int index = getModel().moveUp(getSelectedRows()[0]);
            if (index > -1) {
                getSelectionModel().setSelectionInterval(index, index);
            }
        }
    }

    public void moveDown() {
        int rowCount = getSelectedRowCount();
        if (rowCount == 1) {
            int index = getModel().moveDown(getSelectedRows()[0]);
            if (index > -1) {
                getSelectionModel().setSelectionInterval(index, index);
            }
        }
    }

    public void apply() {
        if (isModified()) {
            apply(getModel().getConfigurations());
        }
    }

    public FileColorConfiguration removeConfiguration(int index) {
        FileColorConfiguration removed = getModel().remove(index);

        int rowCount = getRowCount();
        if (rowCount > 0) {
            if (index > rowCount - 1) {
                getSelectionModel().setSelectionInterval(rowCount - 1, rowCount - 1);
            }
            else {
                getSelectionModel().setSelectionInterval(index, index);
            }
        }

        return removed;
    }

    public void addConfiguration(@Nonnull FileColorConfiguration configuration) {
        getModel().add(configuration);
    }

    private static class ModelAdapter extends AbstractTableModel implements EditableModel {
        private final Project myProject;
        private final FileColorManager myManager;
        private List<FileColorConfiguration> myConfigurations;

        private ModelAdapter(Project project, FileColorManager manager, List<FileColorConfiguration> configurations) {
            myProject = project;
            myManager = manager;
            myConfigurations = configurations;
        }

        @Override
        public String getColumnName(int column) {
            return column == NAME_COLUMN ? "Scope" : "Color";
        }

        @Override
        public int getRowCount() {
            return myConfigurations.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return myConfigurations.get(rowIndex);
        }

        @Nonnull
        public List<FileColorConfiguration> getConfigurations() {
            return myConfigurations;
        }

        public FileColorConfiguration remove(int index) {
            FileColorConfiguration removed = myConfigurations.remove(index);
            fireTableRowsDeleted(index, index);
            return removed;
        }

        public void add(@Nonnull FileColorConfiguration configuration) {
            myConfigurations.add(configuration);
            fireTableRowsInserted(myConfigurations.size() - 1, myConfigurations.size() - 1);
        }

        public void setConfigurations(List<FileColorConfiguration> original) {
            myConfigurations = copy(original);
            fireTableDataChanged();
        }

        public int moveUp(int index) {
            if (index > 0) {
                FileColorConfiguration configuration = myConfigurations.get(index);
                myConfigurations.remove(index);
                myConfigurations.add(index - 1, configuration);
                fireTableRowsUpdated(index - 1, index);
                return index - 1;
            }

            return -1;
        }

        public int moveDown(int index) {
            if (index < getRowCount() - 1) {
                FileColorConfiguration configuration = myConfigurations.get(index);
                myConfigurations.remove(index);
                myConfigurations.add(index + 1, configuration);
                fireTableRowsUpdated(index, index + 1);
                return index + 1;
            }

            return -1;
        }

        @Override
        public void addRow() {
            FileColorConfigurationEditDialog dialog = new FileColorConfigurationEditDialog(myProject, (FileColorManagerImpl) myManager, null);
            dialog.show();

            if (dialog.getExitCode() == 0) {
                myConfigurations.add(dialog.getConfiguration());
            }
        }

        @Override
        public void removeRow(int index) {
            myConfigurations.remove(index);
        }

        @Override
        public void exchangeRows(int oldIndex, int newIndex) {
            myConfigurations.add(newIndex, myConfigurations.remove(oldIndex));
            fireTableRowsUpdated(Math.min(oldIndex, newIndex), Math.max(oldIndex, newIndex));
        }

        @Override
        public boolean canExchangeRows(int oldIndex, int newIndex) {
            return true;
        }
    }

    private static class ScopeNameRenderer extends JLabel implements TableCellRenderer {
        private static final Border NO_FOCUS_BORDER = BorderFactory.createEmptyBorder(1, 1, 1, 1);

        private ScopeNameRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (!(value instanceof FileColorConfiguration)) {
                return this;
            }

            preinit(table, isSelected, hasFocus);

            FileColorConfiguration configuration = (FileColorConfiguration) value;
            setText(FileColorManagerImpl.getAlias(configuration.getScopeName()));
            return this;
        }

        protected void preinit(JTable table, boolean isSelected, boolean hasFocus) {
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());

            setBorder(hasFocus ? UIUtil.getTableFocusCellHighlightBorder()
                : NO_FOCUS_BORDER);
        }
    }

    private static class ColorCellRenderer extends ScopeNameRenderer {
        private Color myColor;
        private final FileColorManager myManager;

        private ColorCellRenderer(FileColorManager manager) {
            setOpaque(true);

            myManager = manager;

            setIcon(EmptyIcon.ICON_16);
        }

        private void setIconColor(Color color) {
            myColor = color;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (!(value instanceof FileColorConfiguration)) {
                return this;
            }

            preinit(table, isSelected, hasFocus);

            FileColorConfiguration configuration = (FileColorConfiguration) value;
            setIconColor(myManager.getColor(configuration.getColorName()));
            setText(FileColorManagerImpl.getAlias(configuration.getColorPresentableName()));

            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (myColor != null) {
                Icon icon = getIcon();
                int width = icon.getIconWidth();
                int height = icon.getIconHeight();

                Color old = g.getColor();

                g.setColor(myColor);
                g.fillRect(0, 0, width, height);

                g.setColor(old);
            }
        }
    }
}
