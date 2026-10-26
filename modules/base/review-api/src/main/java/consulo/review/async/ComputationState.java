// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
import consulo.review.util.ComputedResult;
import consulo.review.util.FutureUtil;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * State of a computation which is restarted on every trigger: {@link ComputedResult#loading()} while the computation is running,
 * then its result or failure. A restart cancels the computation in progress and its result is never published.
 */
public final class ComputationState<T extends @Nullable Object> implements ObservableValue<@Nullable ComputedResult<T>> {
    private final Supplier<CompletableFuture<T>> myComputer;
    private final MutableObservableValue<@Nullable ComputedResult<T>> myState = MutableObservableValue.of(null);

    private @Nullable CompletableFuture<T> myCurrent;
    private boolean myDisposed;

    /**
     * @param trigger registers a restart listener for the lifetime of the given disposable
     */
    public static <T extends @Nullable Object> ComputationState<T> create(Disposable parentDisposable,
                                                                         BiConsumer<Disposable, Runnable> trigger,
                                                                         Supplier<CompletableFuture<T>> computer) {
        ComputationState<T> state = new ComputationState<>(computer);
        if (!Disposer.tryRegister(parentDisposable, state::dispose)) {
            state.dispose();
            return state;
        }
        Disposable listenerDisposable = Disposable.newDisposable("ComputationState trigger");
        Disposer.register(parentDisposable, listenerDisposable);
        trigger.accept(listenerDisposable, state::restart);
        state.restart();
        return state;
    }

    private ComputationState(Supplier<CompletableFuture<T>> computer) {
        myComputer = computer;
    }

    public void restart() {
        CompletableFuture<T> previous;
        CompletableFuture<T> request;
        synchronized (this) {
            if (myDisposed) {
                return;
            }
            previous = myCurrent;
            try {
                request = myComputer.get();
            }
            catch (Throwable e) {
                request = CompletableFuture.failedFuture(e);
            }
            myCurrent = request;
        }
        if (previous != null) {
            previous.cancel(true);
        }
        if (!request.isDone()) {
            myState.set(ComputedResult.loading());
        }
        CompletableFuture<T> started = request;
        request.whenComplete((value, error) -> {
            synchronized (this) {
                if (myCurrent != started || myDisposed) {
                    return;
                }
            }
            if (error == null) {
                myState.set(ComputedResult.success(value));
            }
            else if (!FutureUtil.isCancellation(error)) {
                myState.set(ComputedResult.failure(FutureUtil.unwrap(error)));
            }
        });
    }

    private void dispose() {
        CompletableFuture<T> current;
        synchronized (this) {
            myDisposed = true;
            current = myCurrent;
            myCurrent = null;
        }
        if (current != null) {
            current.cancel(true);
        }
    }

    @Override
    public @Nullable ComputedResult<T> get() {
        return myState.get();
    }

    @Override
    public Runnable addListener(Consumer<? super @Nullable ComputedResult<T>> listener) {
        return myState.addListener(listener);
    }
}
