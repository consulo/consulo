// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.toolwindow;

import com.intellij.openapi.actionSystem.DataKey;
import jakarta.annotation.Nonnull;

public final class ReviewToolwindowDataKeys {
    private ReviewToolwindowDataKeys() {
    }

    @Nonnull
    public static final DataKey<ReviewToolwindowProjectViewModel<?, ?>> REVIEW_TOOLWINDOW_PROJECT_VM =
        DataKey.create("com.intellij.collaboration.toolwindow.review.project.vm");

    @Nonnull
    public static final DataKey<ReviewToolwindowViewModel<?>> REVIEW_TOOLWINDOW_VM =
        DataKey.create("com.intellij.collaboration.toolwindow.review.toolwindow.vm");
}
