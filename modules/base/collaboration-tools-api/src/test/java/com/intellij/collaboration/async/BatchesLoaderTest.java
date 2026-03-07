// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.async;

import app.cash.turbine.TurbineTestKt;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.time.Duration;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.*;
import kotlinx.coroutines.test.TestBuildersKt;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public final class BatchesLoaderTest {
  @Test
  public void batchesLoadIncrementally() {
    TestBuildersKt.runTest((scope, continuation) -> {
      Flow<List<Integer>> batchesFlow = FlowKt.flowOf(List.of(1, 2), List.of(3, 4), List.of(5));
      BatchesLoader<Integer> loader = new BatchesLoader<>(scope.getBackgroundScope(), batchesFlow);

      TurbineTestKt.test(loader.getBatches(), Duration.Companion.seconds(1), (turbine, cont) -> {
        assertThat((List<Integer>)turbine.awaitItem(cont)).containsExactly(1, 2);
        assertThat((List<Integer>)turbine.awaitItem(cont)).containsExactly(3, 4);
        assertThat((List<Integer>)turbine.awaitItem(cont)).containsExactly(5);
        turbine.awaitComplete(cont);
        return Unit.INSTANCE;
      }, continuation);
      return Unit.INSTANCE;
    });
  }

  @Test
  public void emptyFlowProducesNoBatches() {
    TestBuildersKt.runTest((scope, continuation) -> {
      Flow<List<Integer>> batchesFlow = FlowKt.emptyFlow();
      BatchesLoader<Integer> loader = new BatchesLoader<>(scope.getBackgroundScope(), batchesFlow);

      TurbineTestKt.test(loader.getBatches(), Duration.Companion.seconds(1), (turbine, cont) -> {
        turbine.awaitComplete(cont);
        return Unit.INSTANCE;
      }, continuation);
      return Unit.INSTANCE;
    });
  }

  @Test
  public void errorIsPropagatedToCollectors() {
    TestBuildersKt.runTest((scope, continuation) -> {
      RuntimeException testException = new RuntimeException("Test error");
      Flow<List<Integer>> batchesFlow = FlowKt.flow(collector -> {
        throw testException;
      });
      BatchesLoader<Integer> loader = new BatchesLoader<>(scope.getBackgroundScope(), batchesFlow);

      TurbineTestKt.test(loader.getBatches(), Duration.Companion.seconds(1), (turbine, cont) -> {
        Throwable error = turbine.awaitError(cont);
        assertThat(error).isInstanceOf(RuntimeException.class);
        assertThat(error.getMessage()).isEqualTo("Test error");
        return Unit.INSTANCE;
      }, continuation);
      return Unit.INSTANCE;
    });
  }

  @Test
  public void cancelStopsLoadingAndAllowsRestart() {
    TestBuildersKt.runTest((scope, continuation) -> {
      int[] loadCount = {0};
      Flow<List<Integer>> batchesFlow = FlowKt.flow(collector -> {
        loadCount[0]++;
        collector.emit(List.of(loadCount[0]), (Continuation<Unit>)cont -> Unit.INSTANCE);
        return Unit.INSTANCE;
      });
      BatchesLoader<Integer> loader = new BatchesLoader<>(scope.getBackgroundScope(), batchesFlow);

      List<List<Integer>> firstResults = FlowKt.toList(loader.getBatches(), continuation);
      assertThat(firstResults).containsExactly(List.of(1));
      assertThat(loadCount[0]).isEqualTo(1);

      loader.cancel();

      List<List<Integer>> secondResults = FlowKt.toList(loader.getBatches(), continuation);
      assertThat(secondResults).containsExactly(List.of(2));
      assertThat(loadCount[0]).isEqualTo(2);
      return Unit.INSTANCE;
    });
  }

  @Test
  public void multipleSubscribersShareTheSameLoadingProcess() {
    TestBuildersKt.runTest((scope, continuation) -> {
      int[] loadCount = {0};
      Flow<List<Integer>> batchesFlow = FlowKt.flow(collector -> {
        loadCount[0]++;
        collector.emit(List.of(1, 2), (Continuation<Unit>)cont -> Unit.INSTANCE);
        collector.emit(List.of(3, 4), (Continuation<Unit>)cont -> Unit.INSTANCE);
        return Unit.INSTANCE;
      });
      BatchesLoader<Integer> loader = new BatchesLoader<>(scope.getBackgroundScope(), batchesFlow);

      // First collection triggers loading
      List<List<Integer>> results1 = FlowKt.toList(loader.getBatches(), continuation);
      assertThat(results1.stream().flatMap(List::stream).toList()).containsExactly(1, 2, 3, 4);
      assertThat(loadCount[0]).isEqualTo(1);

      // Second collection reuses the shared flow (loading happened only once)
      List<List<Integer>> results2 = FlowKt.toList(loader.getBatches(), continuation);
      assertThat(results2.stream().flatMap(List::stream).toList()).containsExactly(1, 2, 3, 4);
      assertThat(loadCount[0]).isEqualTo(1);
      return Unit.INSTANCE;
    });
  }

  @Test
  public void cancelIsIdempotent() {
    TestBuildersKt.runTest((scope, continuation) -> {
      Flow<List<Integer>> batchesFlow = FlowKt.flowOf(List.of(1));
      BatchesLoader<Integer> loader = new BatchesLoader<>(scope.getBackgroundScope(), batchesFlow);

      FlowKt.toList(loader.getBatches(), continuation);

      // Multiple cancels should not throw
      loader.cancel();
      loader.cancel();
      loader.cancel();

      // Should still work after multiple cancels
      List<List<Integer>> results = FlowKt.toList(loader.getBatches(), continuation);
      assertThat(results).containsExactly(List.of(1));
      return Unit.INSTANCE;
    });
  }
}
