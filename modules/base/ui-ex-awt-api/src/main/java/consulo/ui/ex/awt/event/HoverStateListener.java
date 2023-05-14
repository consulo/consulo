// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt.event;

import jakarta.annotation.Nonnull;
import java.awt.*;

public abstract class HoverStateListener extends HoverListener {
  protected abstract void hoverChanged(Component component, boolean hovered);

  @Override
  public void mouseEntered(@Nonnull Component component, int x, int y) {
    hoverChanged(component, true);
  }

  @Override
  public void mouseMoved(@Nonnull Component component, int x, int y) {
  }

  @Override
  public void mouseExited(@Nonnull Component component) {
    hoverChanged(component, false);
  }
}
