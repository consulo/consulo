// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.details.model;

import com.intellij.collaboration.util.ComputedResult;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.StateFlow;
import org.jetbrains.annotations.ApiStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import kotlin.jvm.functions.Function3;

@ApiStatus.NonExtendable
public interface CodeReviewChangesViewModelDelegate<T> {
    @Nonnull
    StateFlow<String> getSelectedCommit();

    @Nonnull
    StateFlow<ComputedResult<T>> getChangeListVm();

    @Nullable
    T selectCommit(int index);

    @Nullable
    T selectCommit(@Nullable String commitSha);

    @Nullable
    T selectNextCommit();

    @Nullable
    T selectPreviousCommit();

    static <T> @Nonnull CodeReviewChangesViewModelDelegate<T> create(
        @Nonnull CoroutineScope cs,
        @Nonnull Flow<Result<CodeReviewChangesContainer>> changesContainer,
        @Nonnull Function3<? super CoroutineScope, ? super CodeReviewChangesContainer, ? super CodeReviewChangeList, ? extends T> vmProducer
    ) {
        return new CodeReviewChangesViewModelDelegateImpl<>(cs, changesContainer, vmProducer);
    }
}
