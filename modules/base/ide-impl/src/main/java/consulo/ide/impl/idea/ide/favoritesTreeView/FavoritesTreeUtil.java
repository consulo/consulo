/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.favoritesTreeView;

import consulo.bookmark.ui.view.FavoritesListNode;
import consulo.bookmark.ui.view.FavoritesListProvider;
import consulo.bookmark.ui.view.FavoritesTreeNodeDescriptor;
import consulo.ui.ex.awt.dnd.DnDAwareTree;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.ui.ex.awt.tree.Tree;
import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Irina.Chernushina
 * @since 2012-06-09
 */
public class FavoritesTreeUtil {
  @Nonnull
  public static FavoritesTreeNodeDescriptor[] getSelectedNodeDescriptors(DnDAwareTree tree) {
    TreePath[] path = tree.getSelectionPaths();
    if (path == null) {
      return FavoritesTreeNodeDescriptor.EMPTY_ARRAY;
    }
    ArrayList<FavoritesTreeNodeDescriptor> result = new ArrayList<FavoritesTreeNodeDescriptor>();
    for (TreePath treePath : path) {
      DefaultMutableTreeNode lastPathNode = (DefaultMutableTreeNode)treePath.getLastPathComponent();
      Object userObject = lastPathNode.getUserObject();
      if (!(userObject instanceof FavoritesTreeNodeDescriptor)) {
        continue;
      }
      FavoritesTreeNodeDescriptor treeNodeDescriptor = (FavoritesTreeNodeDescriptor)userObject;
      result.add(treeNodeDescriptor);
    }
    return result.toArray(new FavoritesTreeNodeDescriptor[result.size()]);
  }

  public static List<AbstractTreeNode> getLogicalPathToSelected(Tree tree) {
    List<AbstractTreeNode> result = new ArrayList<AbstractTreeNode>();
    TreePath selectionPath = tree.getSelectionPath();
    return getLogicalPathTo(result, selectionPath);
  }

  public static List<Integer> getLogicalIndexPathTo(TreePath selectionPath) {
    List<Integer> result = new ArrayList<Integer>();
    Object component = selectionPath.getLastPathComponent();
    if (component instanceof DefaultMutableTreeNode) {
      Object uo = ((DefaultMutableTreeNode)component).getUserObject();
      if (uo instanceof FavoritesTreeNodeDescriptor) {
        AbstractTreeNode treeNode = ((FavoritesTreeNodeDescriptor)uo).getElement();
        while ((!(treeNode instanceof FavoritesListNode)) && treeNode != null) {
//          final int idx = getIndex(treeNode.getParent().getChildren(), treeNode);
//          if (idx == -1) return null;
          result.add(treeNode.getIndex());
          treeNode = (AbstractTreeNode)treeNode.getParent();
        }
        Collections.reverse(result);
        return result;
      }
    }
    return Collections.emptyList();
  }

  /*private static int getIndex(Collection<AbstractTreeNode> children, AbstractTreeNode node) {
    int idx = 0;
    for (AbstractTreeNode child : children) {
      if (child == node) {
        return idx;
      }
      ++ idx;
    }
    assert false;
    return -1;
  }*/

  public static List<AbstractTreeNode> getLogicalPathTo(List<AbstractTreeNode> result, TreePath selectionPath) {
    Object component = selectionPath.getLastPathComponent();
    if (component instanceof DefaultMutableTreeNode) {
      Object uo = ((DefaultMutableTreeNode)component).getUserObject();
      if (uo instanceof FavoritesTreeNodeDescriptor) {
        AbstractTreeNode treeNode = ((FavoritesTreeNodeDescriptor)uo).getElement();
        while ((!(treeNode instanceof FavoritesListNode)) && treeNode != null) {
          result.add(treeNode);
          treeNode = (AbstractTreeNode)treeNode.getParent();
        }
        Collections.reverse(result);
        return result;
      }
    }
    return Collections.emptyList();
  }

  @Nullable
  public static FavoritesListNode extractParentList(FavoritesTreeNodeDescriptor descriptor) {
    AbstractTreeNode node = descriptor.getElement();
    AbstractTreeNode current = node;
    while (current != null) {
      if (current instanceof FavoritesListNode) {
        return (FavoritesListNode)current;
      }
      current = (AbstractTreeNode)current.getParent();
    }
    return null;
  }

  static FavoritesListProvider getProvider(@Nonnull FavoritesManagerImpl manager, @Nonnull FavoritesTreeNodeDescriptor descriptor) {
    AbstractTreeNode treeNode = descriptor.getElement();
    while (treeNode != null && (!(treeNode instanceof FavoritesListNode))) {
      treeNode = (AbstractTreeNode)treeNode.getParent();
    }
    if (treeNode != null) {
      String name = ((FavoritesListNode)treeNode).getValue();
      return manager.getListProvider(name);
    }
    return null;
  }
}
