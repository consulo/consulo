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

import consulo.annotation.access.RequiredReadAction;
import consulo.bookmark.icon.BookmarkIconGroup;
import consulo.ide.impl.idea.ide.favoritesTreeView.FavoritesManagerImpl;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

import java.util.Collection;

/**
 * @author anna
 * @since 2005-02-28
 */
class AddToNewFavoritesListAction extends AnAction {
    public AddToNewFavoritesListAction() {
        super(
            IdeLocalize.actionAddToNewFavoritesList(),
            IdeLocalize.actionAddToNewFavoritesList().map(Presentation.NO_MNEMONIC),
            BookmarkIconGroup.actionAddbookmarkslist()
        );
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        Collection<AbstractTreeNode> nodesToAdd = AddToFavoritesAction.getNodesToAdd(e.getDataContext(), true);
        if (nodesToAdd != null) {
            String newName = AddNewFavoritesListAction.doAddNewFavoritesList(project);
            if (newName != null) {
                FavoritesManagerImpl.getInstance(project).addRoots(newName, nodesToAdd);
            }
        }
    }

    @Override
    @RequiredUIAccess
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(AddToFavoritesAction.canCreateNodes(e));
    }
}
