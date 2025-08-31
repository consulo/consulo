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

package consulo.language.editor.refactoring.rename;

import consulo.application.ApplicationManager;
import consulo.disposer.Disposer;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.ui.EnableDisableAction;
import consulo.language.editor.refactoring.ui.RefactoringUIUtil;
import consulo.language.editor.refactoring.ui.StringTableCellEditor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNamedElement;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.awt.BooleanTableCellRenderer;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.internal.GuiUtils;
import consulo.ui.ex.awt.table.JBTable;
import consulo.ui.ex.awt.util.TableUtil;
import consulo.usage.UsageInfo;
import consulo.usage.UsagePreviewPanel;
import consulo.usage.UsagePreviewPanelFactory;
import consulo.usage.UsageViewPresentation;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.VirtualFile;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;

/**
 * @author dsl
 */
public class AutomaticRenamingDialog extends DialogWrapper {
  private static final Logger LOG = Logger.getInstance(AutomaticRenamingDialog.class);
  private static final int CHECK_COLUMN = 0;
  private static final int OLD_NAME_COLUMN = 1;
  private static final int NEW_NAME_COLUMN = 2;
  private final AutomaticRenamer myRenamer;
  private boolean[] myShouldRename;
  private String[] myNewNames;
  private PsiNamedElement[] myRenames;
  private MyTableModel myTableModel;
  private JBTable myTable;
  private JPanel myPanelForPreview;
  private JButton mySelectAllButton;
  private JButton myUnselectAllButton;
  private JPanel myPanel;
  private JSplitPane mySplitPane;
  private final Project myProject;
  private final UsagePreviewPanel myUsagePreviewPanel;
  private final JLabel myUsageFileLabel;
  private ListSelectionListener myListSelectionListener;
                                                                                           
  public AutomaticRenamingDialog(Project project, AutomaticRenamer renamer) {
    super(project, true);
    myProject = project;
    myRenamer = renamer;
    myUsagePreviewPanel = UsagePreviewPanelFactory.getInstance().createPreviewPanel(myProject, new UsageViewPresentation());
    myUsageFileLabel = new JLabel();
    populateData();
    setTitle(myRenamer.getDialogTitle());
    init();
  }

  private void populateData() {
    Map<PsiNamedElement, String> renames = myRenamer.getRenames();

    List<PsiNamedElement> temp = new ArrayList<PsiNamedElement>();
    for (PsiNamedElement namedElement : renames.keySet()) {
      String newName = renames.get(namedElement);
      if (newName != null) temp.add(namedElement);
    }

    myRenames = temp.toArray(new PsiNamedElement[temp.size()]);
    Arrays.sort(myRenames, new Comparator<PsiNamedElement>() {
      @Override
      public int compare(PsiNamedElement e1, PsiNamedElement e2) {
        return Comparing.compare(e1.getName(), e2.getName());
      }
    });

    myNewNames = new String[myRenames.length];
    for (int i = 0; i < myNewNames.length; i++) {
      myNewNames[i] = renames.get(myRenames[i]);
    }

    myShouldRename = new boolean[myRenames.length];
    if (myRenamer.isSelectedByDefault()) {
      for (int i = 0; i < myShouldRename.length; i++) {
        myShouldRename [i] = true;
      }
    }
  }

  @Override
  protected String getDimensionServiceKey() {
    return "#consulo.ide.impl.idea.refactoring.rename.AutomaticRenamingDialog";
  }

  @Override
  protected JComponent createNorthPanel() {
    Box box = Box.createHorizontalBox();
    box.add(new JLabel(myRenamer.getDialogDescription()));
    box.add(Box.createHorizontalGlue());
    return box;
  }

  @Override
  public void show() {
    if (ApplicationManager.getApplication().isUnitTestMode()) return;
    super.show();
  }

  protected void handleChanges() {
    int selectedRow = myTable.getSelectedRow();
    if (selectedRow > -1) {
      boolean validName = RenameUtil.isValidName(myProject, myRenames[selectedRow], myNewNames[selectedRow]);
      getOKAction().setEnabled(validName);
      setErrorText(validName ? null : "Identifier \'" + myNewNames[selectedRow] + "\' is invalid");
    }
  }

