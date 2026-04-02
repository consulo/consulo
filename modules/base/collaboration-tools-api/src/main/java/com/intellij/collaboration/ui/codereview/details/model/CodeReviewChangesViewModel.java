// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.details.model;

import kotlinx.coroutines.flow.SharedFlow;
import jakarta.annotation.Nonnull;

import java.util.List;

public interface CodeReviewChangesViewModel<T> {
    @Nonnull
    SharedFlow<List<T>> getReviewCommits();

    @Nonnull
    SharedFlow<T> getSelectedCommit();

    /**
     * {@code -1} for "all commits" mode
     */
    @Nonnull
    SharedFlow<Integer> getSelectedCommitIndex();

    void selectCommit(int index);

    void selectNextCommit();

    void selectPreviousCommit();

    @Nonnull
    String commitHash(@Nonnull T commit);
}
