// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.searcheverywhere;

import consulo.application.progress.ProgressIndicator;
import consulo.searchEverywhere.SearchEverywhereContributor;
import consulo.searchEverywhere.SearchEverywhereFoundElementInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

interface SESearcher {
    ProgressIndicator search(
        Map<? extends SearchEverywhereContributor<?>, Integer> contributorsAndLimits,
        String pattern
    );

    ProgressIndicator findMoreItems(
        Map<? extends SearchEverywhereContributor<?>, Collection<SearchEverywhereFoundElementInfo>> alreadyFound,
        String pattern,
        SearchEverywhereContributor<?> contributor,
        int newLimit
    );

    /**
     * Search process listener interface
     */
    interface Listener {
        void elementsAdded(List<? extends SearchEverywhereFoundElementInfo> list);

        void elementsRemoved(List<? extends SearchEverywhereFoundElementInfo> list);

        void searchFinished(Map<SearchEverywhereContributor<?>, Boolean> hasMoreContributors);
    }
}
