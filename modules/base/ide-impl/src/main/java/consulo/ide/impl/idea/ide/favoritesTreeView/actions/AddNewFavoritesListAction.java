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

import consulo.ide.impl.idea.ide.favoritesTreeView.FavoritesManagerImpl;
import consulo.ide.localize.IdeLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.InputValidator;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;

import java.util.List;

/**
 * User: anna
 * Date: Feb 24, 2005
 */
public class AddNewFavoritesListAction extends AnAction {
  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getData(Project.KEY);
    if (project != null) {
      doAddNewFavoritesList(project);
    }
  }

  @RequiredUIAccess
  public static String doAddNewFavoritesList(final Project project) {
    final FavoritesManagerImpl favoritesManager = FavoritesManagerImpl.getInstance(project);
    final String name = Messages.showInputDialog(
      project,
      IdeLocalize.promptInputNewFavoritesListName().get(),
      IdeLocalize.titleAddNewFavoritesList().get(),
      UIUtil.getInformationIcon(),
      getUniqueName(project),
      new InputValidator() {
        @Override
        @RequiredUIAccess
        public boolean checkInput(String inputString) {
          return inputString != null && inputString.trim().length() > 0;
        }

        @Override
        @RequiredUIAccess
        public boolean canClose(String inputString) {
          inputString = inputString.trim();
          if (favoritesManager.getAvailableFavoritesListNames().contains(inputString)) {
            Messages.showErrorDialog(project,
              IdeLocalize.errorFavoritesListAlreadyExists(inputString.trim()).get(),
              IdeLocalize.titleUnableToAddFavoritesList().get()
            );
            return false;
          }
          return inputString.length() > 0;
        }
      }
    );
    if (name == null || name.length() == 0) return null;
    favoritesManager.createNewList(name);
    return name;
  }

  private static String getUniqueName(Project project) {
    List<String> names = FavoritesManagerImpl.getInstance(project).getAvailableFavoritesListNames();
    for (int i = 0; ; i++) {
      String newName = IdeLocalize.favoritesListUnnamed(i > 0 ? i : "").get();
      if (names.contains(newName)) continue;
      return newName;
    }
  }
}
