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

/*
 * @author max
 */
package consulo.ide.impl.idea.ide.bookmarks.actions;

import consulo.bookmark.Bookmark;
import consulo.bookmark.BookmarkManager;
import consulo.dataContext.DataContext;
import consulo.ide.localize.IdeLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.popup.ComponentPopupBuilder;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.style.StyleManager;

public class ToggleBookmarkWithMnemonicAction extends ToggleBookmarkAction {
  public ToggleBookmarkWithMnemonicAction() {
    getTemplatePresentation().setTextValue(IdeLocalize.actionBookmarkToggleMnemonic());
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(AnActionEvent e) {
    super.actionPerformed(e);

    DataContext dataContext = e.getDataContext();
    final Project project = dataContext.getData(Project.KEY);
    if (project == null) return;
    final BookmarkInContextInfo info = new BookmarkInContextInfo(dataContext, project).invoke();
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

      final ComponentPopupBuilder builder = JBPopupFactory.getInstance().createComponentPopupBuilder(mc, mc);
      popup[0] = builder
        .setTitle("BookmarkImpl Mnemonic")
        .setFocusable(true)
        .setRequestFocus(true)
        .setMovable(false)
        .setCancelKeyEnabled(false)
        .setAdText(
          bookmarks.hasBookmarksWithMnemonics()
            ? (StyleManager.get().getCurrentStyle().isDark() ? "Brown" : "Yellow") + " cells are in use" : null
        )
        .setResizable(false)
        .createPopup();

      popup[0].showInBestPositionFor(dataContext);
    }
  }

  @Override
  @RequiredUIAccess
  public void update(AnActionEvent event) {
    super.update(event);

    event.getPresentation().setTextValue(IdeLocalize.actionBookmarkToggleMnemonic());
  }
}
