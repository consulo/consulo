// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.tree.ui;

import com.intellij.ui.render.LabelBasedRenderer;
import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;
import consulo.ui.image.Image;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

final class LoadingNodeRenderer extends LabelBasedRenderer.Tree {
  static final TreeCellRenderer SHARED = new LoadingNodeRenderer();
  private static final Color COLOR = UIUtil.getInactiveTextColor();
  private static final Image ICON = Image.empty(8, 16);

  @Nonnull
  @Override
  public Component getTreeCellRendererComponent(@Nonnull JTree tree, @Nullable Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean focused) {
    Component component = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, focused);
    if (!selected) setForeground(COLOR);
    setIcon(TargetAWT.to(ICON));
    return component;
  }
}
