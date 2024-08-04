// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.internal.perfomance;

import jakarta.annotation.Nonnull;

public interface Activity {
  void end();

  void setDescription(@Nonnull String description);

  /**
   * Convenient method to end token and start a new sibling one.
   * So, start of new is always equals to this item end and yet another System.nanoTime() call is avoided.
   */
  @Nonnull
  Activity endAndStart(@Nonnull String name);

  @Nonnull
  Activity startChild(@Nonnull String name);
}
