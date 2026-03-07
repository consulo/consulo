// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.diff.model;

import consulo.application.util.ListSelection;
import kotlin.Nothing;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlinx.coroutines.CancellableContinuation;
import kotlinx.coroutines.CancellableContinuationKt;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlowKt;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

final class CodeReviewAsyncDiffViewModelDelegateImpl<C> implements CodeReviewAsyncDiffViewModelDelegate<C> {
    private final @Nonnull MutableStateFlow<ChangesState<C>> changesToShow =
        StateFlowKt.MutableStateFlow(new ChangesState<>());
    private final @Nonnull CopyOnWriteArrayList<Consumer<ListSelection<C>>> selectionListeners = new CopyOnWriteArrayList<>();

    @Override
    public @Nonnull MutableStateFlow<ChangesState<C>> getChangesToShow() {
        return changesToShow;
    }

    @Override
    public void showChanges(@Nonnull ListSelection<C> changes, @Nullable DiffViewerScrollRequest scrollRequest) {
        ChangesState<C> current = changesToShow.getValue();
        ChangesState<C> state;
        if (current.getSelectedChanges().equals(changes)) {
            state = current;
        }
        else {
            state = new ChangesState<>(changes);
            changesToShow.setValue(state);
        }
        notifySelection(state.getSelectedChanges());

        if (scrollRequest != null) {
            state.scroll(scrollRequest);
        }
    }

    @Override
    public void showChange(@Nonnull C change, @Nullable DiffViewerScrollRequest scrollRequest) {
        ChangesState<C> current = changesToShow.getValue();
        int newIdx = current.getSelectedChanges().getList().indexOf(change);
        if (newIdx < 0) {
            return;
        }

        ChangesState<C> state = current;
        ListSelection<C> newChanges = ListSelection.createAt(current.getSelectedChanges().getList(), newIdx);
        if (!newChanges.equals(current.getSelectedChanges())) {
            state = new ChangesState<>(newChanges);
            if (!changesToShow.compareAndSet(current, state)) {
                return;
            }
            notifySelection(state.getSelectedChanges());
        }

        if (scrollRequest != null) {
            state.scroll(scrollRequest);
        }
    }

    private void notifySelection(@Nonnull ListSelection<C> selection) {
        for (Consumer<ListSelection<C>> listener : selectionListeners) {
            try {
                listener.accept(selection);
            }
            catch (Exception e) {
                // notification failed
            }
        }
    }

    @Override
    public @Nullable Object handleSelection(
        @Nonnull Consumer<ListSelection<C>> listener,
        @Nonnull Continuation<? super Nothing> $completion
    ) {
        selectionListeners.add(listener);
        listener.accept(changesToShow.getValue().getSelectedChanges());
        // Equivalent to awaitCancellation() with cleanup in finally block.
        // Uses suspendCancellableCoroutine to suspend indefinitely and remove listener on cancellation.
        return CancellableContinuationKt.suspendCancellableCoroutine(
            (CancellableContinuation<? super Nothing> cont) -> {
                cont.invokeOnCancellation(cause -> selectionListeners.remove(listener));
                return kotlin.Unit.INSTANCE;
            },
            $completion
        );
    }
}
