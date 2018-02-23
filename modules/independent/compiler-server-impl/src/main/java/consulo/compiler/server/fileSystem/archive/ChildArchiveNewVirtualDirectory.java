/*
 * Copyright 2013-2016 consulo.io
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
package consulo.compiler.server.fileSystem.archive;

import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import consulo.vfs.ArchiveFileSystem;
import consulo.vfs.impl.archive.ArchiveEntry;
import consulo.vfs.impl.archive.ArchiveFile;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 1:12/11.09.13
 */
public class ChildArchiveNewVirtualDirectory extends ChildArchiveNewVirtualFile {
  protected List<NewVirtualFile> myChilds = new ArrayList<NewVirtualFile>();

  public ChildArchiveNewVirtualDirectory(@Nonnull ArchiveNewVirtualFile parentFile,
                                         @javax.annotation.Nullable ChildArchiveNewVirtualDirectory parentDir,
                                         @Nonnull String name,
                                         @Nonnull ArchiveFileSystem archiveFileSystem,
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

  @javax.annotation.Nullable
  @Override
  public NewVirtualFile findChild(@Nonnull @NonNls String name) {
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
