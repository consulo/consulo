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
package com.intellij.ui;

import com.intellij.openapi.util.Pair;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class TableExpandableItemsHandler extends AbstractExpandableItemsHandler<TableCell, JTable> {
  protected TableExpandableItemsHandler(final JTable table) {
    super(table);

    final ListSelectionListener selectionListener = new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        updateSelection(table);
      }
    };
    table.getSelectionModel().addListSelectionListener(selectionListener);
    table.getColumnModel().getSelectionModel().addListSelectionListener(selectionListener);

    table.addPropertyChangeListener("selectionModel", new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getOldValue() != null) {
          ((ListSelectionModel)evt.getOldValue()).removeListSelectionListener(selectionListener);
        }
        if (evt.getNewValue() != null) {
          ((ListSelectionModel)evt.getNewValue()).addListSelectionListener(selectionListener);
        }
      }
    });
    table.addPropertyChangeListener("columnModel", new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getOldValue() != null) {
          ((TableColumnModel)evt.getOldValue()).getSelectionModel().removeListSelectionListener(selectionListener);
        }
        if (evt.getNewValue() != null) {
          ((TableColumnModel)evt.getNewValue()).getSelectionModel().addListSelectionListener(selectionListener);
        }
      }
    });

    final TableModelListener modelListener = new TableModelListener() {
      @Override
      public void tableChanged(final TableModelEvent e) {
        // This method may be called from TableModelListener (before selection model update)
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4730055
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            updateSelection(table);
          }
        });
      }
    };

    if (table.getModel() != null) table.getModel().addTableModelListener(modelListener);
    table.addPropertyChangeListener("model", new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        updateSelection(table);

        if (evt.getOldValue() != null) {
          ((TableModel)evt.getOldValue()).removeTableModelListener(modelListener);
        }
        if (evt.getNewValue() != null) {
          ((TableModel)evt.getNewValue()).addTableModelListener(modelListener);
        }
      }
    });
  }

  private void updateSelection(JTable table) {
    int row = table.getSelectedRowCount() == 1 ? table.getSelectedRow() : -1;
    int column = table.getSelectedColumnCount() == 1 ? table.getSelectedColumn() : -1;
    handleSelectionChange((row == -1 || column == -1) ? null : new TableCell(row, column));
  }

  private Rectangle getCellRect(TableCell tableCellKey) {
    return myComponent.getCellRect(tableCellKey.row, tableCellKey.column, false);
  }

  public Pair<Component, Rectangle> getCellRendererAndBounds(TableCell key) {
    Rectangle cellRect = getCellRect(key);

    int modelColumnIndex = myComponent.convertColumnIndexToModel(key.column);
    final int modelRowIndex = myComponent.convertRowIndexToModel(key.row);
    TableModel model = myComponent.getModel();
    if (key.row < 0 || key.row >= model.getRowCount() || key.column < 0 || key.column >= model.getColumnCount()) {
      return null;
    }

    Component renderer = myComponent
      .getCellRenderer(key.row, key.column)
      .getTableCellRendererComponent(myComponent,
                                     model.getValueAt(modelRowIndex, modelColumnIndex),
                                     myComponent.getSelectionModel().isSelectedIndex(key.row),
                                     myComponent.hasFocus(),
                                     key.row, key.column);
    cellRect.width = renderer.getPreferredSize().width;

    return Pair.create(renderer, cellRect);
  }

  public Rectangle getVisibleRect(TableCell key) {
    Rectangle columnVisibleRect = myComponent.getVisibleRect();
    Rectangle cellRect = getCellRect(key);
    int visibleRight = Math.min(columnVisibleRect.x + columnVisibleRect.width, cellRect.x + cellRect.width);
    columnVisibleRect.x = Math.min(columnVisibleRect.x, cellRect.x);
    columnVisibleRect.width = Math.max(0, visibleRight - columnVisibleRect.x);
    return columnVisibleRect;
  }

  public TableCell getCellKeyForPoint(Point point) {
    int rowIndex = myComponent.rowAtPoint(point);
    if (rowIndex == -1) {
      return null;
    }

    int columnIndex = myComponent.columnAtPoint(point); // column index in visible coordinates
    if (columnIndex == -1) {
      return null;
    }

    return new TableCell(rowIndex, columnIndex);
  }
}
