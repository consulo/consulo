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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents a sequence of items where each item is computed upon request ({@link SequenceComputer#computeNext()})
 * Each call to {@code getComputer} should return a new computer which would compute the same sequence from the beginning.
 */
@FunctionalInterface
public interface ComputableSequence<T extends @Nullable Object> {
    SequenceComputer<T> getComputer();

    /**
     * Shorthand for {@link SequenceComputer#byPointer}
     */
    static <PTR, T extends @Nullable Object> ComputableSequence<SequenceItem<T>> byPointer(PTR initialPointer,
                                                                                           Function<PTR, CompletableFuture<Pair<T, @Nullable PTR>>> compute) {
        return () -> SequenceComputer.byPointer(initialPointer, compute);
    }

    /**
     * Shorthand for {@link SequenceComputer#byCursor}
     */
    static <C, T extends @Nullable Object> ComputableSequence<T> byCursor(@Nullable C startCursor,
                                                                          Function<@Nullable C, CompletableFuture<@Nullable Pair<T, C>>> compute) {
        return () -> SequenceComputer.byCursor(startCursor, compute);
    }

    static <T extends @Nullable Object> ComputableSequence<T> fromIterable(Iterable<T> iterable) {
        return () -> {
            Iterator<T> iterator = iterable.iterator();
            return () -> CompletableFuture.completedFuture(iterator.hasNext() ? ComputationOutcome.item(iterator.next()) : ComputationOutcome.done());
        };
    }

    /**
     * Applies {@code transform} to the value of every computed {@link SequenceItem}, preserving its {@link SequenceItem#isLast()} flag.
     * Handy for sequences produced by {@link SequenceComputer#byPointer}, whose items already carry the last-item hint.
     */
    static <T extends @Nullable Object, R extends @Nullable Object> ComputableSequence<SequenceItem<R>> mapItems(ComputableSequence<SequenceItem<T>> sequence,
                                                                                                               Function<T, R> transform) {
        return sequence.map(item -> SequenceItem.of(transform.apply(item.getValue()), item.isLast()));
    }

    /**
     * Returns a sequence that applies {@code transform} to every computed item. Each {@link #getComputer()} of the
     * result maps a fresh computer of the original sequence, so any per-computation state is preserved.
     */
    default <R extends @Nullable Object> ComputableSequence<R> map(Function<T, R> transform) {
        return () -> {
            SequenceComputer<T> computer = getComputer();
            return () -> computer.computeNext().thenApply(outcome -> outcome instanceof ItemComputationOutcome<T> item
                ? ComputationOutcome.item(transform.apply(item.value()))
                : ComputationOutcome.done());
        };
    }

    /**
     * Computes the whole sequence, handing every item to {@code consumer}; the future completes after the last one.
     */
    default CompletableFuture<?> forEach(Consumer<T> consumer) {
        CompletableFuture<Object> result = new CompletableFuture<>();
        ComputableSequenceUtil.computeRemaining(getComputer(), consumer, result);
        return result;
    }

    default CompletableFuture<List<T>> toList() {
        List<T> list = new ArrayList<>();
        return forEach(list::add).thenApply(unused -> list);
    }
}