  @Override
  protected JComponent createCenterPanel() {
    myTableModel = new MyTableModel();
    myTable.setModel(myTableModel);
    myTableModel.getSpaceAction().register();
    myTableModel.addTableModelListener(new TableModelListener() {
      @Override
      public void tableChanged(TableModelEvent e) {
        handleChanges();
      }
    });

    TableColumnModel columnModel = myTable.getColumnModel();
    columnModel.getColumn(CHECK_COLUMN).setCellRenderer(new BooleanTableCellRenderer());
    TableUtil.setupCheckboxColumn(myTable, CHECK_COLUMN);

    columnModel.getColumn(NEW_NAME_COLUMN).setCellEditor(new StringTableCellEditor(myProject));
    mySelectAllButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        for (int i = 0; i < myShouldRename.length; i++) {
          myShouldRename[i] = true;
        }
        fireDataChanged();
      }
    });

    myUnselectAllButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        for (int i = 0; i < myShouldRename.length; i++) {
          myShouldRename[i] = false;
        }
        fireDataChanged();
      }
    });
    myListSelectionListener = new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        myUsageFileLabel.setText("");
        int index = myTable.getSelectionModel().getLeadSelectionIndex();
        if (index != -1) {
          PsiNamedElement element = myRenames[index];
          UsageInfo usageInfo = new UsageInfo(element);
          myUsagePreviewPanel.updateLayout(Collections.singletonList(usageInfo));
          PsiFile containingFile = element.getContainingFile();
          if (containingFile != null) {
            VirtualFile virtualFile = containingFile.getVirtualFile();
            if (virtualFile != null) {
              myUsageFileLabel.setText(virtualFile.getName());
            }
          }
        }
        else {
          myUsagePreviewPanel.updateLayout(null);
        }
      }
    };
    myTable.getSelectionModel().addListSelectionListener(myListSelectionListener);

    myPanelForPreview.add(myUsagePreviewPanel.createComponent(), BorderLayout.CENTER);
    myUsagePreviewPanel.updateLayout(null);
    myPanelForPreview.add(myUsageFileLabel, BorderLayout.NORTH);
    mySplitPane.setDividerLocation(0.5);

    GuiUtils.replaceJSplitPaneWithIDEASplitter(myPanel);

    if (myTableModel.getRowCount() != 0) {
      myTable.getSelectionModel().addSelectionInterval(0,0);
    }
    return myPanel;
  }

  private void fireDataChanged() {
    int[] selectedRows = myTable.getSelectedRows();
    myTable.getSelectionModel().removeListSelectionListener(myListSelectionListener);

    myTableModel.fireTableDataChanged();
    for (int selectedRow : selectedRows) {
      myTable.addRowSelectionInterval(selectedRow, selectedRow);
    }
    myTable.getSelectionModel().addListSelectionListener(myListSelectionListener);
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myTable;
  }

  @Override
  protected void doOKAction() {
    TableUtil.stopEditing(myTable);
    updateRenamer();
    super.doOKAction();
  }

  @Override
  protected void dispose() {
    Disposer.dispose(myUsagePreviewPanel);
    super.dispose();
  }

  private void updateRenamer() {
    for (int i = 0; i < myRenames.length; i++) {
      PsiNamedElement element = myRenames[i];
      if (myShouldRename[i]) {
        myRenamer.setRename(element, myNewNames[i]);
      }
      else {
        myRenamer.doNotRename(element);
      }
    }
  }

  private void createUIComponents() {
    myTable = new JBTable();
    myTable.setRowHeight(myTable.getFontMetrics(UIManager.getFont("Table.font").deriveFont(Font.BOLD)).getHeight() + 4);
  }

  private class MyTableModel extends AbstractTableModel {
    @Override
    public int getColumnCount() {
      return 3;
    }

    @Override
    public int getRowCount() {
      return myShouldRename.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      switch(columnIndex) {
        case CHECK_COLUMN:
          return Boolean.valueOf(myShouldRename[rowIndex]);
        case OLD_NAME_COLUMN:
          return "<html>" + RefactoringUIUtil.getDescription(myRenames[rowIndex], true) + "</html>";
        case NEW_NAME_COLUMN:
          return myNewNames[rowIndex];
        default:
          LOG.assertTrue(false);
          return null;
      }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      switch(columnIndex) {
        case CHECK_COLUMN:
          myShouldRename[rowIndex] = ((Boolean)aValue).booleanValue();
          break;
        case NEW_NAME_COLUMN:
          myNewNames[rowIndex] = (String) aValue;
          break;
        default:
          LOG.assertTrue(false);
      }
      handleChanges();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return columnIndex != OLD_NAME_COLUMN;
    }

    @Override
    public Class getColumnClass(int columnIndex) {
      switch(columnIndex) {
        case CHECK_COLUMN: return Boolean.class;
        case OLD_NAME_COLUMN: return String.class;
        case NEW_NAME_COLUMN: return String.class;
        default: return null;
      }
    }

    @Override
    public String getColumnName(int column) {
      switch(column) {
        case OLD_NAME_COLUMN:
          return RefactoringLocalize.automaticRenamerEnityNameColumn(myRenamer.entityName()).get();
        case NEW_NAME_COLUMN:
          return RefactoringLocalize.automaticRenamerRenameToColumn().get();
        default:
          return " ";
      }
    }

    private MyEnableDisable getSpaceAction() {
      return this.new MyEnableDisable();
    }

    private class MyEnableDisable extends EnableDisableAction {
      @Override
      protected JTable getTable() {
        return myTable;
      }

      @Override
      protected boolean isRowChecked(int row) {
        return myShouldRename[row];
      }

      @Override
      protected void applyValue(int[] rows, boolean valueToBeSet) {
        for (int row : rows) {
          myShouldRename[row] = valueToBeSet;
        }
        fireDataChanged();
      }
    }
  }

}
