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
package com.intellij.ide.plugins;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.TableUtil;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.TextTransferable;
import com.intellij.xml.util.XmlStringUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.datatransfer.Transferable;

/**
 * @author stathik
 * @since 4:19:20 PM Dec 11, 2003
 */
public class PluginTable extends JBTable {
  public PluginTable(final PluginTableModel model) {
    super(model);
    setTableHeader(null);
    getColumnModel().setColumnMargin(0);
    for (int i = 0; i < model.getColumnCount(); i++) {
      TableColumn column = getColumnModel().getColumn(i);
      final ColumnInfo columnInfo = model.getColumnInfos()[i];
      column.setCellEditor(columnInfo.getEditor(null));
      if (columnInfo.getColumnClass() == Boolean.class) {                              
        TableUtil.setupCheckboxColumn(column);
      }
    }

    setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    setShowGrid(false);
    setTransferHandler(new TransferHandler() {
      @Nullable
      @Override
      protected Transferable createTransferable(JComponent c) {
        final PluginDescriptor[] selectedValues = getSelectedObjects();
        if (selectedValues == null) return null;
        final String text = StringUtil.join(selectedValues, PluginDescriptor::getName, ", ");
        final String htmlText = "<body>\n<ul>\n" + StringUtil.join(selectedValues, PluginDescriptor::getName, "</li>\n<li>") + "</ul>\n</body>\n";
        return new TextTransferable(XmlStringUtil.wrapInHtml(htmlText), text);
      }

      @Override
      public int getSourceActions(JComponent c) {
        return COPY;
      }
    });

    if (model.getColumnCount() > 1) {
      setColumnWidth(1, new JCheckBox().getPreferredSize().width + 4);
    }
  }

  public void setColumnWidth(final int columnIndex, final int width) {
    TableColumn column = getColumnModel().getColumn(columnIndex);
    column.setMinWidth(width);
    column.setMaxWidth(width);
  }

  @Override
  protected boolean isSortOnUpdates() {
    return false;
  }

  @Override
  public void setValueAt(final Object aValue, final int row, final int column) {
    super.setValueAt(aValue, row, column);
    repaint(); //in order to update invalid plugins
  }

  @Override
  public TableCellRenderer getCellRenderer(final int row, final int column) {
    final ColumnInfo columnInfo = ((PluginTableModel)getModel()).getColumnInfos()[column];
    return columnInfo.getRenderer(getObjectAt(row));
  }

  public Object[] getElements() {
    return ((PluginTableModel)getModel()).view.toArray();
  }

  public PluginDescriptor getObjectAt(int row) {
    return ((PluginTableModel)getModel()).getObjectAt(convertRowIndexToModel(row));
  }

  public void select(PluginId target) {
    PluginTableModel tableModel = (PluginTableModel)getModel();
    getSelectionModel().clearSelection();
    for (int i = 0; i < tableModel.getRowCount(); i++) {
      PluginDescriptor descriptorAt = tableModel.getObjectAt(i);
      if (target == descriptorAt.getPluginId()) {
        final int row = convertRowIndexToView(i);
        getSelectionModel().addSelectionInterval(row, row);
      }
    }
    TableUtil.scrollSelectionToVisible(this);
  }

  public PluginDescriptor getSelectedObject() {
    PluginDescriptor selected = null;
    if (getSelectedRowCount() > 0) {
      selected = getObjectAt(getSelectedRow());
    }
    return selected;
  }

  public PluginDescriptor[] getSelectedObjects() {
    PluginDescriptor[] selection = null;
    if (getSelectedRowCount() > 0) {
      int[] poses = getSelectedRows();
      selection = new PluginDescriptor[poses.length];
      for (int i = 0; i < poses.length; i++) {
        selection[i] = getObjectAt(poses[i]);
      }
    }
    return selection;
  }
}
