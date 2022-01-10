// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.scroll;

import javax.annotation.Nonnull;

import java.awt.event.MouseWheelEvent;

public final class TouchScrollUtil {
  private static final int TOUCH_BEGIN = 2;
  private static final int TOUCH_UPDATE = 3;
  private static final int TOUCH_END = 4;

  public static boolean isTouchScroll(@Nonnull MouseWheelEvent e) {
    return e.getScrollType() >= TOUCH_BEGIN && e.getScrollType() <= TOUCH_END;
  }

  public static double getDelta(@Nonnull MouseWheelEvent e) {
    return e.getPreciseWheelRotation() * e.getScrollAmount();
  }

  public static boolean isBegin(@Nonnull MouseWheelEvent e) {
    return e.getScrollType() == TOUCH_BEGIN;
  }

  public static boolean isUpdate(@Nonnull MouseWheelEvent e) {
    return e.getScrollType() == TOUCH_UPDATE;
  }

  public static boolean isEnd(@Nonnull MouseWheelEvent e) {
    return e.getScrollType() == TOUCH_END;
  }
}
