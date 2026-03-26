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
package consulo.application.concurrent.coroutine;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.CoroutineStep;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-01-30
 */
public final class ReadLock<I extends @Nullable Object, O extends @Nullable Object> extends CoroutineStep<I, O> {
    public static <I, O> CoroutineStep<I, O> apply(@RequiredReadAction Function<I, O> function) {
        return new ReadLock<>(function);
    }

    private final Function<I, O> myFunction;

    private ReadLock(Function<I, O> function) {
        myFunction = function;
    }

    @Override
    @SuppressWarnings("NullAway")
    protected @Nullable O execute(@Nullable I input, Continuation<?> continuation) {
        Application application = Objects.requireNonNull(continuation.getConfiguration(Application.KEY), "Application required");
        // NullAway problem: input and output are nullable by method contract but in actual usage input can be null only if I is nullable
        // We cannot explain this to the static validator, so suppressing NullAway validation
        return application.runReadAction((Supplier<O>) () -> myFunction.apply(input));
    }
}
