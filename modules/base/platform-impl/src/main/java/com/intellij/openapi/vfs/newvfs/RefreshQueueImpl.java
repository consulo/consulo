// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.vfs.newvfs;

import com.intellij.concurrency.SensitiveProgressWrapper;
import com.intellij.openapi.application.*;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.diagnostic.FrequentEventDetector;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.VfsBundle;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.storage.HeavyProcessLatch;
import consulo.application.TransactionGuardEx;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.ide.PooledThreadExecutor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author max
 */
@Singleton
public class RefreshQueueImpl extends RefreshQueue implements Disposable {
  private static final Logger LOG = Logger.getInstance(RefreshQueueImpl.class);

  private final Executor myQueue = AppExecutorUtil.createBoundedApplicationPoolExecutor("RefreshQueue Pool", PooledThreadExecutor.INSTANCE, 1, this);
  private final Executor myEventProcessingQueue = AppExecutorUtil.createBoundedApplicationPoolExecutor("Async Refresh Event Processing", PooledThreadExecutor.INSTANCE, 1, this);

  private final ProgressIndicator myRefreshIndicator = RefreshProgress.create(VfsBundle.message("file.synchronize.progress"));

  private int myBusyThreads;
  private final Map<Long, RefreshSession> mySessions = new HashMap<>();
  private final FrequentEventDetector myEventCounter = new FrequentEventDetector(100, 100, FrequentEventDetector.Level.WARN);
  private final AtomicLong myWriteActionCounter = new AtomicLong();

  @Nonnull
  private final Application myApplication;

  @Inject
  public RefreshQueueImpl(@Nonnull Application application) {
    myApplication = application;
    application.addApplicationListener(new ApplicationListener() {
      @Override
      public void writeActionStarted(@Nonnull Object action) {
        myWriteActionCounter.incrementAndGet();
      }
    }, this);
  }

  public void execute(@Nonnull RefreshSessionImpl session) {
    if (session.isAsynchronous()) {
      queueSession(session, session.getTransaction());
    }
    else {
      if (myApplication.isWriteThread()) {
        ((TransactionGuardEx)TransactionGuard.getInstance()).assertWriteActionAllowed();
        doScan(session);
        session.fireEvents(session.getEvents(), null);
      }
      else {
        if (((ApplicationEx)myApplication).holdsReadLock()) {
          LOG.error("Do not call synchronous refresh under read lock (except from EDT) - " + "this will cause a deadlock if there are any events to fire.");
          return;
        }
        queueSession(session, TransactionGuard.getInstance().getContextTransaction());
        session.waitFor();
      }
    }
  }

  private void queueSession(@Nonnull RefreshSessionImpl session, @Nullable TransactionId transaction) {
    myQueue.execute(() -> {
      startRefreshActivity();
      try (AccessToken ignored = HeavyProcessLatch.INSTANCE.processStarted("Doing file refresh. " + session)) {
        doScan(session);
      }
      finally {
        finishRefreshActivity();
        if (Registry.is("vfs.async.event.processing")) {
          scheduleAsynchronousPreprocessing(session, transaction);
        }
        else {
          TransactionGuard.getInstance().submitTransaction(myApplication, transaction, () -> session.fireEvents(session.getEvents(), null));
        }
      }
    });
    myEventCounter.eventHappened(session);
  }

  protected void scheduleAsynchronousPreprocessing(@Nonnull RefreshSessionImpl session, @Nullable TransactionId transaction) {
    try {
      myEventProcessingQueue.execute(() -> {
        startRefreshActivity();
        try (AccessToken ignored = HeavyProcessLatch.INSTANCE.processStarted("Processing VFS events. " + session)) {
          processAndFireEvents(session, transaction);
        }
        finally {
          finishRefreshActivity();
        }
      });
    }
    catch (RejectedExecutionException e) {
      LOG.debug(e);
    }
  }

  private synchronized void startRefreshActivity() {
    if (myBusyThreads++ == 0) {
      myRefreshIndicator.start();
    }
  }

  private synchronized void finishRefreshActivity() {
    if (--myBusyThreads == 0) {
      myRefreshIndicator.stop();
    }
  }

  private void processAndFireEvents(@Nonnull RefreshSessionImpl session, @Nullable TransactionId transaction) {
    while (true) {
      ProgressIndicator progress = new SensitiveProgressWrapper(myRefreshIndicator);
      boolean success = ProgressIndicatorUtils.runWithWriteActionPriority(() -> tryProcessingEvents(session, transaction), progress);
      if (success) {
        break;
      }

      ProgressIndicatorUtils.yieldToPendingWriteActions();
    }
  }

  protected void tryProcessingEvents(@Nonnull RefreshSessionImpl session, @Nullable TransactionId transaction) {
    List<? extends VFileEvent> events = ContainerUtil.filter(session.getEvents(), e -> {
      VirtualFile file = e instanceof VFileCreateEvent ? ((VFileCreateEvent)e).getParent() : e.getFile();
      return file == null || file.isValid();
    });

    List<AsyncFileListener.ChangeApplier> appliers = AsyncEventSupport.runAsyncListeners(events);

    long stamp = myWriteActionCounter.get();
    TransactionGuard.getInstance().submitTransaction(ApplicationManager.getApplication(), transaction, () -> {
      if (stamp == myWriteActionCounter.get()) {
        session.fireEvents(events, appliers);
      }
      else {
        scheduleAsynchronousPreprocessing(session, transaction);
      }
    });
  }

  private void doScan(@Nonnull RefreshSessionImpl session) {
    try {
      updateSessionMap(session, true);
      session.scan();
    }
    finally {
      updateSessionMap(session, false);
    }
  }

  private void updateSessionMap(@Nonnull RefreshSession session, boolean add) {
    long id = session.getId();
    if (id != 0) {
      synchronized (mySessions) {
        if (add) {
          mySessions.put(id, session);
        }
        else {
          mySessions.remove(id);
        }
      }
    }
  }

  @Override
  public void cancelSession(long id) {
    RefreshSession session;
    synchronized (mySessions) {
      session = mySessions.get(id);
    }
    if (session instanceof RefreshSessionImpl) {
      ((RefreshSessionImpl)session).cancel();
    }
  }

  @Nonnull
  @Override
  public RefreshSession createSession(boolean async, boolean recursively, @Nullable Runnable finishRunnable, @Nonnull ModalityState state) {
    return new RefreshSessionImpl(async, recursively, finishRunnable, state);
  }

  @Override
  public void processSingleEvent(@Nonnull VFileEvent event) {
    new RefreshSessionImpl(Collections.singletonList(event)).launch();
  }

  @Override
  public boolean isRefreshInProgress() {
    synchronized (mySessions) {
      return !mySessions.isEmpty();
    }
  }

  @Override
  public void dispose() {
    synchronized (mySessions) {
      mySessions.values().forEach(session -> ((RefreshSessionImpl)session).cancel());
    }
  }
}