/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.versionControlSystem.action;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.ui.Refreshable;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.PlaceProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

public interface VcsContext extends PlaceProvider<String> {
  @Nullable
  Project getProject();

  @Nullable
  VirtualFile getSelectedFile();

  @Nonnull
  VirtualFile[] getSelectedFiles();

  @Nonnull
  default Stream<VirtualFile> getSelectedFilesStream() {
    return Arrays.stream(getSelectedFiles());
  }

  Editor getEditor();

  Collection<VirtualFile> getSelectedFilesCollection();

  File[] getSelectedIOFiles();

  int getModifiers();

  Refreshable getRefreshableDialog();

  File getSelectedIOFile();

  @Nonnull
  FilePath[] getSelectedFilePaths();

  @Nonnull
  default Stream<FilePath> getSelectedFilePathsStream() {
    return Arrays.stream(getSelectedFilePaths());
  }

  @Nullable
  FilePath getSelectedFilePath();

  @Nullable
  ChangeList[] getSelectedChangeLists();

  @Nullable
  Change[] getSelectedChanges();

  String getActionName();
}
