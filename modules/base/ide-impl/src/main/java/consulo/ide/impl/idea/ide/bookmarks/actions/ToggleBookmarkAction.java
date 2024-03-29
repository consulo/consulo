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

package consulo.ide.impl.idea.ide.bookmarks.actions;

import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataContext;
import consulo.ide.IdeBundle;
import consulo.bookmark.BookmarkManager;
import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.action.AnActionEvent;

public class ToggleBookmarkAction extends BookmarksAction implements DumbAware {
  public ToggleBookmarkAction() {
    getTemplatePresentation().setText(IdeBundle.message("action.bookmark.toggle"));
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    DataContext dataContext = e.getDataContext();
    Project project = dataContext.getData(CommonDataKeys.PROJECT);
    if (project == null) return;
    BookmarkInContextInfo info = new BookmarkInContextInfo(dataContext, project).invoke();
    if (info.getFile() == null) return;

    if (info.getBookmarkAtPlace() != null) {
      BookmarkManager.getInstance(project).removeBookmark(info.getBookmarkAtPlace());
    }
    else {
      BookmarkManager.getInstance(project).addTextBookmark(info.getFile(), info.getLine(), "");
    }
  }

  @Override
  public void update(AnActionEvent event) {
    DataContext dataContext = event.getDataContext();
    final Project project = dataContext.getData(CommonDataKeys.PROJECT);
    event.getPresentation().setEnabled(project != null &&
                                       (ToolWindowManager.getInstance(project).isEditorComponentActive() &&
                                        dataContext.getData(PlatformDataKeys.EDITOR) != null ||
                                        dataContext.getData(PlatformDataKeys.VIRTUAL_FILE) != null));

    event.getPresentation().setText(IdeBundle.message("action.bookmark.toggle"));
  }
}
