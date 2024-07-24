/*
 * Copyright 2013-2024 consulo.io
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
package consulo.versionControlSystem;

import consulo.localize.LocalizeValue;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 21-Jul-24
 */
public interface VcsFileListenerHandler {
  class MovedFileInfo {
    private final String myOldPath;
    private String myNewPath;
    private final VirtualFile myFile;

    public MovedFileInfo(VirtualFile file, final String newPath) {
      myOldPath = file.getPath();
      myNewPath = newPath;
      myFile = file;
    }

    public String getOldPath() {
      return myOldPath;
    }

    public String getNewPath() {
      return myNewPath;
    }

    public void setNewPath(String newPath) {
      myNewPath = newPath;
    }

    public VirtualFile getFile() {
      return myFile;
    }

    @Override
    public String toString() {
      return "MovedFileInfo{myNewPath=" + myNewPath + ", myFile=" + myFile + '}';
    }
  }

  @Nonnull
  LocalizeValue getAddTitle();

  @Nonnull
  LocalizeValue getSingleFileAddTitle();

  @Nonnull
  LocalizeValue getSingleFileAddPromptTemplate();

  void performAdding(final Collection<VirtualFile> addedFiles, final Map<VirtualFile, VirtualFile> copyFromMap);

  @Nonnull
  LocalizeValue getDeleteTitle();

  @Nonnull
  LocalizeValue getSingleFileDeleteTitle();

  @Nonnull
  String getSingleFileDeletePromptTemplate();

  void performDeletion(List<FilePath> filesToDelete);

  void performMoveRename(List<MovedFileInfo> movedFiles);

  boolean isDirectoryVersioningSupported();
}
