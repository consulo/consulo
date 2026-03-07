// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.editor;

import consulo.diff.util.LineRange;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.StateFlow;

import java.util.Collection;

public interface CodeReviewActiveRangesTracker {
    @Nonnull
    StateFlow<Collection<LineRange>> getActiveRanges();

    @Nullable
    Object rangeActivated(@Nonnull LineRange range, @Nonnull Continuation<? super kotlin.Nothing> continuation);

    static @Nonnull CodeReviewActiveRangesTracker create() {
        return new ActiveRangesTrackerImpl();
    }
}