// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.awt;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.beans.PropertyChangeListener;

public class FocusUtil {
  private static final String SWING_FOCUS_OWNER_PROPERTY = "focusOwner";

  /**
   * @see consulo.ui.FocusManager
   *
   * Add {@link PropertyChangeListener} listener to the current {@link KeyboardFocusManager} until the {@code parentDisposable} is disposed
   */
  public static void addFocusOwnerListener(@Nonnull Disposable parentDisposable, @Nonnull PropertyChangeListener listener) {
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener(SWING_FOCUS_OWNER_PROPERTY, listener);
    Disposer.register(parentDisposable, () -> {
      KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener(SWING_FOCUS_OWNER_PROPERTY, listener);
    });
  }
}
