/*
 * Copyright 2013-2023 consulo.io
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
package consulo.application.concurrent;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.function.ThrowableRunnable;
import consulo.util.lang.function.ThrowableSupplier;
import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * @author VISTALL
 * @since 2023-11-14
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface DataLock {
  @Nonnull
  static DataLock getInstance() {
    return Application.get().getLock();
  }

  default <T extends Throwable> void readSync(@Nonnull ThrowableRunnable<T> supplier) throws T {
    if (isReadAccessAllowed()) {
      supplier.run();
      return;
    }
    
    readSync(() -> {
      supplier.run();
      return null;
    });
  }

  @Nonnull
  <V, T extends Throwable> V readSync(@RequiredReadAction @Nonnull ThrowableSupplier<V, T> supplier) throws T;

  @Nonnull
  default <V> CompletableFuture<V> readAsync(@RequiredReadAction @Nonnull ThrowableSupplier<V, Throwable> supplier) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return supplier.get();
      }
      catch (Throwable throwable) {
        ExceptionUtil.rethrow(throwable);
        return null;
      }
    }, readExecutor());
  }

  @Nonnull
  default CompletableFuture<?> writeAsync(@RequiredWriteAction @Nonnull Runnable runnable) {
    return writeAsync(() -> {
      runnable.run();
      return null;
    });
  }

  @Nonnull
  default <V> CompletableFuture<V> writeAsync(@RequiredWriteAction @Nonnull ThrowableSupplier<V, Throwable> supplier) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return supplier.get();
      }
      catch (Throwable throwable) {
        ExceptionUtil.rethrow(throwable);
        return null;
      }
    }, writeExecutor());
  }

  @Nonnull
  Executor writeExecutor();

  @Nonnull
  Executor readExecutor();

  /**
   * Checks if the read access is currently allowed.
   *
   * @return true if the read access is currently allowed, false otherwise.
   * @see #assertReadAccessAllowed()
   */
  boolean isReadAccessAllowed();

  /**
   * Checks if the write access is currently allowed.
   *
   * @return true if the write access is currently allowed, false otherwise.
   * @see #assertWriteAccessAllowed()
   */
  boolean isWriteAccessAllowed();

  /**
   * Asserts whether the read access is allowed.
   */
  @RequiredReadAction
  void assertReadAccessAllowed();

  /**
   * Asserts whether the write access is allowed.
   */
  @RequiredWriteAction
  void assertWriteAccessAllowed();

  /**
   * Returns {@code true} if there is currently executing write action of the specified class.
   *
   * @param actionClass the class of the write action to return.
   * @return {@code true} if the action is running, or {@code false} if no action of the specified class is currently executing.
   */
  @RequiredReadAction
  boolean hasWriteAction(@Nonnull Class<?> actionClass);
}
