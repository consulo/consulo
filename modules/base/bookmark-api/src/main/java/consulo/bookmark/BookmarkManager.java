/*
 * Copyright 2013-2022 consulo.io
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
package consulo.bookmark;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author VISTALL
 * @since 10-Aug-22
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface BookmarkManager {
  @Nonnull
  public static BookmarkManager getInstance(Project project) {
    return project.getInstance(BookmarkManager.class);
  }

  void editDescription(@Nonnull Bookmark bookmark);

  void setDescription(@Nonnull Bookmark bookmark, String description);

  void setMnemonic(@Nonnull Bookmark bookmark, char c);

  void removeBookmark(@Nonnull Bookmark bookmark);

  boolean hasBookmarksWithMnemonics();

  @Nullable
  Bookmark findBookmarkForMnemonic(char m);

  @Nullable
  Bookmark findFileBookmark(@Nonnull VirtualFile file);

  @Nullable
  Bookmark findEditorBookmark(@Nonnull Document document, int line);

  @Nonnull
  List<Bookmark> getValidBookmarks();

  @Nonnull
  Bookmark addTextBookmark(VirtualFile file, int lineIndex, String description);

  @Nullable
  Bookmark addFileBookmark(VirtualFile file, String description);

  void addEditorBookmark(Editor editor, int lineIndex);

  @Nullable
  Bookmark getNextBookmark(@Nonnull Editor editor, boolean isWrapped);

  @Nullable
  Bookmark getPreviousBookmark(@Nonnull Editor editor, boolean isWrapped);

  @Nonnull
  List<? extends Bookmark> moveBookmarkDown(@Nonnull Bookmark bookmark);

  @Nonnull
  List<? extends Bookmark> moveBookmarkUp(@Nonnull Bookmark bookmark);
}
