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
package consulo.ide.impl.idea.openapi.project.impl;

import consulo.project.ProjectBundle;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.table.JBTable;
import consulo.util.collection.primitive.objects.ObjectIntMap;
import consulo.util.collection.primitive.objects.ObjectMaps;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.File;

/**
 * @author Eugene Zhuravlev
 * @since 2004-12-03
 */
public class DefineMacrosDialog extends DialogWrapper{
  public static final int MACRO_NAME = 0;
  public static final int MACRO_VALUE = 1;
  private final String[][] myMacroTable;
  private final ObjectIntMap myIndex = ObjectMaps.newObjectIntHashMap();

  public DefineMacrosDialog(String[] macroNames) {
    super(true);
    myMacroTable = new String[macroNames.length][2];

    for (int idx = 0; idx < macroNames.length; idx++) {
      String macroName = macroNames[idx];
      myMacroTable[idx] = new String[]{macroName, ""};
      myIndex.putInt(macroName, idx);
    }
    setCancelButtonText(ProjectBundle.message("project.macros.cancel.button"));
    init();
  }

  protected void doOKAction() {
    for (int idx = 0; idx < myMacroTable.length; idx++) {
      String[] row = myMacroTable[idx];
      String path = row[MACRO_VALUE];

      if (path == null || path.length() == 0) {
        Messages.showErrorDialog(getContentPane(), ProjectBundle.message("project.macros.variable.missing.error", row[MACRO_NAME]),
                                 ProjectBundle.message("project.macros.variable.missing.title"));
        return;
      }
      if (!new File(path).exists()) {
        Messages.showErrorDialog(getContentPane(),
                                 ProjectBundle.message("project.macros.variable.missing.error", row[MACRO_NAME]),
                                 ProjectBundle.message("project.macros.variable.missing.title"));
        return;
      }
    }
    super.doOKAction();
  }

  @Nonnull
  protected Action[] createActions() {
    return new Action[]{getOKAction(), getCancelAction()};
  }

  protected JComponent createCenterPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    JBTable table = new JBTable(new MyTableModel());
    JLabel label = new JLabel(ProjectBundle.message("project.macros.prompt"));
    label.setBorder(IdeBorderFactory.createEmptyBorder(6, 6, 6, 6));
    panel.add(label, BorderLayout.NORTH);
    panel.add(ScrollPaneFactory.createScrollPane(table), BorderLayout.CENTER);
    return panel;
  }

  public String getMacroValue(String macro) {
    int index = myIndex.getInt(macro);
    return (index >= 0 && index < myMacroTable.length)? myMacroTable[index][MACRO_VALUE] : null;
  }

  private class MyTableModel extends AbstractTableModel {
    public String getColumnName(int column) {
      switch(column) {
        case MACRO_NAME : return ProjectBundle.message("project.macros.name.column");
        case MACRO_VALUE : return ProjectBundle.message("project.macros.path.column");
      }
      return "";
    }

    public int getColumnCount() {
      return myMacroTable[0].length;
    }

    public int getRowCount() {
      return myMacroTable.length;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
      return myMacroTable[rowIndex][columnIndex];
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      if (columnIndex == MACRO_VALUE && aValue instanceof String) {
        myMacroTable[rowIndex][MACRO_VALUE] = (String)aValue;
      }
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return columnIndex == MACRO_VALUE;
    }
  }

}
