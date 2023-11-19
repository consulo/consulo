/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.application.internal;

import consulo.application.AccessToken;
import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.application.util.ApplicationUtil;
import consulo.component.ComponentManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * @author max
 */
public interface ApplicationEx extends Application {
  String LOCATOR_FILE_NAME = ".home";

  /**
   * Loads the application configuration from the specified path
   *
   * @param optionsPath Path to /config folder
   * @throws IOException
   */
  void load(@Nullable String optionsPath) throws IOException;

  boolean isLoaded();

  /**
   * @return true if this thread is inside read action.
   * @see #runReadAction(Runnable)
   */
  @Deprecated
  default boolean holdsReadLock() {
    throw new UnsupportedOperationException();
  }

  /**
   * @return true if the EDT is performing write action right now.
   * @see #runWriteAction(Runnable)
   */
  @Deprecated
  default boolean isWriteActionInProgress() {
    throw new UnsupportedOperationException();
  }

  /**
   * @return true if the EDT started to acquire write action but has not acquired it yet.
   * @see #runWriteAction(Runnable)
   */
  default boolean isWriteActionPending() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  default AccessToken startSaveBlock() {
    doNotSave();

    return new AccessToken() {
      @Override
      public void finish() {
        doNotSave(false);
      }
    };
  }

  default void doNotSave() {
    doNotSave(true);
  }

  void doNotSave(boolean value);

  boolean isDoNotSave();

  /**
   * @param force         if true, no additional confirmations will be shown. The application is guaranteed to exit
   * @param exitConfirmed if true, suppresses any shutdown confirmation. However, if there are any background processes or tasks running,
   *                      a corresponding confirmation will be shown with the possibility to cancel the operation
   */
  void exit(boolean force, boolean exitConfirmed);

  /**
   * @param exitConfirmed if true, suppresses any shutdown confirmation. However, if there are any background processes or tasks running,
   *                      a corresponding confirmation will be shown with the possibility to cancel the operation
   */
  void restart(boolean exitConfirmed);

  /**
   * Runs modal process. For internal use only, see {@link Task}
   */
  default boolean runProcessWithProgressSynchronously(@Nonnull Runnable process, @Nonnull String progressTitle, boolean canBeCanceled, @Nullable ComponentManager project, JComponent parentComponent) {
    return runProcessWithProgressSynchronously(process, progressTitle, canBeCanceled, true, project, null, null);
  }

  default void executeSuspendingWriteAction(@Nullable ComponentManager project, @Nonnull String title, @Nonnull Runnable runnable) {
    runnable.run();
  }

  /**
   * Runs modal process. For internal use only, see {@link Task}
   */
  default boolean runProcessWithProgressSynchronously(@Nonnull Runnable process, @Nonnull String progressTitle, boolean canBeCanceled, ComponentManager project) {
    return runProcessWithProgressSynchronously(process, progressTitle, canBeCanceled, true, project, null, null);
  }

  /**
   * Runs modal or non-modal process.
   * For internal use only, see {@link Task}
   */
  boolean runProcessWithProgressSynchronously(@Nonnull Runnable process,
                                              @Nonnull String progressTitle,
                                              boolean canBeCanceled,
                                              boolean shouldShowModalWindow,
                                              @Nullable ComponentManager project,
                                              @Nullable JComponent parentComponent,
                                              @Nullable @Nls(capitalization = Nls.Capitalization.Title) String cancelText);

  void assertTimeConsuming();

  /**
   * Grab the lock and run the action, in a non-blocking fashion
   *
   * @return true if action was run while holding the lock, false if was unable to get the lock and action was not run
   */
  boolean tryRunReadAction(@Nonnull Runnable action);

  boolean isInImpatientReader();

  default void executeByImpatientReader(@Nonnull Runnable runnable) throws ApplicationUtil.CannotRunReadActionException {
    throw new UnsupportedOperationException();
  }

  default boolean runWriteActionWithCancellableProgressInDispatchThread(@Nonnull String title,
                                                                        @Nullable ComponentManager project,
                                                                        @Nullable JComponent parentComponent,
                                                                        @Nonnull Consumer<? super ProgressIndicator> action) {
    throw new UnsupportedOperationException();
  }

  default boolean runWriteActionWithNonCancellableProgressInDispatchThread(@Nonnull String title,
                                                                           @Nullable ComponentManager project,
                                                                           @Nullable JComponent parentComponent,
                                                                           @Nonnull Consumer<? super ProgressIndicator> action) {
    throw new UnsupportedOperationException();
  }
}
