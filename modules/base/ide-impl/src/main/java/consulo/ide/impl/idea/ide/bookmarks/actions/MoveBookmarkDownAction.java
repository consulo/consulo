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
import consulo.ide.impl.idea.ide.bookmarks.BookmarkItem;
import consulo.bookmark.BookmarkManager;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CommonShortcuts;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.util.ListUtil;

import javax.swing.*;

class MoveBookmarkDownAction extends DumbAwareAction {
  private final Project myProject;
  private final JList myList;

  MoveBookmarkDownAction(Project project, JList list) {
    super("Down", "Move current bookmark down", AllIcons.Actions.NextOccurence);
    myProject = project;
    myList = list;
    registerCustomShortcutSet(CommonShortcuts.MOVE_DOWN, list);
  }

  @Override
  public void update(AnActionEvent e) {
    int modelSize = myList.getModel().getSize();
    if (modelSize == 0 || !BookmarksAction.notFiltered(myList)) {
      e.getPresentation().setEnabled(false);
    }
    else {
      int lastIndex = modelSize - 1;
      if (!(myList.getModel().getElementAt(lastIndex) instanceof BookmarkItem)) lastIndex--;
      e.getPresentation().setEnabled(BookmarksAction.getSelectedBookmarks(myList).size() == 1 && myList.getSelectedIndex() < lastIndex);
    }
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    ListUtil.moveSelectedItemsDown(myList);
    BookmarkManager.getInstance(myProject).moveBookmarkDown(BookmarksAction.getSelectedBookmarks(myList).get(0));
  }
}
