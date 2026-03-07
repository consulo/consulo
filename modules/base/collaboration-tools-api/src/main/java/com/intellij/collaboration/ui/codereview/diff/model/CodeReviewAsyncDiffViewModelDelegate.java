// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.diff.model;

import consulo.application.util.ListSelection;
import kotlin.Nothing;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.channels.BufferOverflow;
import kotlinx.coroutines.channels.Channel;
import kotlinx.coroutines.channels.ChannelKt;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowKt;
import kotlinx.coroutines.flow.StateFlow;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Consumer;

@ApiStatus.Internal
public interface CodeReviewAsyncDiffViewModelDelegate<C> {
    @Nonnull
    StateFlow<ChangesState<C>> getChangesToShow();

    void showChanges(@Nonnull ListSelection<C> changes, @Nullable DiffViewerScrollRequest scrollRequest);

    default void showChanges(@Nonnull ListSelection<C> changes) {
        showChanges(changes, null);
    }

    void showChange(@Nonnull C change, @Nullable DiffViewerScrollRequest scrollRequest);

    default void showChange(@Nonnull C change) {
        showChange(change, null);
    }

    /**
     * Suspend function - takes a Continuation parameter for Kotlin interop.
     * Returns Nothing (never returns normally).
     */
    @Nullable
    Object handleSelection(
        @Nonnull Consumer<ListSelection<C>> listener,
        @Nonnull Continuation<? super Nothing> $completion
    );

    final class ChangesState<C> {
        private final @Nonnull ListSelection<C> selectedChanges;
        private final @Nonnull Channel<DiffViewerScrollRequest> scrollRequestsChannel;
        private final @Nonnull Flow<DiffViewerScrollRequest> scrollRequests;

        public ChangesState(@Nonnull ListSelection<C> selectedChanges) {
            this.selectedChanges = selectedChanges;
            this.scrollRequestsChannel = ChannelKt.Channel(1, BufferOverflow.DROP_OLDEST, null);
            this.scrollRequests = FlowKt.receiveAsFlow(scrollRequestsChannel);
        }

        public ChangesState() {
            this(ListSelection.empty());
        }

        public @Nonnull ListSelection<C> getSelectedChanges() {
            return selectedChanges;
        }

        public @Nonnull Flow<DiffViewerScrollRequest> getScrollRequests() {
            return scrollRequests;
        }

        public void scroll(@Nonnull DiffViewerScrollRequest cmd) {
            scrollRequestsChannel.trySend(cmd);
        }
    }

    @Nonnull
    static <C> CodeReviewAsyncDiffViewModelDelegate<C> create() {
        return new CodeReviewAsyncDiffViewModelDelegateImpl<>();
    }
}
