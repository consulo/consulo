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

import com.intellij.concurrency.Job;
import com.intellij.concurrency.JobLauncher;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.util.Consumer;
import com.intellij.util.Processor;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 2018-08-25
 */
public class LightJobLauncher extends JobLauncher {
  @Override
  public <T> boolean invokeConcurrentlyUnderProgress(@Nonnull List<T> things,
                                                     ProgressIndicator progress,
                                                     boolean runInReadAction,
                                                     boolean failFastOnAcquireReadAction,
                                                     @Nonnull Processor<? super T> thingProcessor) {
    for (T thing : things) {
      if (!thingProcessor.process(thing)) return false;
    }
    return true;
  }

  @Nonnull
  @Override
  public Job<Void> submitToJobThread(@Nonnull Runnable action, Consumer<Future> onDoneCallback) {
    action.run();
    if (onDoneCallback != null) {
      onDoneCallback.consume(new Future() {
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
          return false;
        }

        @Override
        public boolean isCancelled() {
          return false;
        }

        @Override
        public boolean isDone() {
          return true;
        }

        @Override
        public Object get() {
          return null;
        }

        @Override
        public Object get(long timeout, @Nonnull TimeUnit unit) {
          return null;
        }
      });
    }
    return Job.NULL_JOB;
  }
}
