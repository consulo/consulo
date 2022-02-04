// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.application.impl;

import com.intellij.codeWithMe.ClientId;
import com.intellij.diagnostic.EventWatcher;
import consulo.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import consulo.application.progress.ProcessCanceledException;
import com.intellij.util.ExceptionUtil;
import consulo.util.concurrent.ActionCallback;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

final class FlushQueue {
  private static final Logger LOG = Logger.getInstance(LaterInvocator.class);
  private static final boolean DEBUG = LOG.isDebugEnabled();
  private final Object LOCK = new Object();

  private final List<RunnableInfo> mySkippedItems = new ArrayList<>(); //protected by LOCK

  private final ArrayDeque<RunnableInfo> myQueue = new ArrayDeque<>(); //protected by LOCK
  private final
  @Nonnull
  Consumer<? super Runnable> myRunnableExecutor;

  private volatile boolean myMayHaveItems;

  private RunnableInfo myLastInfo;

  FlushQueue(@Nonnull Consumer<? super Runnable> executor) {
    myRunnableExecutor = executor;
  }

  public void scheduleFlush() {
    myRunnableExecutor.accept(new FlushNow());
  }

  public void flushNow() {
    LaterInvocator.FLUSHER_SCHEDULED.set(false);
    myMayHaveItems = false;

    long startTime = System.currentTimeMillis();
    while (true) {
      if (!runNextEvent()) {
        break;
      }
      if (System.currentTimeMillis() - startTime > 5) {
        myMayHaveItems = true;
        break;
      }
    }
    LaterInvocator.requestFlush();
  }

  public void push(@Nonnull RunnableInfo runnableInfo) {
    synchronized (LOCK) {
      myQueue.add(runnableInfo);
      myMayHaveItems = true;
    }
  }

  boolean mayHaveItems() {
    return myMayHaveItems;
  }

  @TestOnly
  @Nonnull
  Collection<RunnableInfo> getQueue() {
    synchronized (LOCK) {
      // used by leak hunter as root, so we must not copy it here to another list
      // to avoid walking over obsolete queue
      return Collections.unmodifiableCollection(myQueue);
    }
  }

  // Extracted to have a capture point
  private static void doRun(@Nonnull RunnableInfo info) {
    if (ClientId.Companion.getPropagateAcrossThreads()) {
      ClientId.withClientId(info.clientId, info.runnable);
    }
    else {
      info.runnable.run();
    }
  }

  @Override
  public String toString() {
    return "LaterInvocator.FlushQueue" + (myLastInfo == null ? "" : " lastInfo=" + myLastInfo);
  }

  @Nullable
  RunnableInfo getNextEvent(boolean remove) {
    synchronized (LOCK) {
      ModalityState currentModality = LaterInvocator.getCurrentModalityState();

      while (!myQueue.isEmpty()) {
        RunnableInfo info = myQueue.getFirst();

        if (info.expired.getAsBoolean()) {
          myQueue.removeFirst();
          info.markDone();
          continue;
        }

        if (!currentModality.dominates(info.modalityState)) {
          if (remove) {
            myQueue.removeFirst();
          }
          return info;
        }
        mySkippedItems.add(myQueue.removeFirst());
      }

      return null;
    }
  }

  private boolean runNextEvent() {
    long startedAt = System.currentTimeMillis();
    final RunnableInfo lastInfo = getNextEvent(true);
    myLastInfo = lastInfo;

    if (lastInfo != null) {
      EventWatcher watcher = EventWatcher.getInstanceOrNull();
      Runnable runnable = lastInfo.runnable;
      if (watcher != null) {
        watcher.runnableStarted(runnable, startedAt);
      }

      try {
        doRun(lastInfo);
        lastInfo.markDone();
      }
      catch (ProcessCanceledException ignored) {

      }
      catch (Throwable t) {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
          ExceptionUtil.rethrow(t);
        }
        LOG.error(t);
      }
      finally {
        if (!DEBUG) myLastInfo = null;
        if (watcher != null) {
          watcher.runnableFinished(runnable, System.currentTimeMillis());
        }
      }
    }
    return lastInfo != null;
  }

  void reincludeSkippedItems() {
    synchronized (LOCK) {
      for (int i = mySkippedItems.size() - 1; i >= 0; i--) {
        RunnableInfo item = mySkippedItems.get(i);
        myQueue.addFirst(item);
        myMayHaveItems = true;
      }
      mySkippedItems.clear();
    }
  }

  void purgeExpiredItems() {
    synchronized (LOCK) {
      reincludeSkippedItems();

      List<RunnableInfo> alive = new ArrayList<>(myQueue.size());
      for (RunnableInfo info : myQueue) {
        if (info.expired.getAsBoolean()) {
          info.markDone();
        }
        else {
          alive.add(info);
        }
      }
      if (alive.size() < myQueue.size()) {
        myQueue.clear();
        myQueue.addAll(alive);
      }
    }
  }

  final class FlushNow implements Runnable {
    @Override
    public void run() {
      flushNow();
    }
  }

  final static class RunnableInfo {
    @Nonnull
    private final Runnable runnable;
    @Nonnull
    private final ModalityState modalityState;
    @Nonnull
    private final BooleanSupplier expired;
    @Nullable
    private final ActionCallback callback;
    @Nullable
    private final ClientId clientId;

    //@Async.Schedule
    RunnableInfo(@Nonnull Runnable runnable, @Nonnull ModalityState modalityState, @Nonnull BooleanSupplier expired, @Nullable ActionCallback callback) {
      this.runnable = runnable;
      this.modalityState = modalityState;
      this.expired = expired;
      this.callback = callback;
      this.clientId = ClientId.getCurrent();
    }

    void markDone() {
      if (callback != null) callback.setDone();
    }

    @Override
    @NonNls
    public String toString() {
      return "[runnable: " + runnable + "; state=" + modalityState + (expired.getAsBoolean() ? "; expired" : "") + "] ";
    }
  }
}
