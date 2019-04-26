// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.ui.laf;

import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.awt.*;

public interface VisualPaddingsProvider {
  @Nullable
  Insets getVisualPaddings(@Nonnull Component component);
}
