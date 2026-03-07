// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.diff.model;

import com.intellij.collaboration.util.ComputedResult;
import consulo.application.util.ListSelection;
import kotlinx.coroutines.flow.StateFlow;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * A viewmodel for a diff processor which can show multiple diffs and switch between them.
 */
public interface CodeReviewDiffProcessorViewModel<C> {
    @Nonnull
    StateFlow<@Nullable ComputedResult<State<C>>> getChanges();

    void showChange(@Nonnull C change, @Nullable DiffViewerScrollRequest scrollRequest);

    default void showChange(@Nonnull C change) {
        showChange(change, null);
    }

    void showChange(int changeIdx, @Nullable DiffViewerScrollRequest scrollRequest);

    default void showChange(int changeIdx) {
        showChange(changeIdx, null);
    }

    interface State<C> {
        @Nonnull
        ListSelection<C> getSelectedChanges();
    }
}
