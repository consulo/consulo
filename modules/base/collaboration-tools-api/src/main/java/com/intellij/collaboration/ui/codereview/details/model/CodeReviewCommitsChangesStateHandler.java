// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.details.model;

import kotlin.jvm.functions.Function2;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.StateFlow;
import org.jetbrains.annotations.ApiStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

@ApiStatus.Internal
@ApiStatus.NonExtendable
public interface CodeReviewCommitsChangesStateHandler<C, VM> {
    @Nonnull
    StateFlow<C> getSelectedCommit();

    @Nonnull
    StateFlow<VM> getChangeListVm();

    @Nullable
    VM selectCommit(int index);

    @Nullable
    VM selectCommit(@Nullable C commit);

    @Nullable
    VM selectNextCommit();

    @Nullable
    VM selectPreviousCommit();

    static <C, VM> @Nonnull CodeReviewCommitsChangesStateHandler<C, VM> create(
        @Nonnull CoroutineScope cs,
        @Nonnull List<C> commits,
        @Nonnull Function2<? super CoroutineScope, ? super C, ? extends VM> commitChangesVmProducer,
        int initialCommitIdx
    ) {
        return new CodeReviewCommitsChangesStateHandlerImpl<>(cs, commits, commitChangesVmProducer, initialCommitIdx);
    }

    static <C, VM> @Nonnull CodeReviewCommitsChangesStateHandler<C, VM> create(
        @Nonnull CoroutineScope cs,
        @Nonnull List<C> commits,
        @Nonnull Function2<? super CoroutineScope, ? super C, ? extends VM> commitChangesVmProducer
    ) {
        return create(cs, commits, commitChangesVmProducer, -1);
    }
}
