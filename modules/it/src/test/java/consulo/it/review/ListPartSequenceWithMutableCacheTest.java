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
import consulo.review.async.ListPartSequenceWithMutableCache;
import consulo.review.util.ComputableSequence;
import consulo.review.util.ComputationOutcome;
import consulo.review.util.SequenceComputer;
import consulo.review.util.SequenceItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static consulo.it.review.ReviewTestUtil.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ListPartSequenceWithMutableCacheTest {
    private final Disposable myDisposable = Disposable.newDisposable();

    @AfterEach
    public void tearDown() {
        Disposer.dispose(myDisposable);
    }

    @Test
    public void emitsAllPartsInOrderPreservingTheIsLastFlag() throws Exception {
        ListPartSequenceWithMutableCache<Integer> loader = new ListPartSequenceWithMutableCache<>(myDisposable, sourceOf(List.of(1, 2), List.of(3, 4)));

        List<SequenceItem<List<Integer>>> parts = await(loader.toList());

        assertEquals(List.of(List.of(1, 2), List.of(3, 4)), parts.stream().map(SequenceItem::getValue).toList());
        assertEquals(List.of(false, true), parts.stream().map(SequenceItem::isLast).toList());
    }

    @Test
    public void anEmptySourceYieldsNothing() throws Exception {
        ListPartSequenceWithMutableCache<Integer> loader = new ListPartSequenceWithMutableCache<>(myDisposable, sourceOf());

        assertEquals(List.of(), loadedLists(loader));
    }

    @Test
    public void disposingTheCacheDoesNotCancelTheCaller() throws Exception {
        Disposable parent = Disposable.newDisposable();
        ListPartSequenceWithMutableCache<Integer> loader = new ListPartSequenceWithMutableCache<>(parent, sourceOf(List.of(1, 2)));
        Disposer.dispose(parent);

        ComputationOutcome<SequenceItem<List<Integer>>> outcome = await(loader.getComputer().computeNext());
        assertEquals(ComputationOutcome.done(), outcome, "cancelled computer returned data");
    }

    @Test
    public void partsAreComputedOnceAndServedFromCacheOnRepeatedConsumption() throws Exception {
        RecordingSource source = sourceOf(List.of(1), List.of(2));
        ListPartSequenceWithMutableCache<Integer> loader = new ListPartSequenceWithMutableCache<>(myDisposable, source);

        assertEquals(List.of(List.of(1), List.of(2)), loadedLists(loader));
        // fully served from cache
        assertEquals(List.of(List.of(1), List.of(2)), loadedLists(loader));

        // each part is computed exactly once, not re-fetched
        assertEquals(2, source.myPartsComputed);
        // the underlying sequence is walked by a single computer
        assertEquals(1, source.myComputersCreated);
    }

    @Test
    public void updateItemReplacesTheFirstAcceptedItemAndStops() throws Exception {
        ListPartSequenceWithMutableCache<Integer> loader =
            new ListPartSequenceWithMutableCache<>(myDisposable, sourceOf(List.of(1, 2, 3), List.of(2, 4)));
        // materialize the cache
        await(loader.toList());

        loader.updateItem(it -> it == 2 ? 20 : null);

        // only the first 2 (in the first part) is replaced; the 2 in the second part is left untouched
        assertEquals(List.of(List.of(1, 20, 3), List.of(2, 4)), loadedLists(loader));
    }

    @Test
    public void updateItemCanReplaceAnItemInALaterPart() throws Exception {
        ListPartSequenceWithMutableCache<Integer> loader =
            new ListPartSequenceWithMutableCache<>(myDisposable, sourceOf(List.of(1, 2), List.of(3, 4)));
        await(loader.toList());

        loader.updateItem(it -> it == 3 ? 30 : null);

        assertEquals(List.of(List.of(1, 2), List.of(30, 4)), loadedLists(loader));
    }

    @Test
    public void updateItemIsANoOpWhenTheUpdaterAcceptsNothing() throws Exception {
        ListPartSequenceWithMutableCache<Integer> loader =
            new ListPartSequenceWithMutableCache<>(myDisposable, sourceOf(List.of(1, 2), List.of(3, 4)));
        await(loader.toList());

        loader.updateItem(it -> null);

        assertEquals(List.of(List.of(1, 2), List.of(3, 4)), loadedLists(loader));
    }

    @Test
    public void updateItemPreservesTheIsLastFlagOfTheMutatedPart() throws Exception {
        ListPartSequenceWithMutableCache<Integer> loader = new ListPartSequenceWithMutableCache<>(myDisposable, sourceOf(List.of(1), List.of(2)));
        await(loader.toList());

        loader.updateItem(it -> it == 2 ? 20 : null);

        assertEquals(List.of(false, true), await(loader.toList()).stream().map(SequenceItem::isLast).toList());
    }

    @Test
    public void updateItemBeforeAnythingIsLoadedIsDropped() throws Exception {
        ListPartSequenceWithMutableCache<Integer> loader = new ListPartSequenceWithMutableCache<>(myDisposable, sourceOf(List.of(1, 2)));

        // nothing has been consumed yet, so there is no cache to mutate
        loader.updateItem(it -> 99);

        assertEquals(List.of(List.of(1, 2)), loadedLists(loader));
    }

    @Test
    public void resetDiscardsCachedMutationsAndReloadsFromTheSource() throws Exception {
        RecordingSource source = sourceOf(List.of(1, 2));
        ListPartSequenceWithMutableCache<Integer> loader = new ListPartSequenceWithMutableCache<>(myDisposable, source);

        await(loader.toList());
        loader.updateItem(it -> it == 1 ? 10 : null);
        assertEquals(List.of(List.of(10, 2)), loadedLists(loader));

        loader.reset();

        // the mutation is gone, values are fresh from the source
        assertEquals(List.of(List.of(1, 2)), loadedLists(loader));
        // a brand-new computer is created after the reset
        assertEquals(2, source.myComputersCreated);
    }

    @Test
    public void resetMakesTheLoaderObserveFreshSourceData() throws Exception {
        MutableSource source = new MutableSource(partsOf(List.of(1, 2)));
        ListPartSequenceWithMutableCache<Integer> loader = new ListPartSequenceWithMutableCache<>(myDisposable, source);

        assertEquals(List.of(List.of(1, 2)), loadedLists(loader));

        // the underlying data changes...
        source.myParts = partsOf(List.of(3, 4));
        // ...but the cache keeps serving the old snapshot
        assertEquals(List.of(List.of(1, 2)), loadedLists(loader));

        loader.reset();

        // after the reset the new data is walked
        assertEquals(List.of(List.of(3, 4)), loadedLists(loader));
    }

    @Test
    public void resetBeforeAnythingIsLoadedIsANoOp() throws Exception {
        ListPartSequenceWithMutableCache<Integer> loader = new ListPartSequenceWithMutableCache<>(myDisposable, sourceOf(List.of(1, 2)));

        // nothing has been initialized yet, so there is nothing to drop
        loader.reset();

        assertEquals(List.of(List.of(1, 2)), loadedLists(loader));
    }

    /**
     * A source {@link ComputableSequence} that records how many computers it hands out and how many parts it actually computes.
     */
    private static final class RecordingSource implements ComputableSequence<SequenceItem<List<Integer>>> {
        private final List<SequenceItem<List<Integer>>> myParts;
        private int myComputersCreated;
        private int myPartsComputed;

        RecordingSource(List<SequenceItem<List<Integer>>> parts) {
            myParts = parts;
        }

        @Override
        public SequenceComputer<SequenceItem<List<Integer>>> getComputer() {
            myComputersCreated++;
            return new SequenceComputer<>() {
                private int myIndex;

                @Override
                public CompletableFuture<ComputationOutcome<SequenceItem<List<Integer>>>> computeNext() {
                    if (myIndex >= myParts.size()) {
                        return CompletableFuture.completedFuture(ComputationOutcome.done());
                    }
                    myPartsComputed++;
                    return CompletableFuture.completedFuture(ComputationOutcome.item(myParts.get(myIndex++)));
                }
            };
        }
    }

    /**
     * A source whose backing parts can change between walks; each computer snapshots the parts at creation time.
     */
    private static final class MutableSource implements ComputableSequence<SequenceItem<List<Integer>>> {
        private List<SequenceItem<List<Integer>>> myParts;

        MutableSource(List<SequenceItem<List<Integer>>> parts) {
            myParts = parts;
        }

        @Override
        public SequenceComputer<SequenceItem<List<Integer>>> getComputer() {
            List<SequenceItem<List<Integer>>> snapshot = myParts;
            return new SequenceComputer<>() {
                private int myIndex;

                @Override
                public CompletableFuture<ComputationOutcome<SequenceItem<List<Integer>>>> computeNext() {
                    return CompletableFuture.completedFuture(myIndex < snapshot.size()
                        ? ComputationOutcome.item(snapshot.get(myIndex++))
                        : ComputationOutcome.done());
                }
            };
        }
    }

    /**
     * Builds list parts from raw lists, flagging the final one as the last.
     */
    @SafeVarargs
    private static List<SequenceItem<List<Integer>>> partsOf(List<Integer>... lists) {
        List<SequenceItem<List<Integer>>> parts = new ArrayList<>();
        for (int i = 0; i < lists.length; i++) {
            parts.add(SequenceItem.of(lists[i], i == lists.length - 1));
        }
        return parts;
    }

    @SafeVarargs
    private static RecordingSource sourceOf(List<Integer>... lists) {
        return new RecordingSource(partsOf(lists));
    }

    private static List<List<Integer>> loadedLists(ListPartSequenceWithMutableCache<Integer> loader) throws Exception {
        return await(loader.toList()).stream().map(SequenceItem::getValue).toList();
    }
}
