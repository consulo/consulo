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
import consulo.review.async.BatchesLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;

import static consulo.it.review.ReviewTestUtil.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BatchesLoaderTest {
    private final Disposable myDisposable = Disposable.newDisposable();

    @AfterEach
    public void tearDown() {
        Disposer.dispose(myDisposable);
    }

    @Test
    public void batchesLoadIncrementally() throws Exception {
        BatchesLoader<Integer> loader = new BatchesLoader<>(myDisposable, batchesOf(List.of(1, 2), List.of(3, 4), List.of(5)));

        assertThat(collect(loader)).containsExactly(List.of(1, 2), List.of(3, 4), List.of(5));
    }

    @Test
    public void emptyProducerProducesNoBatches() throws Exception {
        BatchesLoader<Integer> loader = new BatchesLoader<>(myDisposable, batchesOf());

        assertThat(collect(loader)).isEmpty();
    }

    @Test
    public void errorIsPropagatedToConsumers() {
        RuntimeException testException = new RuntimeException("Test error");
        BatchesLoader<Integer> loader = new BatchesLoader<>(myDisposable, consumer -> CompletableFuture.failedFuture(testException));

        assertThatThrownBy(() -> collect(loader))
            .isInstanceOf(ExecutionException.class)
            .hasCauseInstanceOf(RuntimeException.class)
            .extracting(Throwable::getCause)
            .isSameAs(testException);
    }

    @Test
    public void cancelStopsLoadingAndAllowsRestart() throws Exception {
        int[] loadCount = new int[1];
        BatchesLoader<Integer> loader = new BatchesLoader<>(myDisposable, consumer -> {
            loadCount[0]++;
            consumer.accept(List.of(loadCount[0]));
            return CompletableFuture.completedFuture(null);
        });

        assertThat(collect(loader)).containsExactly(List.of(1));
        assertThat(loadCount[0]).isEqualTo(1);

        loader.cancel();

        assertThat(collect(loader)).containsExactly(List.of(2));
        assertThat(loadCount[0]).isEqualTo(2);
    }

    @Test
    public void multipleConsumersShareTheSameLoadingProcess() throws Exception {
        int[] loadCount = new int[1];
        BatchesLoader<Integer> loader = new BatchesLoader<>(myDisposable, consumer -> {
            loadCount[0]++;
            consumer.accept(List.of(1, 2));
            consumer.accept(List.of(3, 4));
            return CompletableFuture.completedFuture(null);
        });

        // First collection triggers loading
        assertThat(collect(loader).stream().flatMap(List::stream).toList()).containsExactly(1, 2, 3, 4);
        assertThat(loadCount[0]).isEqualTo(1);

        // Second collection reuses the shared loading (loading happened only once)
        assertThat(collect(loader).stream().flatMap(List::stream).toList()).containsExactly(1, 2, 3, 4);
        assertThat(loadCount[0]).isEqualTo(1);
    }

    @Test
    public void cancelIsIdempotent() throws Exception {
        BatchesLoader<Integer> loader = new BatchesLoader<>(myDisposable, batchesOf(List.of(1)));

        collect(loader);

        // Multiple cancels should not throw
        loader.cancel();
        loader.cancel();
        loader.cancel();

        // Should still work after multiple cancels
        assertThat(collect(loader)).containsExactly(List.of(1));
    }

    @Test
    public void loaderWithStopTimeoutStopsAfterAPeriodOfInactivityAndReloadsDataFromScratchOnNextCall() throws Exception {
        BatchesLoader<Integer> loader = new BatchesLoader<>(myDisposable, batchesOf(List.of(1, 2), List.of(3, 4)), Duration.ofMillis(100));

        assertThat(collect(loader)).containsExactly(List.of(1, 2), List.of(3, 4));

        assertThat(collect(loader)).containsExactly(List.of(1, 2, 3, 4));

        Thread.sleep(300);

        assertThat(collect(loader)).containsExactly(List.of(1, 2), List.of(3, 4));
    }

    @Test
    public void disposingTheParentCancelsLoading() {
        Disposable parent = Disposable.newDisposable();
        CompletableFuture<Object> production = new CompletableFuture<>();
        BatchesLoader<Integer> loader = new BatchesLoader<>(parent, consumer -> production);
        CompletableFuture<?> batches = loader.getBatches(batch -> {
        });

        Disposer.dispose(parent);

        assertThat(production).isCancelled();
        assertThat(batches).isCompletedExceptionally();
    }

    @SafeVarargs
    private static Function<Consumer<List<Integer>>, CompletableFuture<?>> batchesOf(List<Integer>... batches) {
        return consumer -> {
            for (List<Integer> batch : batches) {
                consumer.accept(batch);
            }
            return CompletableFuture.completedFuture(null);
        };
    }

    private static List<List<Integer>> collect(BatchesLoader<Integer> loader) throws Exception {
        List<List<Integer>> result = new ArrayList<>();
        await(loader.getBatches(result::add));
        return result;
    }
}
