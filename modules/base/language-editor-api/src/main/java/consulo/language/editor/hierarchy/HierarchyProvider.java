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

package consulo.language.editor.hierarchy;

import consulo.dataContext.DataContext;
import consulo.language.extension.LanguageExtension;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Implement this interface to provide hierarchy browsing actions (Type Hierarchy, Method Hierarchy,
 * Call Hierarchy) for a custom language.
 *
 * @author yole
 */
public interface HierarchyProvider extends LanguageExtension {
  /**
   * Returns the element for which the hierarchy should be displayed.
   *
   * @param dataContext the data context for the action invocation.
   * @return the target element, or null if the action is not applicable in this context.
   */
  @Nullable
  PsiElement getTarget(@Nonnull DataContext dataContext);

  /**
   * Creates a browser for viewing the hierarchy of the specified element.
   *
   * @param target the element to view the hierarchy for.
   * @return the browser instance.
   */
  @Nonnull
  HierarchyBrowser createHierarchyBrowser(final PsiElement target);

  /**
   * Notifies that the toolwindow has been shown and the specified browser is currently being displayed.
   *
   * @param hierarchyBrowser the browser instance created by {@link #createHierarchyBrowser(PsiElement)}.
   */
  void browserActivated(@Nonnull HierarchyBrowser hierarchyBrowser);
}
