/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.application;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.concurrent.DataLock;
import consulo.util.lang.function.ThrowableRunnable;
import consulo.util.lang.function.ThrowableSupplier;
import org.jetbrains.annotations.Contract;

import jakarta.annotation.Nonnull;
import java.util.concurrent.Callable;

@Nonnull
@Deprecated
public final class ReadAction<T>  {
  @Deprecated
  public static <E extends Throwable> void run(@RequiredReadAction @Nonnull ThrowableRunnable<E> action) throws E {
    DataLock locking = Application.get().getLock();
    locking.readSync(action);
  }

  @Deprecated
  public static <T, E extends Throwable> T compute(@Nonnull ThrowableSupplier<T, E> action) throws E {
    DataLock locking = Application.get().getLock();
    return locking.readSync(action);
  }

  /**
   * Create an {@link NonBlockingReadAction} builder to run the given Runnable in non-blocking read action on a background thread.
   */
  @Nonnull
  @Contract(pure = true)
  public static NonBlockingReadAction<Void> nonBlocking(@RequiredReadAction  @Nonnull Runnable task) {
    return nonBlocking(() -> {
      task.run();
      return null;
    });
  }

  /**
   * Create an {@link NonBlockingReadAction} builder to run the given Callable in a non-blocking read action on a background thread.
   */
  @Nonnull
  @Contract(pure = true)
  public static <T> NonBlockingReadAction<T> nonBlocking(@Nonnull Callable<T> task) {
    return AsyncExecutionService.getService().buildNonBlockingReadAction(task);
  }

}
