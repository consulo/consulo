// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.scroll;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.awt.event.MouseWheelEvent;

public final class SmoothScrollUtil {
  @Nullable
  public static JScrollBar getEventScrollBar(@Nonnull MouseWheelEvent e) {
    return isHorizontalScroll(e) ? getEventHorizontalScrollBar(e) : getEventVerticalScrollBar(e);
  }

  @Nullable
  public static JScrollBar getEventHorizontalScrollBar(@Nonnull MouseWheelEvent e) {
    JScrollPane scroller = (JScrollPane)e.getComponent();
    return scroller == null ? null : scroller.getHorizontalScrollBar();
  }

  @Nullable
  public static JScrollBar getEventVerticalScrollBar(@Nonnull MouseWheelEvent e) {
    JScrollPane scroller = (JScrollPane)e.getComponent();
    return scroller == null ? null : scroller.getVerticalScrollBar();
  }

  public static boolean isHorizontalScroll(@Nonnull MouseWheelEvent e) {
    return e.isShiftDown();
  }
}
