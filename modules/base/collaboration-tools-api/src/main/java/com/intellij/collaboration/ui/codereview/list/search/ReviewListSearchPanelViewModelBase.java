// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.list.search;

import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class ReviewListSearchPanelViewModelBase<S extends ReviewListSearchValue, Q extends ReviewListQuickFilter<S>>
    implements ReviewListSearchPanelViewModel<S, Q> {

    private final CoroutineScope scope;
    private final ReviewListSearchHistoryModel<S> historyModel;
    private final S emptySearch;
    private final S defaultFilter;
    private final MutableStateFlow<S> searchState;
    private final MutableStateFlow<@Nullable String> queryState;

    protected ReviewListSearchPanelViewModelBase(
        @Nonnull CoroutineScope scope,
        @Nonnull ReviewListSearchHistoryModel<S> historyModel,
        @Nonnull S emptySearch,
        @Nonnull S defaultFilter
    ) {
        this.scope = scope;
        this.historyModel = historyModel;
        this.emptySearch = emptySearch;
        this.defaultFilter = defaultFilter;

        S lastFilter = historyModel.getLastFilter();
        this.searchState = StateFlowKt.MutableStateFlow(lastFilter != null ? lastFilter : defaultFilter);
        this.queryState = partialState(searchState, ReviewListSearchValue::getSearchQuery, this::withQuery);

        updateHistoryOnSearchChanges();
    }

    @Override
    public final @Nonnull S getEmptySearch() {
        return emptySearch;
    }

    @Override
    public final @Nonnull S getDefaultFilter() {
        return defaultFilter;
    }

    @Override
    public final @Nonnull MutableStateFlow<S> getSearchState() {
        return searchState;
    }

    @Override
    public final @Nonnull MutableStateFlow<@Nullable String> getQueryState() {
        return queryState;
    }

    @Override
    public final @Nonnull List<S> getSearchHistory() {
        return historyModel.getHistory();
    }

    private void updateHistoryOnSearchChanges() {
        kotlinx.coroutines.BuildersKt.launch(scope, null, kotlinx.coroutines.CoroutineStart.DEFAULT,
            (coroutineScope, continuation) -> {
                Flow<kotlin.Pair<S, S>> withPrevious = FlowKt.runningFold(
                    searchState,
                    new kotlin.Pair<>(null, null),
                    (acc, newValue, cont) -> new kotlin.Pair<>(acc.getSecond(), newValue)
                );

                FlowKt.collectLatest(
                    withPrevious,
                    pair -> {
                        S oldValue = pair.getFirst();
                        S newValue = pair.getSecond();
                        if (newValue != null) {
                            historyModel.setLastFilter(newValue);

                            // don't persist first value
                            if (oldValue == null) {
                                return kotlin.Unit.INSTANCE;
                            }

                            if (newValue.getFilterCount() == 0 || newValue.equals(defaultFilter)) {
                                return kotlin.Unit.INSTANCE;
                            }

                            if (oldValue.getSearchQuery() != null && oldValue.getSearchQuery().equals(newValue.getSearchQuery())
                                || oldValue.getSearchQuery() == null && newValue.getSearchQuery() == null) {
                                kotlinx.coroutines.DelayKt.delay(10_000, continuation);
                            }
                            historyModel.add(newValue);
                        }
                        return kotlin.Unit.INSTANCE;
                    },
                    continuation
                );
                return kotlin.Unit.INSTANCE;
            }
        );
    }

    protected abstract @Nonnull S withQuery(@Nonnull S searchValue, @Nullable String query);

    protected @Nonnull <T> MutableStateFlow<T> partialState(
        @Nonnull MutableStateFlow<S> parentState,
        @Nonnull Function<S, T> getter,
        @Nonnull BiFunction<S, T, S> updater
    ) {
        MutableStateFlow<T> partial = StateFlowKt.MutableStateFlow(getter.apply(parentState.getValue()));
        kotlinx.coroutines.BuildersKt.launch(scope, null, kotlinx.coroutines.CoroutineStart.DEFAULT,
            (coroutineScope, continuation) -> {
                FlowKt.collectLatest(parentState, value -> {
                    StateFlowKt.update(partial, current -> getter.apply(value));
                    return kotlin.Unit.INSTANCE;
                }, continuation);
                return kotlin.Unit.INSTANCE;
            }
        );
        kotlinx.coroutines.BuildersKt.launch(
            scope,
            null,
            kotlinx.coroutines.CoroutineStart.DEFAULT,
            (coroutineScope, continuation) -> {
                FlowKt.collectLatest(
                    partial,
                    value -> {
                        StateFlowKt.update(parentState, current -> updater.apply(current, value));
                        return kotlin.Unit.INSTANCE;
                    },
                    continuation
                );
                return kotlin.Unit.INSTANCE;
            }
        );
        return partial;
    }
}
