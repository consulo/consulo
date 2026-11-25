// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.impl.internal.refresh;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.HeavyProcessLatch;
import consulo.application.ReadAction;
import consulo.application.concurrent.coroutine.ReadWriteLock;
import consulo.application.internal.ApplicationEx;
import consulo.application.internal.FrequentEventDetector;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.util.concurrent.PooledThreadExecutor;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.ModalityState;
import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.lang.Pair;
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

  private final Object myEventProcessingLock = new Object();
  private CompletableFuture<?> myLastEventProcessing = CompletableFuture.completedFuture(null);

  @Inject
  public RefreshQueueImpl(Application application) {
    myApplication = application;
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
          LocalizeValue.localizeTODO("Doing file refresh. " + session),
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
    return Coroutine.first(
        ReadWriteLock.<Void, @Nullable Pair<List<? extends VFileEvent>, List<AsyncFileListener.ChangeApplier>>, Boolean>nonBlocking(
          (input, continuation) -> {
            Pair<List<? extends VFileEvent>, List<AsyncFileListener.ChangeApplier>> prepared = collectChangeAppliers(session);
            if (prepared == null) {
              session.terminate();
              continuation.finishEarly(null);
            }
            return prepared;
          },
          (pair, continuation) -> {
            if (pair != null) {
              session.fireEventsInBackgroundWriteAction(pair.getFirst(), pair.getSecond());
            }
            return Boolean.TRUE;
          }
        ))
      .runAsync(CoroutineScope.of(myApplication.coroutineContext()), null)
      .toFuture()
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
  @RequiredReadAction
  private static @Nullable Pair<List<? extends VFileEvent>, List<AsyncFileListener.ChangeApplier>> collectChangeAppliers(
    RefreshSessionImpl session
  ) {
    List<? extends VFileEvent> events = ContainerUtil.filter(session.getEvents(), e -> {
      VirtualFile file = e instanceof VFileCreateEvent ce ? ce.getParent() : e.getFile();
      return file == null || file.isValid();
    });
    if (events.isEmpty() && !session.hasFinishRunnable()) {
      return null;
    }
    List<AsyncFileListener.ChangeApplier> appliers = AsyncEventSupport.runAsyncListeners(events);
    return Pair.<List<? extends VFileEvent>, List<AsyncFileListener.ChangeApplier>>create(events, appliers);
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