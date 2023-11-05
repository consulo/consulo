/*
 * Copyright 2013-2018 consulo.io
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
package consulo.test.light.impl;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.internal.ApplicationEx;
import consulo.application.progress.ProgressIndicator;
import consulo.application.util.concurrent.Job;
import consulo.application.util.concurrent.JobLauncher;
import consulo.component.ProcessCanceledException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2018-08-25
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.LIGHT_TEST)
public class LightJobLauncher extends JobLauncher {
  @Override
  public <T> boolean invokeConcurrentlyUnderProgress(@Nonnull List<? extends T> things, ProgressIndicator progress, @Nonnull Predicate<? super T> thingProcessor) throws ProcessCanceledException {
    ApplicationEx app = (ApplicationEx)Application.get();
    return invokeConcurrentlyUnderProgress(things, progress, app.isReadAccessAllowed(), app.isInImpatientReader(), thingProcessor);
  }

  @Override
  public <T> boolean invokeConcurrentlyUnderProgress(@Nonnull List<? extends T> things,
                                                     ProgressIndicator progress,
                                                     boolean runInReadAction,
                                                     boolean failFastOnAcquireReadAction,
                                                     @Nonnull Predicate<? super T> thingProcessor) throws ProcessCanceledException {
    for (T thing : things) {
      if (!thingProcessor.test(thing)) return false;
    }
    return true;
  }

  @Nonnull
  @Override
  public Job<Void> submitToJobThread(@Nonnull Runnable action, @Nullable Consumer<? super Future<?>> onDoneCallback) {
    action.run();
    if (onDoneCallback != null) {
      onDoneCallback.accept(CompletableFuture.completedFuture(null));
    }
    return Job.NULL_JOB;
  }
}
