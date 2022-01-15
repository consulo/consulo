// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.hover;

import com.intellij.ui.render.RenderingUtil;
import consulo.util.dataholder.Key;
import javax.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToIntFunction;

//@ApiStatus.Experimental
public abstract class TableHoverListener extends HoverListener {
  public abstract void onHover(@Nonnull JTable table, int row, int column);

  @Override
  public final void mouseEntered(@Nonnull Component component, int x, int y) {
    mouseMoved(component, x, y);
  }

  @Override
  public final void mouseMoved(@Nonnull Component component, int x, int y) {
    update(component, table -> table.rowAtPoint(new Point(x, y)), table -> table.columnAtPoint(new Point(x, y)));
  }

  @Override
  public final void mouseExited(@Nonnull Component component) {
    update(component, table -> -1, table -> -1);
  }


  private final AtomicInteger rowHolder = new AtomicInteger(-1);
  private final AtomicInteger columnHolder = new AtomicInteger(-1);

  private void update(@Nonnull Component component, @Nonnull ToIntFunction<JTable> rowFunc, @Nonnull ToIntFunction<JTable> columnFunc) {
    if (component instanceof JTable) {
      JTable table = (JTable)component;
      int rowNew = rowFunc.applyAsInt(table);
      int rowOld = rowHolder.getAndSet(rowNew);
      int columnNew = columnFunc.applyAsInt(table);
      int columnOld = columnHolder.getAndSet(columnNew);
      if (rowNew != rowOld || columnNew != columnOld) onHover(table, rowNew, columnNew);
    }
  }


  private static final Key<Integer> HOVERED_ROW_KEY = Key.create("TableHoveredRow");
  public static final HoverListener DEFAULT = new TableHoverListener() {
    @Override
    public void onHover(@Nonnull JTable table, int row, int column) {
      setHoveredRow(table, row);
      // support JBTreeTable and similar views
      Object property = table.getClientProperty(RenderingUtil.FOCUSABLE_SIBLING);
      if (property instanceof JTree) TreeHoverListener.setHoveredRow((JTree)property, row);
    }
  };

  //@ApiStatus.Internal
  static void setHoveredRow(@Nonnull JTable table, int rowNew) {
    int rowOld = getHoveredRow(table);
    if (rowNew == rowOld) return;
    table.putClientProperty(HOVERED_ROW_KEY, rowNew < 0 ? null : rowNew);
    if (RenderingUtil.isHoverPaintingDisabled(table)) return;
    repaintRow(table, rowOld, 0);
    repaintRow(table, rowNew, 0);
  }

  /**
   * @param table a table, which hover state is interesting
   * @return a number of a hovered row of the specified table
   * @see #DEFAULT
   */
  public static int getHoveredRow(@Nonnull JTable table) {
    Object property = table.getClientProperty(HOVERED_ROW_KEY);
    return property instanceof Integer ? (Integer)property : -1;
  }

  private static void repaintRow(@Nonnull JTable table, int row, int column) {
    Rectangle bounds = row < 0 ? null : table.getCellRect(row, column, false);
    if (bounds != null) table.repaint(0, bounds.y, table.getWidth(), bounds.height);
  }
}
