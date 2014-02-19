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
package org.consulo.compiler.server.fileSystem;

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
 * @since 16:33/19.08.13
 */
public class CompilerServerNewVirtualFileImpl extends NewVirtualFile {
  private VirtualFile myVirtualFile;
  private NewVirtualFileSystem myFileSystem;

  public CompilerServerNewVirtualFileImpl(VirtualFile virtualFile, NewVirtualFileSystem fileSystem) {
    myVirtualFile = virtualFile;
    myFileSystem = fileSystem;
  }

  @NotNull
  @Override
  public String getName() {
    return myVirtualFile.getName();
  }

  @NotNull
  @Override
  public NewVirtualFileSystem getFileSystem() {
    return myFileSystem;
  }

  @Override
  public boolean exists() {
    return true;
  }

  @Override
  public String getPath() {
    return myVirtualFile.getPath();
  }

  @Override
  public boolean isWritable() {
    return false;
  }

  @Override
  public boolean isDirectory() {
    return myFileSystem instanceof ArchiveFileSystem || myVirtualFile.isDirectory();
  }

  @NotNull
  @Override
  public byte[] contentsToByteArray() throws IOException {
    return myVirtualFile.contentsToByteArray();
  }

  @Override
  public NewVirtualFile getParent() {
    return null;
  }

  @Override
  public VirtualFile[] getChildren() {
    return new VirtualFile[0];
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
    return myVirtualFile.getOutputStream(requestor, newModificationStamp, newTimeStamp);
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
}
