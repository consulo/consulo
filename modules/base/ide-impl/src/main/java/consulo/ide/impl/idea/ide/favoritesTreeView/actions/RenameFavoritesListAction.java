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

package consulo.ide.impl.idea.ide.favoritesTreeView.actions;

import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.ide.favoritesTreeView.FavoritesManagerImpl;
import consulo.ide.impl.idea.ide.favoritesTreeView.FavoritesTreeViewPanel;
import consulo.language.editor.CommonDataKeys;
import consulo.ide.localize.IdeLocalize;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;

/**
 * User: anna
 * Date: Feb 23, 2005
 */
public class RenameFavoritesListAction extends AnAction implements DumbAware {
  public RenameFavoritesListAction() {
    super(
      IdeLocalize.actionRenameFavoritesList(),
      IdeLocalize.actionRenameFavoritesList(),
      null
    );
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      return;
    }
    final FavoritesManagerImpl favoritesManager = FavoritesManagerImpl.getInstance(project);
    String listName = dataContext.getData(FavoritesTreeViewPanel.FAVORITES_LIST_NAME_DATA_KEY);
    if (listName == null || favoritesManager.getListProvider(listName) != null) return;
    favoritesManager.renameList(project, listName);
  }

  @Override
  public void update(AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      e.getPresentation().setEnabled(false);
      return;
    }
    final FavoritesManagerImpl favoritesManager = FavoritesManagerImpl.getInstance(project);
    String listName = dataContext.getData(FavoritesTreeViewPanel.FAVORITES_LIST_NAME_DATA_KEY);
    e.getPresentation().setEnabled(listName != null && favoritesManager.getListProvider(listName) == null);
  }
}
