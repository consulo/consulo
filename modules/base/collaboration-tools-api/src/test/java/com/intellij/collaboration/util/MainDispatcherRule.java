// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.util;

import kotlinx.coroutines.CoroutineDispatcher;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.TestDispatchersKt;
import kotlinx.coroutines.test.UnconfinedTestDispatcher;
import org.jetbrains.annotations.ApiStatus;
import jakarta.annotation.Nonnull;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

@ApiStatus.Internal
@ExperimentalCoroutinesApi
public final class MainDispatcherRule implements TestRule {

  private final @Nonnull CoroutineDispatcher dispatcher;

  public MainDispatcherRule(@Nonnull CoroutineDispatcher dispatcher) {
    this.dispatcher = dispatcher;
  }

  public MainDispatcherRule() {
    this(new UnconfinedTestDispatcher(null, null));
  }

  @Override
  public @Nonnull Statement apply(@Nonnull Statement base, @Nonnull Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        TestDispatchersKt.setMain(Dispatchers.INSTANCE, dispatcher);
        try {
          base.evaluate();
        }
        finally {
          TestDispatchersKt.resetMain(Dispatchers.INSTANCE);
        }
      }
    };
  }
}
