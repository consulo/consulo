// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.create;

import kotlinx.coroutines.flow.StateFlow;
import jakarta.annotation.Nonnull;

public interface CodeReviewTitleDescriptionViewModel {
    @Nonnull
    StateFlow<String> getTitleText();

    @Nonnull
    StateFlow<String> getDescriptionText();

    @Nonnull
    StateFlow<Boolean> getIsTemplateLoading();

    void setTitle(@Nonnull String text);

    void setDescription(@Nonnull String text);
}
