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

package consulo.bookmark.ui.view;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.dataContext.DataContext;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.ui.view.tree.ViewSettings;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;

/**
 * Returns the nodes which should be added to the Favorites for the given data context.
 *
 * @author yole
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface BookmarkNodeProvider {
  @Nullable
  Collection<AbstractTreeNode> getFavoriteNodes(DataContext context, ViewSettings viewSettings);

  @Nullable
  default AbstractTreeNode createNode(Project project, Object element, ViewSettings viewSettings) {
    return null;
  }

  /**
   * Checks if the specified project view node element (the value of {@link AbstractTreeNode}) contains
   * the specified virtual file as one of its children.
   *
   * @param element the value element of a project view node.
   * @param vFile   the file to check.
   * @return true if the file is contained, false if not or if <code>element</code> is not an element supported by this provider.
   */
  boolean elementContainsFile(Object element, VirtualFile vFile);

  /**
   * Returns the weight of the specified project view node element to use when sorting the favorites list.
   *
   * @param element      the element for which the weight is requested.
   * @param isSortByType
   * @return the weight, or -1 if <code>element</code> is not an element supported by this provider.
   */
  int getElementWeight(Object element, boolean isSortByType);

  /**
   * Returns the location text (grey text in parentheses) to display in the Favorites view for the specified element.
   *
   * @param element the element for which the location is requested.
   * @return the location text, or -1 if <code>element</code> is not an element supported by this provider.
   */
  @Nullable
  String getElementLocation(Object element);

  /**
   * Checks if the specified element is invalid and needs to be removed from the tree.
   *
   * @param element the element to check.
   * @return true if the element is invalid, false if the element is valid or not supported by this provider.
   */
  boolean isInvalidElement(Object element);

  /**
   * Returns the identifier used to persist favorites for this provider.
   *
   * @return the string identifier.
   */
  @Nonnull
  String getFavoriteTypeId();

  /**
   * Returns the persistable URL for the specified element.
   *
   * @param element
   * @return the URL, or null if the element is not supported by this provider.
   */
  @Nullable
  String getElementUrl(Object element);

  /**
   * Returns the name of the module containing the specified element.
   *
   * @param element
   * @return the name of the module, or null if the element is not supported by this provider or the module name is unknown.
   */
  @Nullable
  String getElementModuleName(Object element);

  /**
   * Returns the path of node objects to be added to the favorites tree for the specified persisted URL and module name.
   *
   * @param project    the project to which the favorite is related.
   * @param url        the loaded URL (initially returned from {@link #getElementUrl }).
   * @param moduleName the name of the module containing the element (initially returned from {@link #getElementModuleName})
   * @return the path of objects to be added to the tree, or null if it was not possible to locate an object with the
   * specified URL.
   */
  @Nullable
  Object[] createPathFromUrl(Project project, String url, String moduleName);

  @Nullable
  default PsiElement getPsiElement(Object element) {
    if (element instanceof PsiElement) {
      return (PsiElement)element;
    }
    return null;
  }
}
