// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.psi.search;

import consulo.annotation.ReviewAfterIssueFix;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.content.scope.SearchScope;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

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

    public SearchRequestCollector(SearchSession session) {
        mySession = session;
    }

    public SearchSession getSearchSession() {
        return mySession;
    }

    public void searchWord(
        String word,
        SearchScope searchScope,
        boolean caseSensitive,
        PsiElement searchTarget
    ) {
        short searchContext = (short)(UsageSearchContext.IN_CODE | UsageSearchContext.IN_FOREIGN_LANGUAGES | UsageSearchContext.IN_COMMENTS
            | (searchTarget instanceof PsiFileSystemItem ? UsageSearchContext.IN_STRINGS : 0));
        searchWord(word, searchScope, searchContext, caseSensitive, searchTarget);
    }

    public void searchWord(
        String word,
        SearchScope searchScope,
        short searchContext,
        boolean caseSensitive,
        PsiElement searchTarget
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
        String word,
        SearchScope searchScope,
        short searchContext,
        boolean caseSensitive,
        @Nullable String containerName,
        @Nullable PsiElement searchTarget,
        RequestResultProcessor processor
    ) {
        if (!makesSenseToSearch(word, searchScope)) {
            return;
        }

        Collection<PsiSearchRequest> requests = null;
        if (searchTarget != null && searchScope instanceof GlobalSearchScope
            && ((searchContext & UsageSearchContext.IN_CODE) != 0 || searchContext == UsageSearchContext.ANY)) {
            @ReviewAfterIssueFix(value = "github.com/uber/NullAway/issues/1500", todo = "Remove NullAway suppression")
            @SuppressWarnings("NullAway")
            SearchScope restrictedCodeUsageSearchScope = ReadAction.compute(() -> ScopeOptimizer.calculateOverallRestrictedUseScope(
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
        String word,
        SearchScope searchScope,
        short searchContext,
        boolean caseSensitive,
        PsiElement searchTarget,
        RequestResultProcessor processor
    ) {
        searchWord(word, searchScope, searchContext, caseSensitive, getContainerName(searchTarget), searchTarget, processor);
    }

    @ReviewAfterIssueFix(value = "github.com/uber/NullAway/issues/1500", todo = "Remove NullAway suppression")
    @SuppressWarnings("NullAway")
    private static @Nullable String getContainerName(PsiElement target) {
        return ReadAction.compute(() -> {
            PsiElement container = getContainer(target);
            return container instanceof PsiNamedElement namedElement ? namedElement.getName() : null;
        });
    }

    private static @Nullable PsiElement getContainer(PsiElement refElement) {
        // it's assumed that in the general case of unknown language the .getParent() will lead to reparse,
        // (all these Javascript stubbed methods under non-stubbed block statements under stubbed classes - meh)
        // so just return null instead of refElement.getParent() here to avoid making things worse.
        return refElement.getApplication().getExtensionPoint(ContainerProvider.class)
            .computeSafeIfAny(provider -> provider.getContainer(refElement));
    }

    /**
     * @deprecated use {@link #searchWord(String, SearchScope, short, boolean, PsiElement)}
     */
    @Deprecated
    public void searchWord(
        String word,
        SearchScope searchScope,
        short searchContext,
        boolean caseSensitive,
        RequestResultProcessor processor
    ) {
        searchWord(word, searchScope, searchContext, caseSensitive, null, null, processor);
    }

    private static boolean makesSenseToSearch(String word, SearchScope searchScope) {
        return !(searchScope instanceof LocalSearchScope localSearchScope && localSearchScope.getScope().length == 0)
            && searchScope != GlobalSearchScope.EMPTY_SCOPE && StringUtil.isNotEmpty(word);
    }

    public void searchQuery(QuerySearchRequest request) {
        assert request.collector != this;
        assert request.collector.getSearchSession() == mySession;
        synchronized (lock) {
            myQueryRequests.add(request);
        }
    }

    public void searchCustom(Predicate<Predicate<? super PsiReference>> searchAction) {
        synchronized (lock) {
            myCustomSearchActions.add(searchAction);
        }
    }

    public List<QuerySearchRequest> takeQueryRequests() {
        return takeRequests(myQueryRequests);
    }

    private <T> List<T> takeRequests(List<? extends T> list) {
        synchronized (lock) {
            List<T> requests = new ArrayList<>(list);
            list.clear();
            return requests;
        }
    }

    public List<PsiSearchRequest> takeSearchRequests() {
        return takeRequests(myWordRequests);
    }

    
    public List<Predicate<Predicate<? super PsiReference>>> takeCustomSearchActions() {
        return takeRequests(myCustomSearchActions);
    }

    @Override
    public String toString() {
        return myWordRequests.toString().replace(',', '\n') + ";" + myQueryRequests;
    }
}
