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
package consulo.project.startup;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

/**
 * Allows to register activities which are run during project loading.
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class StartupManager {
  /**
   * Returns the startup manager instance for the specified project.
   *
   * @param project the project for which the instance should be returned.
   * @return the startup manager instance.
   */
  @Deprecated
  @DeprecationInfo("Use constructor injecting")
  public static StartupManager getInstance(@Nonnull Project project) {
    return project.getInstance(StartupManager.class);
  }

  public abstract void registerPreStartupActivity(@Nonnull StartupActivity activity);

  /**
   * Registers an activity which is performed during project load while the "Loading Project"
   * progress bar is displayed. You may NOT access the PSI structures from the activity.
   *
   * @param consumer the activity to execute.
   */
  public abstract void registerStartupActivity(@Nonnull StartupActivity activity);

  /**
   * Registers an activity which is performed during project load after the "Loading Project"
   * progress bar is displayed. You may access the PSI structures from the activity.
   *
   * @param runnable the activity to execute.
   */
  public abstract void registerPostStartupActivity(@Nonnull StartupActivity activity);

  /**
   * Registers activity that is executed on pooled thread after project is opened.
   * The runnable will be executed in current thread if project is already opened.</p>
   * <p>
   * See https://github.com/JetBrains/intellij-community/blob/master/platform/service-container/overview.md#startup-activity.
   */
  public abstract void runAfterOpened(@Nonnull StartupActivity activity);

  /**
   * Executes the specified runnable immediately if the initialization of the current project
   * is complete, or registers it as a post-startup activity if the project is being initialized.
   *
   * @param runnable the activity to execute.
   */
  public abstract void runWhenProjectIsInitialized(@Nonnull StartupActivity startupActivity);

  public abstract boolean postStartupActivityPassed();

  // region Deprecated Staff
  @Deprecated
  public final void registerPreStartupActivity(@Nonnull Runnable runnable) {
    if (runnable instanceof DumbAware) {
      registerPreStartupActivity((StartupActivity.DumbAware)(project, uiAccess) -> runnable.run());
    }
    else {
      registerPreStartupActivity((StartupActivity)(project, uiAccess) -> runnable.run());
    }
  }

  @Deprecated
  public final void registerStartupActivity(@Nonnull Runnable runnable) {
    if (runnable instanceof DumbAware) {
      registerStartupActivity((StartupActivity.DumbAware)(project, uiAccess) -> runnable.run());
    }
    else {
      registerStartupActivity((StartupActivity)(project, uiAccess) -> runnable.run());
    }
  }

  @Deprecated
  public final void registerPostStartupActivity(@Nonnull Runnable runnable) {
    if (runnable instanceof DumbAware) {
      registerPostStartupActivity((StartupActivity.DumbAware)(project, uiAccess) -> runnable.run());
    }
    else {
      registerPostStartupActivity((StartupActivity)(project, uiAccess) -> runnable.run());
    }
  }

  /**
   * Executes the specified runnable immediately if the initialization of the current project
   * is complete, or registers it as a post-startup activity if the project is being initialized.
   *
   * @param runnable the activity to execute.
   */
  @Deprecated
  public void runWhenProjectIsInitialized(@Nonnull Runnable runnable) {
    if (runnable instanceof DumbAware) {
      runWhenProjectIsInitialized((StartupActivity.DumbAware)(project, uiAccess) -> runnable.run());
    }
    else {
      runWhenProjectIsInitialized((StartupActivity)(project, uiAccess) -> runnable.run());
    }
  }

  @Deprecated
  public  void runAfterOpened(@Nonnull Runnable runnable) {
    if (runnable instanceof DumbAware) {
      runAfterOpened((StartupActivity.DumbAware)(project, uiAccess) -> runnable.run());
    }
    else {
      runAfterOpened((StartupActivity)(project, uiAccess) -> runnable.run());
    }
  }
  // endregion
}
