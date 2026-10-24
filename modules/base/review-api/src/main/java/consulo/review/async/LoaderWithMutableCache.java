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
import consulo.review.util.FutureUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Caches the result of an asynchronous {@code loader} and lets it be invalidated ({@link #clearCache()}) or overridden in place
 * ({@link #updateLoaded}/{@link #overrideResult}) for optimistic updates. The updated listeners fire whenever the cached value is
 * invalidated or overridden, so consumers can re-{@link #load()}.
 */
public final class LoaderWithMutableCache<T extends @Nullable Object> {
    private final Supplier<CompletableFuture<T>> myLoader;
    private final List<Runnable> myUpdatedListeners = new CopyOnWriteArrayList<>();

    private CompletableFuture<T> myRequest = new CompletableFuture<>();
    private boolean myRequestStarted;
    private boolean myLoaded;
    private @Nullable T myLastLoaded;
    private boolean myDisposed;

    public LoaderWithMutableCache(Disposable parentDisposable, Supplier<CompletableFuture<T>> loader) {
        myLoader = loader;
        if (!Disposer.tryRegister(parentDisposable, this::dispose)) {
            dispose();
        }
    }

    /**
     * Awaits the first non-canceled computation and returns the result or fails with its exception
     */
    public CompletableFuture<T> load() {
        CompletableFuture<T> result = new CompletableFuture<>();
        awaitCompleted(result);
        return result;
    }

    private void awaitCompleted(CompletableFuture<T> result) {
        CompletableFuture<T> request = startedRequest();
        request.whenComplete((value, error) -> {
            if (error == null) {
                result.complete(value);
                return;
            }
            if (FutureUtil.isCancellation(error)) {
                boolean replaced;
                synchronized (this) {
                    replaced = !myDisposed && myRequest != request;
                }
                if (replaced) {
                    awaitCompleted(result);
                    return;
                }
            }
            result.completeExceptionally(FutureUtil.unwrap(error));
        });
    }

    private CompletableFuture<T> startedRequest() {
        CompletableFuture<T> request;
        synchronized (this) {
            request = myRequest;
            if (myRequestStarted) {
                return request;
            }
            myRequestStarted = true;
        }

        CompletableFuture<T> loading;
        try {
            loading = myLoader.get();
        }
        catch (Throwable e) {
            loading = CompletableFuture.failedFuture(e);
        }
        CompletableFuture<T> started = loading;
        request.whenComplete((value, error) -> {
            if (FutureUtil.isCancellation(error)) {
                started.cancel(true);
            }
        });
        started.whenComplete((value, error) -> {
            if (error != null) {
                request.completeExceptionally(FutureUtil.unwrap(error));
                return;
            }
            synchronized (this) {
                if (myRequest == request) {
                    myLoaded = true;
                    myLastLoaded = value;
                }
            }
            request.complete(value);
        });
        return request;
    }

    public void clearCache() {
        CompletableFuture<T> previous;
        synchronized (this) {
            if (myDisposed) {
                return;
            }
            myLoaded = false;
            myLastLoaded = null;
            previous = myRequest;
            myRequest = new CompletableFuture<>();
            myRequestStarted = false;
        }
        previous.cancel(false);
        fireUpdated();
    }

    @SuppressWarnings("NullAway")
    public void updateLoaded(UnaryOperator<T> updater) {
        T updatedValue;
        synchronized (this) {
            if (!myLoaded) {
                return;
            }
            T loadedValue = myLastLoaded;
            updatedValue = updater.apply(loadedValue);
            if (Objects.equals(loadedValue, updatedValue)) {
                return;
            }
        }
        overrideResult(updatedValue);
    }

    public void overrideResult(T result) {
        CompletableFuture<T> previous;
        synchronized (this) {
            if (myDisposed) {
                return;
            }
            myLoaded = true;
            myLastLoaded = result;
            previous = myRequest;
            myRequest = CompletableFuture.completedFuture(result);
            myRequestStarted = true;
        }
        previous.cancel(false);
        fireUpdated();
    }

    /**
     * Fires whenever the cached value is invalidated or overridden
     */
    public void addUpdatedListener(Disposable parentDisposable, Runnable listener) {
        myUpdatedListeners.add(listener);
        Disposer.register(parentDisposable, () -> myUpdatedListeners.remove(listener));
    }

    private void fireUpdated() {
        for (Runnable listener : myUpdatedListeners) {
            listener.run();
        }
    }

    private void dispose() {
        CompletableFuture<T> request;
        synchronized (this) {
            myDisposed = true;
            request = myRequest;
        }
        request.cancel(false);
    }
}
