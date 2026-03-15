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
package consulo.versionControlSystem.impl.internal.change;

import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ModalityState;
import consulo.util.lang.EmptyRunnable;
import consulo.versionControlSystem.change.InvokeAfterUpdateMode;

import org.jspecify.annotations.Nullable;
import java.util.function.Consumer;

class CallbackData {
  private final static Logger LOG = Logger.getInstance(CallbackData.class);

  
  private final Runnable myCallback;
  
  private final Runnable myWrapperStarter;

  CallbackData(Runnable callback, Runnable wrapperStarter) {
    myCallback = callback;
    myWrapperStarter = wrapperStarter;
  }

  
  public Runnable getCallback() {
    return myCallback;
  }

  
  public Runnable getWrapperStarter() {
    return myWrapperStarter;
  }

  
  public static CallbackData create(Project project,
                                    InvokeAfterUpdateMode mode,
                                    Runnable afterUpdate,
                                    @Nullable String title,
                                    @Nullable ModalityState state) {
    return mode.isSilent() ? createSilent(project, mode, afterUpdate) : createInteractive(project, mode, afterUpdate, title, state);
  }

  
  private static CallbackData createSilent(Project project, InvokeAfterUpdateMode mode, Runnable afterUpdate) {
    Consumer<Runnable> callbackCaller = mode.isCallbackOnAwt()
                                        ? ApplicationManager.getApplication()::invokeLater
                                        : ApplicationManager.getApplication()::executeOnPooledThread;
    Runnable callback = () -> {
      logUpdateFinished(project, mode);
      if (!project.isDisposed()) afterUpdate.run();
    };
    return new CallbackData(() -> callbackCaller.accept(callback), EmptyRunnable.INSTANCE);
  }

  
  private static CallbackData createInteractive(Project project,
                                                InvokeAfterUpdateMode mode,
                                                Runnable afterUpdate,
                                                String title,
                                                @Nullable ModalityState state) {
    Task task = mode.isSynchronous()
                ? new Waiter(project, afterUpdate, title, mode.isCancellable())
                : new FictiveBackgroundable(project, afterUpdate, title, mode.isCancellable(), state);
    Runnable callback = () -> {
      logUpdateFinished(project, mode);
      setDone(task);
    };
    return new CallbackData(callback, () -> ProgressManager.getInstance().run(task));
  }

  private static void setDone(Task task) {
    if (task instanceof Waiter) {
      ((Waiter)task).done();
    }
    else if (task instanceof FictiveBackgroundable) {
      ((FictiveBackgroundable)task).done();
    }
    else {
      throw new IllegalArgumentException("Unknown task type " + task.getClass());
    }
  }

  private static void logUpdateFinished(Project project, InvokeAfterUpdateMode mode) {
    LOG.debug(mode + " changes update finished for project " + project.getName());
  }
}
