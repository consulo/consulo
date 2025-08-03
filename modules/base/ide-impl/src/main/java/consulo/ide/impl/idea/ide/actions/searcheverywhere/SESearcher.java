// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.searcheverywhere;

import consulo.application.progress.ProgressIndicator;
import consulo.searchEverywhere.SearchEverywhereContributor;
import consulo.searchEverywhere.SearchEverywhereFoundElementInfo;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

interface SESearcher {
    ProgressIndicator search(
        @Nonnull Map<? extends SearchEverywhereContributor<?>, Integer> contributorsAndLimits,
        @Nonnull String pattern
    );

    ProgressIndicator findMoreItems(
        @Nonnull Map<? extends SearchEverywhereContributor<?>, Collection<SearchEverywhereFoundElementInfo>> alreadyFound,
        @Nonnull String pattern,
        @Nonnull SearchEverywhereContributor<?> contributor,
        int newLimit
    );

    /**
     * Search process listener interface
     */
    interface Listener {
        void elementsAdded(@Nonnull List<? extends SearchEverywhereFoundElementInfo> list);

        void elementsRemoved(@Nonnull List<? extends SearchEverywhereFoundElementInfo> list);

        void searchFinished(@Nonnull Map<SearchEverywhereContributor<?>, Boolean> hasMoreContributors);
    }
}
