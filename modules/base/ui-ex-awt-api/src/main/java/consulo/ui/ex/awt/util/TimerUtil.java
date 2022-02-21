// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt.util;

import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import java.awt.event.ActionListener;

public final class TimerUtil {
  @Nonnull
  public static javax.swing.Timer createNamedTimer(@Nonnull String name, int delay, @Nonnull @RequiredUIAccess ActionListener listener) {
    return new javax.swing.Timer(delay, listener) {
      @Override
      public String toString() {
        return name;
      }
    };
  }

  @Nonnull
  public static javax.swing.Timer createNamedTimer(@Nonnull String name, int delay) {
    return new javax.swing.Timer(delay, null) {
      @Override
      public String toString() {
        return name;
      }
    };
  }
}
