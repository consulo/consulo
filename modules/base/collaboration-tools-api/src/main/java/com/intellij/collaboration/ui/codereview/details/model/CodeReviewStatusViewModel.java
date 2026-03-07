// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.details.model;

import com.intellij.collaboration.ui.codereview.details.data.CodeReviewCIJob;
import kotlinx.coroutines.flow.SharedFlow;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public interface CodeReviewStatusViewModel {
    /**
     * Whether there are conflicts that need to be resolved before merging.
     * <p>
     * If the value is {@code null}, there is a check currently in progress or there is something
     * else preventing us from knowing whether there are conflicts.
     */
    @Nonnull
    SharedFlow<@Nullable Boolean> getHasConflicts();

    /**
     * A flow or conversation resolution check.
     * <p>
     * IMPORTANT: meaning is flipped due to a naming typo.
     * false if every required conversation was resolved, true otherwise.
     */
    @Nonnull
    SharedFlow<Boolean> getRequiredConversationsResolved();

    @Nonnull
    SharedFlow<List<CodeReviewCIJob>> getCiJobs();

    @Nonnull
    SharedFlow<List<CodeReviewCIJob>> getShowJobsDetailsRequests();

    void showJobsDetails();
}
