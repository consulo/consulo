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
 * A coroutine step that awaits a {@link CompletableFuture} produced from the input value.
 * <p>
 * The step takes an input value, applies a function that returns a {@link CompletableFuture},
 * and suspends until the future completes. The result of the future becomes the output of this step.
 * </p>
 *
 * @author VISTALL
 * @since 2026-03-07
 */
public final class CompletableFutureStep<I, O> extends CoroutineStep<I, O> {
    /**
     * Creates a step that applies a function to produce a {@link CompletableFuture}
     * and awaits its result.
     *
     * @param function A function that takes the input and returns a {@link CompletableFuture}
     * @return A new coroutine step
     */
    public static <I, O> CoroutineStep<I, O> await(Function<@Nullable I, CompletableFuture<O>> function) {
        return new CompletableFutureStep<>(function);
    }

    private final Function<@Nullable I, CompletableFuture<O>> myFunction;

    private CompletableFutureStep(Function<@Nullable I, CompletableFuture<O>> function) {
        myFunction = function;
    }

    @Override
    protected O execute(@Nullable I input, Continuation<?> continuation) {
        return myFunction.apply(input).join();
    }
}
