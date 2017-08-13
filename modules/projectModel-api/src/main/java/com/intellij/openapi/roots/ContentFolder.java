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

import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import consulo.roots.ContentFolderTypeProvider;

import java.util.Map;

/**
 * Represents a source or exclude root under the content root of a module.
 *
 * @see ContentEntry#getSourceFolders()
 * @see ContentEntry#getExcludeFolders()
 * @author dsl
 */
public interface ContentFolder extends Synthetic {
  ContentFolder[] EMPTY_ARRAY = new ContentFolder[0];

  /**
   * Returns the type of root directory
   * @return
   */
  @NotNull
  ContentFolderTypeProvider getType();

  /**
   * Returns the root directory for this root.
   *
   * @return the directory, or null if the source path is invalid.
   */
  @Nullable
  VirtualFile getFile();

  /**
   * Returns the content entry to which this root belongs.
   *
   * @return this <code>ContentFolder</code>s {@link com.intellij.openapi.roots.ContentEntry}.
   */
  @NotNull
  ContentEntry getContentEntry();

  /**
   * Returns the URL of the root directory for this root.
   *
   * @return the root directory URL.
   */
  @NotNull
  String getUrl();

  @NotNull
  Map<Key, Object> getProperties();

  @Nullable
  <T> T getPropertyValue(@NotNull Key<T> key);

  <T> void setPropertyValue(@NotNull Key<T> key, @Nullable T value);
}
