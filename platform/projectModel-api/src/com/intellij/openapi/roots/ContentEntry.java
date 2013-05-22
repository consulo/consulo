/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.openapi.roots;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a module content root.
 * You can get existing entries with {@link com.intellij.openapi.roots.ModuleRootModel#getContentEntries()} or
 * create a new one with {@link ModifiableRootModel#addContentEntry(com.intellij.openapi.vfs.VirtualFile)}.
 *
 * @author dsl
 * @see ModuleRootModel#getContentEntries()
 * @see ModifiableRootModel#addContentEntry(com.intellij.openapi.vfs.VirtualFile)
 */
public interface ContentEntry extends Synthetic {
  /**
   * Returns the root directory for the content root, if it is valid.
   *
   * @return the content root directory, or null if content entry is invalid.
   */
  @Nullable
  VirtualFile getFile();

  /**
   * Returns the URL of content root.
   * To validate returned roots, use
   * <code>{@link com.intellij.openapi.vfs.VirtualFileManager#findFileByUrl(String)}</code>
   *
   * @return URL of content root, that should never be null.
   */
  @NotNull
  String getUrl();

  /**
   * Returns the list of content roots under this content root.
   * @param contentFolderType
   * @return
   */
  @NotNull
  ContentFolder[] getFolders(@NotNull ContentFolder.ContentFolderType contentFolderType);

  /**
   * Returns the list of directories roots under this content root.
   * @param contentFolderType
   * @return
   */
  @NotNull
  VirtualFile[] getFolderFiles(@NotNull ContentFolder.ContentFolderType contentFolderType);

  @NotNull
  String[] getFolderUrls(@NotNull ContentFolder.ContentFolderType contentFolderType);

  ContentFolder[] getFolders();

  @NotNull
  ContentFolder addFolder(@NotNull VirtualFile file, @NotNull ContentFolder.ContentFolderType contentFolderType);

  @NotNull
  ContentFolder addFolder(@NotNull String url, @NotNull ContentFolder.ContentFolderType contentFolderType);

  void removeFolder(@NotNull ContentFolder contentFolder);

  void clearFolders(@NotNull ContentFolder.ContentFolderType contentFolderType);
}
