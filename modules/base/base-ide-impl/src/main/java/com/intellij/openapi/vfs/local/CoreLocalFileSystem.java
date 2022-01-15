/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.vfs.local;

import com.intellij.openapi.vfs.DeprecatedVirtualFileSystem;
import com.intellij.openapi.vfs.StandardFileSystems;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * @author yole
 */
public class CoreLocalFileSystem extends DeprecatedVirtualFileSystem {
  @Nonnull
  @Override
  public String getProtocol() {
    return StandardFileSystems.FILE_PROTOCOL;
  }

  @Nullable
  public VirtualFile findFileByIoFile(@Nonnull File ioFile) {
    return ioFile.exists() ? new CoreLocalVirtualFile(this, ioFile) : null;
  }

  @Override
  public VirtualFile findFileByPath(@Nonnull @NonNls String path) {
    return findFileByIoFile(new File(path));
  }

  @Override
  public void refresh(boolean asynchronous) {
  }

  @Override
  public VirtualFile refreshAndFindFileByPath(@Nonnull String path) {
    return findFileByPath(path);
  }

  @Override
  public void deleteFile(Object requestor, @Nonnull VirtualFile vFile) throws IOException {
    throw new UnsupportedOperationException("deleteFile() not supported");
  }

  @Override
  public void moveFile(Object requestor, @Nonnull VirtualFile vFile, @Nonnull VirtualFile newParent) throws IOException {
    throw new UnsupportedOperationException("move() not supported");
  }

  @Override
  public void renameFile(Object requestor, @Nonnull VirtualFile vFile, @Nonnull String newName) throws IOException {
    throw new UnsupportedOperationException("renameFile() not supported");
  }

  @Override
  public VirtualFile createChildFile(Object requestor, @Nonnull VirtualFile vDir, @Nonnull String fileName) throws IOException {
    throw new UnsupportedOperationException("createChildFile() not supported");
  }

  @Nonnull
  @Override
  public VirtualFile createChildDirectory(Object requestor, @Nonnull VirtualFile vDir, @Nonnull String dirName) throws IOException {
    throw new UnsupportedOperationException("createChildDirectory() not supported");
  }

  @Override
  public VirtualFile copyFile(Object requestor,
                                 @Nonnull VirtualFile virtualFile,
                                 @Nonnull VirtualFile newParent,
                                 @Nonnull String copyName) throws IOException {
    throw new UnsupportedOperationException("copyFile() not supported");
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }
}
