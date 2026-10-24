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

import consulo.review.util.ComputableSequence;
import consulo.review.util.ComputationOutcome;
import consulo.review.util.SequenceComputer;
import consulo.review.util.SequenceItem;
import consulo.util.lang.Pair;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static consulo.it.review.ReviewTestUtil.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ComputableSequenceTest {
    @Test
    public void emitsEveryComputedItemInOrder() throws Exception {
        assertEquals(List.of(1, 2, 3), await(ComputableSequence.fromIterable(List.of(1, 2, 3)).toList()));
    }

    @Test
    public void anEmptySequenceYieldsNothing() throws Exception {
        ComputableSequence<Integer> empty = () -> () -> CompletableFuture.completedFuture(ComputationOutcome.done());
        assertEquals(List.of(), await(empty.toList()));
    }

    @Test
    public void nullValuesAreEmittedAsItemsNotTreatedAsTheEnd() throws Exception {
        List<Integer> values = Arrays.asList(1, null, 3);
        assertEquals(values, await(ComputableSequence.fromIterable(values).toList()));
    }

    @Test
    public void getComputerRestartsTheSequenceFromTheBeginning() throws Exception {
        ComputableSequence<Integer> seq = ComputableSequence.fromIterable(List.of(1, 2));
        assertEquals(List.of(1, 2), await(seq.toList()));
        assertEquals(List.of(1, 2), await(seq.toList()));
    }

    @Test
    public void computeNextYieldsEachItemAndThenReportsDone() throws Exception {
        SequenceComputer<Integer> computer = ComputableSequence.fromIterable(List.of(1, 2)).getComputer();
        assertEquals(ComputationOutcome.item(1), await(computer.computeNext()));
        assertEquals(ComputationOutcome.item(2), await(computer.computeNext()));
        assertEquals(ComputationOutcome.done(), await(computer.computeNext()));
    }

    @Test
    public void mapTransformsEveryItem() throws Exception {
        assertEquals(List.of(10, 20), await(ComputableSequence.fromIterable(List.of(1, 2)).map(it -> it * 10).toList()));
    }

    @Test
    public void computeNextIsNotCalledAgainOnceItReportsDone() throws Exception {
        int[] computeCalls = new int[1];
        ComputableSequence<Integer> seq = () -> () -> {
            computeCalls[0]++;
            return CompletableFuture.completedFuture(ComputationOutcome.done());
        };
        assertEquals(List.of(), await(seq.toList()));
        assertEquals(1, computeCalls[0]);
    }

    @Test
    public void aLongSynchronousSequenceDoesNotOverflowTheStack() throws Exception {
        Integer[] values = new Integer[100_000];
        Arrays.fill(values, 1);
        assertEquals(values.length, await(ComputableSequence.fromIterable(Arrays.asList(values)).toList()).size());
    }

    @Test
    public void byPointerWalksThePointerUntilItIsNullFlaggingTheLastItem() throws Exception {
        ComputableSequence<SequenceItem<String>> seq = ComputableSequence.byPointer(1, n ->
            CompletableFuture.completedFuture(Pair.create("item" + n, n < 3 ? n + 1 : null)));
        List<SequenceItem<String>> items = await(seq.toList());
        assertEquals(List.of("item1", "item2", "item3"), items.stream().map(SequenceItem::getValue).toList());
        assertEquals(List.of(false, false, true), items.stream().map(SequenceItem::isLast).toList());
    }

    @Test
    public void byPointerYieldsASingleLastItemWhenTheInitialPointerIsTerminal() throws Exception {
        List<SequenceItem<String>> items = await(ComputableSequence.<Integer, String>byPointer(0, n ->
            CompletableFuture.completedFuture(Pair.create("only", null))).toList());
        assertEquals(List.of(SequenceItem.of("only", true)), items);
    }

    @Test
    public void sequenceComputerByPointerReportsEachItemAndThenDone() throws Exception {
        SequenceComputer<SequenceItem<String>> computer = SequenceComputer.byPointer(1, n ->
            CompletableFuture.completedFuture(Pair.create("v" + n, n < 2 ? n + 1 : null)));
        assertEquals(ComputationOutcome.item(SequenceItem.of("v1", false)), await(computer.computeNext()));
        assertEquals(ComputationOutcome.item(SequenceItem.of("v2", true)), await(computer.computeNext()));
        assertEquals(ComputationOutcome.done(), await(computer.computeNext()));
    }

    @Test
    public void byPointerRestartsFromTheInitialPointerOnEachGetComputer() throws Exception {
        ComputableSequence<SequenceItem<Integer>> seq = ComputableSequence.byPointer(1, n ->
            CompletableFuture.completedFuture(Pair.create(n, n < 3 ? n + 1 : null)));
        assertEquals(List.of(1, 2, 3), await(seq.toList()).stream().map(SequenceItem::getValue).toList());
        assertEquals(List.of(1, 2, 3), await(seq.toList()).stream().map(SequenceItem::getValue).toList());
    }
}
