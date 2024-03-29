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
package consulo.ide.impl.idea.openapi.vcs.changes.issueLinks;

import consulo.ui.ex.awt.ColoredTableCellRenderer;
import consulo.ide.impl.idea.ui.dualView.DualView;
import consulo.ide.impl.idea.ui.dualView.TreeTableView;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * @author yole
 */
public class TableLinkMouseListener extends AbstractBaseTagMouseListener {
  @jakarta.annotation.Nullable
  public Object getTagAt(final MouseEvent e) {
    // TODO[yole]: don't update renderer on every event, like it's done in TreeLinkMouseListener
    Object tag = null;
    JTable table = (JTable)e.getSource();
    int row = table.rowAtPoint(e.getPoint());
    int column = table.columnAtPoint(e.getPoint());
    if (row == -1 || column == -1) return null;
    TableCellRenderer cellRenderer = table.getCellRenderer(row, column);
    if (cellRenderer instanceof DualView.TableCellRendererWrapper) {
      cellRenderer = ((DualView.TableCellRendererWrapper) cellRenderer).getRenderer();
    }
    if (cellRenderer instanceof TreeTableView.CellRendererWrapper) {
      cellRenderer = ((TreeTableView.CellRendererWrapper) cellRenderer).getBaseRenderer();
    }
    if (cellRenderer instanceof ColoredTableCellRenderer) {
      final ColoredTableCellRenderer renderer = (ColoredTableCellRenderer)cellRenderer;
      tag = forColoredRenderer(e, table, row, column, renderer);
    } else {
      tag = tryGetTag(e, table, row, column);
    }
    return tag;
  }

  protected Object tryGetTag(MouseEvent e, JTable table, int row, int column) {
    return null;
  }

  private Object forColoredRenderer(MouseEvent e, JTable table, int row, int column, ColoredTableCellRenderer renderer) {
    renderer.getTableCellRendererComponent(table, table.getValueAt(row, column), false, false, row, column);
    final Rectangle rc = table.getCellRect(row, column, false);
    int index = renderer.findFragmentAt(e.getPoint().x - rc.x);
    if (index >= 0) {
      return renderer.getFragmentTagAt(index);
    }
    return null;
  }
}