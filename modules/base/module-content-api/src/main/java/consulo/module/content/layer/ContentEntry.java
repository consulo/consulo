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
package consulo.module.content.layer;

import consulo.content.ContentFolderTypeProvider;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.Predicate;

/**
 * Represents a module content root.
 * You can get existing entries with {@link consulo.ide.impl.idea.openapi.roots.ModuleRootModel#getContentEntries()} or
 * create a new one with {@link ModifiableRootModel#addContentEntry(VirtualFile)}.
 *
 * @author dsl
 * @see ModuleRootModel#getContentEntries()
 * @see ModifiableRootModel#addContentEntry(VirtualFile)
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
   * <code>{@link VirtualFileManager#findFileByUrl(String)}</code>
   *
   * @return URL of content root, that should never be null.
   */
  @Nonnull
  String getUrl();

  @Nonnull
  ContentFolder[] getFolders(@Nonnull Predicate<ContentFolderTypeProvider> predicate);

  @Nonnull
  VirtualFile[] getFolderFiles(@Nonnull Predicate<ContentFolderTypeProvider> predicate);

  @Nonnull
  String[] getFolderUrls(@Nonnull Predicate<ContentFolderTypeProvider> predicate);

  @Nonnull
  ContentFolder addFolder(@Nonnull VirtualFile file, @Nonnull ContentFolderTypeProvider contentFolderType);

  @Nonnull
  ContentFolder addFolder(@Nonnull String url, @Nonnull ContentFolderTypeProvider contentFolderType);

  void removeFolder(@Nonnull ContentFolder contentFolder);
}
