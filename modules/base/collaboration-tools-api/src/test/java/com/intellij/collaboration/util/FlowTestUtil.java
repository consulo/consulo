// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.util;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.JobKt;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowKt;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static kotlin.test.AssertionsKt.assertEquals;

public final class FlowTestUtil {
  private FlowTestUtil() {
  }

  /**
   * Collects values from a flow while running the given action, then cancels the collector.
   */
  @SuppressWarnings("unchecked")
  public static <T> @Nonnull List<T> testCollect(
    @Nonnull Flow<T> flow,
    @Nonnull kotlin.jvm.functions.Function1<? super Continuation<? super Unit>, ? extends Object> runCollecting,
    @Nonnull Continuation<? super List<T>> continuation
  ) throws Exception {
    List<T> values = new ArrayList<>();
    // This method relies on coroutineScope to manage structured concurrency.
    // The caller should invoke this inside a coroutine context.
    return CoroutineScopeKt.coroutineScope((scope, cont) -> {
      var collectorJob = BuildersKt.launch(scope,
        kotlin.coroutines.EmptyCoroutineContext.INSTANCE,
        kotlinx.coroutines.CoroutineStart.DEFAULT,
        (innerScope, innerCont) -> {
          FlowKt.collect(flow, (item, collectCont) -> {
            values.add(item);
            return Unit.INSTANCE;
          }, innerCont);
          return Unit.INSTANCE;
        });

      runCollecting.invoke(cont);
      collectorJob.cancel(null);
      return values;
    }, continuation);
  }

  /**
   * Asserts that a flow emits the given values while running the given action.
   */
  @SafeVarargs
  public static <T> void assertEmits(
    @Nonnull Flow<T> flow,
    @Nonnull kotlin.jvm.functions.Function1<? super Continuation<? super Unit>, ? extends Object> runCollecting,
    @Nonnull Continuation<? super Unit> continuation,
    @Nonnull T... shouldEmit
  ) throws Exception {
    @SuppressWarnings("unchecked")
    List<T> emitted = testCollect(flow, runCollecting, (Continuation<? super List<T>>)continuation);
    assertEquals(Arrays.asList(shouldEmit), emitted, null);
  }
}
