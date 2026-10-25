// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.diff.model;

import consulo.application.util.ListSelection;
import consulo.disposer.Disposable;
import consulo.review.util.ComputedResult;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A version of the {@link CodeReviewDiffProcessorViewModel} which does some pre-processing on the changes and creates a separate
 * view model for each change
 *
 * @param <C>   change type
 * @param <CVM> change view model type
 */
public interface PreLoadingCodeReviewAsyncDiffViewModelDelegate<C, CVM extends AsyncDiffViewModel> {
    ObservableValue<@Nullable ComputedResult<CodeReviewDiffProcessorState<CVM>>> getChanges();

    void showChanges(ListSelection<C> changes, @Nullable DiffViewerScrollRequest scrollRequest);

    default void showChanges(ListSelection<C> changes) {
        showChanges(changes, null);
    }

    void showChange(C change, @Nullable DiffViewerScrollRequest scrollRequest);

    default void showChange(C change) {
        showChange(change, null);
    }

    void handleSelection(Disposable parentDisposable, Consumer<ListSelection<C>> listener);

    static <D, C, CVM extends AsyncDiffViewModel> PreLoadingCodeReviewAsyncDiffViewModelDelegate<C, CVM> create(
        Disposable parentDisposable,
        ObservableValue<@Nullable ComputedResult<D>> preloadedData,
        CodeReviewDiffViewModelFactory<D, C, CVM> createViewModel
    ) {
        return create(parentDisposable, preloadedData, ObservableValue.of(Function.identity()), createViewModel);
    }

    static <D, C, CVM extends AsyncDiffViewModel> PreLoadingCodeReviewAsyncDiffViewModelDelegate<C, CVM> create(
        Disposable parentDisposable,
        ObservableValue<@Nullable ComputedResult<D>> preloadedData,
        ObservableValue<Function<List<C>, List<C>>> changesPreProcessor,
        CodeReviewDiffViewModelFactory<D, C, CVM> createViewModel
    ) {
        return new PreLoadingCodeReviewAsyncDiffViewModelDelegateImpl<>(parentDisposable, preloadedData, changesPreProcessor, createViewModel);
    }
}
