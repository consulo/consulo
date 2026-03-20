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
import consulo.bookmark.internal.BookmarkManagerInternal;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import org.jspecify.annotations.Nullable;
import java.util.List;

/**
 * @author VISTALL
 * @since 10-Aug-22
 */
@ServiceAPI(ComponentScope.PROJECT)
public sealed interface BookmarkManager permits BookmarkManagerInternal {
  
  public static BookmarkManager getInstance(Project project) {
    return project.getInstance(BookmarkManager.class);
  }

  void editDescription(Bookmark bookmark);

  void setDescription(Bookmark bookmark, String description);

  void setMnemonic(Bookmark bookmark, char c);

  void removeBookmark(Bookmark bookmark);

  boolean hasBookmarksWithMnemonics();

  @Nullable Bookmark findBookmarkForMnemonic(char m);

  @Nullable Bookmark findFileBookmark(VirtualFile file);

  @Nullable Bookmark findEditorBookmark(Document document, int line);

  
  List<Bookmark> getValidBookmarks();

  
  Bookmark addTextBookmark(VirtualFile file, int lineIndex, String description);

  @Nullable Bookmark addFileBookmark(VirtualFile file, String description);

  void addEditorBookmark(Editor editor, int lineIndex);

  @Nullable Bookmark getNextBookmark(Editor editor, boolean isWrapped);

  @Nullable Bookmark getPreviousBookmark(Editor editor, boolean isWrapped);

  
  List<? extends Bookmark> moveBookmarkDown(Bookmark bookmark);

  
  List<? extends Bookmark> moveBookmarkUp(Bookmark bookmark);
}
