// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.vfs.newvfs;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.*;
import consulo.application.event.ApplicationListener;
import consulo.application.internal.TransactionGuardEx;
import consulo.application.progress.ProgressIndicator;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.util.concurrent.PooledThreadExecutor;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.openapi.diagnostic.FrequentEventDetector;
import consulo.logging.Logger;
import consulo.ui.ModalityState;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.RefreshQueue;
import consulo.virtualFileSystem.RefreshSession;
import consulo.virtualFileSystem.VfsBundle;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.AsyncFileListener;
import consulo.virtualFileSystem.event.VFileCreateEvent;
import consulo.virtualFileSystem.event.VFileEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

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
@ServiceImpl(profiles = ComponentProfiles.AWT)
public class RefreshQueueImpl extends RefreshQueue implements Disposable {
  private static final Logger LOG = Logger.getInstance(RefreshQueueImpl.class);

  private final Executor myQueue = AppExecutorUtil.createBoundedApplicationPoolExecutor("RefreshQueue Pool",
                                                                                        PooledThreadExecutor.getInstance(), 1, this);
  private final Executor myEventProcessingQueue = AppExecutorUtil.createBoundedApplicationPoolExecutor("Async Refresh Event Processing",
                                                                                                       PooledThreadExecutor.getInstance(),
                                                                                                       1,
                                                                                                       this);

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
      queueSession(session);
    }
    else {
      if (myApplication.isWriteThread()) {
        ((TransactionGuardEx)TransactionGuard.getInstance()).assertWriteActionAllowed();
        doScan(session);
        session.fireEventsAsync(session.getEvents(), null);
      }
      else {
        if (myApplication.isReadAccessAllowed()) {
          LOG.error("Do not call synchronous refresh under read lock (except from EDT) - " + "this will cause a deadlock if there are any events to fire.");
          return;
        }
        queueSession(session);
        session.waitFor();
      }
    }
  }

  private void queueSession(@Nonnull RefreshSessionImpl session) {
    myQueue.execute(() -> {
      startRefreshActivity();
      try {
        HeavyProcessLatch.INSTANCE.performOperation(HeavyProcessLatch.Type.Syncing,
                                                    "Doing file refresh. " + session,
                                                    () -> doScan(session));
      }
      finally {
        finishRefreshActivity();

        scheduleAsynchronousPreprocessing(session);
      }
    });
    myEventCounter.eventHappened(session);
  }

  protected void scheduleAsynchronousPreprocessing(@Nonnull RefreshSessionImpl session) {
    try {
      myEventProcessingQueue.execute(() -> {
        startRefreshActivity();
        try {
          HeavyProcessLatch.INSTANCE.performOperation(HeavyProcessLatch.Type.Syncing,
                                                      "Processing VFS events. " + session,
                                                      () -> processAndFireEvents(session));
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

  private void processAndFireEvents(@Nonnull RefreshSessionImpl session) {
    // TODO something?
    tryProcessingEvents(session);
  }

  protected void tryProcessingEvents(@Nonnull RefreshSessionImpl session) {
    List<? extends VFileEvent> events = ContainerUtil.filter(session.getEvents(), e -> {
      VirtualFile file = e instanceof VFileCreateEvent ? ((VFileCreateEvent)e).getParent() : e.getFile();
      return file == null || file.isValid();
    });

    List<AsyncFileListener.ChangeApplier> appliers = AsyncEventSupport.runAsyncListeners(events);

    long stamp = myWriteActionCounter.get();
    if (stamp == myWriteActionCounter.get()) {
      session.fireEventsAsync(events, appliers);
    }
    else {
      scheduleAsynchronousPreprocessing(session);
    }
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