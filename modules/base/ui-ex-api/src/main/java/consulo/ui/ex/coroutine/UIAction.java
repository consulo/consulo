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
package consulo.ui.ex.coroutine;

import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.CoroutineStep;
import consulo.util.lang.ref.SimpleReference;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A coroutine step that runs its function on the UI thread.
 * <p>
 * The asynchronous path ({@link #runAsync}) does not block a coroutine thread: the work is
 * scheduled on the UI thread via {@link UIAccess#execute} and the coroutine chain is composed
 * with a future that the UI thread completes. Only the blocking path ({@link #execute}, used by
 * {@code runBlocking}) waits on the UI thread.
 *
 * @author VISTALL
 * @since 2026-01-31
 */
public class UIAction<I, O> extends CoroutineStep<I, O> {
    public static <I, O> CoroutineStep<I, O> apply(@RequiredUIAccess Function<I, O> function) {
        return new UIAction<>((i, c) -> function.apply(i));
    }

    public static <I, O> CoroutineStep<I, O> apply(@RequiredUIAccess BiFunction<I, Continuation<?>, O> function) {
        return new UIAction<>(function);
    }

    private final BiFunction<I, Continuation<?>, O> myFunction;

    private UIAction(BiFunction<I, Continuation<?>, O> function) {
        myFunction = function;
    }

    @Override
    public void runAsync(CompletableFuture<I> previousExecution, @Nullable CoroutineStep<O, ?> nextStep, Continuation<?> continuation) {
        UIAccess uiAccess = Objects.requireNonNull(continuation.getConfiguration(UIAccess.KEY), "UIAccess required");
        continuation.continueCompose(previousExecution, input -> uiAccess.giveAsync(() -> myFunction.apply(input, continuation)), nextStep);
    }

    @Override
    protected @Nullable O execute(@Nullable I input, Continuation<?> continuation) {
        UIAccess uiAccess = Objects.requireNonNull(continuation.getConfiguration(UIAccess.KEY), "UIAccess required");
        SimpleReference<O> ref = new SimpleReference<>();
        uiAccess.giveAndWaitIfNeed(() -> ref.set(myFunction.apply(input, continuation)));
        return ref.get();
    }
}