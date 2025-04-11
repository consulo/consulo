// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.psi.search;

import consulo.application.util.query.ExtensibleQueryFactory;
import consulo.application.util.query.MergeQuery;
import consulo.application.util.query.Query;
import consulo.application.util.query.UniqueResultsQuery;
import consulo.content.scope.SearchScope;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.PsiSearchScopeUtil;
import consulo.project.Project;
import consulo.project.util.query.DumbAwareSearchParameters;
import consulo.util.collection.HashingStrategy;import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Locates all references to a specified PSI element.
 *
 * @author max
 * @see PsiReference
 */
public class ReferencesSearch extends ExtensibleQueryFactory<PsiReference, ReferencesSearch.SearchParameters> {
    private static final ReferencesSearch INSTANCE = new ReferencesSearch();

    private ReferencesSearch() {
        super(ReferencesSearchQueryExecutor.class);
    }

    public static class SearchParameters implements DumbAwareSearchParameters, consulo.project.util.query.SearchParameters<PsiReference> {
        private final PsiElement myElementToSearch;
        private final SearchScope myScope;
        private volatile SearchScope myEffectiveScope;
        private final boolean myIgnoreAccessScope;
        private final SearchRequestCollector myOptimizer;
        private final Project myProject;
        private final boolean isSharedOptimizer;

        public SearchParameters(
            @Nonnull PsiElement elementToSearch,
            @Nonnull SearchScope scope,
            boolean ignoreAccessScope,
            @Nullable SearchRequestCollector optimizer
        ) {
            myElementToSearch = elementToSearch;
            myScope = scope;
            myIgnoreAccessScope = ignoreAccessScope;
            isSharedOptimizer = optimizer != null;
            myOptimizer = optimizer == null ? new SearchRequestCollector(new SearchSession()) : optimizer;
            myProject = PsiUtilCore.getProjectInReadAction(elementToSearch);
        }

        public SearchParameters(@Nonnull PsiElement elementToSearch, @Nonnull SearchScope scope, boolean ignoreAccessScope) {
            this(elementToSearch, scope, ignoreAccessScope, null);
        }

        @Override
        public final boolean areValid() {
            return isQueryValid();
        }

        @Override
        public boolean isQueryValid() {
            return myElementToSearch.isValid();
        }

        @Override
        @Nonnull
        public Project getProject() {
            return myProject;
        }

        @Nonnull
        public PsiElement getElementToSearch() {
            return myElementToSearch;
        }

        /**
         * @return the user-visible search scope, most often "Project Files" or "Project and Libraries".
         * Searchers most likely need to use {@link #getEffectiveSearchScope()}.
         */
        public SearchScope getScopeDeterminedByUser() {
            return myScope;
        }


        /**
         * @deprecated Same as {@link #getScopeDeterminedByUser()}, use {@link #getEffectiveSearchScope} instead
         */
        @Deprecated
        @Nonnull
        public SearchScope getScope() {
            return myScope;
        }

        public boolean isIgnoreAccessScope() {
            return myIgnoreAccessScope;
        }

        @Nonnull
        public SearchRequestCollector getOptimizer() {
            return myOptimizer;
        }

        @Nonnull
        public SearchScope getEffectiveSearchScope() {
            if (myIgnoreAccessScope) {
                return myScope;
            }

            SearchScope scope = myEffectiveScope;
            if (scope == null) {
                if (!myElementToSearch.isValid()) {
                    return GlobalSearchScope.EMPTY_SCOPE;
                }

                SearchScope useScope = PsiSearchScopeUtil.getUseScope(myElementToSearch);
                myEffectiveScope = scope = myScope.intersectWith(useScope);
            }
            return scope;
        }
    }

    /**
     * Searches for references to the specified element in the scope in which such references are expected to be found, according to
     * dependencies and access rules.
     *
     * @param element the element (declaration) the references to which are requested.
     * @return the query allowing to enumerate the references.
     */
    @Nonnull
    public static Query<PsiReference> search(@Nonnull PsiElement element) {
        return search(element, GlobalSearchScope.allScope(PsiUtilCore.getProjectInReadAction(element)), false);
    }

    /**
     * Searches for references to the specified element in the specified scope.
     *
     * @param element     the element (declaration) the references to which are requested.
     * @param searchScope the scope in which the search is performed.
     * @return the query allowing to enumerate the references.
     */
    @Nonnull
    public static Query<PsiReference> search(@Nonnull PsiElement element, @Nonnull SearchScope searchScope) {
        return search(element, searchScope, false);
    }

    /**
     * Searches for references to the specified element in the specified scope, optionally returning also references which
     * are invalid because of access rules (e.g. references to a private method from a different class).
     *
     * @param element           the element (declaration) the references to which are requested.
     * @param searchScope       the scope in which the search is performed.
     * @param ignoreAccessScope if true, references which are invalid because of access rules are included in the results.
     * @return the query allowing to enumerate the references.
     */
    @Nonnull
    public static Query<PsiReference> search(@Nonnull PsiElement element, @Nonnull SearchScope searchScope, boolean ignoreAccessScope) {
        return search(new SearchParameters(element, searchScope, ignoreAccessScope));
    }

    /**
     * Searches for references to the specified element according to the specified parameters.
     *
     * @param parameters the parameters for the search (contain also the element the references to which are requested).
     * @return the query allowing to enumerate the references.
     */
    @Nonnull
    public static Query<PsiReference> search(@Nonnull SearchParameters parameters) {
        Query<PsiReference> result = INSTANCE.createQuery(parameters);
        if (parameters.isSharedOptimizer) {
            return uniqueResults(result);
        }

        SearchRequestCollector requests = parameters.getOptimizer();

        PsiElement element = parameters.getElementToSearch();

        return uniqueResults(new MergeQuery<>(result, new SearchRequestQuery(PsiUtilCore.getProjectInReadAction(element), requests)));
    }

    @Nonnull
    private static Query<PsiReference> uniqueResults(@Nonnull Query<? extends PsiReference> composite) {
        return new UniqueResultsQuery<>(composite, HashingStrategy.canonical(), ReferenceDescriptor.MAPPER);
    }

    public static void searchOptimized(
        @Nonnull PsiElement element,
        @Nonnull SearchScope searchScope,
        boolean ignoreAccessScope,
        @Nonnull SearchRequestCollector collector,
        @Nonnull Predicate<? super PsiReference> processor
    ) {
        searchOptimized(
            element,
            searchScope,
            ignoreAccessScope,
            collector,
            false,
            (psiReference, collector1) -> processor.test(psiReference)
        );
    }

    public static void searchOptimized(
        @Nonnull PsiElement element,
        @Nonnull SearchScope searchScope,
        boolean ignoreAccessScope,
        @Nonnull SearchRequestCollector collector,
        boolean inReadAction,
        @Nonnull BiPredicate<? super PsiReference, ? super SearchRequestCollector> processor
    ) {
        SearchRequestCollector nested = new SearchRequestCollector(collector.getSearchSession());
        Query<PsiReference> query = search(new SearchParameters(element, searchScope, ignoreAccessScope, nested));
        collector.searchQuery(new QuerySearchRequest(query, nested, inReadAction, processor));
    }
}
