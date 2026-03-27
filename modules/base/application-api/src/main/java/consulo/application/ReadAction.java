/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.application;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.internal.AsyncExecutionService;
import consulo.util.lang.function.ThrowableRunnable;
import consulo.util.lang.function.ThrowableSupplier;
import org.jetbrains.annotations.Contract;

import java.util.concurrent.Callable;

public final class ReadAction<T> {
    public static <E extends Throwable> void run(@RequiredReadAction ThrowableRunnable<E> action) throws E {
        Application.get().runReadAction((ThrowableSupplier<Object, E>) () -> {
            action.run();
            return null;
        });
    }

    public static <T, E extends Throwable> T computeNotNull(@RequiredReadAction ThrowableSupplier<T, E> action) throws E {
        return Application.get().runReadAction(action);
    }

    public static <T, E extends Throwable> T compute(@RequiredReadAction ThrowableSupplier<T, E> action) throws E {
        return Application.get().runReadAction(action);
    }

    /**
     * Runs the given computation under a read lock, but only acquires the lock if not already holding read or write access.
     * Use this instead of {@link #compute} when the call site may legitimately be reached from a write-action context
     * (write access implies read access, so re-entering {@code runReadAction} would throw on the EDT in Consulo's lock model).
     */
    public static <T, E extends Throwable> T computeBlocking(@RequiredReadAction ThrowableSupplier<T, E> action) throws E {
        Application app = Application.get();
        if (app.isReadAccessAllowed()) {
            return action.get();
        }
        return app.runReadAction(action);
    }

    /**
     * Create an {@link NonBlockingReadAction} builder to run the given Runnable in non-blocking read action on a background thread.
     */
    @Contract(pure = true)
    public static NonBlockingReadAction<Void> nonBlocking(@RequiredReadAction Runnable task) {
        return nonBlocking(() -> {
            task.run();
            return null;
        });
    }

    /**
     * Create an {@link NonBlockingReadAction} builder to run the given Callable in a non-blocking read action on a background thread.
     */
    @Contract(pure = true)
    public static <T> NonBlockingReadAction<T> nonBlocking(@RequiredReadAction Callable<T> task) {
        return AsyncExecutionService.getService().buildNonBlockingReadAction(task);
    }
}
