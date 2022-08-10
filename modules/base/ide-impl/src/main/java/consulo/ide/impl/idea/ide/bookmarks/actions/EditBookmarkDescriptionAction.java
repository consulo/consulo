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
import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.ide.bookmarks.BookmarkImpl;
import consulo.ide.impl.idea.ide.bookmarks.BookmarkManagerImpl;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.ui.ex.popup.JBPopup;
import consulo.application.util.SystemInfo;

import javax.swing.*;

class EditBookmarkDescriptionAction extends DumbAwareAction {
  private final JList myList;
  private final Project myProject;
  private JBPopup myPopup;

  EditBookmarkDescriptionAction(Project project, JList list) {
    super(IdeBundle.message("action.bookmark.edit.description"),
          IdeBundle.message("action.bookmark.edit.description.description"), AllIcons.Actions.Edit);
    myProject = project;
    myList = list;
    registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(SystemInfo.isMac ? "meta ENTER" : "control ENTER")), list);
  }

  @Override
  public void update(AnActionEvent e) {
    e.getPresentation().setEnabled(BookmarksAction.getSelectedBookmarks(myList).size() == 1);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    BookmarkImpl bookmark = BookmarksAction.getSelectedBookmarks(myList).get(0);
    myPopup.setUiVisible(false);

    BookmarkManagerImpl.getInstance(myProject).editDescription(bookmark);

    myPopup.setUiVisible(true);
    final JComponent content = myPopup.getContent();
    if (content != null) {
      myPopup.setSize(content.getPreferredSize());
    }
  }

  public void setPopup(JBPopup popup) {
    myPopup = popup;
  }
}
