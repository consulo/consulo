/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.template.impl;

import consulo.application.HelpManager;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.template.Variable;
import consulo.language.editor.template.context.TemplateContextType;
import consulo.language.editor.template.macro.Macro;
import consulo.language.editor.template.macro.MacroFactory;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.EditableModel;
import consulo.ui.ex.awt.ToolbarDecorator;
import consulo.ui.ex.awt.table.JBTable;
import consulo.undoRedo.CommandProcessor;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class EditVariableDialog extends DialogWrapper {
    private List<Variable> myVariables = new ArrayList<>();

    private JTable myTable;
    private Editor myEditor;
    private final List<TemplateContextType> myContextTypes;

    public EditVariableDialog(Editor editor, Component parent, ArrayList<Variable> variables, List<TemplateContextType> contextTypes) {
        super(parent, true);
        myContextTypes = contextTypes;
        myVariables = variables;
        myEditor = editor;
        init();
        setTitle(CodeInsightLocalize.templatesDialogEditVariablesTitle());
        setOKButtonText(CommonLocalize.buttonOk());
    }

    @Nonnull
    @Override
    protected Action[] createActions() {
        return new Action[]{getOKAction(), getCancelAction(), getHelpAction()};
    }

    @Override
    @RequiredUIAccess
    protected void doHelpAction() {
        HelpManager.getInstance().invokeHelp("editing.templates.defineTemplates.editTemplVars");
    }

    @Override
    protected String getDimensionServiceKey() {
        return "#consulo.ide.impl.idea.codeInsight.template.impl.EditVariableDialog";
    }

    @Override
    @RequiredUIAccess
    public JComponent getPreferredFocusedComponent() {
        return myTable;
    }

    @Override
    protected JComponent createCenterPanel() {
        return createVariablesTable();
    }

    private JComponent createVariablesTable() {
        final String[] names = {
            CodeInsightLocalize.templatesDialogEditVariablesTableColumnName().get(),
            CodeInsightLocalize.templatesDialogEditVariablesTableColumnExpression().get(),
            CodeInsightLocalize.templatesDialogEditVariablesTableColumnDefaultValue().get(),
            CodeInsightLocalize.templatesDialogEditVariablesTableColumnSkipIfDefined().get()
        };

        // Create a model of the data.
        TableModel dataModel = new VariablesModel(names);

        // Create the table
        myTable = new JBTable(dataModel);
        myTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        myTable.setPreferredScrollableViewportSize(new Dimension(500, myTable.getRowHeight() * 8));
        myTable.getColumn(names[0]).setPreferredWidth(120);
        myTable.getColumn(names[1]).setPreferredWidth(200);
        myTable.getColumn(names[2]).setPreferredWidth(200);
        myTable.getColumn(names[3]).setPreferredWidth(100);
        if (myVariables.size() > 0) {
            myTable.getSelectionModel().setSelectionInterval(0, 0);
        }

        JComboBox comboField = new JComboBox();
        Macro[] macros = MacroFactory.getMacros();
        Arrays.sort(macros, (m1, m2) -> m1.getPresentableName().compareTo(m2.getPresentableName()));
        eachMacro:
        for (Macro macro : macros) {
            for (TemplateContextType contextType : myContextTypes) {
                if (macro.isAcceptableInContext(contextType)) {
                    comboField.addItem(macro.getPresentableName());
                    continue eachMacro;
                }
            }
        }
        comboField.setEditable(true);
        DefaultCellEditor cellEditor = new DefaultCellEditor(comboField);
        cellEditor.setClickCountToStart(1);
        myTable.getColumn(names[1]).setCellEditor(cellEditor);
        myTable.setRowHeight(comboField.getPreferredSize().height);

        JTextField textField = new JTextField();

        /*textField.addMouseListener(
            new PopupHandler(){
                public void invokePopup(Component comp,int x,int y){
                    showCellPopup((JTextField)comp,x,y);
                }
            }
        );*/

        cellEditor = new DefaultCellEditor(textField);
        cellEditor.setClickCountToStart(1);
        myTable.setDefaultEditor(String.class, cellEditor);

        final ToolbarDecorator decorator = ToolbarDecorator.createDecorator(myTable).disableAddAction().disableRemoveAction();
        return decorator.createPanel();
    }

    @Override
    protected void doOKAction() {
        if (myTable.isEditing()) {
            TableCellEditor editor = myTable.getCellEditor();
            if (editor != null) {
                editor.stopCellEditing();
            }
        }
        super.doOKAction();
    }

    /*private void showCellPopup(final JTextField field,int x,int y) {
        JPopupMenu menu = new JPopupMenu();
        final Macro[] macros = MacroFactory.getMacros();
        for (int i = 0; i < macros.length; i++) {
            final Macro macro = macros[i];
            JMenuItem item = new JMenuItem(macro.getName());
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        field.saveToString().insertString(field.getCaretPosition(), macro.getName() + "()", null);
                    }
                    catch (BadLocationException e1) {
                        LOG.error(e1);
                    }
                }
            });
            menu.add(item);
        }
        menu.show(field, x, y);
    }*/

    @RequiredUIAccess
    private void updateTemplateTextByVarNameChange(final Variable oldVar, final Variable newVar) {
        final Document document = myEditor.getDocument();
        CommandProcessor.getInstance().newCommand()
            .document(document)
            .inWriteAction()
            .run(() -> {
                String templateText = document.getText();
                templateText = templateText.replaceAll("\\$" + oldVar.getName() + "\\$", "\\$" + newVar.getName() + "\\$");
                document.replaceString(0, document.getTextLength(), templateText);
            });
    }

    private class VariablesModel extends AbstractTableModel implements EditableModel {
        private final String[] myNames;

        public VariablesModel(String[] names) {
            myNames = names;
        }

        @Override
        public int getColumnCount() {
            return myNames.length;
        }

        @Override
        public int getRowCount() {
            return myVariables.size();
        }

        @Override
        public Object getValueAt(int row, int col) {
            Variable variable = myVariables.get(row);
            if (col == 0) {
                return variable.getName();
            }
            if (col == 1) {
                return variable.getExpressionString();
            }
            if (col == 2) {
                return variable.getDefaultValueString();
            }
            else {
                return variable.isAlwaysStopAt() ? Boolean.FALSE : Boolean.TRUE;
            }
        }

        @Override
        public String getColumnName(int column) {
            return myNames[column];
        }

        @Override
        public Class getColumnClass(int c) {
            if (c <= 2) {
                return String.class;
            }
            else {
                return Boolean.class;
            }
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return true;
        }

        @Override
        @RequiredUIAccess
        public void setValueAt(Object aValue, int row, int col) {
            Variable variable = myVariables.get(row);
            if (col == 0) {
                String varName = (String)aValue;
                Variable newVar = new Variable(
                    varName,
                    variable.getExpressionString(),
                    variable.getDefaultValueString(),
                    variable.isAlwaysStopAt()
                );
                myVariables.set(row, newVar);
                updateTemplateTextByVarNameChange(variable, newVar);
            }
            else if (col == 1) {
                variable.setExpressionString((String)aValue);
            }
            else if (col == 2) {
                variable.setDefaultValueString((String)aValue);
            }
            else {
                variable.setAlwaysStopAt(!(Boolean)aValue);
            }
        }

        @Override
        public void addRow() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeRow(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void exchangeRows(int oldIndex, int newIndex) {
            Collections.swap(myVariables, oldIndex, newIndex);
        }

        @Override
        public boolean canExchangeRows(int oldIndex, int newIndex) {
            return true;
        }
    }
}
