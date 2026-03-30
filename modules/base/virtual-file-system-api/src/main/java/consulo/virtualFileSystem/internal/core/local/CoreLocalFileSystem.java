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
package consulo.virtualFileSystem.internal.core.local;

import consulo.annotation.access.RequiredWriteAction;
import consulo.virtualFileSystem.BaseVirtualFileSystem;
import consulo.virtualFileSystem.StandardFileSystems;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * @author yole
 */
public class CoreLocalFileSystem extends BaseVirtualFileSystem {
  @Override
  public String getProtocol() {
    return StandardFileSystems.FILE_PROTOCOL;
  }

  public @Nullable VirtualFile findFileByIoFile(File ioFile) {
    return ioFile.exists() ? new CoreLocalVirtualFile(this, ioFile) : null;
  }

  @Override
  public @Nullable VirtualFile findFileByPath(String path) {
    return findFileByIoFile(new File(path));
  }

  @Override
  public void refresh(boolean asynchronous) {
  }

  @Override
  public @Nullable VirtualFile refreshAndFindFileByPath(String path) {
    return findFileByPath(path);
  }

  @Override
  @RequiredWriteAction
  public void deleteFile(@Nullable Object requestor, VirtualFile vFile) throws IOException {
    throw new UnsupportedOperationException("deleteFile() not supported");
  }

  @Override
  @RequiredWriteAction
  public void moveFile(@Nullable Object requestor, VirtualFile vFile, VirtualFile newParent) throws IOException {
    throw new UnsupportedOperationException("move() not supported");
  }

  @Override
  @RequiredWriteAction
  public void renameFile(@Nullable Object requestor, VirtualFile vFile, String newName) throws IOException {
    throw new UnsupportedOperationException("renameFile() not supported");
  }

  @Override
  @RequiredWriteAction
  public VirtualFile createChildFile(@Nullable Object requestor, VirtualFile vDir, String fileName) throws IOException {
    throw new UnsupportedOperationException("createChildFile() not supported");
  }

  @Override
  @RequiredWriteAction
  public VirtualFile createChildDirectory(@Nullable Object requestor, VirtualFile vDir, String dirName) throws IOException {
    throw new UnsupportedOperationException("createChildDirectory() not supported");
  }

  @Override
  @RequiredWriteAction
  public VirtualFile copyFile(@Nullable Object requestor, VirtualFile virtualFile, VirtualFile newParent, String copyName) throws IOException {
    throw new UnsupportedOperationException("copyFile() not supported");
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }
}
