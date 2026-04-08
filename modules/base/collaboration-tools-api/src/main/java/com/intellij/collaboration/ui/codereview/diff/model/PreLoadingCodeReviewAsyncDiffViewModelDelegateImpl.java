// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.diff.model;

import com.intellij.collaboration.async.CoroutineUtilKt;
import com.intellij.collaboration.async.MappingScopedItemsContainer;
import com.intellij.collaboration.util.ComputedResult;
import consulo.application.util.ListSelection;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kotlin.Nothing;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.flow.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

final class PreLoadingCodeReviewAsyncDiffViewModelDelegateImpl<D, C, CVM extends AsyncDiffViewModel>
    implements PreLoadingCodeReviewAsyncDiffViewModelDelegate<C, CVM> {

    private final @Nonnull CodeReviewAsyncDiffViewModelDelegate<C> delegate = CodeReviewAsyncDiffViewModelDelegate.create();
    private final @Nonnull Flow<@Nullable ComputedResult<D>> preloadedDataFlow;
    private final @Nonnull Flow<UnaryOperator<List<C>>> changesPreProcessor;
    private final @Nonnull TriFunction<CoroutineScope, D, C, CVM> createViewModel;
    private final @Nonnull Flow<@Nullable ComputedResult<CodeReviewDiffProcessorViewModel.State<CVM>>> changes;

    PreLoadingCodeReviewAsyncDiffViewModelDelegateImpl(
        @Nonnull Flow<@Nullable ComputedResult<D>> preloadedDataFlow,
        @Nonnull Flow<UnaryOperator<List<C>>> changesPreProcessor,
        @Nonnull TriFunction<CoroutineScope, D, C, CVM> createViewModel
    ) {
        this.preloadedDataFlow = preloadedDataFlow;
        this.changesPreProcessor = changesPreProcessor;
        this.createViewModel = createViewModel;
        this.changes = createChangesFlow();
    }

    @SuppressWarnings("unchecked")
    private @Nonnull Flow<@Nullable ComputedResult<CodeReviewDiffProcessorViewModel.State<CVM>>> createChangesFlow() {
        // Equivalent to preloadedDataFlow.transformLatest { result -> ... }
        // This uses Kotlin flow APIs from Java. The flow transformation handles:
        // 1. null -> emit null
        // 2. loading -> emit loading
        // 3. failure -> emit failure
        // 4. success -> create VM container and handle state changes
        return FlowKt.transformLatest(preloadedDataFlow, (collector, dataLoadingResult, continuation) -> {
            if (dataLoadingResult == null) {
                collector.emit(null, continuation);
                return Unit.INSTANCE;
            }
            var result = dataLoadingResult.getResult();
            if (result == null) {
                // in progress
                collector.emit(ComputedResult.loading(), continuation);
                return Unit.INSTANCE;
            }
            if (result.isFailure()) {
                Throwable error = result.exceptionOrNull();
                collector.emit(ComputedResult.failure(error), continuation);
                return Unit.INSTANCE;
            }
            D data = (D) result.getOrNull();
            // handleVmsState for success
            CoroutineScopeKt.coroutineScope(
                scope -> {
                    MappingScopedItemsContainer<C, CVM> vmsContainer =
                        MappingScopedItemsContainer.byEquality(scope, c -> createViewModel.apply(scope, data, c));
                    List<C>[] lastList = new List[]{Collections.emptyList()};

                    FlowKt.collectLatest(
                        changesPreProcessor,
                        (preProcessor, cont2) -> {
                            CoroutineUtilKt.collectScoped(
                                delegate.getChangesToShow(),
                                changesState -> {
                                    try {
                                        if (!changesState.getSelectedChanges().getList().equals(lastList[0])) {
                                            collector.emit(ComputedResult.loading(), cont2);
                                            List<C> processedList = preProcessor.apply(changesState.getSelectedChanges().getList());
                                            vmsContainer.update(processedList);
                                            lastList[0] = changesState.getSelectedChanges().getList();
                                        }
                                        var mappingState = vmsContainer.getMappingState().getValue();
                                        List<CVM> vms = new ArrayList<>(mappingState.values());
                                        ListSelection<C> sel = changesState.getSelectedChanges();
                                        C selectedItem = sel.getSelectedIndex() >= 0 && sel.getSelectedIndex() < sel.getList().size()
                                            ? sel.getList().get(sel.getSelectedIndex()) : null;
                                        int selectedVmIdx = new ArrayList<>(mappingState.keySet()).indexOf(selectedItem);
                                        ViewModelsState<CVM> newState = new ViewModelsState<>(
                                            ListSelection.createAt(vms, selectedVmIdx),
                                            changesState.getScrollRequests()
                                        );
                                        collector.emit(ComputedResult.success(newState), cont2);
                                    }
                                    catch (Exception e) {
                                        collector.emit(ComputedResult.failure(e), cont2);
                                    }
                                    return Unit.INSTANCE;
                                },
                                cont2
                            );
                            return Unit.INSTANCE;
                        },
                        continuation
                    );
                    return Unit.INSTANCE;
                },
                continuation
            );
            return Unit.INSTANCE;
        });
    }

    @Override
    public @Nonnull Flow<@Nullable ComputedResult<CodeReviewDiffProcessorViewModel.State<CVM>>> getChanges() {
        return changes;
    }

    @Override
    public void showChanges(@Nonnull ListSelection<C> changes, @Nullable DiffViewerScrollRequest scrollRequest) {
        delegate.showChanges(changes, scrollRequest);
    }

    @Override
    public void showChange(@Nonnull C change, @Nullable DiffViewerScrollRequest scrollRequest) {
        delegate.showChange(change, scrollRequest);
    }

    @Override
    public @Nullable Object handleSelection(
        @Nonnull Consumer<ListSelection<C>> listener,
        @Nonnull Continuation<? super Nothing> $completion
    ) {
        return delegate.handleSelection(listener, $completion);
    }
}
