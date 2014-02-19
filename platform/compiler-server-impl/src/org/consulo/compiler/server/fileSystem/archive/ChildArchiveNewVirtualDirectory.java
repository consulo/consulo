/*
 * Copyright 2013-2014 must-be.org
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
package org.consulo.compiler.server.fileSystem.archive;

import com.intellij.openapi.vfs.ArchiveEntry;
import com.intellij.openapi.vfs.ArchiveFile;
import com.intellij.openapi.vfs.ArchiveFileSystem;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 1:12/11.09.13
 */
public class ChildArchiveNewVirtualDirectory extends ChildArchiveNewVirtualFile {
  protected List<NewVirtualFile> myChilds = new ArrayList<NewVirtualFile>();

  public ChildArchiveNewVirtualDirectory(@NotNull ArchiveNewVirtualFile parentFile,
                                         @Nullable ChildArchiveNewVirtualDirectory parentDir,
                                         @NotNull String name,
                                         @NotNull ArchiveFileSystem archiveFileSystem,
                                         ArchiveFile archiveFile,
                                         ArchiveEntry archiveEntry) {
    super(parentFile, parentDir, name, archiveFileSystem, archiveFile, archiveEntry);
  }


  @Override
  public NewVirtualFile[] getChildren() {
    return myChilds.toArray(new NewVirtualFile[myChilds.size()]);
  }

  @Override
  public boolean isDirectory() {
    return true;
  }

  @Nullable
  @Override
  public NewVirtualFile findChild(@NotNull @NonNls String name) {
    NewVirtualFile[] children = getChildren();
    if (children == null) return null;
    for (NewVirtualFile child : children) {
      if (child.getName().equals(name)) {
        return child;
      }
    }
    return null;
  }
}
