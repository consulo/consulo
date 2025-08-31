/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.profile.codeInspection.ui.inspectionsTree;

import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.ide.impl.idea.profile.codeInspection.ui.SingleInspectionProfilePanel;
import consulo.ui.ex.awt.table.JBTable;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author Dmitry Batkovich
 */
public class ScopesAndSeveritiesHintTable extends JBTable {
  private final static int SCOPE_COLUMN = 0;
  private final static int SEVERITY_COLUMN = 1;

  public ScopesAndSeveritiesHintTable(LinkedHashMap<String, HighlightDisplayLevel> scopeToAverageSeverityMap, String defaultScopeName) {
    super(new MyModel(scopeToAverageSeverityMap, defaultScopeName));

    getColumnModel().getColumn(SCOPE_COLUMN).setCellRenderer(new DefaultTableCellRenderer() {
      @Override
      public Component getTableCellRendererComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     boolean hasFocus,
                                                     int row,
                                                     int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        setOpaque(false);
        UIUtil.applyStyle(UIUtil.ComponentStyle.SMALL, this);
        return this;
      }
    });

    getColumnModel().getColumn(SEVERITY_COLUMN).setCellRenderer(new DefaultTableCellRenderer() {
      @Override
      public Component getTableCellRendererComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     boolean hasFocus,
                                                     int row,
                                                     int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        HighlightDisplayLevel level = (HighlightDisplayLevel)value;
        setIcon(TargetAWT.to(level.getIcon()));
        setText(SingleInspectionProfilePanel.renderSeverity(level.getSeverity()));
        setOpaque(false);
        UIUtil.applyStyle(UIUtil.ComponentStyle.SMALL, this);
        return this;
      }
    });
    setShowGrid(false);
    setRowSelectionAllowed(false);
    setColumnSelectionAllowed(false);
    setOpaque(false);

    for (int i = 0; i < getColumnModel().getColumnCount(); i++) {
      int w = 0;
      TableColumn column = getColumnModel().getColumn(i);
      for (int j = 0; j < getModel().getRowCount(); j++) {
        Component component = prepareRenderer(column.getCellRenderer(), j, i);
        w = Math.max(component.getPreferredSize().width, w);
      }
      column.setPreferredWidth(w + 1);
    }
  }

  private final static class MyModel extends AbstractTableModel {

    private final LinkedHashMap<String, HighlightDisplayLevel> myScopeToAverageSeverityMap;
    private final String myDefaultScopeName;
    private final List<String> myScopes;

    public MyModel(LinkedHashMap<String, HighlightDisplayLevel> scopeToAverageSeverityMap, String defaultScopeName) {
      myScopeToAverageSeverityMap = scopeToAverageSeverityMap;
      myDefaultScopeName = defaultScopeName;
      myScopes = new ArrayList<String>(myScopeToAverageSeverityMap.keySet());
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      switch (columnIndex) {
        case SCOPE_COLUMN: return String.class;
        case SEVERITY_COLUMN: return HighlightDisplayLevel.class;
        default: throw new IllegalArgumentException();
      }
    }

    @Override
    public int getRowCount() {
      return myScopes.size();
    }

    @Override
    public int getColumnCount() {
      return 2;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      String scopeName = myScopes.get(rowIndex);
      switch (columnIndex) {
        case SCOPE_COLUMN:
          return myDefaultScopeName.equals(scopeName) ? "Everywhere else" : scopeName;
        case SEVERITY_COLUMN: return myScopeToAverageSeverityMap.get(scopeName);
        default: throw new IllegalArgumentException();
      }

    }
  }
}
