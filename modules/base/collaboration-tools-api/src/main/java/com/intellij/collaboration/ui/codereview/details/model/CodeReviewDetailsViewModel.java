// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.details.model;

import com.intellij.collaboration.ui.codereview.details.data.ReviewRequestState;
import kotlinx.coroutines.flow.Flow;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public interface CodeReviewDetailsViewModel {
    @Nonnull
    String getNumber();

    @Nonnull
    String getUrl();

    @Nonnull
    Flow<String> getTitle();

    @Nullable
    Flow<String> getDescription();

    @Nonnull
    Flow<ReviewRequestState> getReviewRequestState();
}
