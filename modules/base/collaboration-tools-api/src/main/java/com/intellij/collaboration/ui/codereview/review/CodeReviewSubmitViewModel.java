// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.review;

import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * A view model for a review submission interface
 */
public interface CodeReviewSubmitViewModel {
    /**
     * The number of comments that will be submitted in a review
     */
    @Nonnull
    StateFlow<Integer> getDraftCommentsCount();

    /**
     * Body of the review comment
     */
    @Nonnull
    MutableStateFlow<String> getText();

    /**
     * Review submission in progress
     */
    @Nonnull
    StateFlow<Boolean> getIsBusy();

    /**
     * Review submission error
     */
    @Nonnull
    StateFlow<@Nullable Throwable> getError();

    /**
     * Cancel the submission
     */
    void cancel();
}
