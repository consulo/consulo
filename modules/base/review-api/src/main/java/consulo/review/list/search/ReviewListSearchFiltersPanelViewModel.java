// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.list.search;

import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface ReviewListSearchFiltersPanelViewModel<S extends ReviewListSearchValue, Q extends ReviewListQuickFilter<S>> {
    List<Q> getQuickFilters();

    ObservableValue<@Nullable String> getSearchTextState();

    ObservableValue<S> getSearchQueryState();

    void setSearchText(String text);

    void submitSearchText();

    void setSearchQuery(S query);

    /**
     * Reset the search query to the default value
     */
    void resetSearchQuery();

    /**
     * Reset the search query to the empty value
     */
    void clearSearchQuery();

    List<S> getSearchHistory();
}
