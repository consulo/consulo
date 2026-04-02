// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.diff.model;

import com.intellij.collaboration.util.ComputedResult;
import consulo.application.util.ListSelection;
import kotlin.Nothing;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowKt;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * A version of the {@link CodeReviewDiffProcessorViewModel} which does some pre-processing on the changes and creates a separate
 * view model for each change.
 *
 * @param <C>   change type
 * @param <CVM> change view model type
 */
public interface PreLoadingCodeReviewAsyncDiffViewModelDelegate<C, CVM extends AsyncDiffViewModel> {
    @Nonnull
    Flow<@Nullable ComputedResult<CodeReviewDiffProcessorViewModel.State<CVM>>> getChanges();

    void showChanges(@Nonnull ListSelection<C> changes, @Nullable DiffViewerScrollRequest scrollRequest);

    default void showChanges(@Nonnull ListSelection<C> changes) {
        showChanges(changes, null);
    }

    void showChange(@Nonnull C change, @Nullable DiffViewerScrollRequest scrollRequest);

    default void showChange(@Nonnull C change) {
        showChange(change, null);
    }

    @Nullable
    Object handleSelection(
        @Nonnull Consumer<ListSelection<C>> listener,
        @Nonnull Continuation<? super Nothing> $completion
    );

    static <D, C, CVM extends AsyncDiffViewModel> @Nonnull PreLoadingCodeReviewAsyncDiffViewModelDelegate<C, CVM> create(
        @Nonnull Flow<@Nullable ComputedResult<D>> preloadedDataFlow,
        @Nonnull Flow<UnaryOperator<java.util.List<C>>> changesPreProcessor,
        @Nonnull TriFunction<CoroutineScope, D, C, CVM> createViewModel
    ) {
        return new PreLoadingCodeReviewAsyncDiffViewModelDelegateImpl<>(preloadedDataFlow, changesPreProcessor, createViewModel);
    }

    static <D, C, CVM extends AsyncDiffViewModel> @Nonnull PreLoadingCodeReviewAsyncDiffViewModelDelegate<C, CVM> create(
        @Nonnull Flow<@Nullable ComputedResult<D>> preloadedDataFlow,
        @Nonnull TriFunction<CoroutineScope, D, C, CVM> createViewModel
    ) {
        return new PreLoadingCodeReviewAsyncDiffViewModelDelegateImpl<>(
            preloadedDataFlow,
            FlowKt.flowOf(UnaryOperator.identity()),
            createViewModel
        );
    }

    @FunctionalInterface
    interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }
}
