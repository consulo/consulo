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
package com.intellij.openapi.progress;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.ThrowableComputable;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.HashSet;
import java.util.Set;

public abstract class ProgressManager extends ProgressIndicatorProvider {
  @Nonnull
  public static ProgressManager getInstance() {
    return Application.get().getProgressManager();
  }

  public abstract boolean hasProgressIndicator();

  public abstract boolean hasModalProgressIndicator();

  public abstract boolean hasUnsafeProgressIndicator();

  /**
   * Runs the given process synchronously in calling thread, associating this thread with the specified progress indicator.
   * This means that it'll be returned by {@link ProgressManager#getProgressIndicator()} inside the {@code process},
   * and {@link ProgressManager#checkCanceled()} will throw a {@link ProcessCanceledException} if the progress indicator is canceled.
   *
   * @param progress an indicator to use, {@code null} means reuse current progress
   */
  public abstract void runProcess(@Nonnull Runnable process, @Nullable ProgressIndicator progress) throws ProcessCanceledException;

  /**
   * Performs the given computation synchronously in calling thread and returns its result, associating this thread with the specified progress indicator.
   * This means that it'll be returned by {@link ProgressManager#getProgressIndicator()} inside the {@code process},
   * and {@link ProgressManager#checkCanceled()} will throw a {@link ProcessCanceledException} if the progress indicator is canceled.
   *
   * @param progress an indicator to use, {@code null} means reuse current progress
   */
  public abstract <T> T runProcess(@Nonnull Computable<T> process, @Nullable ProgressIndicator progress) throws ProcessCanceledException;

  @Override
  public abstract ProgressIndicator getProgressIndicator();

  public static void progress(@Nonnull String text) throws ProcessCanceledException {
    progress(text, "");
  }

  public static void progress2(@Nonnull final String text) throws ProcessCanceledException {
    final ProgressIndicator pi = getInstance().getProgressIndicator();
    if (pi != null) {
      pi.checkCanceled();
      pi.setText2(text);
    }
  }

  public static void progress(@Nonnull String text, @Nullable String text2) throws ProcessCanceledException {
    final ProgressIndicator pi = getInstance().getProgressIndicator();
    if (pi != null) {
      pi.checkCanceled();
      pi.setText(text);
      pi.setText2(text2 == null ? "" : text2);
    }
  }

  public abstract void executeNonCancelableSection(@Nonnull Runnable runnable);

  /**
   * Runs the specified operation in a background thread and shows a modal progress dialog in the
   * main thread while the operation is executing.
   * If a dialog can't be shown (e.g. under write action or in headless environment),
   * runs the given operation synchronously in the calling thread.
   *
   * @param process       the operation to execute.
   * @param progressTitle the title of the progress window.
   * @param canBeCanceled whether "Cancel" button is shown on the progress window.
   * @param project       the project in the context of which the operation is executed.
   * @return true if the operation completed successfully, false if it was cancelled.
   */
  public abstract boolean runProcessWithProgressSynchronously(@Nonnull Runnable process,
                                                              @Nonnull @Nls(capitalization = Nls.Capitalization.Title) String progressTitle,
                                                              boolean canBeCanceled,
                                                              @Nullable Project project);

  /**
   * Runs the specified operation in a background thread and shows a modal progress dialog in the
   * main thread while the operation is executing.
   * If a dialog can't be shown (e.g. under write action or in headless environment),
   * runs the given operation synchronously in the calling thread.
   *
   * @param process       the operation to execute.
   * @param progressTitle the title of the progress window.
   * @param canBeCanceled whether "Cancel" button is shown on the progress window.
   * @param project       the project in the context of which the operation is executed.
   * @return true result of operation
   * @throws E exception thrown by process
   */
  public abstract <T, E extends Exception> T runProcessWithProgressSynchronously(@Nonnull ThrowableComputable<T, E> process,
                                                                                 @Nonnull @Nls(capitalization = Nls.Capitalization.Title) String progressTitle,
                                                                                 boolean canBeCanceled,
                                                                                 @Nullable Project project) throws E;

  /**
   * Runs the specified operation in a background thread and shows a modal progress dialog in the
   * main thread while the operation is executing.
   * If a dialog can't be shown (e.g. under write action or in headless environment),
   * runs the given operation synchronously in the calling thread.
   *
   * @param process         the operation to execute.
   * @param progressTitle   the title of the progress window.
   * @param canBeCanceled   whether "Cancel" button is shown on the progress window.
   * @param project         the project in the context of which the operation is executed.
   * @param parentComponent the component which will be used to calculate the progress window ancestor
   * @return true if the operation completed successfully, false if it was cancelled.
   */
  public abstract boolean runProcessWithProgressSynchronously(@Nonnull Runnable process,
                                                              @Nonnull @Nls(capitalization = Nls.Capitalization.Title) String progressTitle,
                                                              boolean canBeCanceled,
                                                              @Nullable Project project,
                                                              @Nullable JComponent parentComponent);

