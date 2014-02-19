/*
 * Copyright 2013 must-be.org
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

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 19:24/13.07.13
 */
public interface IVirtualFileSystem {
  /**
   * Gets the protocol for this file system. Protocols should differ for all file systems.
   * Should be the same as corresponding {@link com.intellij.util.KeyedLazyInstanceEP#key}.
   *
   * @return String representing the protocol
   * @see com.intellij.openapi.vfs.VirtualFile#getUrl
   * @see com.intellij.openapi.vfs.VirtualFileManager#getFileSystem
   */
  @NonNls
  @NotNull
  String getProtocol();

  /**
   * Searches for the file specified by given path. Path is a string which uniquely identifies file within given
   * <code>{@link com.intellij.openapi.vfs.VirtualFileSystem}</code>. Format of the path depends on the concrete file system.
   * For <code>LocalFileSystem</code> it is an absolute file path with file separator characters (File.separatorChar)
   * replaced to the forward slash ('/').<p>
   * <p/>
   * Example: to find a <code>{@link com.intellij.openapi.vfs.VirtualFile}</code> corresponding to the physical file with the specified path one
   * can use the following code: <code>LocalFileSystem.getInstance().findFileByPath(path.replace(File.separatorChar, '/'));</code>
   *
   * @param path the path to find file by
   * @return <code>{@link com.intellij.openapi.vfs.VirtualFile}</code> if the file was found, <code>null</code> otherwise
   */
  @Nullable
  VirtualFile findFileByPath(@NotNull @NonNls String path);

  @NotNull
  String extractPresentableUrl(@NotNull String path);

  /**
   * Refreshes the cached information for all files in this file system from the physical file system.<p>
   * <p/>
   * If <code>asynchronous</code> is <code>false</code> this method should be only called within write-action.
   * See {@link com.intellij.openapi.application.Application#runWriteAction}.
   *
   * @param asynchronous if <code>true</code> then the operation will be performed in a separate thread,
   *                     otherwise will be performed immediately
   * @see com.intellij.openapi.vfs.VirtualFile#refresh
   * @see com.intellij.openapi.vfs.VirtualFileManager#syncRefresh
   * @see com.intellij.openapi.vfs.VirtualFileManager#asyncRefresh
   */
  void refresh(boolean asynchronous);

  /**
   * Refreshes only the part of the file system needed for searching the file by the given path and finds file
   * by the given path.<br>
   * <p/>
   * This method is useful when the file was created externally and you need to find <code>{@link com.intellij.openapi.vfs.VirtualFile}</code>
   * corresponding to it.<p>
   * <p/>
   * This method should be only called within write-action.
   * See {@link com.intellij.openapi.application.Application#runWriteAction}.
   *
   * @param path the path
   * @return <code>{@link com.intellij.openapi.vfs.VirtualFile}</code> if the file was found, <code>null</code> otherwise
   */
  @Nullable
  VirtualFile refreshAndFindFileByPath(@NotNull String path);

  /**
   * Adds listener to the file system. Normally one should use {@link com.intellij.openapi.vfs.VirtualFileManager#addVirtualFileListener}.
   *
   * @param listener the listener
   * @see com.intellij.openapi.vfs.VirtualFileListener
   * @see com.intellij.openapi.vfs.VirtualFileManager#addVirtualFileListener
   */
  void addVirtualFileListener(@NotNull VirtualFileListener listener);

  /**
   * Removes listener form the file system.
   *
   * @param listener the listener
   */
  void removeVirtualFileListener(@NotNull VirtualFileListener listener);

  @Deprecated
  /**
   * Deprecated. Current implementation blindly calls plain refresh against the file passed
   */
  void forceRefreshFile(boolean asynchronous, @NotNull VirtualFile file);

  boolean isReadOnly();
}
