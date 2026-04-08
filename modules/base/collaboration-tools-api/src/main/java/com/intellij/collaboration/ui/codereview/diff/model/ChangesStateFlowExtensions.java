// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.diff.model;

import com.intellij.collaboration.async.MappingScopedItemsContainer;
import com.intellij.collaboration.ui.codereview.diff.model.CodeReviewAsyncDiffViewModelDelegate.ChangesState;
import com.intellij.collaboration.util.ComputedResult;
import consulo.application.util.ListSelection;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.channels.ProducerScope;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowKt;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Utility class replacing the Kotlin extension function {@code Flow<ChangesState<C>>.mapChangesToVms}.
 */
@ApiStatus.Internal
public final class ChangesStateFlowExtensions {
    private ChangesStateFlowExtensions() {
    }

    /**
     * Maps a flow of {@link ChangesState} to a flow of computed view model states.
     *
     * @param changesStateFlow the source flow of changes states
     * @param createViewModel  factory function to create a view model for a change, scoped to a CoroutineScope
     * @param <C>              change type
     * @param <CVM>            change view model type
     * @return flow of computed results containing the processor view model state
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Nonnull
    public static <C, CVM extends AsyncDiffViewModel> Flow<ComputedResult<CodeReviewDiffProcessorViewModel.State<CVM>>>
    mapChangesToVms(
        @Nonnull Flow<ChangesState<C>> changesStateFlow,
        @Nonnull BiFunction<CoroutineScope, C, CVM> createViewModel
    ) {
        // This is a channelFlow-based implementation.
        // The Kotlin equivalent uses channelFlow { ... collect { ... send(...) } }
        return FlowKt.channelFlow((ProducerScope<ComputedResult<CodeReviewDiffProcessorViewModel.State<CVM>>> scope) -> {
            MappingScopedItemsContainer<C, CVM> vmsContainer =
                MappingScopedItemsContainer.byEquality(scope, c -> createViewModel.apply(scope, c));
            List<?>[] lastListHolder = {Collections.emptyList()};

            FlowKt.collect(changesStateFlow, changesState -> {
                ComputedResult<CodeReviewDiffProcessorViewModel.State<CVM>> result = ComputedResult.compute(() -> {
                    List<C> currentList = changesState.getSelectedChanges().getList();
                    if (!currentList.equals(lastListHolder[0])) {
                        vmsContainer.update(currentList);
                        lastListHolder[0] = currentList;
                    }
                    var mappingState = vmsContainer.getMappingState().getValue();
                    List<CVM> vms = new ArrayList<>(mappingState.values());
                    ListSelection<C> sel = changesState.getSelectedChanges();
                    C selectedItem = sel.getSelectedIndex() >= 0 && sel.getSelectedIndex() < sel.getList().size()
                        ? sel.getList().get(sel.getSelectedIndex()) : null;
                    int selectedVmIdx = new ArrayList<>(mappingState.keySet()).indexOf(selectedItem);
                    return new ViewModelsState<>(ListSelection.createAt(vms, selectedVmIdx), changesState.getScrollRequests());
                });
                scope.trySend(result);
                return kotlin.Unit.INSTANCE;
            });
            return kotlin.Unit.INSTANCE;
        });
    }
}
