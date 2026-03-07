// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.async;

import app.cash.turbine.TurbineTestKt;
import com.intellij.util.containers.HashingStrategy;
import kotlin.Pair;
import kotlin.Result;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.time.Duration;
import kotlinx.coroutines.*;
import kotlinx.coroutines.flow.*;
import kotlinx.coroutines.test.TestBuildersKt;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static kotlin.test.AssertionsKt.assertEquals;

public final class CoroutineUtilTest {

  @Test
  public void flatMapLatestEachEmitsAnEmptyListForEmptyInputs() {
    TestBuildersKt.runTest((scope, continuation) -> {
      Flow<List<Integer>> input = FlowKt.flowOf(List.<Integer>of());
      Flow<Object[]> output = CoroutineUtilKt.flatMapLatestEach(input, (item, cont) -> FlowKt.flowOf(item + 1));
      List<Object[]> results = FlowKt.toList(output, continuation);

      assertThat(results)
        .containsExactly(new Object[]{});
      return Unit.INSTANCE;
    });
  }

  @Test
  public void flatMapLatestEachCorrectlyHandlesSingleItem() {
    TestBuildersKt.runTest((scope, continuation) -> {
      Flow<List<Integer>> input = FlowKt.flowOf(List.of(1));
      Flow<Object[]> output = CoroutineUtilKt.flatMapLatestEach(input, (item, cont) -> FlowKt.flowOf(item + 1));
      List<Object[]> results = FlowKt.toList(output, continuation);

      assertThat(results)
        .containsExactly(new Object[]{2});
      return Unit.INSTANCE;
    });
  }

  @Test
  public void flatMapLatestEachCorrectlyHandlesManyItems() {
    TestBuildersKt.runTest((scope, continuation) -> {
      Flow<List<Integer>> input = FlowKt.onEach(
        FlowKt.flowOf(List.<Integer>of(), List.of(1), List.of(1, 3)),
        (item, cont) -> {
          DelayKt.delay(100, cont);
          return Unit.INSTANCE;
        }
      );
      Flow<Object[]> output = CoroutineUtilKt.flatMapLatestEach(input, (item, cont) -> FlowKt.flowOf(item + 1));
      List<Object[]> results = FlowKt.toList(output, continuation);

      assertThat(results)
        .containsExactly(new Object[]{}, new Object[]{2}, new Object[]{2, 4});
      return Unit.INSTANCE;
    });
  }

  @Test
  public void collectingBatchesWorks() {
    TestBuildersKt.runTest((scope, continuation) -> {
      Flow<List<Integer>> source = FlowKt.flowOf(List.of(1, 2), List.of(3), List.of(4, 5));
      Flow<List<Integer>> batched = CoroutineUtil.collectBatches(source);
      List<Integer> collectedList = FlowKt.last(batched, continuation);

      assertEquals(List.of(1, 2, 3, 4, 5), collectedList, null);
      return Unit.INSTANCE;
    });
  }

  private sealed interface Action<T> permits Action.Emit {
    record Emit<T>(T value) implements Action<T> {
    }
  }

  private static final class SomeException extends Exception {
    static final SomeException INSTANCE = new SomeException();

    private SomeException() {
    }

    private Object readResolve() {
      return INSTANCE;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof SomeException;
    }

    @Override
    public int hashCode() {
      return SomeException.class.hashCode();
    }
  }

  /**
   * Tries to make sure the actions given to this function are executed in-order,
   * even though they might be executed asynchronously.
   */
  @SuppressWarnings("unchecked")
  private static <T, R> List<R> executeFlowInstructionsAndCollectAsListIn(
    CoroutineScope cs,
    List<Action<T>> actions,
    kotlin.jvm.functions.Function1<Flow<T>, Flow<R>> transformer,
    Continuation<? super Unit> outerContinuation
  ) {
    List<Action<T>> mutActions = new ArrayList<>(actions);
    List<R> results = new ArrayList<>();

    Job job = CoroutineUtil.launchNow(cs, (scope, continuation) -> {
      Flow<T> baseFlow = FlowKt.flow(collector -> {
        int prevSize = mutActions.size() + 1;
        while (!mutActions.isEmpty()) {
          while ((!mutActions.isEmpty() && !(mutActions.getFirst() instanceof Action.Emit)) || mutActions.size() == prevSize) {
            DelayKt.delay(200, (Continuation<? super Unit>)collector);
          }

          if (mutActions.isEmpty()) {
            CoroutineScopeKt.cancel(JobKt.getJob(kotlin.coroutines.ContinuationKt.getContext((Continuation<?>)collector)), null);
            JobKt.ensureActive(kotlin.coroutines.ContinuationKt.getContext((Continuation<?>)collector));
            return Unit.INSTANCE;
          }

          collector.emit(((Action.Emit<T>)mutActions.getFirst()).value(), (Continuation<? super Unit>)collector);
          prevSize = mutActions.size();
        }
        return Unit.INSTANCE;
      });

      Flow<R> transformed = transformer.invoke(baseFlow);
      FlowKt.collect(transformed, (item, cont) -> {
        results.add(item);
        if (!mutActions.isEmpty()) {
          mutActions.removeFirst();
        }
        return Unit.INSTANCE;
      }, continuation);
      return Unit.INSTANCE;
    });

    // Wait for actions to be consumed
    while (!mutActions.isEmpty()) {
      try {
        Thread.sleep(200);
      }
      catch (InterruptedException ignored) {
      }
    }
    job.cancel(null);

    return new ArrayList<>(results);
  }

