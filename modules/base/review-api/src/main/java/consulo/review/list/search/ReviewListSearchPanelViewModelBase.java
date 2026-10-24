// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.review.list.search;

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class ReviewListSearchPanelViewModelBase<S extends ReviewListSearchValue, Q extends ReviewListQuickFilter<S>>
    implements ReviewListSearchPanelViewModel<S, Q> {
    private static final long HISTORY_UPDATE_DELAY_MILLISECONDS = 10_000;

    private final Disposable myParentDisposable;
    private final ReviewListSearchHistoryModel<S> myHistoryModel;
    private final S myEmptySearch;
    private final S myDefaultFilter;

    private final MutableObservableValue<S> mySearchState;
    private final MutableObservableValue<@Nullable String> myQueryState;

    private final AtomicReference<@Nullable Future<?>> myPendingHistoryUpdate = new AtomicReference<>();

    protected ReviewListSearchPanelViewModelBase(Disposable parentDisposable,
                                                 ReviewListSearchHistoryModel<S> historyModel,
                                                 S emptySearch,
                                                 S defaultFilter) {
        myParentDisposable = parentDisposable;
        myHistoryModel = historyModel;
        myEmptySearch = emptySearch;
        myDefaultFilter = defaultFilter;

        S lastFilter = historyModel.getLastFilter();
        mySearchState = MutableObservableValue.of(lastFilter != null ? lastFilter : defaultFilter);
        myQueryState = partialState(mySearchState, ReviewListSearchValue::getSearchQuery, this::withQuery);

        updateHistoryOnSearchChanges();
    }

    @Override
    public final S getEmptySearch() {
        return myEmptySearch;
    }

    @Override
    public final S getDefaultFilter() {
        return myDefaultFilter;
    }

    @Override
    public final MutableObservableValue<S> getSearchState() {
        return mySearchState;
    }

    @Override
    public final MutableObservableValue<@Nullable String> getQueryState() {
        return myQueryState;
    }

    @Override
    public final List<S> getSearchHistory() {
        return myHistoryModel.getHistory();
    }

    private void updateHistoryOnSearchChanges() {
        AtomicReference<S> previous = new AtomicReference<>(mySearchState.get());
        Runnable unsubscribe = mySearchState.addListener(newValue -> {
            S old = previous.getAndSet(newValue);
            cancelPendingHistoryUpdate();

            myHistoryModel.setLastFilter(newValue);

            if (newValue.getFilterCount() == 0 || newValue.equals(myDefaultFilter)) {
                return;
            }

            if (Objects.equals(old.getSearchQuery(), newValue.getSearchQuery())) {
                myPendingHistoryUpdate.set(AppExecutorUtil.getAppScheduledExecutorService().schedule(
                    () -> myHistoryModel.add(newValue),
                    HISTORY_UPDATE_DELAY_MILLISECONDS,
                    TimeUnit.MILLISECONDS
                ));
                return;
            }
            myHistoryModel.add(newValue);
        });
        Disposer.register(myParentDisposable, () -> {
            unsubscribe.run();
            cancelPendingHistoryUpdate();
        });
    }

    private void cancelPendingHistoryUpdate() {
        Future<?> pending = myPendingHistoryUpdate.getAndSet(null);
        if (pending != null) {
            pending.cancel(false);
        }
    }

    protected abstract S withQuery(S search, @Nullable String query);

    protected <T extends @Nullable Object> MutableObservableValue<T> partialState(MutableObservableValue<S> state,
                                                                                Function<S, T> getter,
                                                                                BiFunction<S, T, S> updater) {
        MutableObservableValue<T> partialState = MutableObservableValue.of(getter.apply(state.get()));
        Runnable stateUnsubscribe = state.addListener(value -> partialState.set(getter.apply(value)));
        Runnable partialUnsubscribe = partialState.addListener(value -> state.update(it -> updater.apply(it, value)));
        Disposer.register(myParentDisposable, () -> {
            stateUnsubscribe.run();
            partialUnsubscribe.run();
        });
        return partialState;
    }
}
