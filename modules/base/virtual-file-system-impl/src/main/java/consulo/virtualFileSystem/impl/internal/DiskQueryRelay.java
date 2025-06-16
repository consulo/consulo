// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.impl.internal;

import consulo.process.io.ProcessIOExecutorService;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.application.internal.ProgressIndicatorUtils;
import jakarta.annotation.Nonnull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * A utility to run a potentially long function on a pooled thread, wait for it in an interruptible way and reuse that computation if it's
 * needed again if it's still running. Function results should be ready for concurrent access, preferably thread-safe.
 */
public final class DiskQueryRelay<Param, Result> {
  private final Function<? super Param, ? extends Result> myFunction;

  /**
   * We remember the submitted tasks in "myTasks" until they're finished, to avoid creating many-many similar threads
   * in case the callee is interrupted by "checkCanceled", restarted, comes again with the same query, is interrupted again, and so on.
   */
  private final Map<Param, Future<Result>> myTasks = new ConcurrentHashMap<>();

  public DiskQueryRelay(@Nonnull Function<? super Param, ? extends Result> function) {
    myFunction = function;
  }

  public Result accessDiskWithCheckCanceled(@Nonnull Param arg) {
    ProgressIndicator indicator = ProgressIndicatorProvider.getGlobalProgressIndicator();
    if (indicator == null) {
      return myFunction.apply(arg);
    }

    Future<Result> future = myTasks.computeIfAbsent(arg, eachArg -> ProcessIOExecutorService.INSTANCE.submit(() -> {
      try {
        return myFunction.apply(eachArg);
      }
      finally {
        myTasks.remove(eachArg);
      }
    }));
    if (future.isDone()) {
      // maybe it was very fast and completed before being put into a map
      myTasks.remove(arg, future);
    }
    return ProgressIndicatorUtils.awaitWithCheckCanceled(future, indicator);
  }
}
