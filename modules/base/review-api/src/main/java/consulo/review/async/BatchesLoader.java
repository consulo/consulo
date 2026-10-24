// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.review.async;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A utility class for incremental batch loading from paginated APIs with lazy initialization.
 * <p>
 * This class wraps a producer of batches (pages) and provides:
 * <ul>
 * <li><b>Lazy initialization</b>: Loading starts only when {@link #getBatches} is first called</li>
 * <li><b>Shared loading</b>: Multiple consumers share the same loading process, a late one gets the batches loaded so far first</li>
 * <li><b>Cancellation support</b>: Loading can be cancelled and restarted via {@link #cancel()}</li>
 * </ul>
 *
 * <pre>{@code
 * // Create a loader with a producer that hands out batches of items
 * BatchesLoader<Item> loader = new BatchesLoader<>(parentDisposable, paginatedApi);
 *
 * // Consume batches incrementally as they arrive
 * loader.getBatches(batch -> {
 *     items.addAll(batch);
 *     updateUI(items);
 * });
 *
 * // Cancel loading and allow restart
 * loader.cancel();
 * }</pre>
 *
 * @param <T> the type of items in each batch
 */
public final class BatchesLoader<T> {
    private final Function<Consumer<List<T>>, CompletableFuture<?>> myBatches;
    private final @Nullable Duration myStopTimeout;
    private @Nullable SharedBatches<T> myShared;
    private boolean myDisposed;

    public BatchesLoader(Disposable parentDisposable, Function<Consumer<List<T>>, CompletableFuture<?>> batches) {
        this(parentDisposable, batches, null);
    }

    /**
     * @param parentDisposable the parent for managing the loading lifecycle
     * @param batches          given a consumer of batches, produces them one after another (typically from a paginated API) and
     *                         completes after the last one
     * @param stopTimeout      a delay between the disappearance of the last consumer and the stopping of the loading and cleaning
     *                         up the loaded batches. If not specified, the loading will never be stopped.
     */
    public BatchesLoader(Disposable parentDisposable,
                         Function<Consumer<List<T>>, CompletableFuture<?>> batches,
                         @Nullable Duration stopTimeout) {
        myBatches = batches;
        myStopTimeout = stopTimeout;
        if (!Disposer.tryRegister(parentDisposable, this::dispose)) {
            myDisposed = true;
        }
    }

    /**
     * Hands batches of items to {@code consumer} as they are loaded.
     * <p>
     * Each call gets only the newly loaded items since the last one.
     * The returned future completes when all batches are loaded or fails if an error occurs.
     * Cancelling the returned future unsubscribes {@code consumer}.
     */
    public CompletableFuture<?> getBatches(Consumer<List<T>> consumer) {
        SharedBatches<T> shared = startLoading();
        if (shared == null) {
            return CompletableFuture.failedFuture(new CancellationException());
        }
        CompletableFuture<?> result = shared.subscribe(consumer, () -> scheduleStop(shared));
        shared.startIfNeeded(myBatches);
        return result;
    }

    private synchronized @Nullable SharedBatches<T> startLoading() {
        if (myDisposed) {
            return null;
        }
        SharedBatches<T> shared = myShared;
        if (shared == null) {
            shared = new SharedBatches<>();
            myShared = shared;
        }
        return shared;
    }

    private void scheduleStop(SharedBatches<T> shared) {
        Duration stopTimeout = myStopTimeout;
        if (stopTimeout == null) {
            return;
        }
        CompletableFuture.delayedExecutor(stopTimeout.toMillis(), TimeUnit.MILLISECONDS).execute(() -> {
            synchronized (this) {
                if (myShared != shared || shared.hasSubscribers()) {
                    return;
                }
                myShared = null;
            }
            shared.cancel();
        });
    }

    /**
     * Cancels the current loading process and resets the loader state.
     * <p>
     * After calling this method, the next call to {@link #getBatches} will start a fresh loading process.
     */
    public void cancel() {
        SharedBatches<T> shared;
        synchronized (this) {
            shared = myShared;
            myShared = null;
        }
        if (shared != null) {
            shared.cancel();
        }
    }

    private void dispose() {
        synchronized (this) {
            myDisposed = true;
        }
        cancel();
    }
}
