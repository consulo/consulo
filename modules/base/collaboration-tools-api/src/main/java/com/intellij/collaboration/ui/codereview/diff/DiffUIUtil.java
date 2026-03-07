// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.diff;

import consulo.diff.chain.DiffRequestProducer;
import consulo.diff.chain.SimpleDiffRequestChain;
import consulo.diff.request.ErrorDiffRequest;
import consulo.diff.request.LoadingDiffRequest;
import jakarta.annotation.Nonnull;

import kotlin.time.Duration;
import kotlin.time.DurationKt;

@ApiStatus.Internal
public final class DiffUIUtil {
    private DiffUIUtil() {
    }

    /**
     * Progress is delayed to avoid a flicker.
     */
    public static final Duration PROGRESS_DISPLAY_DELAY = DurationKt.toDuration(100, kotlin.time.DurationUnit.MILLISECONDS);

    public static final DiffRequestProducer LOADING_PRODUCER =
        new SimpleDiffRequestChain.DiffRequestProducerWrapper(new LoadingDiffRequest());

    public static @Nonnull DiffRequestProducer createErrorProducer(@Nonnull Throwable error) {
        return new SimpleDiffRequestChain.DiffRequestProducerWrapper(new ErrorDiffRequest(error));
    }
}
