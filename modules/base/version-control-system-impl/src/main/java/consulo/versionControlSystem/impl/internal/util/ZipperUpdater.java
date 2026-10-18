// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.impl.internal.util;

import consulo.application.internal.BackgroundTaskUtil;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Runs the last queued task on a pooled thread after a delay; requests queued while one is pending are merged into it.
 */
public class ZipperUpdater {
  private final Object myLock = new Object();
  private final int myDelay;
  private final Disposable myParentDisposable;
  private boolean myRaised;
  private boolean myIsEmpty = true;
  private boolean myDisposed;
  private @Nullable Future<?> myFuture;

  public ZipperUpdater(int delay, Disposable parentDisposable) {
    myDelay = delay;
    myParentDisposable = parentDisposable;
    Disposer.register(parentDisposable, () -> {
      synchronized (myLock) {
        myDisposed = true;
      }
      stop();
    });
  }

  public void queue(Runnable runnable) {
    queue(runnable, false);
  }

  public void queue(Runnable runnable, boolean urgent) {
    synchronized (myLock) {
      if (myDisposed) {
        return;
      }
      boolean wasRaised = myRaised;
      myRaised = true;
      myIsEmpty = false;
      if (wasRaised) {
        return;
      }

      myFuture = AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> {
        synchronized (myLock) {
          if (!myRaised) {
            return;
          }
          myRaised = false;
        }
        BackgroundTaskUtil.runUnderDisposeAwareIndicator(myParentDisposable, runnable);
        synchronized (myLock) {
          myIsEmpty = !myRaised;
        }
      }, urgent ? 0 : myDelay, TimeUnit.MILLISECONDS);
    }
  }

  public boolean isEmpty() {
    synchronized (myLock) {
      return myIsEmpty;
    }
  }

  public void stop() {
    synchronized (myLock) {
      myRaised = false;
      myIsEmpty = true;
      Future<?> future = myFuture;
      if (future != null) {
        future.cancel(false);
      }
    }
  }
}
