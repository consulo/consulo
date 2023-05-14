// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.ui.plaf;

import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import java.awt.*;

public interface VisualPaddingsProvider {
  @Nullable
  Insets getVisualPaddings(@Nonnull Component component);
}
