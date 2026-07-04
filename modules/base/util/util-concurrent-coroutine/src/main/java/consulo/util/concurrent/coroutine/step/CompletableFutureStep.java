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
 * A step that awaits a {@link CompletableFuture} produced from the input value. The
 * asynchronous execution does not block a thread: the coroutine chain is composed with
 * the awaited future and proceeds when it completes.
 *
 * @author VISTALL
 * @since 2026-07-04
 */
public final class CompletableFutureStep<I extends @Nullable Object, O extends @Nullable Object> extends CoroutineStep<I, O> {
    public static <I, O> CompletableFutureStep<I, O> await(Function<I, CompletableFuture<O>> futureFactory) {
        return new CompletableFutureStep<>(futureFactory);
    }

    private final Function<I, CompletableFuture<O>> myFutureFactory;

    private CompletableFutureStep(Function<I, CompletableFuture<O>> futureFactory) {
        myFutureFactory = futureFactory;
    }

    @Override
    public void runAsync(CompletableFuture<I> previousExecution, @Nullable CoroutineStep<O, ?> nextStep, Continuation<?> continuation) {
        continuation.continueCompose(previousExecution, myFutureFactory, nextStep);
    }

    @Override
    @SuppressWarnings("NullAway")
    protected O execute(@Nullable I input, Continuation<?> continuation) {
        // NullAway problem: input and output are nullable by method contract but in actual usage
        // input can be null only if I is nullable. We cannot explain this to the static validator.
        return myFutureFactory.apply(input).join();
    }
}
