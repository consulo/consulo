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
package consulo.util.concurrent.coroutine.internal;

import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.CoroutineStep;
import consulo.util.dataholder.UserDataHolderBase;
import org.jspecify.annotations.Nullable;

/**
 * An empty coroutine placeholder that performs no processing. It can be used
 * where a coroutine is required but no work has to be performed. Running it does
 * nothing and chaining additional steps with {@link #then(CoroutineStep)} is not
 * supported.
 *
 * @author eso
 */
public final class EmptyCoroutine<I extends @Nullable Object, O extends @Nullable Object> extends UserDataHolderBase implements Coroutine<I, O> {

    private static final EmptyCoroutine<?, ?> INSTANCE = new EmptyCoroutine<>();

    private EmptyCoroutine() {
    }

    @SuppressWarnings("unchecked")
    public static <I extends @Nullable Object, O extends @Nullable Object> Coroutine<I, O> instance() {
        return (Coroutine<I, O>) INSTANCE;
    }

    @Override
    public Continuation<O> runAsync(CoroutineScope scope, I input) {
        return finishedContinuation(scope);
    }

    @Override
    public Continuation<O> runBlocking(CoroutineScope scope, I input) {
        return finishedContinuation(scope);
    }

    /**
     * Creates a continuation for this empty coroutine that has immediately
     * finished with a {@code null} result without executing any steps.
     *
     * @param scope The scope to run in
     * @return The finished empty continuation
     */
    private Continuation<O> finishedContinuation(CoroutineScope scope) {
        ContinuationImpl<O> continuation = new ContinuationImpl<>(scope, this);

        continuation.finish(null);

        return continuation;
    }

    @Override
    public <T> Coroutine<I, T> then(CoroutineStep<O, T> step) {
        throw new UnsupportedOperationException("Cannot chain steps on an empty coroutine");
    }

    @Override
    public @Nullable String getName() {
        return "empty";
    }

    @Override
    public Coroutine<I, O> withName(String name) {
        return this;
    }
}
