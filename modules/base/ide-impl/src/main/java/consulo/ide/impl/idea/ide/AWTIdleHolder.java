/*
 * Copyright 2013-2023 consulo.io
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
package consulo.ide.impl.idea.ide;

import consulo.application.Application;
import consulo.logging.Logger;
import consulo.ui.ModalityState;
import consulo.util.collection.Lists;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * extract code from {@link IdeEventQueue}
 */
public class AWTIdleHolder {
  private static final Logger LOG = Logger.getInstance(AWTIdleHolder.class);

  private final class MyFireIdleRequest implements Runnable {
    private final Runnable myRunnable;
    private final int myTimeout;


    MyFireIdleRequest(@Nonnull Runnable runnable, final int timeout) {
      myTimeout = timeout;
      myRunnable = runnable;
    }


    @Override
    public void run() {
      myRunnable.run();
      
      synchronized (myLock) {
        // do not reschedule if not interested anymore
        if (myIdleListeners.contains(myRunnable)) {
          addRequest(this);
        }
      }
    }

    public int getTimeout() {
      return myTimeout;
    }

    @Override
    public String toString() {
      return "Fire idle request. delay: " + getTimeout() + "; runnable: " + myRunnable;
    }
  }

  private Map<MyFireIdleRequest, Future<?>> myIdleRequestFutures = new ConcurrentHashMap<>();
  private Future<?> myIdleTimeCounterAlarm = CompletableFuture.completedFuture(null);
  private long myIdleTime;

  /**
   * Adding/Removing of "idle" listeners should be thread safe.
   */
  private final Object myLock = new Object();

  private final List<Runnable> myIdleListeners = Lists.newLockFreeCopyOnWriteList();
  private final Map<Runnable, MyFireIdleRequest> myListener2Request = new HashMap<>();
  private long myLastActiveTime;

  public long getIdleTime() {
    return myIdleTime;
  }

  public void addIdleTimeCounterRequest() {
    myIdleTimeCounterAlarm.cancel(false);
    myLastActiveTime = System.currentTimeMillis();
    myIdleTimeCounterAlarm = Application.get().getLastUIAccess().getScheduler().schedule(() -> {
      myIdleTime += System.currentTimeMillis() - myLastActiveTime;
      addIdleTimeCounterRequest();
    }, ModalityState.nonModal(), 20000, TimeUnit.MILLISECONDS);
  }

  /**
   * This class performs special processing in order to have {@link #getIdleTime()} return more or less up-to-date data.
   * <p/>
   * This method allows to stop that processing (convenient in non-intellij environment like upsource).
   */
  @SuppressWarnings("unused") // Used in upsource.
  public void stopIdleTimeCalculation() {
    myIdleTimeCounterAlarm.cancel(false);
  }

  public void addIdleListener(@Nonnull final Runnable runnable, final int timeoutMillis) {
    if (timeoutMillis <= 0 || TimeUnit.MILLISECONDS.toHours(timeoutMillis) >= 24) {
      throw new IllegalArgumentException("This timeout value is unsupported: " + timeoutMillis);
    }
    synchronized (myLock) {
      myIdleListeners.add(runnable);
      final MyFireIdleRequest request = new MyFireIdleRequest(runnable, timeoutMillis);
      myListener2Request.put(runnable, request);

      addRequest(request);
    }
  }

  public void removeIdleListener(@Nonnull final Runnable runnable) {
    synchronized (myLock) {
      final boolean wasRemoved = myIdleListeners.remove(runnable);
      if (!wasRemoved) {
        LOG.error("unknown runnable: " + runnable);
      }

      final MyFireIdleRequest request = myListener2Request.remove(runnable);
      LOG.assertTrue(request != null);
      cancelRequest(request);
    }
  }

  public void resetIdle(AWTEvent e) {
    synchronized (myLock) {
      for (Future<?> future : myIdleRequestFutures.values()) {
        future.cancel(false);
      }
      myIdleRequestFutures.clear();

      for (Runnable idleListener : myIdleListeners) {
        final MyFireIdleRequest request = myListener2Request.get(idleListener);
        if (request == null) {
          LOG.error("There is no request for " + idleListener);
        }
        else {
          addRequest(request);
        }
      }
      
      if (KeyEvent.KEY_PRESSED == e.getID() ||
        KeyEvent.KEY_TYPED == e.getID() ||
        MouseEvent.MOUSE_PRESSED == e.getID() ||
        MouseEvent.MOUSE_RELEASED == e.getID() ||
        MouseEvent.MOUSE_CLICKED == e.getID()) {
        addIdleTimeCounterRequest();
      }
    }
  }

  private void cancelRequest(MyFireIdleRequest request) {
    Future<?> oldFuture = myIdleRequestFutures.remove(request);
    if (oldFuture != null) {
      oldFuture.cancel(false);
    }
  }

  private void addRequest(MyFireIdleRequest request) {
    cancelRequest(request);

    int timeoutMillis = request.getTimeout();

    Future<?> future = Application.get().getLastUIAccess().getScheduler().schedule(request, timeoutMillis, TimeUnit.MILLISECONDS);

    myIdleRequestFutures.put(request, future);
  }
}
