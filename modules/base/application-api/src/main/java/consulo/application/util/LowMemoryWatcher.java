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
package consulo.application.util;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.util.collection.WeakList;
import org.jetbrains.annotations.Contract;

import jakarta.annotation.Nonnull;

/**
 * @author Eugene Zhuravlev
 */
public class LowMemoryWatcher {
  private static final Logger LOG = Logger.getInstance(LowMemoryWatcher.class);

  public enum LowMemoryWatcherType {
    ALWAYS,
    ONLY_AFTER_GC
  }

  private static final WeakList<LowMemoryWatcher> ourListeners = new WeakList<>();
  private final Runnable myRunnable;
  private final LowMemoryWatcherType myType;

  public static void onLowMemorySignalReceived(boolean afterGc) {
    LOG.info("Low memory signal received: afterGc=" + afterGc);
    for (LowMemoryWatcher watcher : ourListeners.toStrongList()) {
      try {
        if (watcher.myType == LowMemoryWatcherType.ALWAYS || watcher.myType == LowMemoryWatcherType.ONLY_AFTER_GC && afterGc) {
          watcher.myRunnable.run();
        }
      }
      catch (Throwable e) {
        LOG.info(e);
      }
    }
  }

  /**
   * Registers a runnable to run on low memory events
   *
   * @param runnable         the action which executes on low-memory condition. Can be executed:
   *                         - in arbitrary thread
   *                         - in unpredictable time
   *                         - multiple copies in parallel so please make it reentrant.
   * @param notificationType When ONLY_AFTER_GC, then the runnable will be invoked only if the low-memory condition still exists after GC.
   *                         When ALWAYS, then the runnable also will be invoked when the low-memory condition is detected before GC.
   * @return a LowMemoryWatcher instance holding the runnable. This instance should be kept in memory while the
   * low memory notification functionality is needed. As soon as it's garbage-collected, the runnable won't receive any further notifications.
   */
  @Contract(pure = true) // to avoid ignoring the result
  public static LowMemoryWatcher register(@Nonnull Runnable runnable, @Nonnull LowMemoryWatcherType notificationType) {
    return new LowMemoryWatcher(runnable, notificationType);
  }

  @Contract(pure = true) // to avoid ignoring the result
  public static LowMemoryWatcher register(@Nonnull Runnable runnable) {
    return new LowMemoryWatcher(runnable, LowMemoryWatcherType.ALWAYS);
  }

  /**
   * Registers a runnable to run on low memory events. The notifications will be issued until parentDisposable is disposed.
   */
  public static void register(@Nonnull Runnable runnable, @Nonnull LowMemoryWatcherType notificationType, @Nonnull Disposable parentDisposable) {
    final LowMemoryWatcher watcher = new LowMemoryWatcher(runnable, notificationType);
    Disposer.register(parentDisposable, () -> watcher.stop());
  }

  public static void register(@Nonnull Runnable runnable, @Nonnull Disposable parentDisposable) {
    register(runnable, LowMemoryWatcherType.ALWAYS, parentDisposable);
  }

  private LowMemoryWatcher(@Nonnull Runnable runnable, @Nonnull LowMemoryWatcherType type) {
    myRunnable = runnable;
    myType = type;
    ourListeners.add(this);
  }

  public void stop() {
    ourListeners.remove(this);
  }

  /**
   * LowMemoryWatcher maintains a background thread where all the handlers are invoked.
   * In server environments, this thread may run indefinitely and prevent the class loader from
   * being gc-ed. Thus it's necessary to invoke this method to stop that thread and let the classes be garbage-collected.
   */
  static void stopAll() {
    ourListeners.clear();
  }
}
