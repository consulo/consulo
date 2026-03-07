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
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-03-06
 */
public final class OptionalReadLock<I, O> extends CoroutineStep<I, O> {
    public static <I, O> CoroutineStep<I, O> apply(@RequiredReadAction @Nonnull Function<I, O> function,
                                                   @Nonnull Runnable onFail) {
        return new OptionalReadLock<>(function, onFail);
    }

    private final Function<I, O> myFunction;
    private final Runnable myOnFail;

    private OptionalReadLock(Function<I, O> function, Runnable onFail) {
        myFunction = function;
        myOnFail = onFail;
    }

    @Override
    protected O execute(I input, Continuation<?> continuation) {
        Application application = Objects.requireNonNull(continuation.getConfiguration(Application.KEY), "Application required");
        SimpleReference<O> ref = new SimpleReference<>();
        if (application.tryRunReadAction(ref, () -> myFunction.apply(input))) {
            return ref.get();
        }
        myOnFail.run();
        return null;
    }
}
