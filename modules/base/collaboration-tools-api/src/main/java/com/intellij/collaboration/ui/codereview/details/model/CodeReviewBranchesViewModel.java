// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.details.model;

import kotlinx.coroutines.flow.SharedFlow;
import kotlinx.coroutines.flow.StateFlow;
import jakarta.annotation.Nonnull;

public interface CodeReviewBranchesViewModel {
    @Nonnull
    StateFlow<String> getSourceBranch();

    @Nonnull
    SharedFlow<Boolean> getIsCheckedOut();

    @Nonnull
    SharedFlow<CodeReviewBranches> getShowBranchesRequests();

    void fetchAndCheckoutRemoteBranch();

    default boolean getCanShowInLog() {
        return false;
    }

    default void fetchAndShowInLog() {
    }

    void showBranches();
}
