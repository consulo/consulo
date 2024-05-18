// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.service;

import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

public interface ServiceViewToolWindowDescriptor {
  @Nonnull
  String getToolWindowId();

  @Nonnull
  Image getToolWindowIcon();

  @Nonnull
  String getStripeTitle();

  default boolean isExcludedByDefault() {
    return false;
  }

  default boolean isExclusionAllowed() {
    return true;
  }
}
