// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.impl.internal.refresh;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.WriteAction;
import consulo.application.util.Semaphore;
import consulo.logging.Logger;
import consulo.ui.ModalityState;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.*;
import consulo.virtualFileSystem.event.AsyncFileListener;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.impl.internal.AsyncEventSupport;
import consulo.virtualFileSystem.impl.internal.local.LocalFileSystemImpl;
import consulo.virtualFileSystem.internal.VirtualFileManagerEx;
import consulo.virtualFileSystem.internal.VirtualFileSystemInternalHelper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author max
 */
public class RefreshSessionImpl extends RefreshSession {
  private static final Logger LOG = Logger.getInstance(RefreshSession.class);

  private static final AtomicLong ID_COUNTER = new AtomicLong(0);

  private final long myId = ID_COUNTER.incrementAndGet();
  private final boolean myIsAsync;
  private final boolean myIsRecursive;
  private final Runnable myFinishRunnable;
  private final Throwable myStartTrace;
  private final Semaphore mySemaphore = new Semaphore();

  private List<VirtualFile> myWorkQueue = new ArrayList<>();
  private final List<VFileEvent> myEvents = new ArrayList<>();
  private volatile RefreshWorker myWorker;
  private volatile boolean myCancelled;
  private final ModalityState myModality;
  private boolean myLaunched;

  public RefreshSessionImpl(boolean async, boolean recursive, @Nullable Runnable finishRunnable, @Nonnull ModalityState modality) {
    myIsAsync = async;
    myIsRecursive = recursive;
    myFinishRunnable = finishRunnable;
    myModality = getSaneModalityState(modality);
    myStartTrace = rememberStartTrace();
  }

  public RefreshSessionImpl(@Nonnull List<? extends VFileEvent> events) {
    this(false, false, null, Application.get().getDefaultModalityState());
    myEvents.addAll(events);
  }

  private static ModalityState getSaneModalityState(ModalityState state) {
    return state != ModalityState.any() ? state : ModalityState.nonModal();
  }

  private Throwable rememberStartTrace() {
    if (ApplicationManager.getApplication().isUnitTestMode() && (myIsAsync || !ApplicationManager.getApplication().isDispatchThread())) {
      return new Throwable();
    }
    return null;
  }

  @Override
  public long getId() {
    return myId;
  }

  @Override
  public void addAllFiles(@Nonnull Collection<? extends VirtualFile> files) {
    for (VirtualFile file : files) {
      if (file == null) {
        LOG.error("null passed among " + files);
      }
      else {
        addFile(file);
      }
    }
  }

  @Override
  public void addFile(@Nonnull VirtualFile file) {
    if (myLaunched) {
      throw new IllegalStateException("Adding files is only allowed before launch");
    }
    if (file instanceof NewVirtualFile) {
      myWorkQueue.add(file);
    }
    else {
      LOG.debug("skipped: " + file + " / " + file.getClass());
    }
  }

  @Override
  public boolean isAsynchronous() {
    return myIsAsync;
  }

  @Override
  public void launch() {
    if (myLaunched) {
      throw new IllegalStateException("launch() can be called only once");
    }
    myLaunched = true;
    mySemaphore.down();
    ((RefreshQueueImpl)RefreshQueue.getInstance()).execute(this);
  }

  void scan() {
    List<VirtualFile> workQueue = myWorkQueue;
    myWorkQueue = new ArrayList<>();
    boolean forceRefresh = !myIsRecursive && !myIsAsync;  // shallow sync refresh (e.g. project config files on open)

    if (!workQueue.isEmpty()) {
      LocalFileSystem fs = LocalFileSystem.getInstance();
      if (!forceRefresh && fs instanceof LocalFileSystemImpl) {
        ((LocalFileSystemImpl)fs).markSuspiciousFilesDirty(workQueue);
      }

      long t = 0;
      if (LOG.isTraceEnabled()) {
        LOG.trace("scanning " + workQueue);
        t = System.currentTimeMillis();
      }

      int count = 0;
      refresh:
      do {
        if (LOG.isTraceEnabled()) LOG.trace("try=" + count);

        for (VirtualFile file : workQueue) {
          if (myCancelled) break refresh;

          NewVirtualFile nvf = (NewVirtualFile)file;
          if (forceRefresh) {
            nvf.markDirty();
          }
          else if (!nvf.isDirty()) {
            continue;
          }

          RefreshWorker worker = new RefreshWorker(nvf, myIsRecursive);
          myWorker = worker;
          worker.scan();
          myEvents.addAll(worker.getEvents());
        }

        count++;
        if (LOG.isTraceEnabled()) LOG.trace("events=" + myEvents.size());
      }
      while (!myCancelled && myIsRecursive && count < 3 && ContainerUtil.exists(workQueue, f -> ((NewVirtualFile)f).isDirty()));

      if (t != 0) {
        t = System.currentTimeMillis() - t;
        LOG.trace((myCancelled ? "cancelled, " : "done, ") + t + " ms, events " + myEvents);
      }
    }

    myWorker = null;
  }

  void cancel() {
    myCancelled = true;

    RefreshWorker worker = myWorker;
    if (worker != null) {
      worker.cancel();
    }
  }

  public void fireEvents(@Nonnull List<? extends VFileEvent> events, @Nullable List<? extends AsyncFileListener.ChangeApplier> appliers) {
    try {
      if ((myFinishRunnable != null || !events.isEmpty()) && !ApplicationManager.getApplication().isDisposed()) {
        if (LOG.isDebugEnabled()) LOG.debug("events are about to fire: " + events);
        WriteAction.run(() -> fireEventsInWriteAction(events, appliers));
      }
    }
    finally {
      mySemaphore.up();
    }
  }

  private void fireEventsInWriteAction(List<? extends VFileEvent> events, @Nullable List<? extends AsyncFileListener.ChangeApplier> appliers) {
    VirtualFileManagerEx manager = (VirtualFileManagerEx)VirtualFileManager.getInstance();

    manager.fireBeforeRefreshStart(myIsAsync);
    try {
      AsyncEventSupport.processEvents(events, appliers);
    }
    catch (AssertionError e) {
      if (VirtualFileSystemInternalHelper.CHANGES_NOT_ALLOWED_DURING_HIGHLIGHTING.equals(e.getMessage())) {
        throw new AssertionError("VFS changes are not allowed during highlighting", myStartTrace);
      }
      throw e;
    }
    finally {
      try {
        manager.fireAfterRefreshFinish(myIsAsync);
      }
      finally {
        if (myFinishRunnable != null) {
          myFinishRunnable.run();
        }
      }
    }
  }

  void waitFor() {
    mySemaphore.waitFor();
  }

  @Nonnull
  ModalityState getModality() {
    return myModality;
  }

  @Override
  public String toString() {
    return myWorkQueue.size() <= 1 ? "" : myWorkQueue.size() + " roots in queue.";
  }

  @Nonnull
  public List<? extends VFileEvent> getEvents() {
    return new ArrayList<>(new LinkedHashSet<>(myEvents));
  }
}