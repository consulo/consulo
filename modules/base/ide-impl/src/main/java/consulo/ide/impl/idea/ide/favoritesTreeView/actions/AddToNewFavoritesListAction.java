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

import consulo.application.AllIcons;
import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.ide.favoritesTreeView.FavoritesManager;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.project.Project;

import java.util.Collection;

/**
 * User: anna
 * Date: Feb 28, 2005
 */
class AddToNewFavoritesListAction extends AnAction {
  public AddToNewFavoritesListAction() {
    super(IdeBundle.message("action.add.to.new.favorites.list"),
          "Add To New Favorites List", AllIcons.General.AddFavoritesList);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    Collection<AbstractTreeNode> nodesToAdd = AddToFavoritesAction.getNodesToAdd(e.getDataContext(), true);
    if (nodesToAdd != null) {
      final String newName = AddNewFavoritesListAction.doAddNewFavoritesList(project);
      if (newName != null) {
        FavoritesManager.getInstance(project).addRoots(newName, nodesToAdd);
      }
    }
  }

  @Override
  public void update(AnActionEvent e) {
    e.getPresentation().setEnabled(AddToFavoritesAction.canCreateNodes(e));
  }
}
