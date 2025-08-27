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

import consulo.annotation.component.ActionImpl;
import consulo.ide.impl.idea.ide.favoritesTreeView.FavoritesManagerImpl;
import consulo.ide.impl.idea.ide.favoritesTreeView.FavoritesTreeViewPanel;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnSeparator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author anna
 * @since 2005-03-03
 */
@ActionImpl(id = "AddAllToFavorites")
public class AddAllToFavoritesActionGroup extends ActionGroup {
    public AddAllToFavoritesActionGroup() {
        super(ActionLocalize.groupAddalltofavoritesText());
        setPopup(true);
    }

    @Override
    @Nonnull
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        if (e == null) {
            return AnAction.EMPTY_ARRAY;
        }
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return AnAction.EMPTY_ARRAY;
        }
        List<String> listNames = FavoritesManagerImpl.getInstance(project).getAvailableFavoritesListNames();
        List<String> availableFavoritesLists = FavoritesManagerImpl.getInstance(project).getAvailableFavoritesListNames();
        availableFavoritesLists.remove(e.getData(FavoritesTreeViewPanel.FAVORITES_LIST_NAME_DATA_KEY));
        if (availableFavoritesLists.isEmpty()) {
            return new AnAction[]{new AddAllOpenFilesToNewFavoritesListAction()};
        }

        AnAction[] actions = new AnAction[listNames.size() + 2];
        int idx = 0;
        for (String favoritesList : listNames) {
            actions[idx++] = new AddAllOpenFilesToFavorites(favoritesList);
        }
        actions[idx++] = AnSeparator.getInstance();
        actions[idx] = new AddAllOpenFilesToNewFavoritesListAction();
        return actions;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabled(AddToFavoritesAction.canCreateNodes(e));
    }
}
