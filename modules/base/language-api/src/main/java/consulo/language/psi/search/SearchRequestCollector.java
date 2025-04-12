// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.psi.search;

import consulo.application.AccessRule;
import consulo.application.Application;
import consulo.content.scope.SearchScope;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import java.util.*;
import java.util.function.Predicate;

/**
 * @author peter
 */
public class SearchRequestCollector {
    private final Object lock = new Object();
    private final List<PsiSearchRequest> myWordRequests = new ArrayList<>();
    private final List<QuerySearchRequest> myQueryRequests = new ArrayList<>();
    private final List<Predicate<Predicate<? super PsiReference>>> myCustomSearchActions = new ArrayList<>();
    private final SearchSession mySession;

    public SearchRequestCollector(@Nonnull SearchSession session) {
        mySession = session;
    }

    @Nonnull
    public SearchSession getSearchSession() {
        return mySession;
    }

    public void searchWord(
        @Nonnull String word,
        @Nonnull SearchScope searchScope,
        boolean caseSensitive,
        @Nonnull PsiElement searchTarget
    ) {
        short searchContext = (short)(UsageSearchContext.IN_CODE | UsageSearchContext.IN_FOREIGN_LANGUAGES | UsageSearchContext.IN_COMMENTS
            | (searchTarget instanceof PsiFileSystemItem ? UsageSearchContext.IN_STRINGS : 0));
        searchWord(word, searchScope, searchContext, caseSensitive, searchTarget);
    }

    public void searchWord(
        @Nonnull String word,
        @Nonnull SearchScope searchScope,
        short searchContext,
        boolean caseSensitive,
        @Nonnull PsiElement searchTarget
    ) {
        searchWord(
            word,
            searchScope,
            searchContext,
            caseSensitive,
            getContainerName(searchTarget),
            searchTarget,
            new SingleTargetRequestResultProcessor(searchTarget)
        );
    }

    private void searchWord(
        @Nonnull String word,
        @Nonnull SearchScope searchScope,
        short searchContext,
        boolean caseSensitive,
        String containerName,
        PsiElement searchTarget,
        @Nonnull RequestResultProcessor processor
    ) {
        if (!makesSenseToSearch(word, searchScope)) {
            return;
        }

        Collection<PsiSearchRequest> requests = null;
        if (searchTarget != null && searchScope instanceof GlobalSearchScope
            && ((searchContext & UsageSearchContext.IN_CODE) != 0 || searchContext == UsageSearchContext.ANY)) {
            SearchScope restrictedCodeUsageSearchScope = AccessRule.read(() -> ScopeOptimizer.calculateOverallRestrictedUseScope(
                Application.get().getExtensionList(ScopeOptimizer.class),
                searchTarget
            ));
            if (restrictedCodeUsageSearchScope != null) {
                short exceptCodeSearchContext = searchContext == UsageSearchContext.ANY
                    ? UsageSearchContext.IN_COMMENTS | UsageSearchContext.IN_STRINGS
                    | UsageSearchContext.IN_FOREIGN_LANGUAGES | UsageSearchContext.IN_PLAIN_TEXT
                    : (short)(searchContext ^ UsageSearchContext.IN_CODE);
                SearchScope searchCodeUsageEffectiveScope = searchScope.intersectWith(restrictedCodeUsageSearchScope);
                requests = Arrays.asList(
                    new PsiSearchRequest(
                        searchCodeUsageEffectiveScope,
                        word,
                        UsageSearchContext.IN_CODE,
                        caseSensitive,
                        containerName,
                        processor
                    ),
                    new PsiSearchRequest(searchScope, word, exceptCodeSearchContext, caseSensitive, containerName, processor)
                );
            }
        }
        if (requests == null) {
            requests =
                Collections.singleton(new PsiSearchRequest(searchScope, word, searchContext, caseSensitive, containerName, processor));
        }

        synchronized (lock) {
            myWordRequests.addAll(requests);
        }
    }

    public void searchWord(
        @Nonnull String word,
        @Nonnull SearchScope searchScope,
        short searchContext,
        boolean caseSensitive,
        @Nonnull PsiElement searchTarget,
        @Nonnull RequestResultProcessor processor
    ) {
        searchWord(word, searchScope, searchContext, caseSensitive, getContainerName(searchTarget), searchTarget, processor);
    }

    private static String getContainerName(@Nonnull PsiElement target) {
        return AccessRule.read(() -> {
            PsiElement container = getContainer(target);
            return container instanceof PsiNamedElement namedElement ? namedElement.getName() : null;
        });
    }

    private static PsiElement getContainer(@Nonnull PsiElement refElement) {
        for (ContainerProvider provider : ContainerProvider.EP_NAME.getExtensionList()) {
            PsiElement container = provider.getContainer(refElement);
            if (container != null) {
                return container;
            }
        }
        // it's assumed that in the general case of unknown language the .getParent() will lead to reparse,
        // (all these Javascript stubbed methods under non-stubbed block statements under stubbed classes - meh)
        // so just return null instead of refElement.getParent() here to avoid making things worse.
        return null;
    }

    /**
     * @deprecated use {@link #searchWord(String, SearchScope, short, boolean, PsiElement)}
     */
    @Deprecated
    public void searchWord(
        @Nonnull String word,
        @Nonnull SearchScope searchScope,
        short searchContext,
        boolean caseSensitive,
        @Nonnull RequestResultProcessor processor
    ) {
        searchWord(word, searchScope, searchContext, caseSensitive, null, null, processor);
    }

    private static boolean makesSenseToSearch(@Nonnull String word, @Nonnull SearchScope searchScope) {
        return !(searchScope instanceof LocalSearchScope localSearchScope && localSearchScope.getScope().length == 0)
            && searchScope != GlobalSearchScope.EMPTY_SCOPE && !StringUtil.isEmpty(word);
    }

    public void searchQuery(@Nonnull QuerySearchRequest request) {
        assert request.collector != this;
        assert request.collector.getSearchSession() == mySession;
        synchronized (lock) {
            myQueryRequests.add(request);
        }
    }

    public void searchCustom(@Nonnull Predicate<Predicate<? super PsiReference>> searchAction) {
        synchronized (lock) {
            myCustomSearchActions.add(searchAction);
        }
    }

    @Nonnull
    public List<QuerySearchRequest> takeQueryRequests() {
        return takeRequests(myQueryRequests);
    }

    @Nonnull
    private <T> List<T> takeRequests(@Nonnull List<? extends T> list) {
        synchronized (lock) {
            List<T> requests = new ArrayList<>(list);
            list.clear();
            return requests;
        }
    }

    @Nonnull
    public List<PsiSearchRequest> takeSearchRequests() {
        return takeRequests(myWordRequests);
    }

    @Nonnull
    public List<Predicate<Predicate<? super PsiReference>>> takeCustomSearchActions() {
        return takeRequests(myCustomSearchActions);
    }

    @Override
    public String toString() {
        return myWordRequests.toString().replace(',', '\n') + ";" + myQueryRequests;
    }
}
