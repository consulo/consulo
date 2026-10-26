// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.review.util;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ResultUtil {
    private ResultUtil() {
    }

    /**
     * Runs the {@code block} catching user exceptions (not {@link Error}) thrown before the future is produced
     */
    public static <R extends @Nullable Object> CompletableFuture<R> runCatchingUser(Supplier<CompletableFuture<R>> block) {
        try {
            return block.get();
        }
        catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Allows processing the error before re-throwing it
     * Will silently re-throw cancellation and JVM errors
     */
    public static <T extends @Nullable Object> CompletableFuture<T> processErrorAndGet(CompletableFuture<T> result, Consumer<Throwable> handler) {
        return result.whenComplete((value, error) -> {
            if (error == null) {
                return;
            }
            Throwable cause = FutureUtil.unwrap(error);
            if (!FutureUtil.isCancellation(cause) && !(cause instanceof Error)) {
                handler.accept(cause);
            }
        });
    }
}
