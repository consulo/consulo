/*
 * Copyright 2013-2022 consulo.io
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
package consulo.desktop.awt.uiOld.win;

import consulo.application.Application;
import consulo.component.util.NativeFileLoader;
import consulo.container.boot.ContainerPathManager;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author VISTALL
 * @since 28-Feb-22
 */
public class RecentTasksManager {
  private static final int NOT_INITIALIZED = 0;
  private static final int INITIALIZING = 1;
  private static final int INITIALIZED = 2;
  private static final int FAILED = 3;

  private static final Logger LOG = Logger.getInstance(RecentTasks.class);

  private static AtomicInteger ourInitializeState = new AtomicInteger(NOT_INITIALIZED);

  private static void init(@Nonnull Application application) {
    if (ourInitializeState.compareAndSet(NOT_INITIALIZED, INITIALIZING)) {
      try {
        ContainerPathManager containerPathManager = ContainerPathManager.get();

        NativeFileLoader.loadLibrary("jumplistbridge", System::load);

        RecentTasks.initialize(application.getName().get() + "." + containerPathManager.getConfigPath().hashCode());

        ourInitializeState.compareAndSet(INITIALIZING, INITIALIZED);
      }
      catch (Throwable e) {
        LOG.warn(e);

        ourInitializeState.compareAndSet(INITIALIZING, FAILED);
      }
    }
  }

  public static void clear(@Nonnull Application application) {
    if (ourInitializeState.get() == INITIALIZED) {
      init(application);

      RecentTasks.clearNative();
    }
  }

  /**
   * Use #clearNative method instead of passing empty array of tasks.
   *
   * @param tasks
   */
  public static void addTasks(@Nonnull Application application, String category, Task[] tasks) {
    if (tasks.length == 0) return;

    init(application);

    if (ourInitializeState.get() == INITIALIZED) {
      RecentTasks.addTasksNativeForCategory(category, tasks);
    }
  }

  public static String getShortenPath(@Nonnull Application application, @Nonnull String path) {
    init(application);

    if (ourInitializeState.get() == INITIALIZED) {
      return RecentTasks.getShortenPath(path);
    }
    return path;
  }
}
