// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.tree.ui;

import com.intellij.ui.JBColor;
import consulo.util.dataholder.Key;
import javax.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

public interface Control {
  @Nonnull
  Icon getIcon(boolean expanded, boolean selected);

  int getWidth();

  int getHeight();

  void paint(@Nonnull Component c, @Nonnull Graphics g, int x, int y, int width, int height, boolean expanded, boolean selected);


  interface Painter {
    /**
     * This key is used to specify a custom tree control painter for a tree or a whole application.
     */
    Key<Painter> KEY = Key.create("tree control painter");

    Control.Painter DEFAULT = new ClassicPainter(null, null, null, null);
    Control.Painter COMPACT = new ClassicPainter(null, null, 0, null);

    JBColor LINE_COLOR = JBColor.namedColor("Tree.hash", new JBColor(0xE6E6E6, 0x505355));

    int getRendererOffset(@Nonnull Control control, int depth, boolean leaf);

    int getControlOffset(@Nonnull Control control, int depth, boolean leaf);

    void paint(@Nonnull Component c, @Nonnull Graphics g, int x, int y, int width, int height, @Nonnull Control control, int depth, boolean leaf, boolean expanded, boolean selected);
  }
}
