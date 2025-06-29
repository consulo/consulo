/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.application.progress;

import consulo.application.Application;
import consulo.component.ComponentManager;
import consulo.logging.Logger;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.lang.reflect.InvocationTargetException;

/**
 * Allows to execute {@link SequentialTask} under modal progress.
 * 
 * @author Denis Zhdanov
 * @since 2011-09-27
 */
public class SequentialModalProgressTask extends Task.Modal {
  private static final Logger LOG = Logger.getInstance(SequentialModalProgressTask.class);
  
  private static final long DEFAULT_MIN_ITERATION_MIN_TIME = 500;

  /**
   * We want to perform the task by big chunks at EDT. However, there is a possible case that particular task iteration
   * is executed in short amount of time. Hence, we may want to execute more than one chunk during single EDT iteration.
   * This field holds min amount of time (in milliseconds) to spend to performing the task.
   */
  private long myMinIterationTime = DEFAULT_MIN_ITERATION_MIN_TIME;

  private final String myTitle;

  private ProgressIndicator myIndicator;
  private SequentialTask myTask;

  public SequentialModalProgressTask(@Nullable ComponentManager project, @Nonnull String title) {
    this(project, title, true);
  }

  public SequentialModalProgressTask(@Nullable ComponentManager project, @Nonnull String title, boolean canBeCancelled) {
    super(project, title, canBeCancelled);
    myTitle = title;
  }

  @Override
  public void run(@Nonnull ProgressIndicator indicator) {
    try {
      doRun(indicator);
    }
    catch (Exception e) {
      LOG.info("Unexpected exception occurred during processing sequential task '" + myTitle + "'", e);
    }
    finally {
      indicator.stop();
    }
  }

  public void doRun(@Nonnull ProgressIndicator indicator) throws InvocationTargetException, InterruptedException {
    final SequentialTask task = myTask;
    if (task == null) {
      return;
    }
    
    myIndicator = indicator;
    indicator.setIndeterminate(false);
    prepare(task);
    
    // We need to sync background thread and EDT here in order to avoid situation when event queue is full of processing requests.
    while (!task.isDone()) {
      if (indicator.isCanceled()) {
        task.stop();
        break;
      }
      Application.get().invokeAndWait(new Runnable() {
        @Override
        public void run() {
          long start = System.currentTimeMillis();
          try {
            while (!task.isDone() && System.currentTimeMillis() - start < myMinIterationTime) {
              task.iteration();
            }
          }
          catch (RuntimeException e) {
            task.stop();
            throw e;
          }
        }
      });
      //if (ApplicationManager.getApplication().isDispatchThread()) {
      //  runnable.run();
      //}
      //else {
      //  ApplicationManagerEx.getApplicationEx().suspendReadAccessAndRunWriteAction(runnable);
      //}
    }
  }

  public void setMinIterationTime(long minIterationTime) {
    myMinIterationTime = minIterationTime;
  }

  public void setTask(@Nullable SequentialTask task) {
    myTask = task;
  }
  
  @Nullable
  public ProgressIndicator getIndicator() {
    return myIndicator;
  }

  /**
   * Executes preliminary jobs prior to the target sequential task processing ({@link SequentialTask#prepare()} by default).
   * 
   * @param task  task to be executed
   */
  protected void prepare(@Nonnull SequentialTask task) {
    task.prepare();
  }
}