  /**
   * Runs a specified {@code process} in a background thread and shows a progress dialog, which can be made non-modal by pressing
   * background button. Upon successful termination of the process a {@code successRunnable} will be called in Swing UI thread and
   * {@code canceledRunnable} will be called if terminated on behalf of the user by pressing either cancel button, while running in
   * a modal state or stop button if running in background.
   *
   * @param project          the project in the context of which the operation is executed.
   * @param progressTitle    the title of the progress window.
   * @param process          the operation to execute.
   * @param successRunnable  a callback to be called in Swing UI thread upon normal termination of the process.
   * @param canceledRunnable a callback to be called in Swing UI thread if the process have been canceled by the user.
   * @deprecated use {@link #run(Task)}
   */
  @Deprecated
  public abstract void runProcessWithProgressAsynchronously(@Nonnull Project project,
                                                            @Nonnull @Nls String progressTitle,
                                                            @Nonnull Runnable process,
                                                            @Nullable Runnable successRunnable,
                                                            @Nullable Runnable canceledRunnable);

  /**
   * Runs a specified {@code process} in a background thread and shows a progress dialog, which can be made non-modal by pressing
   * background button. Upon successful termination of the process a {@code successRunnable} will be called in Swing UI thread and
   * {@code canceledRunnable} will be called if terminated on behalf of the user by pressing either cancel button, while running in
   * a modal state or stop button if running in background.
   *
   * @param project          the project in the context of which the operation is executed.
   * @param progressTitle    the title of the progress window.
   * @param process          the operation to execute.
   * @param successRunnable  a callback to be called in Swing UI thread upon normal termination of the process.
   * @param canceledRunnable a callback to be called in Swing UI thread if the process have been canceled by the user.
   * @param option           progress indicator behavior controller.
   * @deprecated use {@link #run(Task)}
   */
  @Deprecated
  public abstract void runProcessWithProgressAsynchronously(@Nonnull Project project,
                                                            @Nonnull @Nls String progressTitle,
                                                            @Nonnull Runnable process,
                                                            @Nullable Runnable successRunnable,
                                                            @Nullable Runnable canceledRunnable,
                                                            @Nonnull PerformInBackgroundOption option);

  /**
   * Runs a specified {@code task} in either background/foreground thread and shows a progress dialog.
   *
   * @param task task to run (either {@link Task.Modal} or {@link Task.Backgroundable}).
   */
  public abstract void run(@Nonnull Task task);

  /**
   * Runs a specified computation with a modal progress dialog.
   */
  public <T, E extends Exception> T run(@Nonnull Task.WithResult<T, E> task) throws E {
    run((Task)task);
    return task.getResult();
  }

  public abstract void runProcessWithProgressAsynchronously(@Nonnull Task.Backgroundable task, @Nonnull ProgressIndicator progressIndicator);

  protected void indicatorCanceled(@Nonnull ProgressIndicator indicator) {
  }

  public static void canceled(@Nonnull ProgressIndicator indicator) {
    getInstance().indicatorCanceled(indicator);
  }

  public static void checkCanceled() throws ProcessCanceledException {
    getInstance().doCheckCanceled();
  }

  /**
   * @param progress an indicator to use, {@code null} means reuse current progress
   */
  public abstract void executeProcessUnderProgress(@Nonnull Runnable process, @Nullable ProgressIndicator progress) throws ProcessCanceledException;

  public static void assertNotCircular(@Nonnull ProgressIndicator original) {
    Set<ProgressIndicator> wrappedParents = null;
    for (ProgressIndicator i = original; i instanceof WrappedProgressIndicator; i = ((WrappedProgressIndicator)i).getOriginalProgressIndicator()) {
      if (wrappedParents == null) wrappedParents = new HashSet<>();
      if (!wrappedParents.add(i)) {
        throw new IllegalArgumentException(i + " wraps itself");
      }
    }
  }

  /**
   * This method attempts to run provided action synchronously in a read action, so that, if possible, it wouldn't impact any pending,
   * executing or future write actions (for this to work effectively the action should invoke {@link ProgressManager#checkCanceled()} or
   * {@link ProgressIndicator#checkCanceled()} often enough).
   * It returns {@code true} if action was executed successfully. It returns {@code false} if the action was not
   * executed successfully, i.e. if:
   * <ul>
   * <li>write action was in progress when the method was called</li>
   * <li>write action was pending when the method was called</li>
   * <li>action started to execute, but was aborted using {@link ProcessCanceledException} when some other thread initiated
   * write action</li>
   * </ul>
   *
   * @param action    the code to execute under read action
   * @param indicator progress indicator that should be cancelled if a write action is about to start. Can be null.
   */
  public abstract boolean runInReadActionWithWriteActionPriority(@Nonnull final Runnable action, @Nullable ProgressIndicator indicator);

  public abstract boolean isInNonCancelableSection();

  /**
   * Performs the given computation while giving more priority to the current thread
   * (by forcing all other non-prioritized threads to sleep a bit whenever they call {@link #checkCanceled()}.<p></p>
   * <p>
   * This is intended for relatively short (expected to be under 10 seconds) background activities that the user is waiting for
   * (e.g. code navigation), and which shouldn't be slowed down by CPU-intensive background tasks like highlighting or indexing.
   */
  public abstract <T, E extends Throwable> T computePrioritized(@Nonnull ThrowableComputable<T, E> computable) throws E;
}