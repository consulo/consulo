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
package consulo.ide.impl.idea.ide.favoritesTreeView.actions;

import consulo.bookmark.ui.view.FavoritesListProvider;
import consulo.ide.impl.idea.ide.favoritesTreeView.FavoritesManagerImpl;
import consulo.ide.impl.idea.ide.favoritesTreeView.FavoritesTreeViewPanel;
import consulo.ide.impl.idea.ide.favoritesTreeView.FavoritesViewTreeBuilder;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.project.Project;
import consulo.ui.ex.awt.CommonActionsPanel;

import java.util.Set;

/**
 * User: Vassiliy.Kudryashov
 */
public class EditFavoritesAction extends AnAction {
  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    FavoritesViewTreeBuilder treeBuilder = e.getDataContext().getData(FavoritesTreeViewPanel.FAVORITES_TREE_BUILDER_KEY);
    String listName = e.getDataContext().getData(FavoritesTreeViewPanel.FAVORITES_LIST_NAME_DATA_KEY);
    if (project == null || treeBuilder == null || listName == null) {
      return;
    }
    FavoritesManagerImpl favoritesManager = FavoritesManagerImpl.getInstance(project);
    FavoritesListProvider provider = favoritesManager.getListProvider(listName);
    Set<Object> selection = treeBuilder.getSelectedElements();
    if (provider != null && provider.willHandle(CommonActionsPanel.Buttons.EDIT, project, selection)) {
      provider.handle(CommonActionsPanel.Buttons.EDIT, project, selection, treeBuilder.getTree());
      return;
    }
    favoritesManager.renameList(project, listName);
  }

  @Override
  @RequiredUIAccess
  public void update(AnActionEvent e) {
    e.getPresentation().setText(CommonActionsPanel.Buttons.EDIT.getText());
    e.getPresentation().setIcon(CommonActionsPanel.Buttons.EDIT.getIcon());
    e.getPresentation().setEnabled(true);
    Project project = e.getData(Project.KEY);
    FavoritesViewTreeBuilder treeBuilder = e.getDataContext().getData(FavoritesTreeViewPanel.FAVORITES_TREE_BUILDER_KEY);
    String listName = e.getDataContext().getData(FavoritesTreeViewPanel.FAVORITES_LIST_NAME_DATA_KEY);
    if (project == null || treeBuilder == null || listName == null) {
      e.getPresentation().setEnabled(false);
      return;
    }
    FavoritesManagerImpl favoritesManager = FavoritesManagerImpl.getInstance(project);
    FavoritesListProvider provider = favoritesManager.getListProvider(listName);
    Set<Object> selection = treeBuilder.getSelectedElements();
    if (provider != null) {
      e.getPresentation().setEnabled(provider.willHandle(CommonActionsPanel.Buttons.EDIT, project, selection));
      e.getPresentation().setText(provider.getCustomName(CommonActionsPanel.Buttons.EDIT));
    }
  }
}
