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
package consulo.language.codeStyle.ui.internal.arrangement;

import consulo.language.codeStyle.arrangement.ArrangementColorsProvider;
import consulo.language.codeStyle.arrangement.group.ArrangementGroupingRule;
import consulo.language.codeStyle.arrangement.std.ArrangementSettingsToken;
import consulo.language.codeStyle.arrangement.std.ArrangementStandardSettingsManager;
import consulo.language.codeStyle.arrangement.std.CompositeArrangementSettingsToken;
import consulo.ui.ex.awt.AbstractTableCellEditor;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.table.JBTable;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Denis Zhdanov
 * @since 11/13/12 7:27 PM
 */
public class ArrangementGroupingRulesControl extends JBTable {

  @Nonnull
  public static final Key<ArrangementGroupingRulesControl> KEY = Key.create("Arrangement.Rule.Group.Control");

  @Nonnull
  private final Map<ArrangementSettingsToken, ArrangementGroupingComponent> myComponents = new HashMap<>();

  @Nonnull
  private final ArrangementStandardSettingsManager mySettingsManager;

  private int myRowUnderMouse = -1;

  public ArrangementGroupingRulesControl(@Nonnull ArrangementStandardSettingsManager settingsManager,
                                         @Nonnull ArrangementColorsProvider colorsProvider)
  {
    super(new DefaultTableModel(0, 1));
    mySettingsManager = settingsManager;
    setDefaultRenderer(Object.class, new MyRenderer());
    getColumnModel().getColumn(0).setCellEditor(new MyEditor());
    setShowColumns(false);
    setShowGrid(false);
    setBorder(IdeBorderFactory.createBorder());
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

    List<CompositeArrangementSettingsToken> groupingTokens = settingsManager.getSupportedGroupingTokens();
    if (groupingTokens != null) {
      for (CompositeArrangementSettingsToken token : groupingTokens) {
        ArrangementGroupingComponent component = new ArrangementGroupingComponent(token, colorsProvider, settingsManager);
        myComponents.put(token.getToken(), component);
        getModel().addRow(new Object[]{component});
      }
    }
  }

  @Override
  public DefaultTableModel getModel() {
    return (DefaultTableModel)super.getModel();
  }

  public void setRules(@Nullable List<ArrangementGroupingRule> rules) {
    for (ArrangementGroupingComponent component : myComponents.values()) {
      component.setSelected(false);
    }

    if (rules == null) {
      return;
    }

    DefaultTableModel model = getModel();
    while (model.getRowCount() > 0) {
      model.removeRow(model.getRowCount() - 1);
    }

    List<ArrangementSettingsToken> types = new ArrayList<>(myComponents.keySet());

    types = mySettingsManager.sort(types);
    for (ArrangementSettingsToken type : types) {
      model.addRow(new Object[]{myComponents.get(type)});
    }
    for (ArrangementGroupingRule rule : rules) {
      ArrangementGroupingComponent component = myComponents.get(rule.getGroupingType());
      component.setSelected(true);
      ArrangementSettingsToken orderType = rule.getOrderType();
      component.setOrderType(orderType);
    }
  }

  @Nonnull
  public List<ArrangementGroupingRule> getRules() {
    List<ArrangementGroupingRule> result = new ArrayList<ArrangementGroupingRule>();
    DefaultTableModel model = getModel();
    for (int i = 0, max = model.getRowCount(); i < max; i++) {
      ArrangementGroupingComponent component = (ArrangementGroupingComponent)model.getValueAt(i, 0);
      if (!component.isSelected()) {
        continue;
      }
      ArrangementSettingsToken orderType = component.getOrderType();
      if (orderType == null) {
        result.add(new ArrangementGroupingRule(component.getGroupingType()));
      }
      else {
        result.add(new ArrangementGroupingRule(component.getGroupingType(), orderType));
      }
    }
    return result;
  }

  @Override
  protected void processMouseMotionEvent(MouseEvent e) {
    if (e.getID() == MouseEvent.MOUSE_MOVED) {
      int oldRow = myRowUnderMouse;
      myRowUnderMouse = rowAtPoint(e.getPoint());
      if (oldRow >= 0 && myRowUnderMouse != oldRow) {
        getModel().fireTableRowsUpdated(oldRow, oldRow);
      }
      if (myRowUnderMouse >= 0 && myRowUnderMouse != oldRow) {
        getModel().fireTableRowsUpdated(myRowUnderMouse, myRowUnderMouse);
      }
    }
    super.processMouseMotionEvent(e);
  }

  @Override
  protected void processMouseEvent(MouseEvent e) {
    if (e.getID() == MouseEvent.MOUSE_EXITED && myRowUnderMouse >= 0) {
      int row = myRowUnderMouse;
      myRowUnderMouse = -1;
      getModel().fireTableRowsUpdated(row, row);
    }
    super.processMouseEvent(e);
  }

  @SuppressWarnings("ConstantConditions")
  private class MyRenderer implements TableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      if (value instanceof ArrangementGroupingComponent) {
        ArrangementGroupingComponent component = (ArrangementGroupingComponent)value;
        component.setRowIndex(row + 1);
        component.setHighlight(myRowUnderMouse == row || table.isRowSelected(row));
        return component;
      }
      else if (value instanceof ArrangementRepresentationAware) {
        return ((ArrangementRepresentationAware)value).getComponent();
      }
      return null;
    }
  }
  
  @SuppressWarnings("ConstantConditions")
  private static class MyEditor extends AbstractTableCellEditor {
    
    @Nullable Object myValue;
    
    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
      if (value instanceof ArrangementEditorAware) {
        myValue = value;
        return ((ArrangementEditorAware)value).getComponent();
      }
      return null;
    }

    @Override
    public Object getCellEditorValue() {
      return myValue;
    }

    @Override
    public boolean stopCellEditing() {
      super.stopCellEditing();
      myValue = null;
      return true;
    }
  }
}
