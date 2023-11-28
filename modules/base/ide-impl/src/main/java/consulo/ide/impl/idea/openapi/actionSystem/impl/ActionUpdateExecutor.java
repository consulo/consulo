// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.actionSystem.impl;

import consulo.logging.Logger;

import java.util.concurrent.*;
import java.util.function.Supplier;

public class ActionUpdateExecutor {
  private static final Logger LOG = Logger.getInstance(ActionUpdateExecutor.class);

  public static <T> T compute(Supplier<T> supplier) {
    ForkJoinTask<T> task = ForkJoinPool.commonPool().submit(supplier::get);
    try {
      return task.get();
    }
    catch (InterruptedException | ExecutionException e) {
      LOG.error(e);
      return null;
    }
  }
}
