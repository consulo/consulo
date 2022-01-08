package com.intellij.openapi.externalSystem.model.task;

import com.intellij.openapi.progress.ProgressIndicator;
import javax.annotation.Nonnull;

/**
 * @author Denis Zhdanov
 * @since 1/24/12 7:16 AM
 */
public interface ExternalSystemTask {

  @Nonnull
  ExternalSystemTaskId getId();

  @Nonnull
  ExternalSystemTaskState getState();

  /**
   * @return    error occurred during the task execution (if any)
   */
  @javax.annotation.Nullable
  Throwable getError();

  /**
   * Executes current task and updates given indicator's {@link ProgressIndicator#setText2(String) status} during that.
   * 
   * @param indicator  target progress indicator
   * @param listeners  callbacks to be notified on task execution update
   */
  void execute(@Nonnull ProgressIndicator indicator, @Nonnull ExternalSystemTaskNotificationListener... listeners);
  
  /**
   * Executes current task at the calling thread, i.e. the call to this method blocks.
   * 
   * @param listeners  callbacks to be notified about the task execution update
   */
  void execute(@Nonnull ExternalSystemTaskNotificationListener... listeners);

  /**
   * Cancels current task and updates given indicator's {@link ProgressIndicator#setText2(String) status} during that.
   *
   * @param indicator  target progress indicator
   * @param listeners  callbacks to be notified on task execution update
   */
  boolean cancel(@Nonnull ProgressIndicator indicator, @Nonnull ExternalSystemTaskNotificationListener... listeners);

  /**
   * Cancels current task at the calling thread, i.e. the call to this method blocks.
   *
   * @param listeners  callbacks to be notified about the task execution update
   */
  boolean cancel(@Nonnull ExternalSystemTaskNotificationListener... listeners);

  /**
   * Forces current task to refresh {@link #getState() its state}.
   */
  void refreshState();
}
