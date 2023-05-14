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

package consulo.ide.impl.idea.ide.favoritesTreeView;

import consulo.ide.IdeBundle;
import consulo.annotation.access.RequiredReadAction;
import consulo.ui.ex.tree.PresentationData;
import consulo.project.ui.view.tree.ViewSettings;
import consulo.ide.impl.idea.ide.projectView.impl.ProjectTreeStructure;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.component.extension.Extensions;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.ide.impl.idea.util.ArrayUtil;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class FavoritesTreeStructure extends ProjectTreeStructure {
  public FavoritesTreeStructure(Project project) {
    super(project, FavoritesViewTreeBuilder.ID);
  }

  @Override
  protected AbstractTreeNode createRoot(final Project project, ViewSettings settings) {
    return new FavoritesRootNode(project);
  }

  public void rootsChanged() {
    ((FavoritesRootNode)getRootElement()).rootsChanged();
  }


  @Override
  public Object[] getChildElements(Object element) {
    if (!(element instanceof AbstractTreeNode)) {
      return ArrayUtil.EMPTY_OBJECT_ARRAY;
    }

    final AbstractTreeNode favTreeElement = (AbstractTreeNode)element;
    try {
      if (!(element instanceof FavoritesListNode)) {
        return super.getChildElements(favTreeElement);
      }

      final List<AbstractTreeNode> result = new ArrayList<AbstractTreeNode>();
      final FavoritesListNode listNode = (FavoritesListNode)element;
      if (listNode.getProvider() != null) {
        return ArrayUtil.toObjectArray(listNode.getChildren());
      }
      final Collection<AbstractTreeNode> roots = FavoritesListNode.getFavoritesRoots(myProject, listNode.getName(), listNode);
      for (AbstractTreeNode<?> abstractTreeNode : roots) {
        final Object value = abstractTreeNode.getValue();

        if (value == null) continue;
        if (value instanceof PsiElement && !((PsiElement)value).isValid()) continue;
        if (value instanceof SmartPsiElementPointer && ((SmartPsiElementPointer)value).getElement() == null) continue;

        boolean invalid = false;
        for (FavoriteNodeProvider nodeProvider : Extensions.getExtensions(FavoriteNodeProvider.EP_NAME, myProject)) {
          if (nodeProvider.isInvalidElement(value)) {
            invalid = true;
            break;
          }
        }
        if (invalid) continue;

        result.add(abstractTreeNode);
      }
      //myFavoritesRoots = result;
      //if (result.isEmpty()) {
      //  result.add(getEmptyScreen());
      //}
      return ArrayUtil.toObjectArray(result);
    }
    catch (Exception e) {
    }

    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }

  private AbstractTreeNode<String> getEmptyScreen() {
    return new AbstractTreeNode<String>(myProject, IdeBundle.message("favorites.empty.screen")) {
      @RequiredReadAction
      @Override
      @Nonnull
      public Collection<AbstractTreeNode> getChildren() {
        return Collections.emptyList();
      }

      @Override
      public void update(final PresentationData presentation) {
        presentation.setPresentableText(getValue());
      }
    };
  }

  @Override
  public Object getParentElement(Object element) {
    AbstractTreeNode parent = null;
    if (element == getRootElement()) {
      return null;
    }
    if (element instanceof AbstractTreeNode) {
      parent = (AbstractTreeNode)((AbstractTreeNode)element).getParent();
    }
    if (parent == null) {
      return getRootElement();
    }
    return parent;
  }

  @Override
  @Nonnull
  public NodeDescriptor createDescriptor(Object element, NodeDescriptor parentDescriptor) {
    return new FavoritesTreeNodeDescriptor(myProject, parentDescriptor, (AbstractTreeNode)element);
  }
}
