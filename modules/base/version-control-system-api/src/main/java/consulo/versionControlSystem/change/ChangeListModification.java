// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.change;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @see ChangeListManager
 */
public interface ChangeListModification {
  @Nonnull
  LocalChangeList addChangeList(@Nonnull String name, @Nullable  String comment);

  void setDefaultChangeList(@Nonnull String name);

  void setDefaultChangeList(@Nonnull LocalChangeList list);

  void removeChangeList(@Nonnull String name);

  void removeChangeList(@Nonnull LocalChangeList list);

  void moveChangesTo(@Nonnull LocalChangeList list, Change @Nonnull ... changes);

  void moveChangesTo(@Nonnull LocalChangeList list, @Nonnull List<@Nonnull Change> changes);

  /**
   * Prohibit changelist deletion or rename until the project is closed
   */
  boolean setReadOnly(@Nonnull String name, final boolean value);

  boolean editName(@Nonnull String fromName, @Nonnull String toName);

  @Nullable
  String editComment(@Nonnull String name, final String newComment);
}
