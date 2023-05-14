/*
 * Copyright 2000-2019 JetBrains s.r.o.
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
package consulo.language.psi.search;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.progress.ProgressManager;
import consulo.content.scope.SearchScope;
import consulo.language.psi.PsiElement;
import consulo.language.psi.scope.GlobalSearchScope;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * A general interface to perform PsiElement's search scope optimization. The interface should be used only for optimization purposes.
 * It's used in:
 * <ol>
 * <li>
 * {@link PsiSearchHelper#getUseScope(PsiElement)},
 * {@link com.intellij.psi.impl.search.PsiSearchHelperImpl#USE_SCOPE_OPTIMIZER_EP_NAME}
 * to perform optimization of PsiElement's use scope.
 * </li>
 * <li>
 * {@link SearchRequestCollector#searchWord(String, SearchScope, short, boolean, PsiElement)},
 * {@link SearchRequestCollector#CODE_USAGE_SCOPE_OPTIMIZER_EP_NAME}
 * to exclude a scope without references in code from a usages search when the search with {@link UsageSearchContext#IN_CODE} or {@link UsageSearchContext#ANY}
 * context was requested.
 * </li>
 * </ol>
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ScopeOptimizer {

  /**
   * @return is null when given optimizer can't provide a scope to exclude
   * @deprecated use {@link ScopeOptimizer#getRestrictedUseScope(PsiElement)} instead.
   */
  @Deprecated
  @Nullable
  default GlobalSearchScope getScopeToExclude(@Nonnull PsiElement element) {
    return null;
  }

  /**
   * @param element
   * @return is null when given optimizer can't provide a scope to restrict
   */
  @Nullable
  default SearchScope getRestrictedUseScope(@Nonnull PsiElement element) {
    GlobalSearchScope scopeToExclude = getScopeToExclude(element);

    return scopeToExclude == null ? null : GlobalSearchScope.notScope(scopeToExclude);
  }

  @Nullable
  static SearchScope calculateOverallRestrictedUseScope(@Nonnull List<ScopeOptimizer> optimizers, @Nonnull PsiElement element) {
    return optimizers.stream().peek(optimizer -> ProgressManager.checkCanceled()).map(optimizer -> optimizer.getRestrictedUseScope(element)).filter(Objects::nonNull)
            .reduce(SearchScope::intersectWith).orElse(null);
  }
}
