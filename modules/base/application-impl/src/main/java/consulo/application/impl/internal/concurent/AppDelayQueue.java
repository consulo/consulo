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
package consulo.application.impl.internal.concurent;

import consulo.util.lang.ObjectUtil;
import consulo.util.lang.reflect.ReflectionUtil;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class implements the global delayed queue which is used by
 * {@link AppScheduledExecutorService} and {@link BoundedScheduledExecutorService}.
 * It starts the background thread which polls the queue for tasks ready to run and sends them to the appropriate executor.
 * The {@link #shutdown()} must be called before disposal.
 */
public class AppDelayQueue extends DelayQueue<SchedulingWrapper.MyScheduledFutureTask> {
  private static final Logger LOG = LoggerFactory.getLogger(AppDelayQueue.class);
  private final Thread scheduledToPooledTransferrer;
  private final AtomicBoolean shutdown = new AtomicBoolean();

  public AppDelayQueue() {
    /* this thread takes the ready-to-execute scheduled tasks off the queue and passes them for immediate execution to {@link SchedulingWrapper#backendExecutorService} */
    scheduledToPooledTransferrer = new Thread(() -> {
      while (!shutdown.get()) {
        try {
          final SchedulingWrapper.MyScheduledFutureTask task = take();
          if (LOG.isTraceEnabled()) {
            LOG.trace("Took " + info(task));
          }
          if (!task.isDone()) {  // can be cancelled already
            try {
              task.executeMeInBackendExecutor();
            }
            catch (Throwable e) {
              try {
                LOG.error("Error executing " + task, e);
              }
              catch (Throwable ignored) {
                // do not let it stop the thread
              }
            }
          }
        }
        catch (InterruptedException e) {
          if (!shutdown.get()) {
            LOG.error(e.getMessage(), e);
          }
        }
      }
      LOG.debug("scheduledToPooledTransferrer Stopped");
    }, "Periodic tasks thread");
    scheduledToPooledTransferrer.setDaemon(true); // mark as daemon to not prevent JVM to exit (needed for Kotlin CLI compiler)
    scheduledToPooledTransferrer.start();
  }

  // for diagnostics
  public static Object info(Runnable info) {
    Object task = info;
    String extra = null;
    if (task instanceof FutureTask) {
      extra = ((FutureTask<?>)task).isCancelled() ? " (future cancelled)" : ((FutureTask<?>)task).isDone() ? " (future done)" : null;
      task = ObjectUtil.chooseNotNull(ReflectionUtil.getField(task.getClass(), task, Callable.class, "callable"), task);
    }
    if (task instanceof Callable && task.getClass().getName().equals("java.util.concurrent.Executors$RunnableAdapter")) {
      task = ObjectUtil.chooseNotNull(ReflectionUtil.getField(task.getClass(), task, Runnable.class, "task"), task);
    }
    return extra == null ? task : task.getClass() + extra;
  }

  public void shutdown() {
    if (shutdown.getAndSet(true)) {
      throw new IllegalStateException("Already shutdown");
    }
    scheduledToPooledTransferrer.interrupt();

    try {
      scheduledToPooledTransferrer.join();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Nonnull
  public Thread getThread() {
    return scheduledToPooledTransferrer;
  }
}
