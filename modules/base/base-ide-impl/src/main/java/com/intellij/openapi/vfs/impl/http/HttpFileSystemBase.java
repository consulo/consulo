/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.openapi.vfs.impl.http;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.ex.http.HttpFileSystem;
import com.intellij.openapi.vfs.ex.http.HttpVirtualFileListener;
import consulo.disposer.Disposable;

import javax.annotation.Nonnull;

import java.io.IOException;

/**
 * @author nik
 */
public abstract class HttpFileSystemBase extends HttpFileSystem {
  private final String myProtocol;

  public HttpFileSystemBase(String protocol) {
    myProtocol = protocol;
  }

  @Override
  public VirtualFile findFileByPath(@Nonnull String path) {
    return findFileByPath(path, false);
  }

  public VirtualFile findFileByPath(@Nonnull String path, boolean isDirectory) {
    try {
      String url = VirtualFileManager.constructUrl(myProtocol, path);
      return getRemoteFileManager().getOrCreateFile(url, path, isDirectory);
    }
    catch (IOException e) {
      return null;
    }
  }

  @Override
  public void addFileListener(@Nonnull HttpVirtualFileListener listener) {
    getRemoteFileManager().addFileListener(listener);
  }

  @Override
  public void addFileListener(@Nonnull HttpVirtualFileListener listener, @Nonnull Disposable parentDisposable) {
    getRemoteFileManager().addFileListener(listener, parentDisposable);
  }

  @Override
  public void removeFileListener(@Nonnull HttpVirtualFileListener listener) {
    getRemoteFileManager().removeFileListener(listener);
  }

  @Override
  public boolean isFileDownloaded(@Nonnull final VirtualFile file) {
    return file instanceof HttpVirtualFile && ((HttpVirtualFile)file).getFileInfo().getState() == RemoteFileState.DOWNLOADED;
  }

  @Override
  @Nonnull
  public VirtualFile createChildDirectory(Object requestor, @Nonnull VirtualFile vDir, @Nonnull String dirName) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public VirtualFile createChildFile(Object requestor, @Nonnull VirtualFile vDir, @Nonnull String fileName) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteFile(Object requestor, @Nonnull VirtualFile vFile) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void moveFile(Object requestor, @Nonnull VirtualFile vFile, @Nonnull VirtualFile newParent) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public VirtualFile copyFile(Object requestor, @Nonnull VirtualFile vFile, @Nonnull VirtualFile newParent, @Nonnull final String copyName) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void renameFile(Object requestor, @Nonnull VirtualFile vFile, @Nonnull String newName) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public String extractPresentableUrl(@Nonnull String path) {
    return VirtualFileManager.constructUrl(myProtocol, path);
  }

  @Override
  public VirtualFile refreshAndFindFileByPath(@Nonnull String path) {
    return findFileByPath(path);
  }

  @Override
  public void refresh(boolean asynchronous) {
  }

  @Nonnull
  @Override
  public String getProtocol() {
    return myProtocol;
  }

  private static RemoteFileManagerImpl getRemoteFileManager() {
    return (RemoteFileManagerImpl)RemoteFileManager.getInstance();
  }
}
