// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.impl.internal.refresh;

import consulo.annotation.component.ServiceImpl;
import consulo.application.AppUIExecutor;
import consulo.application.Application;
import consulo.application.HeavyProcessLatch;
import consulo.application.event.ApplicationListener;
import consulo.application.internal.ApplicationEx;
import consulo.application.internal.FrequentEventDetector;
import consulo.application.internal.ProgressIndicatorUtils;
import consulo.application.internal.SensitiveProgressWrapper;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.util.concurrent.PooledThreadExecutor;
import consulo.application.util.registry.Registry;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.ui.ModalityState;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.RefreshQueue;
import consulo.virtualFileSystem.RefreshSession;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.AsyncFileListener;
import consulo.virtualFileSystem.event.VFileCreateEvent;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.impl.internal.AsyncEventSupport;
import org.jspecify.annotations.Nullable;
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
@ServiceImpl
public class RefreshQueueImpl extends RefreshQueue implements Disposable {
  private static final Logger LOG = Logger.getInstance(RefreshQueueImpl.class);

  private final Executor myQueue = AppExecutorUtil.createBoundedApplicationPoolExecutor(
    "RefreshQueue Pool",
    PooledThreadExecutor.getInstance(),
    1,
    this
  );
  private final Executor myEventProcessingQueue = AppExecutorUtil.createBoundedApplicationPoolExecutor(
    "Async Refresh Event Processing",
    PooledThreadExecutor.getInstance(),
    1,
    this
  );

  private final Map<Long, RefreshSession> mySessions = new HashMap<>();
  private final FrequentEventDetector myEventCounter = new FrequentEventDetector(100, 100, FrequentEventDetector.Level.WARN);
  private final AtomicLong myWriteActionCounter = new AtomicLong();

  
  private final Application myApplication;

  @Inject
  public RefreshQueueImpl(Application application) {
    myApplication = application;
    application.addApplicationListener(new ApplicationListener() {
      @Override
      public void writeActionStarted(Object action) {
        myWriteActionCounter.incrementAndGet();
      }
    }, this);
  }

  public void execute(RefreshSessionImpl session) {
    if (session.isAsynchronous()) {
      queueSession(session, session.getModality());
    }
    else {
      if (myApplication.isWriteThread()) {
        doScan(session);
        session.fireEvents(session.getEvents(), null);
      }
      else {
        if (((ApplicationEx)myApplication).holdsReadLock()) {
          LOG.error("Do not call synchronous refresh under read lock (except from EDT) - " + "this will cause a deadlock if there are any events to fire.");
          return;
        }
        queueSession(session, session.getModality());
        session.waitFor();
      }
    }
  }

  private void queueSession(RefreshSessionImpl session, ModalityState modality) {
    myQueue.execute(() -> {
      startRefreshActivity();
      try {
        HeavyProcessLatch.INSTANCE.performOperation(
          HeavyProcessLatch.Type.Syncing,
          "Doing file refresh. " + session,
          () -> doScan(session)
        );
      }
      finally {
        finishRefreshActivity();
        if (Registry.is("vfs.async.event.processing")) {
          scheduleAsynchronousPreprocessing(session, modality);
        }
        else {
          AppUIExecutor.onWriteThread(modality).later().submit(() -> session.fireEvents(session.getEvents(), null));
        }
      }
    });
    myEventCounter.eventHappened(session);
  }

  protected void scheduleAsynchronousPreprocessing(RefreshSessionImpl session, ModalityState modality) {
    try {
      myEventProcessingQueue.execute(() -> {
        startRefreshActivity();
        try {
          HeavyProcessLatch.INSTANCE.performOperation(
            HeavyProcessLatch.Type.Syncing,
            "Processing VFS events. " + session,
            () -> processAndFireEvents(session, modality)
          );
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
  }

  private synchronized void finishRefreshActivity() {
  }

  private void processAndFireEvents(RefreshSessionImpl session, ModalityState modality) {
    while (true) {
      ProgressIndicator progress = new SensitiveProgressWrapper(new EmptyProgressIndicator());
      boolean success = ProgressIndicatorUtils.runWithWriteActionPriority(() -> tryProcessingEvents(session, modality), progress);
      if (success) {
        break;
      }

      ProgressIndicatorUtils.yieldToPendingWriteActions();
    }
  }

  protected void tryProcessingEvents(RefreshSessionImpl session, ModalityState modality) {
    List<? extends VFileEvent> events = ContainerUtil.filter(session.getEvents(), e -> {
      VirtualFile file = e instanceof VFileCreateEvent ce ? ce.getParent() : e.getFile();
      return file == null || file.isValid();
    });

    List<AsyncFileListener.ChangeApplier> appliers = AsyncEventSupport.runAsyncListeners(events);

    long stamp = myWriteActionCounter.get();
    AppUIExecutor.onWriteThread(modality).later().submit(() -> {
      if (stamp == myWriteActionCounter.get()) {
        session.fireEvents(events, appliers);
      }
      else {
        scheduleAsynchronousPreprocessing(session, modality);
      }
    });
  }

  private void doScan(RefreshSessionImpl session) {
    try {
      updateSessionMap(session, true);
      session.scan();
    }
    finally {
      updateSessionMap(session, false);
    }
  }

  private void updateSessionMap(RefreshSession session, boolean add) {
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
    if (session instanceof RefreshSessionImpl refreshSession) {
      refreshSession.cancel();
    }
  }

  
  @Override
  public RefreshSession createSession(boolean async, boolean recursively, @Nullable Runnable finishRunnable, ModalityState state) {
    return new RefreshSessionImpl(async, recursively, finishRunnable, state);
  }

  @Override
  public void processSingleEvent(VFileEvent event) {
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