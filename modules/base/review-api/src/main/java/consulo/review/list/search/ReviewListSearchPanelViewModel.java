// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.list.search;

import consulo.util.concurrent.coroutine.MutableObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface ReviewListSearchPanelViewModel<S extends ReviewListSearchValue, Q extends ReviewListQuickFilter<S>> {
    List<Q> getQuickFilters();

    MutableObservableValue<S> getSearchState();

    MutableObservableValue<@Nullable String> getQueryState();

    S getEmptySearch();

    S getDefaultFilter();

    List<S> getSearchHistory();
}
