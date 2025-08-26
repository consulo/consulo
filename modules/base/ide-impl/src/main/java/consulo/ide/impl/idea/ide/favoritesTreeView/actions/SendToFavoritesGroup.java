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
import consulo.bookmark.ui.view.FavoritesTreeNodeDescriptor;
import consulo.ide.impl.idea.ide.favoritesTreeView.FavoritesManagerImpl;
import consulo.ide.impl.idea.ide.favoritesTreeView.FavoritesTreeViewPanel;
import consulo.ide.localize.IdeLocalize;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnSeparator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author anna
 * @since 2005-02-24
 */
@ActionImpl(id = "SendToFavoritesGroup")
public class SendToFavoritesGroup extends ActionGroup {
    public SendToFavoritesGroup() {
        super(ActionLocalize.groupSendtofavoritesgroupText());
        setPopup(true);
    }

    @Nonnull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        if (e == null) {
            return EMPTY_ARRAY;
        }
        Project project = e.getRequiredData(Project.KEY);
        List<String> availableFavoritesLists = FavoritesManagerImpl.getInstance(project).getAvailableFavoritesListNames();
        availableFavoritesLists.remove(e.getData(FavoritesTreeViewPanel.FAVORITES_LIST_NAME_DATA_KEY));
        if (availableFavoritesLists.isEmpty()) {
            return new AnAction[]{new SendToNewFavoritesListAction()};
        }

        List<AnAction> actions = new ArrayList<>();

        for (String list : availableFavoritesLists) {
            actions.add(new SendToFavoritesAction(list));
        }
        actions.add(AnSeparator.getInstance());
        actions.add(new SendToNewFavoritesListAction());
        return actions.toArray(new AnAction[actions.size()]);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);
        e.getPresentation().setVisible(
            SendToFavoritesAction.isEnabled(e) && e.hasData(FavoritesTreeViewPanel.FAVORITES_LIST_NAME_DATA_KEY)
        );
    }

    private static class SendToNewFavoritesListAction extends AnAction {
        public SendToNewFavoritesListAction() {
            super(IdeLocalize.actionSendToNewFavoritesList());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            Project project = e.getRequiredData(Project.KEY);
            FavoritesTreeNodeDescriptor[] roots = e.getRequiredData(FavoritesTreeViewPanel.CONTEXT_FAVORITES_ROOTS_DATA_KEY);
            String listName = e.getData(FavoritesTreeViewPanel.FAVORITES_LIST_NAME_DATA_KEY);

            String newName = AddNewFavoritesListAction.doAddNewFavoritesList(project);
            if (newName != null) {
                new SendToFavoritesAction(newName).doSend(FavoritesManagerImpl.getInstance(project), roots, listName);
            }
        }
    }
}
