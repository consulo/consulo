// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.concurrency;

import javax.annotation.Nonnull;

import java.util.concurrent.Executor;

public final class SameThreadExecutor implements Executor {
  private SameThreadExecutor() {
  }

  public static final Executor INSTANCE = new SameThreadExecutor();

  @Override
  public void execute(@Nonnull Runnable command) {
    command.run();
  }
}
