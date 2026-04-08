// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.timeline.thread;

import kotlinx.coroutines.flow.StateFlow;
import jakarta.annotation.Nonnull;

public interface CodeReviewResolvableItemViewModel {
    @Nonnull
    StateFlow<Boolean> getIsResolved();

    @Nonnull
    StateFlow<Boolean> getCanChangeResolvedState();

    @Nonnull
    StateFlow<Boolean> getIsBusy();

    void changeResolvedState();
}