  @Test
  public void transformingConsecutiveSuccessesWorks() {
    TestBuildersKt.runTest((scope, continuation) -> {
      List<Action<Result<List<Integer>>>> actions = List.of(
        new Action.Emit<>(Result.Companion.success(List.of(1))),
        new Action.Emit<>(Result.Companion.success(List.of(2))),
        new Action.Emit<>(Result.Companion.failure(SomeException.INSTANCE)),
        new Action.Emit<>(Result.Companion.success(List.of(3)))
      );

      List<Result<List<Integer>>> results = executeFlowInstructionsAndCollectAsListIn(
        scope, actions,
        flow -> CoroutineUtilKt.transformConsecutiveSuccesses(flow, true, (successFlow, cont) -> CoroutineUtil.collectBatches(successFlow)),
        continuation
      );

      List<Result<List<Integer>>> expected = List.of(
        Result.Companion.success(List.of(1)),
        Result.Companion.success(List.of(1, 2)),
        Result.Companion.failure(SomeException.INSTANCE),
        Result.Companion.success(List.of(3))
      );
      assertThat(results).isEqualTo(expected);
      return Unit.INSTANCE;
    });
  }

  @Test
  public void transformingConsecutiveSuccessesWithoutInterruptingWorks() {
    TestBuildersKt.runTest((scope, continuation) -> {
      List<Action<Result<List<Integer>>>> actions = List.of(
        new Action.Emit<>(Result.Companion.success(List.of(1))),
        new Action.Emit<>(Result.Companion.success(List.of(2))),
        new Action.Emit<>(Result.Companion.failure(SomeException.INSTANCE)),
        new Action.Emit<>(Result.Companion.success(List.of(3)))
      );

      List<Result<List<Integer>>> results = executeFlowInstructionsAndCollectAsListIn(
        scope, actions,
        flow -> CoroutineUtilKt.transformConsecutiveSuccesses(flow, false, (successFlow, cont) -> CoroutineUtil.collectBatches(successFlow)),
        continuation
      );

      List<Result<List<Integer>>> expected = List.of(
        Result.Companion.success(List.of(1)),
        Result.Companion.success(List.of(1, 2)),
        Result.Companion.failure(SomeException.INSTANCE),
        Result.Companion.success(List.of(1, 2, 3))
      );
      assertThat(results).isEqualTo(expected);
      return Unit.INSTANCE;
    });
  }

  @Test
  public void transformingConsecutiveSuccessesResetsAfterEveryFailure() {
    TestBuildersKt.runTest((scope, continuation) -> {
      List<Action<Result<List<Integer>>>> actions = List.of(
        new Action.Emit<>(Result.Companion.success(List.of(1))),
        new Action.Emit<>(Result.Companion.failure(SomeException.INSTANCE)),
        new Action.Emit<>(Result.Companion.success(List.of(2))),
        new Action.Emit<>(Result.Companion.failure(SomeException.INSTANCE)),
        new Action.Emit<>(Result.Companion.success(List.of(3))),
        new Action.Emit<>(Result.Companion.success(List.of(4))),
        new Action.Emit<>(Result.Companion.failure(SomeException.INSTANCE)),
        new Action.Emit<>(Result.Companion.success(List.of(5)))
      );

      List<Result<List<Integer>>> results = executeFlowInstructionsAndCollectAsListIn(
        scope, actions,
        flow -> CoroutineUtilKt.transformConsecutiveSuccesses(flow, true, (successFlow, cont) -> CoroutineUtil.collectBatches(successFlow)),
        continuation
      );

      List<Result<List<Integer>>> expected = List.of(
        Result.Companion.success(List.of(1)),
        Result.Companion.failure(SomeException.INSTANCE),
        Result.Companion.success(List.of(2)),
        Result.Companion.failure(SomeException.INSTANCE),
        Result.Companion.success(List.of(3)),
        Result.Companion.success(List.of(3, 4)),
        Result.Companion.failure(SomeException.INSTANCE),
        Result.Companion.success(List.of(5))
      );
      assertThat(results).isEqualTo(expected);
      return Unit.INSTANCE;
    });
  }

