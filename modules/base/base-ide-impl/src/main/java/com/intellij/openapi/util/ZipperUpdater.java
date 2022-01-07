// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.util;

import consulo.disposer.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.util.BackgroundTaskUtil;
import com.intellij.util.Alarm;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.TestOnly;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ZipperUpdater {
  private final Alarm myAlarm;
  private boolean myRaised;
  private final Object myLock = new Object();
  private final int myDelay;
  private final Alarm.ThreadToUse myThreadToUse;
  private boolean myIsEmpty;

  public ZipperUpdater(final int delay, @Nonnull Disposable parentDisposable) {
    myDelay = delay;
    myIsEmpty = true;
    myThreadToUse = Alarm.ThreadToUse.POOLED_THREAD;
    myAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, parentDisposable);
  }

  public ZipperUpdater(final int delay, final Alarm.ThreadToUse threadToUse, @Nonnull Disposable parentDisposable) {
    myDelay = delay;
    myThreadToUse = threadToUse;
    myIsEmpty = true;
    myAlarm = new Alarm(threadToUse, parentDisposable);
  }

  public void queue(@Nonnull final Runnable runnable) {
    queue(runnable, false);
  }

  private void queue(@Nonnull final Runnable runnable, final boolean urgent) {
    queue(runnable, urgent, false);
  }

  public void queue(@Nonnull final Runnable runnable, final boolean urgent, final boolean anyModality) {
    synchronized (myLock) {
      if (myAlarm.isDisposed()) return;
      final boolean wasRaised = myRaised;
      myRaised = true;
      myIsEmpty = false;
      if (!wasRaised) {
        final Runnable request = new Runnable() {
          @Override
          public void run() {
            synchronized (myLock) {
              if (!myRaised) return;
              myRaised = false;
            }
            BackgroundTaskUtil.runUnderDisposeAwareIndicator(myAlarm, runnable);
            synchronized (myLock) {
              myIsEmpty = !myRaised;
            }
          }

          @Override
          public String toString() {
            return runnable.toString();
          }
        };
        if (Alarm.ThreadToUse.SWING_THREAD.equals(myThreadToUse)) {
          if (anyModality) {
            myAlarm.addRequest(request, urgent ? 0 : myDelay, ModalityState.any());
          }
          else if (!ApplicationManager.getApplication().isDispatchThread()) {
            myAlarm.addRequest(request, urgent ? 0 : myDelay, ModalityState.NON_MODAL);
          }
          else {
            myAlarm.addRequest(request, urgent ? 0 : myDelay);
          }
        }
        else {
          myAlarm.addRequest(request, urgent ? 0 : myDelay);
        }
      }
    }
  }

  public boolean isEmpty() {
    synchronized (myLock) {
      return myIsEmpty;
    }
  }

  public void stop() {
    myAlarm.cancelAllRequests();
  }

  @TestOnly
  public void waitForAllExecuted(long timeout, @Nonnull TimeUnit unit) {
    try {
      myAlarm.waitForAllExecuted(timeout, unit);
    }
    catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }
}
