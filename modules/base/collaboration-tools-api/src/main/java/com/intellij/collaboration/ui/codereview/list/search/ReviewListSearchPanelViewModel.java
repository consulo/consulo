// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.list.search;

import kotlinx.coroutines.flow.MutableStateFlow;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public interface ReviewListSearchPanelViewModel<S extends ReviewListSearchValue, Q extends ReviewListQuickFilter<S>> {
    @Nonnull
    List<Q> getQuickFilters();

    @Nonnull
    MutableStateFlow<S> getSearchState();

    @Nonnull
    MutableStateFlow<@Nullable String> getQueryState();

    @Nonnull
    S getEmptySearch();

    @Nonnull
    S getDefaultFilter();

    @Nonnull
    List<S> getSearchHistory();
}
