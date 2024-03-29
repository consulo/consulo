/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.util.lang;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ShutDownTracker implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(ShutDownTracker.class);
  private final List<Thread> myThreads = new ArrayList<Thread>();
  private final LinkedList<Thread> myShutdownThreads = new LinkedList<Thread>();
  private final LinkedList<Runnable> myShutdownTasks = new LinkedList<Runnable>();
  private volatile boolean myIsShutdownHookRunning = false;

  private ShutDownTracker() {
    //noinspection HardCodedStringLiteral
    Runtime.getRuntime().addShutdownHook(new Thread(this, "Shutdown tracker"));
  }

  private static class ShutDownTrackerHolder {
    private static final ShutDownTracker ourInstance = new ShutDownTracker();
  }

  @Nonnull
  public static ShutDownTracker getInstance() {
    return ShutDownTrackerHolder.ourInstance;
  }

  public static boolean isShutdownHookRunning() {
    return getInstance().myIsShutdownHookRunning;
  }

  @Override
  public void run() {
    myIsShutdownHookRunning = true;

    ensureStopperThreadsFinished();

    for (Runnable task = removeLast(myShutdownTasks); task != null; task = removeLast(myShutdownTasks)) {
      //  task can change myShutdownTasks
      try {
        task.run();
      }
      catch (Throwable e) {
        LOG.error(e.getMessage(), e);
      }
    }

    for (Thread thread = removeLast(myShutdownThreads); thread != null; thread = removeLast(myShutdownThreads)) {
      thread.start();
      try {
        thread.join();
      }
      catch (InterruptedException ignored) {
      }
    }
  }

  public final void ensureStopperThreadsFinished() {
    Thread[] threads = getStopperThreads();
    while (threads.length > 0) {
      Thread thread = threads[0];
      if (!thread.isAlive()) {
        if (isRegistered(thread)) {
          LOG.error("Thread '" + thread.getName() + "' did not unregister itself from ShutDownTracker.");
          unregisterStopperThread(thread);
        }
      }
      else {
        try {
          thread.join(100);
        }
        catch (InterruptedException ignored) {
        }
      }
      threads = getStopperThreads();
    }
  }

  private synchronized boolean isRegistered(@Nonnull Thread thread) {
    return myThreads.contains(thread);
  }

  @Nonnull
  private synchronized Thread[] getStopperThreads() {
    return myThreads.toArray(new Thread[myThreads.size()]);
  }

  public synchronized void registerStopperThread(@Nonnull Thread thread) {
    myThreads.add(thread);
  }

  public synchronized void unregisterStopperThread(@Nonnull Thread thread) {
    myThreads.remove(thread);
  }

  public synchronized void registerShutdownThread(@Nonnull Thread thread) {
    myShutdownThreads.addLast(thread);
  }

  public synchronized void registerShutdownThread(int index, @Nonnull Thread thread) {
    myShutdownThreads.add(index, thread);
  }

  public synchronized void registerShutdownTask(@Nonnull Runnable task) {
    myShutdownTasks.addLast(task);
  }

  public synchronized void unregisterShutdownTask(@Nonnull Runnable task) {
    myShutdownTasks.remove(task);
  }
  
  private synchronized <T> T removeLast(@Nonnull LinkedList<T> list) {
    return list.isEmpty()? null : list.removeLast();
  }
}
