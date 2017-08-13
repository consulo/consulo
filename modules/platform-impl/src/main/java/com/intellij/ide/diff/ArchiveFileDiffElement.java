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
package com.intellij.ide.diff;

import consulo.fileTypes.ArchiveFileType;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.ArchiveFileSystem;
import consulo.vfs.util.ArchiveVfsUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Konstantin Bulenkov
 */
public class ArchiveFileDiffElement extends VirtualFileDiffElement {
  @SuppressWarnings({"ConstantConditions"})
  public ArchiveFileDiffElement(@NotNull VirtualFile file) {
    super(file.getFileSystem() instanceof ArchiveFileSystem ? file : ArchiveVfsUtil.getArchiveRootForLocalFile(file));
  }

  @Override
  protected VirtualFileDiffElement createElement(VirtualFile file) {
    final VirtualFile archiveRootForLocalFile = ArchiveVfsUtil.getArchiveRootForLocalFile(file);
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
