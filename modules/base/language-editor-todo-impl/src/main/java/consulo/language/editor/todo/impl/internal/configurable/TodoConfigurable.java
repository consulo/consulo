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
package consulo.language.editor.todo.impl.internal.configurable;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.*;
import consulo.language.editor.todo.TodoAttributesUtil;
import consulo.language.editor.todo.TodoConfiguration;
import consulo.language.editor.todo.TodoFilter;
import consulo.language.editor.todo.impl.internal.localize.LanguageTodoLocalize;
import consulo.language.psi.search.TodoPattern;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.PanelWithButtons;
import consulo.ui.ex.awt.ToolbarDecorator;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.table.JBTable;
import consulo.ui.ex.awt.util.TableUtil;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vladimir Kondratyev
 */
@ExtensionImpl
public class TodoConfigurable implements SearchableConfigurable, Configurable.NoScroll, ApplicationConfigurable {
    /*
     * UI resources
     */
    private JPanel myPanel;
    private JBTable myPatternsTable;
    private JBTable myFiltersTable;
    private final List<TodoPattern> myPatterns;
    private final PatternsTableModel myPatternsModel;
    private final List<TodoFilter> myFilters;
    private final FiltersTableModel myFiltersModel;

    /**
     * Invoked by reflection
     */
    public TodoConfigurable() {
        myPatterns = new ArrayList<>();
        myFilters = new ArrayList<>();
        myFiltersModel = new FiltersTableModel(myFilters);
        myPatternsModel = new PatternsTableModel(myPatterns);
    }

