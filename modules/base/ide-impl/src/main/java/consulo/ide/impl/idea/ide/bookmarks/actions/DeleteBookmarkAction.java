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
package consulo.ide.impl.idea.ide.bookmarks.actions;

import consulo.application.AllIcons;
import consulo.ide.impl.idea.ide.bookmarks.BookmarkImpl;
import consulo.ide.impl.idea.ide.bookmarks.BookmarkManagerImpl;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.ui.ex.awt.util.ListUtil;

import javax.swing.*;
import java.util.List;

class DeleteBookmarkAction extends DumbAwareAction {
  private final Project myProject;
  private final JList myList;

  DeleteBookmarkAction(Project project, JList list) {
    super("Delete", "Delete current bookmark", AllIcons.General.Remove);
    myProject = project;
    myList = list;
    registerCustomShortcutSet(CustomShortcutSet.fromString("DELETE", "BACK_SPACE"), list);
  }

  @Override
  public void update(AnActionEvent e) {
    e.getPresentation().setEnabled(BookmarksAction.getSelectedBookmarks(myList).size() > 0);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    List<BookmarkImpl> bookmarks = BookmarksAction.getSelectedBookmarks(myList);
    ListUtil.removeSelectedItems(myList);

    for (BookmarkImpl bookmark : bookmarks) {
      BookmarkManagerImpl.getInstance(myProject).removeBookmark(bookmark);
    }
  }
}