  @Test
  public void associateCachingByPreservesOrder() {
    TestBuildersKt.runTest((scope, continuation) -> {
      MutableStateFlow<List<Integer>> inputFlow = StateFlowKt.MutableStateFlow(List.<Integer>of());
      Flow<?> outputFlow = CoroutineUtilKt.associateCachingBy(
        inputFlow,
        item -> item,
        HashingStrategy.canonical(),
        (cs2, item, cont) -> item,
        null
      );

      TurbineTestKt.test(outputFlow, null, (turbine, cont) -> {
        assertThat(((java.util.Map<?, ?>)turbine.awaitItem(cont)).values()).isEmpty();

        // initial add
        inputFlow.setValue(List.of(1, 2, 3));
        assertThat(new ArrayList<>(((java.util.Map<?, ?>)turbine.awaitItem(cont)).values())).isEqualTo(List.of(1, 2, 3));

        // simple add
        inputFlow.setValue(List.of(1, 2, 3, 4));
        assertThat(new ArrayList<>(((java.util.Map<?, ?>)turbine.awaitItem(cont)).values())).isEqualTo(List.of(1, 2, 3, 4));

        // simple remove
        inputFlow.setValue(List.of(1, 3, 4));
        assertThat(new ArrayList<>(((java.util.Map<?, ?>)turbine.awaitItem(cont)).values())).isEqualTo(List.of(1, 3, 4));

        // simple remove + add
        inputFlow.setValue(List.of(1, 2, 3));
        assertThat(new ArrayList<>(((java.util.Map<?, ?>)turbine.awaitItem(cont)).values())).isEqualTo(List.of(1, 2, 3));

        return Unit.INSTANCE;
      }, continuation);
      return Unit.INSTANCE;
    });
  }

  static final class UpdatableValue<T> {
    final int key;
    T value;

