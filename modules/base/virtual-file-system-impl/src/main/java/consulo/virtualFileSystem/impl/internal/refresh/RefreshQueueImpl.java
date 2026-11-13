// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.impl.internal.refresh;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.HeavyProcessLatch;
import consulo.application.ReadAction;
import consulo.application.concurrent.coroutine.WriteLock;
import consulo.application.event.ApplicationListener;
import consulo.application.internal.ProgressIndicatorUtils;
import consulo.application.internal.ApplicationEx;
import consulo.application.internal.FrequentEventDetector;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.util.concurrent.PooledThreadExecutor;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.ui.ModalityState;
import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.lang.Pair;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.RefreshQueue;
import consulo.virtualFileSystem.RefreshSession;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.internal.VirtualFileManagerEx;
import consulo.virtualFileSystem.event.AsyncFileListener;
import consulo.virtualFileSystem.event.VFileCreateEvent;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.impl.internal.AsyncEventSupport;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
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

  private final Application myApplication;

  private final AtomicLong myWriteActionCounter = new AtomicLong();
  private final Object myEventProcessingLock = new Object();
  private CompletableFuture<?> myLastEventProcessing = CompletableFuture.completedFuture(null);

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
      if (myApplication.isDispatchThread()) {
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
      try {
        HeavyProcessLatch.INSTANCE.performOperation(
          HeavyProcessLatch.Type.Syncing,
          "Doing file refresh. " + session,
          () -> doScan(session)
        );
      }
      finally {
        scheduleAsynchronousPreprocessing(session, modality);
      }
    });
    myEventCounter.eventHappened(session);
  }

  protected void scheduleAsynchronousPreprocessing(RefreshSessionImpl session, ModalityState modality) {
    if (modality == ModalityState.nonModal()) {
      fireEventsInBackgroundWriteAction(session);
    }
    else {
      fireEventsOnUiThread(session, modality);
    }
  }

  /**
   * The sessions' events are processed one session after another, in the order the scans finished.
   */
  private void fireEventsInBackgroundWriteAction(RefreshSessionImpl session) {
    synchronized (myEventProcessingLock) {
      myLastEventProcessing = myLastEventProcessing
        .handle((result, throwable) -> null)
        .thenCompose(o -> processEventsInBackgroundWriteAction(session));
    }
  }

  /**
   * The async listeners run in a read action which gives way to a pending write action and restarts after it; the events
   * are applied only if no other write action happened since the listeners ran, otherwise the listeners run again.
   */
  private CompletableFuture<?> processEventsInBackgroundWriteAction(RefreshSessionImpl session) {
    AtomicLong stamp = new AtomicLong();
    CompletableFuture<Boolean> attempt = Coroutine.<Void, @Nullable Pair<List<? extends VFileEvent>, List<AsyncFileListener.ChangeApplier>>>first(
          CodeExecution.<Void, @Nullable Pair<List<? extends VFileEvent>, List<AsyncFileListener.ChangeApplier>>>apply((input, continuation) -> {
            Pair<List<? extends VFileEvent>, List<AsyncFileListener.ChangeApplier>> prepared = collectChangeAppliers(session, stamp);
            if (prepared == null) {
              session.terminate();
              continuation.finishEarly(null);
            }
            return prepared;
          }))
        .then(WriteLock.<@Nullable Pair<List<? extends VFileEvent>, List<AsyncFileListener.ChangeApplier>>, Boolean>applyIf(
          pair -> myWriteActionCounter.get() == stamp.get(),
          pair -> {
            if (pair != null) {
              session.fireEventsInBackgroundWriteAction(pair.getFirst(), pair.getSecond());
            }
            return Boolean.TRUE;
          },
          pair -> Boolean.FALSE
        ))
      .runAsync(CoroutineScope.of(myApplication.coroutineContext()), null)
      .toFuture();

    return attempt
      .thenCompose(done -> Boolean.FALSE.equals(done) && !myApplication.isDisposed()
        ? processEventsInBackgroundWriteAction(session)
        : CompletableFuture.completedFuture(null))
      .whenComplete((result, throwable) -> {
        if (throwable != null) {
          session.terminate();
          LOG.error(throwable);
        }
      });
  }

  /**
   * @return the events to fire and their change appliers, or {@code null} if there is nothing to fire
   */
  private @Nullable Pair<List<? extends VFileEvent>, List<AsyncFileListener.ChangeApplier>> collectChangeAppliers(
    RefreshSessionImpl session,
    AtomicLong stamp
  ) {
    while (true) {
      SimpleReference<Pair<List<? extends VFileEvent>, List<AsyncFileListener.ChangeApplier>>> result = new SimpleReference<>();
      AtomicBoolean nothingToFire = new AtomicBoolean();
      boolean finished = ProgressIndicatorUtils.runInReadActionWithWriteActionPriority(() -> {
        stamp.set(myWriteActionCounter.get());
        List<? extends VFileEvent> events = ContainerUtil.filter(session.getEvents(), e -> {
          VirtualFile file = e instanceof VFileCreateEvent ce ? ce.getParent() : e.getFile();
          return file == null || file.isValid();
        });
        if (events.isEmpty() && !session.hasFinishRunnable()) {
          nothingToFire.set(true);
          return;
        }
        List<AsyncFileListener.ChangeApplier> appliers = AsyncEventSupport.runAsyncListeners(events);
        result.set(Pair.<List<? extends VFileEvent>, List<AsyncFileListener.ChangeApplier>>create(events, appliers));
      });
      if (finished) {
        return nothingToFire.get() ? null : result.get();
      }
      ProgressIndicatorUtils.yieldToPendingWriteActions();
    }
  }


  private void fireEventsOnUiThread(RefreshSessionImpl session, ModalityState modality) {
    try {
      ReadAction.nonBlocking(() -> {
          List<? extends VFileEvent> events = ContainerUtil.filter(session.getEvents(), e -> {
            VirtualFile file = e instanceof VFileCreateEvent ce ? ce.getParent() : e.getFile();
            return file == null || file.isValid();
          });
          List<AsyncFileListener.ChangeApplier> appliers = AsyncEventSupport.runAsyncListeners(events);
          return Pair.<List<? extends VFileEvent>, List<AsyncFileListener.ChangeApplier>>create(events, appliers);
        })
        .expireWith(this)
        .finishOnUiThread(app -> modality, pair -> session.fireEvents(pair.getFirst(), pair.getSecond()))
        .submit(myEventProcessingQueue);
    }
    catch (RejectedExecutionException e) {
      LOG.debug(e);
    }
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
  public void refreshPaths(boolean async, boolean recursive, @Nullable Runnable finishRunnable, Collection<Path> paths) {
    VirtualFileManagerEx manager = (VirtualFileManagerEx)VirtualFileManager.getInstance();

    boolean fireCommonRefreshSession = myApplication.isDispatchThread() || myApplication.isWriteAccessAllowed();
    if (fireCommonRefreshSession) {
      manager.fireBeforeRefreshStart(false);
    }

    try {
      List<VirtualFile> filesToRefresh = new ArrayList<>();
      for (Path path : paths) {
        VirtualFile virtualFile = manager.refreshAndFindFileByNioPath(path);
        if (virtualFile != null) {
          filesToRefresh.add(virtualFile);
        }
      }

      refresh(async, recursive, finishRunnable, filesToRefresh);
    }
    finally {
      if (fireCommonRefreshSession) {
        manager.fireAfterRefreshFinish(false);
      }
    }
  }

  @Override
  public void processSingleEvent(VFileEvent event) {
    new RefreshSessionImpl(Collections.singletonList(event)).launch();
  }

  @Override
  public void processEvents(List<? extends VFileEvent> events) {
    if (events.isEmpty()) {
      return;
    }
    new RefreshSessionImpl(events).launch();
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