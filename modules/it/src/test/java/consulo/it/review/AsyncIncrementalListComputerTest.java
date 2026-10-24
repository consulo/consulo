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
package consulo.it.review;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.review.util.AsyncIncrementalListComputer;
import consulo.review.util.ComputableSequence;
import consulo.review.util.ComputationOutcome;
import consulo.review.util.IncrementallyComputedValue;
import consulo.review.util.SequenceComputer;
import consulo.review.util.SequenceItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static consulo.it.review.ReviewTestUtil.first;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AsyncIncrementalListComputerTest {
    private final Disposable myDisposable = Disposable.newDisposable();

    @AfterEach
    public void tearDown() {
        Disposer.dispose(myDisposable);
    }

    @Test
    public void nothingIsComputedUntilMoreIsRequested() {
        AsyncIncrementalListComputer<Integer> computer = AsyncIncrementalListComputer.createIn(myDisposable, computerOf(List.of(1)));

        IncrementallyComputedValue<List<Integer>> state = computer.getState().get();
        assertFalse(state.isValueAvailable());
        assertFalse(state.isLoading());
        assertFalse(state.isComplete());
        assertNull(state.getExceptionOrNull());
    }

    @Test
    public void requestMoreComputesTheFirstPage() throws Exception {
        AsyncIncrementalListComputer<Integer> computer = AsyncIncrementalListComputer.createIn(myDisposable, computerOf(List.of(1, 2)));

        computer.requestMore();

        IncrementallyComputedValue<List<Integer>> state = first(computer.getState(), it -> it.getValueOrNull() != null);
        assertEquals(List.of(1, 2), state.getValueOrNull());
        assertFalse(state.isLoading());
    }

    @Test
    public void subsequentRequestsAppendTheFollowingPages() throws Exception {
        AsyncIncrementalListComputer<Integer> computer =
            AsyncIncrementalListComputer.createIn(myDisposable, computerOf(List.of(1, 2), List.of(3, 4)));

        computer.requestMore();
        IncrementallyComputedValue<List<Integer>> afterFirst = first(computer.getState(), it -> List.of(1, 2).equals(it.getValueOrNull()));
        // there is still a page to load
        assertFalse(afterFirst.isComplete());

        computer.requestMore();
        IncrementallyComputedValue<List<Integer>> afterSecond =
            first(computer.getState(), it -> List.of(1, 2, 3, 4).equals(it.getValueOrNull()));
        assertTrue(afterSecond.isComplete());
    }

    @Test
    public void aRequestFromAPageCollectorLoadsTheNextPage() throws Exception {
        AsyncIncrementalListComputer<Integer> computer =
            AsyncIncrementalListComputer.createIn(myDisposable, computerOf(List.of(1), List.of(2)));
        boolean[] requested = new boolean[1];
        computer.getState().addListener(state -> {
            if (!requested[0] && state.isValueAvailable() && !state.isLoading()) {
                requested[0] = true;
                computer.requestMore();
            }
        });
        computer.requestMore();

        IncrementallyComputedValue<List<Integer>> state = first(computer.getState(), IncrementallyComputedValue::isComplete);
        assertEquals(List.of(1, 2), state.getValueOrNull());
    }

    @Test
    public void aRequestMadeWhileAPageIsLoadingLoadsTheNextPage() throws Exception {
        ManualComputer loader = new ManualComputer();
        AsyncIncrementalListComputer<Integer> computer = AsyncIncrementalListComputer.createIn(myDisposable, () -> loader);

        computer.requestMore();
        first(computer.getState(), IncrementallyComputedValue::isLoading);
        computer.requestMore();

        loader.emitPage(List.of(1));
        loader.emitPage(List.of(2));

        first(computer.getState(), it -> List.of(1, 2).equals(it.getValueOrNull()));
    }

    @Test
    public void theValueIsMarkedCompleteAfterTheLastPage() throws Exception {
        AsyncIncrementalListComputer<Integer> computer = AsyncIncrementalListComputer.createIn(myDisposable, computerOf(List.of(1, 2)));

        computer.requestMore();

        IncrementallyComputedValue<List<Integer>> state = first(computer.getState(), IncrementallyComputedValue::isComplete);
        assertEquals(List.of(1, 2), state.getValueOrNull());
        assertFalse(state.isLoading());
    }

    @Test
    public void requestMoreDoesNothingOnceTheSequenceIsComplete() throws Exception {
        AsyncIncrementalListComputer<Integer> computer = AsyncIncrementalListComputer.createIn(myDisposable, computerOf(List.of(1, 2)));

        computer.requestMore();
        first(computer.getState(), IncrementallyComputedValue::isComplete);

        computer.requestMore();

        IncrementallyComputedValue<List<Integer>> state = computer.getState().get();
        assertEquals(List.of(1, 2), state.getValueOrNull());
        assertTrue(state.isComplete());
    }

    @Test
    public void anEmptySequenceProducesACompleteEmptyList() throws Exception {
        AsyncIncrementalListComputer<Integer> computer = AsyncIncrementalListComputer.createIn(myDisposable, computerOf());

        computer.requestMore();

        IncrementallyComputedValue<List<Integer>> state = first(computer.getState(), IncrementallyComputedValue::isValueAvailable);
        assertEquals(List.of(), state.getValueOrNull());
        assertTrue(state.isComplete());
        assertFalse(state.isLoading());
    }

    @Test
    public void loadingIsReportedWhileAPageIsBeingComputed() throws Exception {
        ManualComputer loader = new ManualComputer();
        AsyncIncrementalListComputer<Integer> computer = AsyncIncrementalListComputer.createIn(myDisposable, () -> loader);

        computer.requestMore();
        IncrementallyComputedValue<List<Integer>> loading = first(computer.getState(), IncrementallyComputedValue::isLoading);
        // still parked on the first page
        assertNull(loading.getValueOrNull());

        loader.emitPage(List.of(1));
        IncrementallyComputedValue<List<Integer>> loaded = first(computer.getState(), it -> !it.isLoading());
        assertEquals(List.of(1), loaded.getValueOrNull());
    }

    @Test
    public void anErrorOnTheFirstPageIsReportedAndStopsLoading() throws Exception {
        RuntimeException error = new RuntimeException("boom");
        ManualComputer loader = new ManualComputer();
        AsyncIncrementalListComputer<Integer> computer = AsyncIncrementalListComputer.createIn(myDisposable, () -> loader);

        computer.requestMore();
        first(computer.getState(), IncrementallyComputedValue::isLoading);
        loader.emitError(error);

        IncrementallyComputedValue<List<Integer>> state = first(computer.getState(), it -> it.getExceptionOrNull() != null);
        assertSame(error, state.getExceptionOrNull());
        assertNull(state.getValueOrNull());
        assertFalse(state.isLoading());
    }

    @Test
    public void anErrorOnALaterPageKeepsTheAlreadyComputedValue() throws Exception {
        RuntimeException error = new RuntimeException("boom");
        ManualComputer loader = new ManualComputer();
        AsyncIncrementalListComputer<Integer> computer = AsyncIncrementalListComputer.createIn(myDisposable, () -> loader);

        computer.requestMore();
        loader.emitPage(List.of(1));
        first(computer.getState(), it -> List.of(1).equals(it.getValueOrNull()));

        computer.requestMore();
        first(computer.getState(), IncrementallyComputedValue::isLoading);
        loader.emitError(error);

        IncrementallyComputedValue<List<Integer>> state = first(computer.getState(), it -> it.getExceptionOrNull() != null);
        assertSame(error, state.getExceptionOrNull());
        assertEquals(List.of(1), state.getValueOrNull());
        assertFalse(state.isLoading());
    }

    @Test
    public void cancellationResetsTheState() throws Exception {
        Disposable loaderDisposable = Disposable.newDisposable();
        Disposer.register(myDisposable, loaderDisposable);
        ManualComputer loader = new ManualComputer();
        AsyncIncrementalListComputer<Integer> computer = AsyncIncrementalListComputer.createIn(loaderDisposable, () -> loader);

        computer.requestMore();
        first(computer.getState(), IncrementallyComputedValue::isLoading);

        Disposer.dispose(loaderDisposable);

        IncrementallyComputedValue<List<Integer>> state = computer.getState().get();
        assertFalse(state.isValueAvailable());
        assertFalse(state.isLoading());
        assertFalse(state.isComplete());
        assertNull(state.getExceptionOrNull());
    }

    @SafeVarargs
    private static ComputableSequence<SequenceItem<List<Integer>>> computerOf(List<Integer>... pages) {
        List<SequenceItem<List<Integer>>> items = new ArrayList<>();
        for (int index = 0; index < pages.length; index++) {
            items.add(SequenceItem.of(pages[index], index == pages.length - 1));
        }
        return ComputableSequence.fromIterable(items);
    }

    /**
     * An infinite {@link SequenceComputer} whose every {@code computeNext} waits until the test releases the next
     * result via {@link #emitPage}/{@link #emitError}, so the test controls exactly when each page load completes.
     */
    private static final class ManualComputer implements SequenceComputer<SequenceItem<List<Integer>>> {
        private final Deque<CompletableFuture<ComputationOutcome<SequenceItem<List<Integer>>>>> myWaiting = new ArrayDeque<>();
        private final Deque<Object> myResults = new ArrayDeque<>();

        @Override
        public CompletableFuture<ComputationOutcome<SequenceItem<List<Integer>>>> computeNext() {
            CompletableFuture<ComputationOutcome<SequenceItem<List<Integer>>>> future = new CompletableFuture<>();
            Object result;
            synchronized (this) {
                result = myResults.poll();
                if (result == null) {
                    myWaiting.add(future);
                    return future;
                }
            }
            complete(future, result);
            return future;
        }

        void emitPage(List<Integer> page) {
            emit(page);
        }

        void emitError(Exception error) {
            emit(error);
        }

        private void emit(Object result) {
            CompletableFuture<ComputationOutcome<SequenceItem<List<Integer>>>> future;
            synchronized (this) {
                future = myWaiting.poll();
                if (future == null) {
                    myResults.add(result);
                    return;
                }
            }
            complete(future, result);
        }

        @SuppressWarnings("unchecked")
        private static void complete(CompletableFuture<ComputationOutcome<SequenceItem<List<Integer>>>> future, Object result) {
            if (result instanceof Exception error) {
                future.completeExceptionally(error);
            }
            else {
                future.complete(ComputationOutcome.item(SequenceItem.of((List<Integer>) result, false)));
            }
        }
    }
}
