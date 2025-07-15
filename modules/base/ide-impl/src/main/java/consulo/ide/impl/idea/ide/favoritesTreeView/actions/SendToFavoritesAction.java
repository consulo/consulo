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
import consulo.bookmark.ui.view.FavoritesListNode;
import consulo.bookmark.ui.view.FavoritesTreeNodeDescriptor;
import consulo.ide.impl.idea.ide.favoritesTreeView.FavoritesManagerImpl;
import consulo.ide.impl.idea.ide.favoritesTreeView.FavoritesTreeViewPanel;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

import java.util.Collections;

/**
 * @author anna
 * @author Konstantin Bulenkov
 */
public class SendToFavoritesAction extends AnAction {
    private final String toName;

    public SendToFavoritesAction(String name) {
        super(name);
        toName = name;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        FavoritesManagerImpl favoritesManager = FavoritesManagerImpl.getInstance(project);

        FavoritesTreeNodeDescriptor[] roots = e.getRequiredData(FavoritesTreeViewPanel.CONTEXT_FAVORITES_ROOTS_DATA_KEY);
        for (FavoritesTreeNodeDescriptor root : roots) {
            FavoritesTreeNodeDescriptor listNode = root.getFavoritesRoot();
            if (listNode != null && listNode != root && listNode.getElement() instanceof FavoritesListNode favoritesListNode) {
                doSend(favoritesManager, new FavoritesTreeNodeDescriptor[]{root}, favoritesListNode.getName());
            }
        }
    }

    @RequiredReadAction
    public void doSend(FavoritesManagerImpl favoritesManager, FavoritesTreeNodeDescriptor[] roots, String listName) {
        for (FavoritesTreeNodeDescriptor root : roots) {
            AbstractTreeNode rootElement = root.getElement();
            String name = listName;
            if (name == null) {
                name = root.getFavoritesRoot().getName();
            }
            favoritesManager.removeRoot(name, Collections.singletonList(rootElement));
            favoritesManager.addRoots(toName, Collections.singletonList(rootElement));
        }
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabled(isEnabled(e));
    }

    static boolean isEnabled(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        FavoritesTreeNodeDescriptor[] roots = e.getData(FavoritesTreeViewPanel.CONTEXT_FAVORITES_ROOTS_DATA_KEY);
        if (project == null || roots == null || roots.length == 0) {
            return false;
        }
        for (FavoritesTreeNodeDescriptor root : roots) {
            FavoritesTreeNodeDescriptor listNode = root.getFavoritesRoot();
            if (listNode == null || listNode == root || !(listNode.getElement() instanceof FavoritesListNode)) {
                return false;
            }
        }
        return true;
    }
}
