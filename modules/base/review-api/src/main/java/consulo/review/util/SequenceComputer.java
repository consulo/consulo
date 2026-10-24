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

import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface SequenceComputer<T extends @Nullable Object> {
    /**
     * Tries to compute the next item.
     *
     * @return {@link ItemComputationOutcome} holding the computed value,
     * or {@link DoneComputationOutcome} if the sequence is finished
     */
    CompletableFuture<ComputationOutcome<T>> computeNext();

    /**
     * Builds a computer that walks a pointer {@code PTR}: {@code compute} maps the current pointer to the next value and the pointer to
     * use afterwards, or {@code null} when the value just produced is the last one.
     * <p>
     * The produced items are {@link SequenceItem}s carrying {@link SequenceItem#isLast()} (derived from that trailing pointer), so a
     * consumer may finish as soon as the last value is produced instead of asking for one more item only to get
     * {@link DoneComputationOutcome}.
     */
    static <PTR, T extends @Nullable Object> SequenceComputer<SequenceItem<T>> byPointer(PTR initialPointer,
                                                                                         Function<PTR, CompletableFuture<Pair<T, @Nullable PTR>>> compute) {
        return new SequenceComputer<>() {
            private @Nullable PTR myPointer = initialPointer;

            @Override
            public CompletableFuture<ComputationOutcome<SequenceItem<T>>> computeNext() {
                PTR currentPointer = myPointer;
                if (currentPointer == null) {
                    return CompletableFuture.completedFuture(ComputationOutcome.done());
                }
                return compute.apply(currentPointer).thenApply(computed -> {
                    PTR nextPointer = computed.getSecond();
                    myPointer = nextPointer;
                    return ComputationOutcome.item(SequenceItem.of(computed.getFirst(), nextPointer == null));
                });
            }
        };
    }

    /**
     * Builds a computer that computes the next item using a cursor {@code C} of the last computed item.
     * <p>
     * For the first request a {@code null} cursor is passed to {@code compute}
     * If {@code compute} returns {@code null}, the last computed cursor will be used for the next computation
     */
    static <C, T extends @Nullable Object> SequenceComputer<T> byCursor(@Nullable C startCursor,
                                                                        Function<@Nullable C, CompletableFuture<@Nullable Pair<T, C>>> compute) {
        return new SequenceComputer<>() {
            private @Nullable C myLastCursor = startCursor;

            @Override
            public CompletableFuture<ComputationOutcome<T>> computeNext() {
                return compute.apply(myLastCursor).thenApply(computed -> {
                    if (computed == null) {
                        return ComputationOutcome.done();
                    }
                    myLastCursor = computed.getSecond();
                    return ComputationOutcome.item(computed.getFirst());
                });
            }
        };
    }
}
