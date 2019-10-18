/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.openapi.vfs.newvfs;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.*;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.diagnostic.FrequentEventDetector;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vfs.VfsBundle;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.io.storage.HeavyProcessLatch;
import consulo.application.TransactionGuardEx;
import consulo.logging.Logger;
import gnu.trove.TLongObjectHashMap;
import org.jetbrains.ide.PooledThreadExecutor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.concurrent.Executor;

/**
 * @author max
 */
@Singleton
public class RefreshQueueImpl extends RefreshQueue implements Disposable {
  private static final Logger LOG = Logger.getInstance(RefreshQueueImpl.class);

  private final Executor myQueue = AppExecutorUtil.createBoundedApplicationPoolExecutor("RefreshQueue Pool", PooledThreadExecutor.INSTANCE, 1, this);
  private final ProgressIndicator myRefreshIndicator = RefreshProgress.create(VfsBundle.message("file.synchronize.progress"));
  private final TLongObjectHashMap<RefreshSession> mySessions = new TLongObjectHashMap<>();
  private final FrequentEventDetector myEventCounter = new FrequentEventDetector(100, 100, FrequentEventDetector.Level.WARN);

  public void execute(@Nonnull RefreshSessionImpl session) {
    if (session.isAsynchronous()) {
      queueSession(session, session.getTransaction());
    }
    else {
      Application app = ApplicationManager.getApplication();
      if (app.isDispatchThread()) {
        ((TransactionGuardEx)TransactionGuard.getInstance()).assertWriteActionAllowed();
        doScan(session);
        session.fireEvents();
      }
      else {
        if (((ApplicationEx)app).holdsReadLock()) {
          LOG.error("Do not call synchronous refresh under read lock (except from EDT) - " +
                    "this will cause a deadlock if there are any events to fire.");
          return;
        }
        queueSession(session, TransactionGuard.getInstance().getContextTransaction());
        session.waitFor();
      }
    }
  }

  @Nonnull
  protected AccessToken createHeavyLatch(String id) {
    return HeavyProcessLatch.INSTANCE.processStarted(id);
  }

  protected void queueSession(@Nonnull RefreshSessionImpl session, @Nullable TransactionId transaction) {
    myQueue.execute(() -> {
      myRefreshIndicator.start();
      try (AccessToken ignored = createHeavyLatch("Doing file refresh. " + session)) {
        doScan(session);
      }
      finally {
        myRefreshIndicator.stop();
        TransactionGuard.getInstance().submitTransaction(ApplicationManager.getApplication(), transaction, session::fireEvents);
      }
    });
    myEventCounter.eventHappened(session);
  }

  protected void doScan(RefreshSessionImpl session) {
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
    if (session instanceof RefreshSessionImpl) {
      ((RefreshSessionImpl)session).cancel();
    }
  }

  @Nonnull
  @Override
  public RefreshSession createSession(boolean async, boolean recursively, @Nullable Runnable finishRunnable, @Nonnull ModalityState state) {
    return new RefreshSessionImpl(async, recursively, finishRunnable, ((TransactionGuardEx)TransactionGuard.getInstance()).getModalityTransaction(state));
  }

  @Override
  public void processSingleEvent(@Nonnull VFileEvent event) {
    new RefreshSessionImpl(Collections.singletonList(event)).launch();
  }

  public static boolean isRefreshInProgress() {
    RefreshQueueImpl refreshQueue = (RefreshQueueImpl)RefreshQueue.getInstance();
    synchronized (refreshQueue.mySessions) {
      return !refreshQueue.mySessions.isEmpty();
    }
  }

  @Override
  public void dispose() { }
}