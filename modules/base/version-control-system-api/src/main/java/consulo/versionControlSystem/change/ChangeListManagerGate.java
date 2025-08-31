/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.versionControlSystem.change;

import consulo.versionControlSystem.FilePath;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * only to be used by {@link ChangeProvider} in order to create IDEA's peer changelist
 * in response to finding not registered VCS native list
 * it can NOT be done through {@link ChangeListManager} interface; it is for external/IDEA user modifications
 */
public interface ChangeListManagerGate {
  List<LocalChangeList> getListsCopy();

  @Nullable
  LocalChangeList findChangeList(String name);

  LocalChangeList addChangeList(String name, String comment);

  LocalChangeList findOrCreateList(String name, String comment);

  void editComment(String name, String comment);

  void editName(String oldName, String newName);

  void setListsToDisappear(Collection<String> names);

  FileStatus getStatus(VirtualFile file);

  @Nullable
  FileStatus getStatus(@Nonnull FilePath filePath);

  /**
   * Use {@link #getStatus(FilePath)
   *
   * @deprecated to remove in IDEA 16
   */
  @Deprecated
  FileStatus getStatus(File file);

  void setDefaultChangeList(@Nonnull String list);
}
