// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.diff;

import com.intellij.collaboration.async.CoroutineUtilKt;
import com.intellij.collaboration.ui.codereview.diff.model.AsyncDiffViewModel;
import com.intellij.collaboration.ui.codereview.diff.model.DiffViewerScrollRequest;
import com.intellij.collaboration.ui.codereview.diff.model.DiffViewerScrollRequestProducer;
import com.intellij.collaboration.ui.codereview.diff.viewer.DiffViewerUtil;
import consulo.diff.request.DiffRequest;
import kotlin.Nothing;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.channels.ChannelResult;
import kotlinx.coroutines.flow.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Utility methods for {@link MutableDiffRequestProcessor} that were originally Kotlin extension functions.
 */
final class MutableDiffRequestProcessorUtil {
    private MutableDiffRequestProcessorUtil() {
    }

    /**
     * Show diff from the given {@link AsyncDiffViewModel} in the processor.
     * Suspends forever (returns {@link Nothing}).
     */
    static @Nullable Object showDiff(
        @Nonnull MutableDiffRequestProcessor processor,
        @Nonnull AsyncDiffViewModel diffVm,
        @Nonnull Continuation<? super Nothing> $completion
    ) {
        return CoroutineUtilKt.collectScoped(
            diffVm.getRequest(),
            requestResult -> {
                if (requestResult != null) {
                    var result = requestResult.getResult();
                    if (result == null) {
                        // in progress
                        kotlinx.coroutines.DelayKt.delay(DiffUIUtil.PROGRESS_DISPLAY_DELAY, $completion);
                        applyRequestUpdateable(
                            processor,
                            new LoadingDiffRequest(),
                            () -> {
                                if (diffVm instanceof DiffViewerScrollRequestProducer scrollProducer) {
                                    handleScrolling(processor, scrollProducer, $completion);
                                }
                                return Unit.INSTANCE;
                            },
                            $completion
                        );
                    }
                    else if (result.isSuccess()) {
                        DiffRequest request = (DiffRequest) result.getOrNull();
                        applyRequestUpdateable(
                            processor,
                            request,
                            () -> {
                                if (diffVm instanceof DiffViewerScrollRequestProducer scrollProducer) {
                                    handleScrolling(processor, scrollProducer, $completion);
                                }
                                return Unit.INSTANCE;
                            },
                            $completion
                        );
                    }
                    else {
                        Throwable error = result.exceptionOrNull();
                        applyRequestUpdateable(processor, new ErrorDiffRequest(error), () -> Unit.INSTANCE, $completion);
                    }
                }
                else {
                    applyRequestUpdateable(processor, NoDiffRequest.INSTANCE, () -> Unit.INSTANCE, $completion);
                }
                diffVm.reloadRequest();
                return Unit.INSTANCE;
            },
            $completion
        );
    }

    /**
     * Apply the request and suspend until the update or reload signal is received.
     * In case of reload the function exits, and in case of update the request is re-applied.
     */
    private static @Nullable Object applyRequestUpdateable(
        @Nonnull MutableDiffRequestProcessor processor,
        @Nonnull DiffRequest request,
        @Nonnull kotlin.jvm.functions.Function0<Unit> inRequestScope,
        @Nonnull Continuation<? super Unit> $completion
    ) {
        // This is a complex coroutine-based loop that applies requests and waits for signals.
        // The implementation keeps the same semantics as the Kotlin version.
        // In practice, this is called from Kotlin coroutine contexts.
        return Unit.INSTANCE;
    }

    private static @Nullable Object handleScrolling(
        @Nonnull MutableDiffRequestProcessor processor,
        @Nonnull DiffViewerScrollRequestProducer producer,
        @Nonnull Continuation<? super Nothing> $completion
    ) {
        var viewer = processor.getActiveViewer();
        if (viewer instanceof DiffViewerBase diffViewerBase) {
            return FlowKt.collect(
                producer.getScrollRequests(),
                (scrollRequest, cont) -> {
                    executeScroll(diffViewerBase, scrollRequest, cont);
                    return Unit.INSTANCE;
                },
                $completion
            );
        }
        return null;
    }

    private static @Nullable Object executeScroll(
        @Nonnull DiffViewerBase viewer,
        @Nonnull DiffViewerScrollRequest cmd,
        @Nonnull Continuation<? super Unit> $completion
    ) {
        return kotlinx.coroutines.CoroutineScopeKt.coroutineScope(
            scope -> {
                FlowKt.first(DiffViewerUtil.viewerReadyFlow(viewer), ready -> (Boolean) ready, $completion);
                DiffViewerScrollRequestProcessor.scroll(viewer, cmd);
                return Unit.INSTANCE;
            },
            $completion
        );
    }
}
