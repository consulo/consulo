/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.diff.dir;

import consulo.fileChooser.FileChooserDescriptor;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import consulo.virtualFileSystem.archive.ArchiveFileType;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;
import jakarta.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
public class ArchiveFileDiffElement extends VirtualFileDiffElement {
  @SuppressWarnings({"ConstantConditions"})
  public ArchiveFileDiffElement(@Nonnull VirtualFile file) {
    super(file.getFileSystem() instanceof ArchiveFileSystem ? file : ArchiveVfsUtil.getArchiveRootForLocalFile(file));
  }

  @Override
  protected VirtualFileDiffElement createElement(VirtualFile file) {
    VirtualFile archiveRootForLocalFile = ArchiveVfsUtil.getArchiveRootForLocalFile(file);
    return archiveRootForLocalFile == null ? null : new ArchiveFileDiffElement(file);
  }

  @Override
  protected FileChooserDescriptor getChooserDescriptor() {
    return new FileChooserDescriptor(true, false, true, true, false, false) {
      @Override
      public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
        return file.isDirectory() || (!file.isDirectory() && file.getFileType() instanceof ArchiveFileType);
      }
    };
  }
}
