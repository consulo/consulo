/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import com.intellij.openapi.application.Application;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.util.KeyedLazyInstanceEP;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;

/**
 * Represents a virtual file system.
 *
 * @see VirtualFile
 * @see VirtualFileManager
 */
public interface VirtualFileSystem {
  ExtensionPointName<VirtualFileSystem> EP_NAME = ExtensionPointName.create("com.intellij.virtualFileSystem");

  /**
   * Gets the protocol for this file system. Protocols should differ for all file systems.
   * Should be the same as corresponding {@link KeyedLazyInstanceEP#key}.
   *
   * @return String representing the protocol
   * @see VirtualFile#getUrl
   * @see VirtualFileManager#getFileSystem
   */
  @NonNls
  @Nonnull
  String getProtocol();

  /**
   * Searches for the file specified by given path. Path is a string which uniquely identifies file within given
   * {@link VirtualFileSystem}. Format of the path depends on the concrete file system.
   * For {@code LocalFileSystem} it is an absolute path (both Unix- and Windows-style separator chars are allowed).
   *
   * @param path the path to find file by
   * @return a virtual file if found, {@code null} otherwise
   */
  @Nullable
  public abstract VirtualFile findFileByPath(@Nonnull @NonNls String path);

  /**
   * Fetches presentable URL of file with the given path in this file system.
   *
   * @param path the path to get presentable URL for
   * @return presentable URL
   * @see VirtualFile#getPresentableUrl
   */
  @Nonnull
  default String extractPresentableUrl(@Nonnull String path) {
    return path.replace('/', File.separatorChar);
  }

  /**
   * Refreshes the cached information for all files in this file system from the physical file system.<p>
   * <p>
   * If <code>asynchronous</code> is <code>false</code> this method should be only called within write-action.
   * See {@link Application#runWriteAction}.
   *
   * @param asynchronous if <code>true</code> then the operation will be performed in a separate thread,
   *                     otherwise will be performed immediately
   * @see VirtualFile#refresh
   * @see VirtualFileManager#syncRefresh
   * @see VirtualFileManager#asyncRefresh
   */
  void refresh(boolean asynchronous);

  /**
   * Refreshes only the part of the file system needed for searching the file by the given path and finds file
   * by the given path.<br>
   * <p>
   * This method is useful when the file was created externally and you need to find <code>{@link VirtualFile}</code>
   * corresponding to it.<p>
   * <p>
   * If this method is invoked not from Swing event dispatch thread, then it must not happen inside a read action. The reason is that
   * then the method call won't return until proper VFS events are fired, which happens on Swing thread and in write action. So invoking
   * this method in a read action would result in a deadlock.
   *
   * @param path the path
   * @return <code>{@link VirtualFile}</code> if the file was found, <code>null</code> otherwise
   */
  @Nullable
  VirtualFile refreshAndFindFileByPath(@Nonnull String path);

  /**
   * Adds listener to the file system. Normally one should use {@link VirtualFileManager#addVirtualFileListener}.
   *
   * @param listener the listener
   * @see VirtualFileListener
   * @see VirtualFileManager#addVirtualFileListener
   */
  void addVirtualFileListener(@Nonnull VirtualFileListener listener);

  /**
   * Removes listener form the file system.
   *
   * @param listener the listener
   */
  public abstract void removeVirtualFileListener(@Nonnull VirtualFileListener listener);

  /**
   * Implementation of deleting files in this file system
   *
   * @see VirtualFile#delete(Object)
   */
  void deleteFile(Object requestor, @Nonnull VirtualFile vFile) throws IOException;

  /**
   * Implementation of moving files in this file system
   *
   * @see VirtualFile#move(Object, VirtualFile)
   */
  void moveFile(Object requestor, @Nonnull VirtualFile vFile, @Nonnull VirtualFile newParent) throws IOException;

  /**
   * Implementation of renaming files in this file system
   *
   * @see VirtualFile#rename(Object, String)
   */
  void renameFile(Object requestor, @Nonnull VirtualFile vFile, @Nonnull String newName) throws IOException;

  /**
   * Implementation of adding files in this file system
   *
   * @see VirtualFile#createChildData(Object, String)
   */
  @Nonnull
  VirtualFile createChildFile(Object requestor, @Nonnull VirtualFile vDir, @Nonnull String fileName) throws IOException;

  /**
   * Implementation of adding directories in this file system
   *
   * @see VirtualFile#createChildDirectory(Object, String)
   */
  @Nonnull
  VirtualFile createChildDirectory(Object requestor, @Nonnull VirtualFile vDir, @Nonnull String dirName) throws IOException;

  /**
   * Implementation of copying files in this file system
   *
   * @see VirtualFile#copy(Object, VirtualFile, String)
   */
  @Nonnull
  VirtualFile copyFile(Object requestor, @Nonnull VirtualFile virtualFile, @Nonnull VirtualFile newParent, @Nonnull String copyName) throws IOException;

  boolean isReadOnly();

  default boolean isCaseSensitive() {
    return true;
  }

  default boolean isValidName(@Nonnull String name) {
    return name.length() > 0 && name.indexOf('\\') < 0 && name.indexOf('/') < 0;
  }
}