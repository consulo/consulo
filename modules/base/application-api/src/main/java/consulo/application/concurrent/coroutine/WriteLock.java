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

import consulo.annotation.access.RequiredWriteAction;
import consulo.application.Application;
import consulo.application.internal.ApplicationEx;
import consulo.application.internal.ApplicationWithIntentWriteLock;
import consulo.ui.UIAccess;
import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.CoroutineStep;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-02-01
 */
public final class WriteLock<I, O> extends CoroutineStep<I, O> {
    public static <I, O> CoroutineStep<I, O> apply(@RequiredWriteAction Function<I, O> function) {
        return new WriteLock<>((i, c) -> function.apply(i));
    }

    public static <I, O> CoroutineStep<I, O> apply(@RequiredWriteAction BiFunction<I, Continuation<?>, O> function) {
        return new WriteLock<>(function);
    }

    private final BiFunction<I, Continuation<?>, O> myFunction;

    private WriteLock(BiFunction<I, Continuation<?>, O> function) {
        myFunction = function;
    }

    @Override
    protected @Nullable O execute(@Nullable I input, Continuation<?> continuation) {
        UIAccess.assetIsNotUIThread();

        ApplicationEx application = applicationOf(continuation);

        // the coroutine context runs steps on virtual threads, and the write lock identifies its owner by thread
        // identity - so the write action has to be taken on the application's write thread, never here
        if (application.isWriteThread()) {
            return doExecute(application, input, continuation);
        }

        return CompletableFuture.supplyAsync(() -> doExecute(application, input, continuation), application.getWriteExecutor()).join();
    }

    private static ApplicationEx applicationOf(Continuation<?> continuation) {
        return (ApplicationEx) Objects.requireNonNull(continuation.getConfiguration(Application.KEY), "Application required");
    }

    private @Nullable O doExecute(ApplicationEx application, @Nullable I input, Continuation<?> continuation) {
        ApplicationWithIntentWriteLock writeLockApplication = (ApplicationWithIntentWriteLock) application;

        try {
            writeLockApplication.acquireWriteIntentLock(WriteLock.class.getName());
            //noinspection RequiredXAction
            return writeLockApplication.runWriteAction((Supplier<O>) () -> myFunction.apply(input, continuation));
        }
        finally {
            writeLockApplication.releaseWriteIntentLock();
        }
    }
}