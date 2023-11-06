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
package consulo.language.psi.scope;

import consulo.content.scope.SearchScope;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.UseScopeEnlarger;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class PsiSearchScopeUtil {
  /**
   * Returns the scope in which references to the specified element are searched. This scope includes the result of
   * {@link PsiElement#getUseScope()} and also the results returned from the registered
   * {@link UseScopeEnlarger} instances.
   *
   * @param element the element to return the use scope form.
   * @return the search scope instance.
   */
  @Nonnull
  public static SearchScope getUseScope(@Nonnull PsiElement element) {
    Project project = element.getProject();
    SearchScope scope = element.getUseScope();
    for (UseScopeEnlarger enlarger : project.getExtensionList(UseScopeEnlarger.class)) {
      final SearchScope additionalScope = enlarger.getAdditionalUseScope(element);
      if (additionalScope != null) {
        scope = scope.union(additionalScope);
      }
    }
    return scope;
  }

  @Nullable
  public static SearchScope union(@Nullable SearchScope a, @Nullable SearchScope b) {
    return a == null ? b : b == null ? a : a.union(b);
  }

  /**
   * @deprecated
   * Use com.intellij.psi.search.SearchScope#union(com.intellij.psi.search.SearchScope)
   */
  @Deprecated
  @Nonnull
  public static SearchScope scopesUnion(@Nonnull SearchScope scope1, @Nonnull SearchScope scope2) {
    return scope1.union(scope2);
  }

  public static boolean isInScope(@Nonnull SearchScope scope, @Nonnull PsiElement element) {
    if (scope instanceof LocalSearchScope) {
      LocalSearchScope local = (LocalSearchScope)scope;
      return isInScope(local, element);
    }
    else {
      GlobalSearchScope globalScope = (GlobalSearchScope)scope;
      return isInScope(globalScope, element);
    }
  }

  public static boolean isInScope(@Nonnull GlobalSearchScope globalScope, @Nonnull PsiElement element) {
    PsiFile file = element.getContainingFile();
    if (file == null) {
      return true;
    }
    final PsiElement context = file.getContext();
    if (context != null) file = context.getContainingFile();
    if (file == null) return false;
    VirtualFile virtualFile = file.getVirtualFile();
    return virtualFile == null || globalScope.contains(file.getVirtualFile());
  }

  public static boolean isInScope(@Nonnull LocalSearchScope local, @Nonnull PsiElement element) {
    PsiElement[] scopeElements = local.getScope();
    for (final PsiElement scopeElement : scopeElements) {
      if (PsiTreeUtil.isAncestor(scopeElement, element, false)) return true;
    }
    return false;
  }
}