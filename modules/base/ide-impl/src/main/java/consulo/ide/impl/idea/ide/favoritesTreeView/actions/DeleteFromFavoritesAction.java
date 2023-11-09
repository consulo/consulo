/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import consulo.bookmark.ui.view.FavoritesListNode;
import consulo.bookmark.ui.view.FavoritesListProvider;
import consulo.bookmark.ui.view.FavoritesTreeNodeDescriptor;
import consulo.ide.IdeBundle;
import consulo.ui.ex.awt.dnd.DnDAwareTree;
import consulo.ide.impl.idea.ide.favoritesTreeView.*;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.dataContext.DataContext;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.ui.ex.awt.CommonActionsPanel;
import consulo.ide.impl.idea.util.IconUtil;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;
import java.util.*;

/**
 * @author anna
 * @author Konstantin Bulenkov
 */
public class DeleteFromFavoritesAction extends AnAction implements DumbAware {
  private static final Logger LOG = Logger.getInstance(DeleteFromFavoritesAction.class);

  public DeleteFromFavoritesAction() {
    super(IdeBundle.message("action.remove.from.current.favorites"), null, IconUtil.getRemoveIcon());

    registerCustomShortcutSet(CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.REMOVE), null);
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    Project project = e.getData(CommonDataKeys.PROJECT);
    FavoritesViewTreeBuilder builder = dataContext.getData(FavoritesTreeViewPanel.FAVORITES_TREE_BUILDER_KEY);
    if (project == null || builder == null) {
      return;
    }
    Set<Object> selection = builder.getSelectedElements();
    if (selection.isEmpty()) {
      return;
    }
    FavoritesManagerImpl favoritesManager = FavoritesManagerImpl.getInstance(project);
    String listName = dataContext.getData(FavoritesTreeViewPanel.FAVORITES_LIST_NAME_DATA_KEY);
    FavoritesListProvider provider = favoritesManager.getListProvider(listName);
    if (provider != null && provider.willHandle(CommonActionsPanel.Buttons.REMOVE, project, selection)) {
      provider.handle(CommonActionsPanel.Buttons.REMOVE, project, selection, builder.getTree());
      return;
    }
    FavoritesTreeNodeDescriptor[] roots = dataContext.getData(FavoritesTreeViewPanel.CONTEXT_FAVORITES_ROOTS_DATA_KEY);
    final DnDAwareTree tree = dataContext.getData(FavoritesTreeViewPanel.FAVORITES_TREE_KEY);

    assert roots != null && tree != null;
    Map<String, List<AbstractTreeNode>> toRemove = new HashMap<String, List<AbstractTreeNode>>();
    for (FavoritesTreeNodeDescriptor root : roots) {
      final AbstractTreeNode node = root.getElement();
      if (node instanceof FavoritesListNode) {
        favoritesManager.removeFavoritesList((String)node.getValue());
      }
      else {
        final FavoritesListNode listNode = FavoritesTreeUtil.extractParentList(root);
        LOG.assertTrue(listNode != null);
        final String name = listNode.getName();
        if (!toRemove.containsKey(name)) {
          toRemove.put(name, new ArrayList<AbstractTreeNode>());
        }
        toRemove.get(name).add(node);
      }
    }

    for (String name : toRemove.keySet()) {
      favoritesManager.removeRoot(name, toRemove.get(name));
    }
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setText(getTemplatePresentation().getText());
    final DataContext dataContext = e.getDataContext();
    Project project = e.getData(CommonDataKeys.PROJECT);
    FavoritesViewTreeBuilder builder = dataContext.getData(FavoritesTreeViewPanel.FAVORITES_TREE_BUILDER_KEY);
    if (project == null || builder == null) {
      e.getPresentation().setEnabled(false);
      return;
    }
    Set<Object> selection = builder.getSelectedElements();
    String listName = dataContext.getData(FavoritesTreeViewPanel.FAVORITES_LIST_NAME_DATA_KEY);

    FavoritesManagerImpl favoritesManager = FavoritesManagerImpl.getInstance(project);
    FavoritesListProvider provider = favoritesManager.getListProvider(listName);
    if (provider != null) {
      boolean willHandle = provider.willHandle(CommonActionsPanel.Buttons.REMOVE, project, selection);
      e.getPresentation().setEnabled(willHandle);
      if (willHandle) {
        e.getPresentation().setText(provider.getCustomName(CommonActionsPanel.Buttons.REMOVE));
      }
      return;
    }

    FavoritesTreeNodeDescriptor[] roots = dataContext.getData(FavoritesTreeViewPanel.CONTEXT_FAVORITES_ROOTS_DATA_KEY);

    if (roots == null || roots.length == 0 || selection.isEmpty()) {
      e.getPresentation().setEnabled(false);
      return;
    }
    for (Object o : selection) {
      if (o instanceof AbstractTreeNode) {
        AbstractTreeNode node = (AbstractTreeNode)o;
        int deep = getDeep(node);
        if (deep != 2 && deep != 3) {//favorite list or it's nested "root"
          e.getPresentation().setEnabled(false);
          return;
        }
      }
      else {
        e.getPresentation().setEnabled(false);
        return;
      }
    }
  }

  private static int getDeep(AbstractTreeNode node) {
    int result = 0;
    while (node != null) {
      node = (AbstractTreeNode)node.getParent();
      result++;
    }
    return result;
  }
}
