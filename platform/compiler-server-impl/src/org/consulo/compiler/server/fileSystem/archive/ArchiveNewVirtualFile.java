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

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.ArchiveEntry;
import com.intellij.openapi.vfs.ArchiveFile;
import com.intellij.openapi.vfs.ArchiveFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.openapi.vfs.newvfs.NewVirtualFileSystem;
import org.consulo.lombok.annotations.Logger;
import org.consulo.vfs.ArchiveFileSystemBase;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author VISTALL
 * @since 1:02/11.09.13
 */
@Logger
public class ArchiveNewVirtualFile extends NewVirtualFile {
  private final VirtualFile myParent;
  private final ArchiveFileSystem myFileSystem;

  protected List<NewVirtualFile> myChilds = new ArrayList<NewVirtualFile>();

  public ArchiveNewVirtualFile(VirtualFile parent, ArchiveFileSystemBase fileSystem) {
    myParent = parent;

    myFileSystem = fileSystem;

    try {
      ArchiveFile jarFile = fileSystem.getArchiveWrapperFile(this);

      Iterator<? extends ArchiveEntry> entries = jarFile.entries();
      while (entries.hasNext()) {
        ArchiveEntry next = entries.next();

        String name = next.getName();

        if (!next.isDirectory()) {
          createFile(name, jarFile, next);
        }
      }
    }
    catch (IOException e) {
      throw new Error(e);
    }
  }

  private ChildArchiveNewVirtualDirectory createDirs(String name) {
    List<String> split = StringUtil.split(name, "/");

    String pre = null;
    ChildArchiveNewVirtualDirectory preF = null;
    for (String s : split) {
      String fullPath = null;
      if (pre != null) {
        fullPath = pre;
        fullPath += s;
      }
      else {
        fullPath = s;
      }
      fullPath += "/";

      ChildArchiveNewVirtualDirectory myFile = (ChildArchiveNewVirtualDirectory)findFileByRelativePath(fullPath);
      if (myFile == null) {
        myFile = new ChildArchiveNewVirtualDirectory(this, preF, s, myFileSystem, null, null);

        if (preF != null) {
          preF.myChilds.add(myFile);
        }
        else {
          myChilds.add(myFile);
        }
      }

      pre = fullPath;
      preF = myFile;
    }
    return preF;
  }

  private void createFile(String name, ArchiveFile jarFile, ArchiveEntry next) {

    ChildArchiveNewVirtualDirectory parentDir = null;
    int lastIndex = name.lastIndexOf("/");
    if (lastIndex != -1) {
      String parentDirName = name.substring(0, lastIndex);

      parentDir = (ChildArchiveNewVirtualDirectory)findFileByRelativePath(parentDirName);

      if(parentDir == null) {
        parentDir = createDirs(parentDirName);
      }

      LOGGER.assertTrue(parentDir != null, "Parent dir for: " + name + ", url file: " + getUrl() + " is not created, parent: " + parentDirName);
    }

    ChildArchiveNewVirtualFile file =
      new ChildArchiveNewVirtualFile(this, parentDir, name.substring(lastIndex + 1, name.length()), myFileSystem, jarFile, next);
    if (parentDir != null) {
      parentDir.myChilds.add(file);
    }
    else {
      myChilds.add(file);
    }
  }

  @NotNull
  @Override
  public String getName() {
    return myParent.getName();
  }

  @Override
  public boolean exists() {
    return true;
  }

  @NotNull
  @Override
  public NewVirtualFileSystem getFileSystem() {
    return (NewVirtualFileSystem)myFileSystem;
  }

  @Override
  public String getPath() {
    return myParent.getPath() + ArchiveFileSystem.ARCHIVE_SEPARATOR;
  }

  @Override
  public boolean isWritable() {
    return false;
  }

  @Override
  public boolean isDirectory() {
    return true;
  }

  @Override
  public NewVirtualFile getParent() {
    return null;
  }

  @Override
  public NewVirtualFile[] getChildren() {
    return myChilds.toArray(new NewVirtualFile[myChilds.size()]);
  }

  @Nullable
  @Override
  public NewVirtualFile getCanonicalFile() {
    return null;
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

  @NotNull
  @Override
  public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
    return null;
  }

  @Override
  public long getTimeStamp() {
    return 0;
  }

  @Override
  public long getLength() {
    return 0;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return null;
  }

  @Nullable
  @Override
  public NewVirtualFile refreshAndFindChild(@NotNull String name) {
    return null;
  }

  @Nullable
  @Override
  public NewVirtualFile findChildIfCached(@NotNull String name) {
    return null;
  }

  @Override
  public void setTimeStamp(long time) throws IOException {
  }

  @NotNull
  @Override
  public CharSequence getNameSequence() {
    return getName();
  }

  @Override
  public int getId() {
    return 0;
  }

  @Override
  public void setWritable(boolean writable) throws IOException {
  }

  @Override
  public void markDirty() {
  }

  @Override
  public void markDirtyRecursively() {
  }

  @Override
  public boolean isDirty() {
    return false;
  }

  @Override
  public void markClean() {
  }

  @NotNull
  @Override
  public Collection<VirtualFile> getCachedChildren() {
    return null;
  }

  @NotNull
  @Override
  public Iterable<VirtualFile> iterInDbChildren() {
    return null;
  }

  @Override
  public boolean equals(Object obj) {
    if(obj instanceof VirtualFile) {
      return getUrl().equals(((VirtualFile)obj).getUrl());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return getUrl().hashCode();
  }
}