    private boolean arePatternsModified() {
        TodoConfiguration todoConfiguration = TodoConfiguration.getInstance();
        TodoPattern[] initialPatterns = todoConfiguration.getTodoPatterns();
        if (initialPatterns.length != myPatterns.size()) {
            return true;
        }
        for (TodoPattern initialPattern : initialPatterns) {
            if (!myPatterns.contains(initialPattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean areFiltersModified() {
        TodoConfiguration todoConfiguration = TodoConfiguration.getInstance();
        TodoFilter[] initialFilters = todoConfiguration.getTodoFilters();
        if (initialFilters.length != myFilters.size()) {
            return true;
        }
        for (TodoFilter initialFilter : initialFilters) {
            if (!myFilters.contains(initialFilter)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isModified() {
        // This method is always invoked before close configuration dialog or leave "ToDo" page.
        // So it's a good place to commit all changes.
        stopEditing();
        return arePatternsModified() || areFiltersModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        stopEditing();
        if (arePatternsModified()) {
            TodoPattern[] patterns = myPatterns.toArray(new TodoPattern[myPatterns.size()]);
            TodoConfiguration.getInstance().setTodoPatterns(patterns);
        }
        if (areFiltersModified()) {
            TodoFilter[] filters = myFilters.toArray(new TodoFilter[myFilters.size()]);
            TodoConfiguration.getInstance().setTodoFilters(filters);
        }
    }

    @Override
    public void disposeUIResources() {
        myPanel = null;
        myPatternsModel.removeTableModelListener(myPatternsTable);
        myPatternsTable = null;

        myFiltersModel.removeTableModelListener(myFiltersTable);
        myFiltersTable = null;
    }

    @Override
    public JComponent createComponent() {
        // Panel with patterns
        PanelWithButtons patternsPanel = new PanelWithButtons() {
            {
                initPanel();
            }

            @Override
            protected String getLabelText() {
                return LanguageTodoLocalize.labelTodoPatterns().get();
            }

            @Override
            protected JComponent createMainComponent() {
                // JTable with TodoPaterns
                myPatternsTable = new JBTable(myPatternsModel);
                myPatternsTable.getEmptyText().setText(LanguageTodoLocalize.textTodoNoPatterns());
                myPatternsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

                // Column "Icon"
                JComboBox<Image> todoTypeCombo = new JComboBox<>(new Image[]{
                    PlatformIconGroup.generalTododefault(),
                    PlatformIconGroup.generalTodoquestion(),
                    PlatformIconGroup.generalTodoimportant()
                });
                todoTypeCombo.setRenderer(new TodoTypeListCellRenderer());
                TableColumn typeColumn = myPatternsTable.getColumnModel().getColumn(0);
                DefaultCellEditor todoTypeEditor = new DefaultCellEditor(todoTypeCombo);
                todoTypeEditor.setClickCountToStart(1);
                typeColumn.setCellEditor(todoTypeEditor);
                TodoTypeTableCellRenderer todoTypeRenderer = new TodoTypeTableCellRenderer();
                typeColumn.setCellRenderer(todoTypeRenderer);

                int width = myPatternsTable.getFontMetrics(myPatternsTable.getFont()).stringWidth(myPatternsTable.getColumnName(0)) + 10;
                typeColumn.setPreferredWidth(width);
                typeColumn.setMaxWidth(width);
                typeColumn.setMinWidth(width);

                // Column "Case Sensitive"
                TableColumn todoCaseSensitiveColumn = myPatternsTable.getColumnModel().getColumn(1);
                width = myPatternsTable.getFontMetrics(myPatternsTable.getFont()).stringWidth(myPatternsTable.getColumnName(1)) + 10;
                todoCaseSensitiveColumn.setPreferredWidth(width);
                todoCaseSensitiveColumn.setMaxWidth(width);
                todoCaseSensitiveColumn.setMinWidth(width);

                // Column "Pattern"
                TodoPatternTableCellRenderer todoPatternRenderer = new TodoPatternTableCellRenderer(myPatterns);
                TableColumn patternColumn = myPatternsTable.getColumnModel().getColumn(2);
                patternColumn.setCellRenderer(todoPatternRenderer);

                ((DefaultCellEditor) myPatternsTable.getDefaultEditor(String.class)).setClickCountToStart(2);

                JPanel panel = ToolbarDecorator.createDecorator(myPatternsTable)
                    .setAddAction(button -> {
                        stopEditing();
                        TodoPattern pattern = new TodoPattern(TodoAttributesUtil.createDefault());
                        PatternDialog dialog = new PatternDialog(myPanel, pattern);
                        dialog.setTitle(LanguageTodoLocalize.titleAddTodoPattern());
                        dialog.show();
                        if (!dialog.isOK()) {
                            return;
                        }
                        myPatterns.add(pattern);
                        int index = myPatterns.size() - 1;
                        myPatternsModel.fireTableRowsInserted(index, index);
                        myPatternsTable.getSelectionModel().setSelectionInterval(index, index);
                        myPatternsTable.scrollRectToVisible(myPatternsTable.getCellRect(index, 0, true));
                    })
                    .setEditAction(button -> editSelectedPattern())
                    .setRemoveAction(button -> {
                        stopEditing();
                        int selectedIndex = myPatternsTable.getSelectedRow();
                        if (selectedIndex < 0 || selectedIndex >= myPatternsModel.getRowCount()) {
                            return;
                        }
                        TodoPattern patternToBeRemoved = myPatterns.get(selectedIndex);
                        TableUtil.removeSelectedItems(myPatternsTable);
                        for (int i = 0; i < myFilters.size(); i++) {
                            TodoFilter filter = myFilters.get(i);
                            if (filter.contains(patternToBeRemoved)) {
                                filter.removeTodoPattern(patternToBeRemoved);
                                myFiltersModel.fireTableRowsUpdated(i, i);
                            }
                        }
                    })
                    .disableUpDownActions()
                    .createPanel();

                return panel;
            }

            @Override
            protected JButton[] createButtons() {
                return new JButton[]{};
            }
        };

        // double click in "Patterns" table should also start editing of selected pattern
        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(MouseEvent e) {
                editSelectedPattern();
                return true;
            }
        }.installOn(myPatternsTable);

        // Panel with filters
        PanelWithButtons filtersPanel = new PanelWithButtons() {
            {
                initPanel();
            }

            @Override
            protected String getLabelText() {
                return LanguageTodoLocalize.labelTodoFilters().get();
            }

            @Override
            protected JComponent createMainComponent() {
                myFiltersTable = new JBTable(myFiltersModel);
                myFiltersTable.getEmptyText().setText(LanguageTodoLocalize.textTodoNoFilters());
                myFiltersTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

                // Column "Name"

                TableColumn nameColumn = myFiltersTable.getColumnModel().getColumn(0);
                int width = myPatternsTable.getColumnModel().getColumn(0).getPreferredWidth()/*typeColumn*/ +
                    myPatternsTable.getColumnModel().getColumn(1).getPreferredWidth()/*todoCaseSensitiveColumn*/;
                nameColumn.setPreferredWidth(width);
                nameColumn.setMaxWidth(width);
                nameColumn.setMinWidth(width);
                nameColumn.setCellRenderer(new MyFilterNameTableCellRenderer());

                JPanel panel = ToolbarDecorator.createDecorator(myFiltersTable)
                    .setAddAction(button -> {
                        stopEditing();
                        TodoFilter filter = new TodoFilter();
                        FilterDialog dialog = new FilterDialog(myPanel, filter, -1, myFilters, myPatterns);
                        dialog.setTitle(LanguageTodoLocalize.titleAddTodoFilter());
                        dialog.show();
                        int exitCode = dialog.getExitCode();
                        if (DialogWrapper.OK_EXIT_CODE == exitCode) {
                            myFilters.add(filter);
                            int index = myFilters.size() - 1;
                            myFiltersModel.fireTableRowsInserted(index, index);
                            myFiltersTable.getSelectionModel().setSelectionInterval(index, index);
                            myFiltersTable.scrollRectToVisible(myFiltersTable.getCellRect(index, 0, true));
                        }
                    })
                    .setEditAction(button -> editSelectedFilter())
                    .setRemoveAction(button -> {
                        stopEditing();
                        TableUtil.removeSelectedItems(myFiltersTable);
                    })
                    .disableUpDownActions()
                    .createPanel();

                return panel;
            }

            @Override
            protected JButton[] createButtons() {
                return new JButton[]{};
            }
        };
        // double click in "Filters" table should also start editing of selected filter
        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(MouseEvent e) {
                editSelectedFilter();
                return true;
            }
        }.installOn(myFiltersTable);

        myPanel = new JPanel(new GridBagLayout());
        myPanel.add(patternsPanel, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0
        ));
        myPanel.add(filtersPanel, new GridBagConstraints(0, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0
        ));

        return myPanel;
    }

    @RequiredUIAccess
    private void editSelectedPattern() {
        stopEditing();
        int selectedIndex = myPatternsTable.getSelectedRow();
        if (selectedIndex < 0 || selectedIndex >= myPatternsModel.getRowCount()) {
            return;
        }
        TodoPattern sourcePattern = myPatterns.get(selectedIndex);
        TodoPattern pattern = sourcePattern.clone();
        PatternDialog dialog = new PatternDialog(myPanel, pattern);
        dialog.setTitle(LanguageTodoLocalize.titleEditTodoPattern());
        dialog.show();
        if (!dialog.isOK()) {
            return;
        }
        myPatterns.set(selectedIndex, pattern);
        myPatternsModel.fireTableRowsUpdated(selectedIndex, selectedIndex);
        myPatternsTable.getSelectionModel().setSelectionInterval(selectedIndex, selectedIndex);
        // Update model with patterns
        for (int i = 0; i < myFilters.size(); i++) {
            TodoFilter filter = myFilters.get(i);
            if (filter.contains(sourcePattern)) {
                filter.removeTodoPattern(sourcePattern);
                filter.addTodoPattern(pattern);
                myFiltersModel.fireTableRowsUpdated(i, i);
            }
        }
    }

    @RequiredUIAccess
    private void editSelectedFilter() {
        stopEditing();
        int selectedIndex = myFiltersTable.getSelectedRow();
        if (selectedIndex < 0 || selectedIndex >= myFiltersModel.getRowCount()) {
            return;
        }
        TodoFilter sourceFilter = myFilters.get(selectedIndex);
        TodoFilter filter = sourceFilter.clone();
        FilterDialog dialog = new FilterDialog(myPanel, filter, selectedIndex, myFilters, myPatterns);
        dialog.setTitle(LanguageTodoLocalize.titleEditTodoFilter());
        dialog.show();
        int exitCode = dialog.getExitCode();
        if (DialogWrapper.OK_EXIT_CODE == exitCode) {
            myFilters.set(selectedIndex, filter);
            myFiltersModel.fireTableRowsUpdated(selectedIndex, selectedIndex);
            myFiltersTable.getSelectionModel().setSelectionInterval(selectedIndex, selectedIndex);
        }
    }

    private void stopEditing() {
        if (myPatternsTable.isEditing()) {
            TableCellEditor editor = myPatternsTable.getCellEditor();
            if (editor != null) {
                editor.stopCellEditing();
            }
        }
        if (myFiltersTable.isEditing()) {
            TableCellEditor editor = myFiltersTable.getCellEditor();
            if (editor != null) {
                editor.stopCellEditing();
            }
        }
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return LanguageTodoLocalize.titleTodo();
    }

    @Override
    @RequiredUIAccess
    public void reset() {
        // Patterns
        myPatterns.clear();
        TodoConfiguration todoConfiguration = TodoConfiguration.getInstance();
        TodoPattern[] patterns = todoConfiguration.getTodoPatterns();
        for (TodoPattern pattern : patterns) {
            myPatterns.add(pattern.clone());
        }
        myPatternsModel.fireTableDataChanged();
        // Filters
        myFilters.clear();
        TodoFilter[] filters = todoConfiguration.getTodoFilters();
        for (TodoFilter filter : filters) {
            myFilters.add(filter.clone());
        }
        myFiltersModel.fireTableDataChanged();
    }

    private final class MyFilterNameTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column
        ) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            TodoFilter filter = myFilters.get(row);
            if (isSelected) {
                setForeground(UIUtil.getTableSelectionForeground());
            }
            else {
                if (filter.isEmpty()) {
                    setForeground(JBColor.RED);
                }
                else {
                    setForeground(UIUtil.getTableForeground());
                }
            }
            return this;
        }
    }

    @Override
    @Nonnull
    public String getId() {
        return "preferences.toDoOptions";
    }

    @Nullable
    @Override
    public String getParentId() {
        return StandardConfigurableIds.EDITOR_GROUP;
    }
}
