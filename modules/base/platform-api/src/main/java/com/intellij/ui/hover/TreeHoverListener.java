// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.hover;

import com.intellij.ui.render.RenderingUtil;
import com.intellij.util.ui.tree.TreeUtil;
import consulo.util.dataholder.Key;
import javax.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToIntFunction;

public abstract class TreeHoverListener extends HoverListener {
  public abstract void onHover(@Nonnull JTree tree, int row);

  @Override
  public final void mouseEntered(@Nonnull Component component, int x, int y) {
    mouseMoved(component, x, y);
  }

  @Override
  public final void mouseMoved(@Nonnull Component component, int x, int y) {
    update(component, tree -> TreeUtil.getRowForLocation(tree, x, y));
  }

  @Override
  public final void mouseExited(@Nonnull Component component) {
    update(component, tree -> -1);
  }


  private final AtomicInteger rowHolder = new AtomicInteger(-1);

  private void update(@Nonnull Component component, @Nonnull ToIntFunction<JTree> rowFunc) {
    if (component instanceof JTree) {
      JTree tree = (JTree)component;
      int rowNew = rowFunc.applyAsInt(tree);
      int rowOld = rowHolder.getAndSet(rowNew);
      if (rowNew != rowOld) onHover(tree, rowNew);
    }
  }


  private static final Key<Integer> HOVERED_ROW_KEY = Key.create("TreeHoveredRow");
  public static final HoverListener DEFAULT = new TreeHoverListener() {
    @Override
    public void onHover(@Nonnull JTree tree, int row) {
      setHoveredRow(tree, row);
      // support JBTreeTable and similar views
      Object property = tree.getClientProperty(RenderingUtil.FOCUSABLE_SIBLING);
      if (property instanceof JTable) TableHoverListener.setHoveredRow((JTable)property, row);
    }
  };

  //@ApiStatus.Internal
  static void setHoveredRow(@Nonnull JTree tree, int rowNew) {
    int rowOld = getHoveredRow(tree);
    if (rowNew == rowOld) return;
    tree.putClientProperty(HOVERED_ROW_KEY, rowNew < 0 ? null : rowNew);
    if (RenderingUtil.isHoverPaintingDisabled(tree)) return;
    TreeUtil.repaintRow(tree, rowOld);
    TreeUtil.repaintRow(tree, rowNew);
  }

  /**
   * @param tree a tree, which hover state is interesting
   * @return a number of a hovered row of the specified tree
   * @see #DEFAULT
   */
  public static int getHoveredRow(@Nonnull JTree tree) {
    Object property = tree.getClientProperty(HOVERED_ROW_KEY);
    return property instanceof Integer ? (Integer)property : -1;
  }
}
