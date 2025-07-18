/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import consulo.annotation.component.ActionImpl;
import consulo.bookmark.Bookmark;
import consulo.bookmark.BookmarkManager;
import consulo.bookmark.localize.BookmarkLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.popup.ComponentPopupBuilder;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import jakarta.annotation.Nonnull;

/**
 * @author max
 */
@ActionImpl(id = "ToggleBookmarkWithMnemonic")
public class ToggleBookmarkWithMnemonicAction extends ToggleBookmarkAction {
  public ToggleBookmarkWithMnemonicAction() {
    super(BookmarkLocalize.actionBookmarkToggleWithMnemonicText(), BookmarkLocalize.actionBookmarkToggleDescription());
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    super.actionPerformed(e);

    Project project = e.getRequiredData(Project.KEY);
    BookmarkInContextInfo info = new BookmarkInContextInfo(e.getDataContext(), project).invoke();
    final Bookmark bookmark = info.getBookmarkAtPlace();
    final BookmarkManager bookmarks = BookmarkManager.getInstance(project);
    if (bookmark != null) {
      final JBPopup[] popup = new JBPopup[1];
      MnemonicChooser mc = new MnemonicChooser() {
        @Override
        protected void onMnemonicChosen(char c) {
          popup[0].cancel();
          bookmarks.setMnemonic(bookmark, c);
        }

        @Override
        protected void onCancelled() {
          popup[0].cancel();
          bookmarks.removeBookmark(bookmark);
        }

        @Override
        protected boolean isOccupied(char c) {
          return bookmarks.findBookmarkForMnemonic(c) != null;
        }
      };

      ComponentPopupBuilder builder = JBPopupFactory.getInstance().createComponentPopupBuilder(mc, mc);
      popup[0] = builder
        .setTitle(BookmarkLocalize.dialogBookmarkAddWithMnemonicTitle())
        .setFocusable(true)
        .setRequestFocus(true)
        .setMovable(true)
        .setCancelKeyEnabled(false)
        .setResizable(false)
        .createPopup();

      popup[0].showInBestPositionFor(e.getDataContext());
    }
  }

  @Override
  public void update(@Nonnull AnActionEvent event) {
    super.update(event);

    event.getPresentation().setTextValue(BookmarkLocalize.actionBookmarkToggleWithMnemonicText());
  }
}
