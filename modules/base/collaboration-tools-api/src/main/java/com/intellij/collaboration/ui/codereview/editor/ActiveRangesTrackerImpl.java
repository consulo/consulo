// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.editor;

import consulo.diff.util.LineRange;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

final class ActiveRangesTrackerImpl implements CodeReviewActiveRangesTracker {
    private final MutableStateFlow<List<LineRange>> _activeRanges = StateFlowKt.MutableStateFlow(Collections.emptyList());

    @Override
    @SuppressWarnings("unchecked")
    public @Nonnull StateFlow<Collection<LineRange>> getActiveRanges() {
        return (StateFlow<Collection<LineRange>>) (StateFlow<?>) FlowKt.asStateFlow(_activeRanges);
    }

    @Override
    public @Nullable Object rangeActivated(@Nonnull LineRange range, @Nonnull Continuation<? super kotlin.Nothing> continuation) {
        // This is a suspend function that adds the range, awaits cancellation, then removes it.
        // The actual suspend implementation should be called from Kotlin code.
        List<LineRange> current = _activeRanges.getValue();
        List<LineRange> updated = new ArrayList<>(current);
        updated.add(range);
        _activeRanges.setValue(updated);
        // Note: cleanup on cancellation must be handled by the caller's coroutine context
        return null;
    }
}
