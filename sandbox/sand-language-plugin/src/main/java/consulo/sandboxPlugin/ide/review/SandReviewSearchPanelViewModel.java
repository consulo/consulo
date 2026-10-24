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
package consulo.sandboxPlugin.ide.review;

import consulo.disposer.Disposable;
import consulo.review.details.data.ReviewRequestState;
import consulo.review.list.search.ReviewListSearchPanelViewModelBase;
import consulo.review.util.IncrementallyComputedValue;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 2026-10-24
 */
public class SandReviewSearchPanelViewModel extends ReviewListSearchPanelViewModelBase<SandReviewSearchValue, SandReviewQuickFilter> {
    private static final List<String> LABELS = List.of("api", "review", "platform", "unified", "commit", "qt", "inlays", "draft");

    private final ScheduledExecutorService myScheduler;
    private final MutableObservableValue<@Nullable ReviewRequestState> myStateFilterState;
    private final MutableObservableValue<@Nullable String> myAuthorFilterState;
    private final MutableObservableValue<@Nullable List<String>> myLabelsFilterState;

    public SandReviewSearchPanelViewModel(Disposable parentDisposable, ScheduledExecutorService scheduler) {
        super(parentDisposable, new SandReviewSearchHistoryModel(), SandReviewSearchValue.EMPTY, SandReviewSearchValue.DEFAULT);
        myScheduler = scheduler;
        myStateFilterState = partialState(getSearchState(), SandReviewSearchValue::state, SandReviewSearchValue::withState);
        myAuthorFilterState = partialState(getSearchState(), SandReviewSearchValue::author, SandReviewSearchValue::withAuthor);
        myLabelsFilterState = partialState(getSearchState(),
            value -> value.labels().isEmpty() ? null : value.labels(),
            SandReviewSearchValue::withLabels);
    }

    @Override
    protected SandReviewSearchValue withQuery(SandReviewSearchValue search, @Nullable String query) {
        return search.withQuery(query);
    }

    @Override
    public List<SandReviewQuickFilter> getQuickFilters() {
        return List.of(SandReviewQuickFilter.values());
    }

    public MutableObservableValue<@Nullable ReviewRequestState> getStateFilterState() {
        return myStateFilterState;
    }

    public MutableObservableValue<@Nullable String> getAuthorFilterState() {
        return myAuthorFilterState;
    }

    public MutableObservableValue<@Nullable List<String>> getLabelsFilterState() {
        return myLabelsFilterState;
    }

    /**
     * The labels arrive in two parts, the second one later, the way a server pages them.
     */
    public ObservableValue<IncrementallyComputedValue<List<String>>> loadLabels() {
        MutableObservableValue<IncrementallyComputedValue<List<String>>> labels =
            MutableObservableValue.of(IncrementallyComputedValue.loading());
        myScheduler.schedule(() -> labels.set(IncrementallyComputedValue.partialSuccess(LABELS.subList(0, 4), true)),
            600, TimeUnit.MILLISECONDS);
        myScheduler.schedule(() -> labels.set(IncrementallyComputedValue.success(LABELS)), 1500, TimeUnit.MILLISECONDS);
        return labels;
    }
}
