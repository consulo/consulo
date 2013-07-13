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
package com.intellij.openapi.vfs;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * Represents a virtual file system.
 *
 * @see VirtualFile
 * @see VirtualFileManager
 */
public abstract class VirtualFileSystem implements IVirtualFileSystem {
  protected VirtualFileSystem() {
  }

  /**
   * Fetches presentable URL of file with the given path in this file system.
   *
   * @param path the path to get presentable URL for
   * @return presentable URL
   * @see VirtualFile#getPresentableUrl
   */
  @Override
  @NotNull
  public String extractPresentableUrl(@NotNull String path) {
    return path.replace('/', File.separatorChar);
  }

  @Override
  @Deprecated
  /**
   * Deprecated. Current implementation blindly calls plain refresh against the file passed
   */
  public void forceRefreshFile(final boolean asynchronous, @NotNull VirtualFile file) {
    file.refresh(asynchronous, false);
  }

  /**
   * Implementation of deleting files in this file system
   *
   * @see VirtualFile#delete(Object)
   */
  protected abstract void deleteFile(Object requestor, @NotNull VirtualFile vFile) throws IOException;

  /**
   * Implementation of moving files in this file system
   *
   * @see VirtualFile#move(Object,VirtualFile)
   */
  protected abstract void moveFile(Object requestor, @NotNull VirtualFile vFile, @NotNull VirtualFile newParent) throws IOException;

  /**
   * Implementation of renaming files in this file system
   *
   * @see VirtualFile#rename(Object,String)
   */
  protected abstract void renameFile(Object requestor, @NotNull VirtualFile vFile, @NotNull String newName) throws IOException;

  /**
   * Implementation of adding files in this file system
   *
   * @see VirtualFile#createChildData(Object,String)
   */
  protected abstract VirtualFile createChildFile(Object requestor, @NotNull VirtualFile vDir, @NotNull String fileName) throws IOException;

  /**
   * Implementation of adding directories in this file system
   *
   * @see VirtualFile#createChildDirectory(Object,String)
   */
  @NotNull
  protected abstract VirtualFile createChildDirectory(Object requestor, @NotNull VirtualFile vDir, @NotNull String dirName) throws IOException;

  /**
   * Implementation of copying files in this file system
   *
   * @see VirtualFile#copy(Object,VirtualFile,String)
   */
  protected abstract VirtualFile copyFile(final Object requestor,
                                          @NotNull VirtualFile virtualFile,
                                          @NotNull VirtualFile newParent,
                                          @NotNull String copyName) throws IOException;

}
