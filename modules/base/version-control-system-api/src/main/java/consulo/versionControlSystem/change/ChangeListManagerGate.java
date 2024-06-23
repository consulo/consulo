// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.change;

import consulo.versionControlSystem.FilePath;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * only to be used by {@link ChangeProvider} in order to create IDEA's peer changelist
 * in response to finding not registered VCS native list
 * it can NOT be done through {@link ChangeListManager} interface; it is for external/IDEA user modifications
 */
public interface ChangeListManagerGate {
  /**
   * @return lists with <b>populated</b> {@link LocalChangeList#getChanges()}
   */
  @Nonnull
  List<LocalChangeList> getListsCopy();

  /**
   * @return list with <b>non-populated</b> {@link LocalChangeList#getChanges()}
   */
  @Nullable
  LocalChangeList findChangeList(@Nullable String name);

  /**
   * If a changelist with this name already exists, an error is logged.
   *
   * @return list with <b>non-populated</b> {@link LocalChangeList#getChanges()}
   */
  @Nonnull
  LocalChangeList addChangeList(@Nonnull String name, @Nullable String comment);

  /**
   * @return list with <b>non-populated</b> {@link LocalChangeList#getChanges()}
   */
  @Nonnull
  LocalChangeList findOrCreateList(@Nonnull String name, @Nullable String comment);

  void editComment(@Nonnull String name, @Nullable String comment);

  void editName(@Nonnull String oldName, @Nonnull String newName);

  void setListsToDisappear(@Nonnull Collection<String> names);

  @Nullable
  FileStatus getStatus(@Nonnull VirtualFile file);

  @Nullable
  FileStatus getStatus(@Nonnull FilePath filePath);

  void setDefaultChangeList(@Nonnull String list);
}
