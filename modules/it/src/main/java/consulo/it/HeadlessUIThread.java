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
package consulo.it;

import consulo.it.internal.HeadlessUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class HeadlessUIThread {
    private static final long TIMEOUT_SECONDS = 30;

    private HeadlessUIThread() {
    }

    public static boolean isUIThread() {
        return HeadlessUIAccess.INSTANCE.isUIThread();
    }

    public static void run(Runnable action) {
        if (isUIThread()) {
            action.run();
            return;
        }

        await(HeadlessUIAccess.INSTANCE.giveAsync(action), deadline());
    }

    public static <T extends @Nullable Object> T compute(Supplier<T> action) {
        if (isUIThread()) {
            return action.get();
        }

        return await(HeadlessUIAccess.INSTANCE.giveAsync(action), deadline());
    }

    public static void flush() {
        flush(deadline());
    }

    static void flush(long deadline) {
        await(HeadlessUIAccess.INSTANCE.giveAsync(() -> {
        }), deadline);
    }

    static long deadline() {
        return System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
    }

    static <T extends @Nullable Object> T await(CompletableFuture<T> future, long deadline) {
        if (isUIThread() && !future.isDone()) {
            throw new IllegalStateException("The UI thread cannot wait for work which itself waits for the UI thread");
        }

        try {
            return future.get(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
        }
        catch (TimeoutException e) {
            throw new AssertionError("The UI did not settle in " + TIMEOUT_SECONDS + " seconds", e);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
        catch (ExecutionException e) {
            throw new AssertionError(e.getCause());
        }
    }
}
