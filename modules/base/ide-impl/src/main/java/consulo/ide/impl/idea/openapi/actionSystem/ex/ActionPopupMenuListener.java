// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.actionSystem.ex;

import consulo.ui.ex.action.ActionPopupMenu;
import jakarta.annotation.Nonnull;

/**
 * Allows to receive notifications when popup menus created from action groups are shown and closed.
 */
public interface ActionPopupMenuListener {
  default void actionPopupMenuCreated(@Nonnull ActionPopupMenu menu) {
  }

  default void actionPopupMenuReleased(@Nonnull ActionPopupMenu menu) {
  }
}
