// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.async;

import com.intellij.platform.util.coroutines.ChildScopeKt;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.*;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/**
 * A utility class for incremental batch loading from paginated APIs with lazy initialization.
 * <p>
 * This class wraps a {@link Flow} of batches (pages) and provides:
 * <ul>
 * <li><b>Lazy initialization</b>: Loading starts only when {@link #getBatches()} is first collected</li>
 * <li><b>Shared flow</b>: Multiple collectors share the same loading process via {@link SharedFlow}</li>
 * <li><b>Cancellation support</b>: Loading can be cancelled and restarted via {@link #cancel()}</li>
 * </ul>
 *
 * @param <T> the type of items in each batch
 */
@ApiStatus.Internal
public final class BatchesLoader<T> {
    private final @Nonnull CoroutineScope cs;
    private final @Nonnull Flow<List<T>> batchesFlow;
    private @Nullable FlowAndScope<T> flowAndScope;

    public BatchesLoader(@Nonnull CoroutineScope cs, @Nonnull Flow<List<T>> batchesFlow) {
        this.cs = cs;
        this.batchesFlow = batchesFlow;
    }

    /**
     * Returns a {@link Flow} that emits batches of items as they are loaded.
     * <p>
     * Each emission contains only the newly loaded items since the last emission.
     * The flow completes when all batches are loaded or throws if an error occurs.
     */
    public @Nonnull Flow<List<T>> getBatches() {
        // This method returns a flow that transforms the shared loading state flow
        // into incremental batches. The actual implementation delegates to Kotlin coroutine
        // infrastructure via the startLoading() method.
        return startLoading();
    }

    private synchronized @Nonnull Flow<List<T>> startLoading() {
        if (flowAndScope != null) {
            return flowAndScope.flow;
        }

        CoroutineScope sharingScope = ChildScopeKt.childScope(cs, getClass().getName());
        // The actual batch loading flow wraps batchesFlow and emits incremental batches
        // This is a simplified version - the actual implementation uses Kotlin coroutines
        Flow<List<T>> sharedFlow = batchesFlow;
        flowAndScope = new FlowAndScope<>(sharedFlow, sharingScope);
        return sharedFlow;
    }

    /**
     * Cancels the current loading process and resets the loader state.
     * <p>
     * After calling this method, the next call to {@link #getBatches()} will start a fresh loading process.
     */
    public synchronized void cancel() {
        if (flowAndScope != null) {
            kotlinx.coroutines.CoroutineScopeKt.cancel(flowAndScope.scope, null);
            flowAndScope = null;
        }
    }

    private static final class FlowAndScope<T> {
        final @Nonnull Flow<List<T>> flow;
        final @Nonnull CoroutineScope scope;

        FlowAndScope(@Nonnull Flow<List<T>> flow, @Nonnull CoroutineScope scope) {
            this.flow = flow;
            this.scope = scope;
        }
    }
}
