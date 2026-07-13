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
package consulo.util.concurrent.coroutine.step;

import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.CoroutineStep;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * A step that awaits another {@link Continuation} produced from the input value. Unlike
 * {@link CompletableFutureStep}, callers hand over the {@link Continuation} of a nested coroutine
 * directly, so an already-launched coroutine can be joined into the parent chain without converting
 * it to a {@link CompletableFuture} at the call site.
 *
 * @author VISTALL
 * @since 2026-07-13
 */
public final class ContinuationStep<I extends @Nullable Object, O extends @Nullable Object> extends CoroutineStep<I, O> {
    public static <I, O> ContinuationStep<I, O> await(Function<I, Continuation<O>> continuationFactory) {
        return new ContinuationStep<>(continuationFactory);
    }

    private final Function<I, Continuation<O>> myContinuationFactory;

    private ContinuationStep(Function<I, Continuation<O>> continuationFactory) {
        myContinuationFactory = continuationFactory;
    }

    @Override
    public void runAsync(CompletableFuture<I> previousExecution, @Nullable CoroutineStep<O, ?> nextStep, Continuation<?> continuation) {
        continuation.continueCompose(previousExecution, input -> myContinuationFactory.apply(input).toFuture(), nextStep);
    }

    @Override
    @SuppressWarnings("NullAway")
    protected O execute(@Nullable I input, Continuation<?> continuation) {
        // NullAway problem: input and output are nullable by method contract but in actual usage
        // input can be null only if I is nullable. We cannot explain this to the static validator.
        return myContinuationFactory.apply(input).toFuture().join();
    }
}
