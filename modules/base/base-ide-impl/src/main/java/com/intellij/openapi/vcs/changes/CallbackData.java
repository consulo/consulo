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
package com.intellij.openapi.vcs.changes;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import consulo.logging.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.EmptyRunnable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.function.Consumer;

class CallbackData {
  private final static Logger LOG = Logger.getInstance(CallbackData.class);

  @Nonnull
  private final Runnable myCallback;
  @Nonnull
  private final Runnable myWrapperStarter;

  CallbackData(@Nonnull Runnable callback, @Nonnull Runnable wrapperStarter) {
    myCallback = callback;
    myWrapperStarter = wrapperStarter;
  }

  @Nonnull
  public Runnable getCallback() {
    return myCallback;
  }

  @Nonnull
  public Runnable getWrapperStarter() {
    return myWrapperStarter;
  }

  @Nonnull
  public static CallbackData create(@Nonnull Project project,
                                    @Nonnull InvokeAfterUpdateMode mode,
                                    @Nonnull Runnable afterUpdate,
                                    @Nullable String title,
                                    @Nullable ModalityState state) {
    return mode.isSilent() ? createSilent(project, mode, afterUpdate) : createInteractive(project, mode, afterUpdate, title, state);
  }

  @Nonnull
  private static CallbackData createSilent(@Nonnull Project project, @Nonnull InvokeAfterUpdateMode mode, @Nonnull Runnable afterUpdate) {
    Consumer<Runnable> callbackCaller = mode.isCallbackOnAwt()
                                        ? ApplicationManager.getApplication()::invokeLater
                                        : ApplicationManager.getApplication()::executeOnPooledThread;
    Runnable callback = () -> {
      logUpdateFinished(project, mode);
      if (!project.isDisposed()) afterUpdate.run();
    };
    return new CallbackData(() -> callbackCaller.accept(callback), EmptyRunnable.INSTANCE);
  }

  @Nonnull
  private static CallbackData createInteractive(@Nonnull Project project,
                                                @Nonnull InvokeAfterUpdateMode mode,
                                                @Nonnull Runnable afterUpdate,
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

  private static void setDone(@Nonnull Task task) {
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

  private static void logUpdateFinished(@Nonnull Project project, @Nonnull InvokeAfterUpdateMode mode) {
    LOG.debug(mode + " changes update finished for project " + project.getName());
  }
}
