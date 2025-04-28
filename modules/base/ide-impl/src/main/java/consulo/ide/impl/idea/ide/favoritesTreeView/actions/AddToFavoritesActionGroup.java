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
import consulo.ide.impl.idea.ide.favoritesTreeView.FavoritesTreeViewPanel;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnSeparator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * User: anna
 * Date: Mar 3, 2005
 */
public class AddToFavoritesActionGroup extends ActionGroup {
    @Override
    @Nonnull
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        if (e == null) {
            return AnAction.EMPTY_ARRAY;
        }
        final Project project = e.getData(Project.KEY);
        if (project == null) {
            return AnAction.EMPTY_ARRAY;
        }
        final List<String> availableFavoritesLists = FavoritesManagerImpl.getInstance(project).getAvailableFavoritesListNames();
        availableFavoritesLists.remove(e.getDataContext().getData(FavoritesTreeViewPanel.FAVORITES_LIST_NAME_DATA_KEY));
        if (availableFavoritesLists.isEmpty()) {
            return new AnAction[]{new AddToNewFavoritesListAction()};
        }

        AnAction[] actions = new AnAction[availableFavoritesLists.size() + 2];
        int idx = 0;
        for (String favoritesList : availableFavoritesLists) {
            actions[idx++] = new AddToFavoritesAction(favoritesList);
        }
        actions[idx++] = AnSeparator.getInstance();
        actions[idx] = new AddToNewFavoritesListAction();
        return actions;
    }

    @Override
    @RequiredUIAccess
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);
        e.getPresentation().setVisible(AddToFavoritesAction.canCreateNodes(e));
    }
}
