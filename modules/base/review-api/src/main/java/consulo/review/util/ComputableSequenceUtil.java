// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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

final class ComputableSequenceUtil {
    private ComputableSequenceUtil() {
    }

    static <T extends @Nullable Object> void computeRemaining(SequenceComputer<T> computer, Consumer<T> consumer, CompletableFuture<Object> result) {
        while (!result.isDone()) {
            CompletableFuture<ComputationOutcome<T>> next;
            try {
                next = computer.computeNext();
            }
            catch (Throwable e) {
                result.completeExceptionally(e);
                return;
            }

            if (!next.isDone()) {
                next.whenComplete((outcome, error) -> {
                    if (accept(outcome, error, consumer, result)) {
                        computeRemaining(computer, consumer, result);
                    }
                });
                return;
            }

            ComputationOutcome<T> outcome;
            try {
                outcome = next.join();
            }
            catch (Throwable e) {
                result.completeExceptionally(FutureUtil.unwrap(e));
                return;
            }
            if (!accept(outcome, null, consumer, result)) {
                return;
            }
        }
    }

    private static <T extends @Nullable Object> boolean accept(@Nullable ComputationOutcome<T> outcome,
                                                              @Nullable Throwable error,
                                                              Consumer<T> consumer,
                                                              CompletableFuture<Object> result) {
        if (error != null) {
            result.completeExceptionally(FutureUtil.unwrap(error));
            return false;
        }
        if (!(outcome instanceof ItemComputationOutcome<T> item)) {
            result.complete(null);
            return false;
        }
        try {
            consumer.accept(item.value());
        }
        catch (Throwable e) {
            result.completeExceptionally(e);
            return false;
        }
        return true;
    }
}
