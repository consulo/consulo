// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.ui;

import consulo.ui.Point2D;
import consulo.ui.Size2D;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;

public interface WindowState {
  /**
   * @return a window location
   * @see Window#getLocation()
   */
  @Nullable
  Point2D getLocation();

  /**
   * @return a window size
   * @see Window#getSize()
   */
  @Nullable
  Size2D getSize();

  /**
   * @return a bitwise mask that represents an extended frame state
   * @see Frame#getExtendedState()
   */
  int getExtendedState();

  /**
   * @return {@code true} if a frame should be opened in a full screen mode
   * @see IdeFrame#isInFullScreen()
   */
  boolean isFullScreen();

  /**
   * @param window a window to apply this state
   */
  default void applyTo(@Nonnull Window window) {
    throw new AbstractMethodError("Desktop only: " + getClass());
  }
}
