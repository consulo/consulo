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
import consulo.review.async.LoaderWithMutableCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static consulo.it.review.ReviewTestUtil.await;
import static org.assertj.core.api.Assertions.assertThat;

public class LoaderWithMutableCacheTest {
    private final Disposable myDisposable = Disposable.newDisposable();

    @AfterEach
    public void tearDown() {
        Disposer.dispose(myDisposable);
    }

    @Test
    public void loadIsComputedOnceAndCached() throws Exception {
        int[] loads = new int[1];
        LoaderWithMutableCache<Integer> loader = new LoaderWithMutableCache<>(myDisposable, () -> CompletableFuture.completedFuture(++loads[0]));

        assertThat(await(loader.load())).isEqualTo(1);
        assertThat(await(loader.load())).isEqualTo(1);
        assertThat(loads[0]).isEqualTo(1);
    }

    @Test
    public void clearCacheReloadsAndSignals() throws Exception {
        int[] loads = new int[1];
        int[] signals = new int[1];
        LoaderWithMutableCache<Integer> loader = new LoaderWithMutableCache<>(myDisposable, () -> CompletableFuture.completedFuture(++loads[0]));
        loader.addUpdatedListener(myDisposable, () -> signals[0]++);

        assertThat(await(loader.load())).isEqualTo(1);
        loader.clearCache();

        assertThat(signals[0]).isEqualTo(1);
        assertThat(await(loader.load())).isEqualTo(2);
    }

    @Test
    public void aLoadWaitingOnAClearedRequestGetsTheNextResult() throws Exception {
        List<CompletableFuture<Integer>> requests = new ArrayList<>();
        LoaderWithMutableCache<Integer> loader = new LoaderWithMutableCache<>(myDisposable, () -> {
            CompletableFuture<Integer> request = new CompletableFuture<>();
            requests.add(request);
            return request;
        });

        CompletableFuture<Integer> waiting = loader.load();
        loader.clearCache();

        assertThat(requests.get(0)).isCancelled();
        assertThat(waiting).isNotDone();
        requests.get(1).complete(42);
        assertThat(await(waiting)).isEqualTo(42);
    }

    @Test
    public void updateLoadedOverridesTheResult() throws Exception {
        int[] signals = new int[1];
        LoaderWithMutableCache<Integer> loader = new LoaderWithMutableCache<>(myDisposable, () -> CompletableFuture.completedFuture(1));
        loader.addUpdatedListener(myDisposable, () -> signals[0]++);

        // nothing loaded yet, so there is nothing to update
        loader.updateLoaded(it -> it + 10);
        assertThat(signals[0]).isEqualTo(0);

        assertThat(await(loader.load())).isEqualTo(1);
        loader.updateLoaded(it -> it + 10);
        assertThat(await(loader.load())).isEqualTo(11);
        assertThat(signals[0]).isEqualTo(1);

        // the same value does not signal
        loader.updateLoaded(it -> it);
        assertThat(signals[0]).isEqualTo(1);
    }
}
