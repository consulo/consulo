// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui;

import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowKt;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;
import jakarta.annotation.Nonnull;

/**
 * Utility methods for {@link SingleValueModel}, converted from Kotlin extension functions.
 */
public final class SingleValueModelUtil {
  private SingleValueModelUtil() {
  }

  public static <T> @Nonnull StateFlow<T> asStateFlow(@Nonnull SingleValueModel<T> model) {
    MutableStateFlow<T> flow = kotlinx.coroutines.flow.StateFlowKt.MutableStateFlow(model.getValue());
    model.addAndInvokeListener(v -> flow.setValue(model.getValue()));
    return flow;
  }

  public static <T> void bindValueIn(@Nonnull SingleValueModel<T> model, @Nonnull CoroutineScope scope, @Nonnull Flow<T> valueFlow) {
    kotlinx.coroutines.BuildersKt.launch(scope, scope.getCoroutineContext(), kotlinx.coroutines.CoroutineStart.DEFAULT,
      (coroutineScope, continuation) -> FlowKt.collect(valueFlow, newValue -> {
        model.setValue((T)newValue);
        return kotlin.Unit.INSTANCE;
      }, continuation));
  }
}
