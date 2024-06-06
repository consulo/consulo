// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt.util;

import consulo.platform.Platform;
import consulo.ui.ex.ComponentWithExpandableItems;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

import static consulo.ui.ex.awt.JBScrollPane.IGNORE_SCROLLBAR_IN_INSETS;

public final class RenderingHelper {
  /**
   * This key can be set to a tree to resize renderer component if it exceed a visible area.
   *
   * @see JComponent#putClientProperty
   */
  public static final Key<Boolean> SHRINK_LONG_RENDERER = Key.create("SHRINK_LONG_RENDERER");

  private final Rectangle myViewBounds;
  private final int myHintIndex;
  private int myRightMargin;
  private boolean myShrinkingDisabled;

  public RenderingHelper(@Nonnull JComponent component) {
    myViewBounds = new Rectangle(component.getWidth(), component.getHeight());
    myHintIndex = getExpandableHintIndex(component);
    Container parent = component.getParent();
    if (parent instanceof JViewport) {
      myViewBounds.setBounds(-component.getX(), -component.getY(), parent.getWidth(), parent.getHeight());
      parent = parent.getParent();
      if (parent instanceof JScrollPane) {
        JScrollPane pane = (JScrollPane)parent;
        JScrollBar hsb = pane.getHorizontalScrollBar();
        if (hsb != null && hsb.isVisible()) {
          myShrinkingDisabled = isClientPropertyFalse(component, SHRINK_LONG_RENDERER, false);
        }
        JScrollBar vsb = pane.getVerticalScrollBar();
        if (vsb != null && vsb.isVisible() && !vsb.isOpaque() && isClientPropertyFalse(vsb, IGNORE_SCROLLBAR_IN_INSETS, Platform.current().os().isMac())) {
          myRightMargin = vsb.getWidth();
        }
      }
    }
  }

  public int getX() {
    return myViewBounds.x;
  }

  public int getY() {
    return myViewBounds.y;
  }

  public int getWidth() {
    return myViewBounds.width;
  }

  public int getHeight() {
    return myViewBounds.height;
  }

  public int getRightMargin() {
    return myRightMargin;
  }

  public boolean isRendererShrinkingDisabled(int index) {
    return myShrinkingDisabled || isExpandableHintShown(index);
  }

  public boolean isExpandableHintShown(int index) {
    return myHintIndex == index;
  }

  private static int getExpandableHintIndex(@Nonnull JComponent component) {
    if (component instanceof ComponentWithExpandableItems) {
      ComponentWithExpandableItems<?> c = (ComponentWithExpandableItems<?>)component;
      Collection<?> items = c.getExpandableItemsHandler().getExpandedItems();
      Object item = items.isEmpty() ? null : items.iterator().next();
      if (item instanceof Integer) return (Integer)item;
    }
    return -1;
  }

  private static boolean isClientPropertyFalse(@Nonnull JComponent component, @Nonnull Object key, boolean strict) {
    Object property = component.getClientProperty(key);
    return strict ? Boolean.FALSE.equals(property) : !Boolean.TRUE.equals(property);
  }
}
