// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application;

import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.function.ThrowableSupplier;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public final class WriteThread {
  private WriteThread() {
  }

  /**
   * Schedules {@code runnable} to execute from under IW lock on some thread later.
   *
   * @param runnable the action to run
   * @return a future representing the result of the scheduled computation
   */
  public static Future<Void> submit(Runnable runnable) {
    return submit((ThrowableSupplier<@Nullable Void, Throwable>) () -> {
      runnable.run();
      return null;
    });
  }

  /**
   * Schedules {@code computable} to execute from under IW lock on some thread later.
   *
   * @param computable the action to run
   * @param <T>        return type of scheduled computation
   * @return a future representing the result of the scheduled computation
   */
  public static <T extends @Nullable Object> Future<T> submit(ThrowableSupplier<? extends T, ?> computable) {
    CompletableFuture<T> future = new CompletableFuture<>();
    Application.get().invokeLaterOnWriteThread(() -> {
      try {
        future.complete(computable.get());
      }
      catch (Throwable t) {
        future.completeExceptionally(t);
      }
    });
    return future;
  }

  /**
   * Schedules {@code runnable} to execute from under IW lock on some thread later and blocks until
   * the execution is finished.
   *
   * @param runnable the action to run
   */
  public static void invokeAndWait(Runnable runnable) {
    try {
      submit(runnable).get();
    }
    catch (InterruptedException ignore) {
    }
    catch (ExecutionException e) {
      ExceptionUtil.rethrowUnchecked(e.getCause());
    }
  }
}
