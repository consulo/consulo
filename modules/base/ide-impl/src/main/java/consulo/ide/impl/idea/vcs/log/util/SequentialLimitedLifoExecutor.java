/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.vcs.log.util;

import consulo.disposer.Disposable;
import consulo.util.lang.function.ThrowableConsumer;
import consulo.application.util.concurrent.QueueProcessor;
import consulo.disposer.Disposer;

import jakarta.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * Queue with a limited number of tasks, and with higher priority for new tasks, than for older ones.
 */
public class SequentialLimitedLifoExecutor<Task> implements Disposable {

  private final int myMaxTasks;
  @Nonnull
  private final ThrowableConsumer<Task, ? extends Throwable> myLoadProcess;
  @Nonnull
  private final QueueProcessor<Task> myLoader;

  public SequentialLimitedLifoExecutor(Disposable parentDisposable, int maxTasks,
                                       @Nonnull ThrowableConsumer<Task, ? extends Throwable> loadProcess) {
    myMaxTasks = maxTasks;
    myLoadProcess = loadProcess;
    myLoader = new QueueProcessor<>(new DetailsLoadingTask());
    Disposer.register(parentDisposable, this);
  }

  public void queue(Task task) {
    myLoader.addFirst(task);
  }

  public void clear() {
    myLoader.clear();
  }

  @Override
  public void dispose() {
    clear();
  }

  private class DetailsLoadingTask implements Consumer<Task> {
    @Override
    public void accept(final Task task) {
      try {
        myLoader.dismissLastTasks(myMaxTasks);
        myLoadProcess.consume(task);
      }
      catch (Throwable e) {
        throw new RuntimeException(e); // todo
      }
    }
  }
}
