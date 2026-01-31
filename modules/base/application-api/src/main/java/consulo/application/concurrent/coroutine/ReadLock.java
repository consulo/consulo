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
import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-01-30
 */
public final class ReadLock<I, O> extends CoroutineStep<I, O> {
    public static <I, O> CoroutineStep<I, O> apply(@RequiredReadAction @Nonnull Function<I, O> function) {
        return new ReadLock<>(function);
    }

    private final Function<I, O> myFunction;

    private ReadLock(Function<I, O> function) {
        myFunction = function;
    }

    @Override
    protected O execute(I input, Continuation<?> continuation) {
        Application application = Objects.requireNonNull(continuation.getConfiguration(Application.KEY), "Application required");
        return application.runReadAction((Supplier<O>) () -> myFunction.apply(input));
    }
}