    UpdatableValue(int key, T value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof UpdatableValue<?> that)) return false;
      return key == that.key && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(key, value);
    }

    @Override
    public String toString() {
      return "UpdatableValue(key=" + key + ", value=" + value + ")";
    }
  }

  @Test
  public void changesFlowSendsAnInitialValue() {
    TestBuildersKt.runTest((scope, continuation) -> {
      MutableStateFlow<List<Integer>> underlying = StateFlowKt.MutableStateFlow(List.of(1, 2, 3));

      TurbineTestKt.test(CoroutineUtilKt.changesFlow(underlying), Duration.Companion.seconds(1), (turbine, cont) -> {
        @SuppressWarnings("unchecked")
        List<ComputedListChange<Integer>> item = (List<ComputedListChange<Integer>>)turbine.awaitItem(cont);
        assertThat(item).isEqualTo(List.of(new ComputedListChange.Insert<>(0, List.of(1, 2, 3))));

        turbine.expectNoEvents(cont);
        return Unit.INSTANCE;
      }, continuation);
      return Unit.INSTANCE;
    });
  }

  @Test
  public void changesFlowSendsAnAddUpdate() {
    TestBuildersKt.runTest((scope, continuation) -> {
      MutableStateFlow<List<Integer>> underlying = StateFlowKt.MutableStateFlow(List.of(1, 2, 3));

      TurbineTestKt.test(CoroutineUtilKt.changesFlow(underlying), Duration.Companion.seconds(1), (turbine, cont) -> {
        turbine.awaitItem(cont); // initial

        underlying.setValue(List.of(1, 2, 3, 4));
        @SuppressWarnings("unchecked")
        List<ComputedListChange<Integer>> item = (List<ComputedListChange<Integer>>)turbine.awaitItem(cont);
        assertThat(item).isEqualTo(List.of(new ComputedListChange.Insert<>(3, List.of(4))));

        turbine.expectNoEvents(cont);
        return Unit.INSTANCE;
      }, continuation);
      return Unit.INSTANCE;
    });
  }

  @Test
  public void changesFlowSendsARemoveUpdate() {
    TestBuildersKt.runTest((scope, continuation) -> {
      MutableStateFlow<List<Integer>> underlying = StateFlowKt.MutableStateFlow(List.of(1, 2, 3));

      TurbineTestKt.test(CoroutineUtilKt.changesFlow(underlying), Duration.Companion.seconds(1), (turbine, cont) -> {
        turbine.awaitItem(cont); // initial

        underlying.setValue(List.of(1, 3));
        @SuppressWarnings("unchecked")
        List<ComputedListChange<Integer>> item = (List<ComputedListChange<Integer>>)turbine.awaitItem(cont);
        assertThat(item).isEqualTo(List.of(new ComputedListChange.Remove(1, 1)));

        turbine.expectNoEvents(cont);
        return Unit.INSTANCE;
      }, continuation);
      return Unit.INSTANCE;
    });
  }

  @Test
  public void changesFlowSendsAListOfReproducibleChanges() {
    TestBuildersKt.runTest((scope, continuation) -> {
      List<Integer> l1 = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
      List<Integer> l2 = List.of(1, 3, 2, 7, 20, 10, 2, 5, 7, 3);

      MutableStateFlow<List<Integer>> underlying = StateFlowKt.MutableStateFlow(l1);

      TurbineTestKt.test(CoroutineUtilKt.changesFlow(underlying), Duration.Companion.seconds(1), (turbine, cont) -> {
        turbine.awaitItem(cont); // initial

        underlying.setValue(l2);
        @SuppressWarnings("unchecked")
        List<ComputedListChange<Integer>> updates = (List<ComputedListChange<Integer>>)turbine.awaitItem(cont);
        assertThat(applyUpdates(l1, updates)).isEqualTo(l2);

        turbine.expectNoEvents(cont);
        return Unit.INSTANCE;
      }, continuation);
      return Unit.INSTANCE;
    });
  }

  // If flaky on TC, just ignore and use locally. It would be because of timings
  // Should take about ~5 seconds due to explicit timeouts
  @Test
  public void changesFlowFuzzyTest() {
    TestBuildersKt.runTest((scope, continuation) -> {
      // deterministic random
      Random rand = new Random(123456789);

      for (int iter = 0; iter < 10; iter++) {
        MutableStateFlow<List<Integer>> underlying = StateFlowKt.MutableStateFlow(randomList(rand));
        int nUpdates = rand.nextInt(10);

        final List<Integer>[] outputState = new List[]{null};

        TurbineTestKt.test(CoroutineUtilKt.changesFlow(underlying), Duration.Companion.milliseconds(250), (turbine, cont) -> {
          @SuppressWarnings("unchecked")
          List<ComputedListChange<Integer>> initialChanges = (List<ComputedListChange<Integer>>)turbine.awaitItem(cont);
          outputState[0] = ((ComputedListChange.Insert<Integer>)initialChanges.getFirst()).values();

          for (int i = 0; i < nUpdates; i++) {
            // update the list
            underlying.setValue(randomList(rand));

            // await the coming updates or not
            if (i == nUpdates - 1 || rand.nextBoolean()) {
              @SuppressWarnings("unchecked")
              List<ComputedListChange<Integer>> updates = (List<ComputedListChange<Integer>>)turbine.awaitItem(cont);

              while (updates != null) {
                assertThat(updates).isNotEmpty();
                outputState[0] = applyUpdates(outputState[0], updates);
                try {
                  @SuppressWarnings("unchecked")
                  List<ComputedListChange<Integer>> next = (List<ComputedListChange<Integer>>)turbine.awaitItem(cont);
                  updates = next;
                }
                catch (Exception e) {
                  updates = null;
                }
              }

              assertThat(outputState[0]).isEqualTo(underlying.getValue());
            }
          }

          turbine.expectNoEvents(cont);
          return Unit.INSTANCE;
        }, continuation);
      }
      return Unit.INSTANCE;
    });
  }

  private static List<Integer> randomList(Random rand) {
    List<Integer> l = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 6, 7, 8, 9, 10, 10, 10, 11, 11, 11));
    java.util.Collections.shuffle(l, rand);
    int removeCount = rand.nextInt(l.size());
    for (int i = 0; i < removeCount; i++) {
      l.remove(rand.nextInt(l.size()));
    }
    return l;
  }

  private static <V> List<V> applyUpdates(List<V> base, List<ComputedListChange<V>> changes) {
    List<V> list = new ArrayList<>(base);
    for (ComputedListChange<V> change : changes) {
      switch (change) {
        case ComputedListChange.Remove remove -> {
          for (int i = 0; i < remove.length(); i++) {
            list.remove(remove.atIndex());
          }
        }
        case ComputedListChange.Insert<V> insert -> list.addAll(insert.atIndex(), insert.values());
      }
    }
    return List.copyOf(list);
  }
}
