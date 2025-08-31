// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.util.ui;

import consulo.util.lang.Couple;
import consulo.ui.ex.awt.JBUI;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

public class Centerizer extends JPanel {
  public enum TYPE {
    HORIZONTAL,
    VERTICAL,
    BOTH
  }

  private TYPE type;

  public Centerizer(@Nonnull JComponent comp) {
    this(comp, TYPE.BOTH);
  }

  public Centerizer(@Nonnull JComponent comp, @Nonnull TYPE type) {
    super(false);
    this.type = type;

    setOpaque(false);
    setBorder(JBUI.Borders.empty());
    setFocusable(false);

    add(comp);
  }

  @Nullable
  private Component getComponent() {
    if (getComponentCount() != 1) return null;
    return getComponent(0);
  }

  @Override
  public void doLayout() {
    Component c = getComponent();
    if (c == null) return;

    Dimension compSize = c.getPreferredSize();

    Dimension size = getSize();

    Couple<Integer> x = (type == TYPE.BOTH || type == TYPE.HORIZONTAL) ? getFit(compSize.width, size.width) : Couple.of(0, compSize.width);
    Couple<Integer> y = (type == TYPE.BOTH || type == TYPE.VERTICAL) ? getFit(compSize.height, size.height) : Couple.of(0, compSize.height);

    c.setBounds(x.first.intValue(), y.first.intValue(), x.second.intValue(), y.second.intValue());
  }

  private static Couple<Integer> getFit(int compSize, int containerSize) {
    if (compSize >= containerSize) {
      return Couple.of(0, compSize);
    }
    else {
      int position = containerSize / 2 - compSize / 2;
      return Couple.of(position, compSize);
    }
  }

  @Override
  public Dimension getPreferredSize() {
    return getComponent() != null ? getComponent().getPreferredSize() : super.getPreferredSize();
  }

  @Override
  public Dimension getMinimumSize() {
    return getComponent() != null ? getComponent().getMinimumSize() : super.getPreferredSize();
  }

  @Override
  public Dimension getMaximumSize() {
    return getComponent() != null ? getComponent().getMaximumSize() : super.getPreferredSize();
  }
}
