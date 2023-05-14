// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Provides a minimal accessible information about the object.
 */
public interface SimpleAccessible {
  /**
   * Returns a human-readable string that designates the purpose of the object.
   */
  @Nonnull
  String getAccessibleName();

  /**
   * Returns the tooltip text or null when the tooltip is not available
   */
  @Nullable
  String getAccessibleTooltipText();
}
