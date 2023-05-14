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
package consulo.application.impl.internal.progress;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.progress.ProgressIndicator;
import consulo.application.util.function.Computable;
import consulo.component.ProcessCanceledException;
import consulo.project.DumbService;
import consulo.ui.ModalityState;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * A computation that needs to be run in background and inside a read action, and canceled whenever a write action is about to occur.
 *
 * @see ProgressIndicatorUtils#scheduleWithWriteActionPriority(ReadTask)
 *
 */
public abstract class ReadTask {
  /**
   * Performs the computation.
   * Is invoked inside a read action and under a progress indicator that's canceled when a write action is about to occur.
   * For tasks that have some Swing thread activity afterwards (e.g. applying changes, showing dialogs etc),
   * use {@link #performInReadAction(ProgressIndicator)} instead
   */
  @RequiredReadAction
  public void computeInReadAction(@Nonnull ProgressIndicator indicator) throws ProcessCanceledException {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs the computation.
   * Is invoked inside a read action and under a progress indicator that's canceled when a write action is about to occur.
   * @return an action that should be performed later on Swing thread if no write actions have happened before that
   */
  @Nullable
  @RequiredReadAction
  public Continuation performInReadAction(@Nonnull ProgressIndicator indicator) throws ProcessCanceledException {
    computeInReadAction(indicator);
    return null;
  }

  /**
   * Is invoked on Swing thread whenever the computation is canceled by a write action.
   * A likely implementation is to restart the computation, maybe based on the new state of the system.
   */
  public abstract void onCanceled(@Nonnull ProgressIndicator indicator);

  /**
   * Is invoked on a background thread. The responsibility of this method is to start a read action and
   * call {@link #computeInReadAction(ProgressIndicator)}. Overriders might also do something else.
   * For example, use {@link DumbService#runReadActionInSmartMode(Runnable)}.
   * @param indicator the progress indicator of the background thread
   */
  public Continuation runBackgroundProcess(@Nonnull final ProgressIndicator indicator) throws ProcessCanceledException {
    return ApplicationManager.getApplication().runReadAction((Computable<Continuation>)() -> performInReadAction(indicator));
  }

  /**
   * An object representing the action that should be done on Swing thread after the background computation is finished.
   * It's invoked only if tasks' progress indicator hasn't been canceled since the task has ended.
   */
  public static final class Continuation {
    private final Runnable myAction;
    private final ModalityState myModalityState;

    /**
     * @param action code to be executed in Swing thread in default modality state
     * @see IdeaModalityState#defaultModalityState()
     */
    public Continuation(@Nonnull Runnable action) {
      this(action, IdeaModalityState.defaultModalityState());
    }

    /**
     * @param action code to be executed in Swing thread in default modality state
     * @param modalityState modality state when the action is to be executed
     */
    public Continuation(@Nonnull Runnable action, @Nonnull ModalityState modalityState) {
      myAction = action;
      myModalityState = modalityState;
    }

    /**
     * @return modality state when {@link #getAction()} is to be executed
     */
    @Nonnull
    public ModalityState getModalityState() {
      return myModalityState;
    }

    /**
     * @return runnable to be executed in Swing thread in default modality state
     */
    @Nonnull
    public Runnable getAction() {
      return myAction;
    }
  }
}
