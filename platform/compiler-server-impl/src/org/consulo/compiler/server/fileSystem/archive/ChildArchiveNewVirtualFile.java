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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.openapi.vfs.newvfs.NewVirtualFileSystem;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

/**
 * @author VISTALL
 * @since 2:17/12.09.13
 */
public class ChildArchiveNewVirtualFile extends NewVirtualFile {
  private final ArchiveNewVirtualFile myParentFile;
  private final ChildArchiveNewVirtualDirectory myParentDir;
  private final String myName;
  private final ArchiveFileSystem myArchiveFileSystem;
  private final ArchiveFile myArchiveFile;
  private final ArchiveEntry myArchiveEntry;

  public ChildArchiveNewVirtualFile(@NotNull ArchiveNewVirtualFile parentFile,
                                    @Nullable ChildArchiveNewVirtualDirectory parentDir,
                                    @NotNull String name,
                                    @NotNull ArchiveFileSystem archiveFileSystem,
                                    ArchiveFile archiveFile,
                                    ArchiveEntry archiveEntry) {
    myParentFile = parentFile;
    myParentDir = parentDir;
    myName = name;
    myArchiveFileSystem = archiveFileSystem;
    myArchiveFile = archiveFile;
    myArchiveEntry = archiveEntry;
  }

  public VirtualFile getArchiveFile() {
    return myParentFile;
  }

  @Override
  public boolean exists() {
    return true;
  }

  @NotNull
  @Override
  public String getName() {
    return myName;
  }

  @NotNull
  @Override
  public NewVirtualFileSystem getFileSystem() {
    return (NewVirtualFileSystem)myArchiveFileSystem;
  }

  @Override
  public String getPath() {
    StringBuilder builder = new StringBuilder();
    if(myParentDir != null) {
      builder.append(myParentDir.getPath());
      builder.append("/");
    }
    else {
      builder.append(myParentFile.getPath());
    }
    builder.append(myName);
    return builder.toString();
  }

  @Override
  public boolean isWritable() {
    return false;
  }

  @Override
  public boolean isDirectory() {
    return false;
  }

  @Override
  public NewVirtualFile getParent() {
    return myParentDir == null ? myParentFile : myParentDir;
  }

  @Override
  public NewVirtualFile[] getChildren() {
    return new NewVirtualFile[0];
  }

  @Nullable
  @Override
  public NewVirtualFile getCanonicalFile() {
    return null;
  }

  @Nullable
  @Override
  public NewVirtualFile findChild(@NotNull @NonNls String name) {
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
    return myArchiveFile.getInputStream(myArchiveEntry);
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
