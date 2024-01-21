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
package consulo.application.impl.internal.concurent.locking;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.event.ApplicationListener;
import consulo.proxy.EventDispatcher;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.function.ThrowableSupplier;
import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;

/**
 * @author VISTALL
 * @since 2023-11-17
 */
public class NewDataLock extends BaseDataLock {
  private final NewLock myLock = new NewStampedLock();

  private final ExecutorService myInternalWriteExecutor = Executors.newSingleThreadExecutor(r -> {
    Thread thread = new Thread(r, "Write Thread");
    thread.setPriority(Thread.MAX_PRIORITY);
    return thread;
  });

  private final Executor myWriteExecutor = command -> {
    myInternalWriteExecutor.execute(() -> {
      runWriteAction(() -> {
        command.run();
        return null;
      }, true);
    });
  };

  private final EventDispatcher<ApplicationListener> myDispatcher;

  private final Executor myReadExecutor = command -> {
    runReadAction(() -> {
      command.run();
      return null;
    });
  };

  public NewDataLock(EventDispatcher<ApplicationListener> dispatcher) {
    myDispatcher = dispatcher;
  }

  @Nonnull
  @Override
  public <V, T extends Throwable> V readSync(@Nonnull ThrowableSupplier<V, T> supplier) throws T {
    if (isReadAccessAllowed()) {
      return supplier.get();
    }

    Lock readLock = myLock.asReadLock();
    try {
      readLock.lock();
      return supplier.get();
    }
    finally {
      readLock.unlock();
    }
  }

  @Nonnull
  @Override
  public <V> CompletableFuture<V> readAsync(@Nonnull ThrowableSupplier<V, Throwable> supplier) {
    if (isReadAccessAllowed()) {
      try {
        return CompletableFuture.completedFuture(supplier.get());
      }
      catch (Throwable t) {
        return CompletableFuture.failedFuture(t);
      }
    }

    return super.readAsync(supplier);
  }

  @Nonnull
  @Override
  public <V> CompletableFuture<V> writeAsync(@Nonnull ThrowableSupplier<V, Throwable> supplier) {
    // is inside current write thread
    if (isWriteAccessAllowed()) {
      try {
        return CompletableFuture.completedFuture(runWriteActionUnsafe(supplier));
      }
      catch (Throwable t) {
        return CompletableFuture.failedFuture(t);
      }
    }

    return super.writeAsync(supplier);
  }

  @Nonnull
  @Override
  public Executor writeExecutor() {
    return myWriteExecutor;
  }

  @Nonnull
  @Override
  public Executor readExecutor() {
    return myReadExecutor;
  }

  @Override
  public boolean isReadAccessAllowed() {
    return myLock.isReadLockedByCurrentThread() || myLock.isWriteLockedByCurrentThread();
  }

  @Override
  public boolean isWriteAccessAllowed() {
    return myLock.isWriteLockedByCurrentThread();
  }

  @Override
  public boolean isWriteLockedByAnyThread() {
    return myLock.isWriteLocked();
  }

  @Override
  public boolean isWriteActionInProgress() {
    return myLock.isWriteLocked();
  }

  @Override
  public boolean tryReadSync(Runnable runnable) {
    if (isReadAccessAllowed()) {
      runnable.run();
      return true;
    }

    Lock readLock = myLock.asReadLock();
    if (readLock.tryLock()) {
      try {
        runnable.run();
      }
      finally {
        readLock.unlock();
      }
      return true;
    }
    return false;
  }

  @Override
  public <T, E extends Throwable> T runWriteActionUnsafe(@Nonnull ThrowableSupplier<T, E> computation) throws E {
    return runWriteAction(computation, false);
  }

  @RequiredReadAction
  @Override
  public boolean hasWriteAction(@Nonnull Class<?> actionClass) {
    return false;
  }

  private <V> V runReadAction(ThrowableSupplier<V, Throwable> supplier) {
    Lock readLock = myLock.asReadLock();
    try {
      readLock.lock();
      return supplier.get();
    }
    catch (Throwable cause) {
      ExceptionUtil.rethrow(cause);
      return null;
    }
    finally {
      readLock.unlock();
    }
  }

  private <T, E extends Throwable> T runWriteAction(@Nonnull ThrowableSupplier<T, E> computation, boolean lock) throws E {
    myDispatcher.getMulticaster().beforeWriteActionStart(computation);

    Lock writeLock = myLock.asWriteLock();
    try {
      if (lock) {
        writeLock.lock();
      }

      myDispatcher.getMulticaster().writeActionStarted(computation);
      T value = computation.get();
      myDispatcher.getMulticaster().writeActionFinished(computation);
      return value;
    }
    catch (Throwable cause) {
      ExceptionUtil.rethrow(cause);
      return null;
    }
    finally {
      if (lock) {
        writeLock.unlock();
      }

      myDispatcher.getMulticaster().afterWriteActionFinished(computation);
    }
  }
}
