// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.list.search;

import kotlinx.coroutines.flow.StateFlow;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public interface ReviewListSearchFiltersPanelViewModel<S extends ReviewListSearchValue, Q extends ReviewListQuickFilter<S>> {
    @Nonnull
    List<Q> getQuickFilters();

    @Nonnull
    StateFlow<@Nullable String> getSearchTextState();

    @Nonnull
    StateFlow<S> getSearchQueryState();

    void setSearchText(@Nonnull String text);

    void submitSearchText();

    void setSearchQuery(@Nonnull S query);

    /**
     * Reset the search query to the default value
     */
    void resetSearchQuery();

    /**
     * Reset the search query to the empty value
     */
    void clearSearchQuery();

    @Nonnull
    List<S> getSearchHistory();
}
