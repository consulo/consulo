/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import consulo.application.ApplicationManager;
import consulo.application.util.function.Computable;
import consulo.application.util.query.ExtensibleQueryFactory;
import consulo.application.util.query.Query;
import consulo.content.scope.SearchScope;
import consulo.language.psi.PsiElement;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * The search is used in two IDE navigation functions namely Go To Implementation (Ctrl+Alt+B) and
 * Quick View Definition (Ctrl+Shift+I). Default searchers produce implementing/overriding methods if the method
 * have been searched and class inheritors for the class.
 */
public class DefinitionsScopedSearch extends ExtensibleQueryFactory<PsiElement, DefinitionsScopedSearch.SearchParameters> {
  public static final DefinitionsScopedSearch INSTANCE = new DefinitionsScopedSearch();

  private DefinitionsScopedSearch() {
    super(DefinitionsScopedSearchExecutor.class);
  }

  public static Query<PsiElement> search(PsiElement definitionsOf) {
    return INSTANCE.createUniqueResultsQuery(new SearchParameters(definitionsOf));
  }

  public static Query<PsiElement> search(PsiElement definitionsOf, SearchScope searchScope) {
    return search(definitionsOf, searchScope, true);
  }

  /**
   * @param checkDeep false for show implementations to present definition only
   */
  public static Query<PsiElement> search(PsiElement definitionsOf, SearchScope searchScope, final boolean checkDeep) {
    return INSTANCE.createUniqueResultsQuery(new SearchParameters(definitionsOf, searchScope, checkDeep));
  }

  public static class SearchParameters {
    private final PsiElement myElement;
    private final SearchScope myScope;
    private final boolean myCheckDeep;

    public SearchParameters(@Nonnull final PsiElement element) {
      this(element, ApplicationManager.getApplication().runReadAction(new Supplier<SearchScope>() {
        @Override
        public SearchScope get() {
          return element.getUseScope();
        }
      }), true);
    }

    public SearchParameters(@Nonnull PsiElement element, @Nonnull SearchScope scope, final boolean checkDeep) {
      myElement = element;
      myScope = scope;
      myCheckDeep = checkDeep;
    }

    @Nonnull
    public PsiElement getElement() {
      return myElement;
    }

    public boolean isCheckDeep() {
      return myCheckDeep;
    }

    @Nonnull
    public SearchScope getScope() {
      return ApplicationManager.getApplication().runReadAction(new Computable<SearchScope>() {
        @Override
        public SearchScope compute() {
          return myScope.intersectWith(PsiSearchHelper.SERVICE.getInstance(myElement.getProject()).getUseScope(myElement));
        }
      });
    }
  }

}
