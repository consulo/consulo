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
package consulo.process.util;

import consulo.logging.Logger;
import consulo.process.TaskExecutor;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import java.util.concurrent.*;
import java.util.function.Consumer;

public class ProcessWaitFor {
  private static final Logger LOG = Logger.getInstance(ProcessWaitFor.class);

  private final Future<?> myWaitForThreadFuture;
  private final BlockingQueue<Consumer<Integer>> myTerminationCallback = new ArrayBlockingQueue<>(1);

  public ProcessWaitFor(@Nonnull final Process process, @Nonnull TaskExecutor executor, @Nonnull final String presentableName) {
    myWaitForThreadFuture = executor.executeTask(new Runnable() {
      @Override
      public void run() {
        String oldThreadName = Thread.currentThread().getName();
        if (!StringUtil.isEmptyOrSpaces(presentableName)) {
          Thread.currentThread().setName("ProcessWaitFor: " + presentableName);
        }
        int exitCode = 0;
        try {
          while (true) {
            try {
              exitCode = process.waitFor();
              break;
            }
            catch (InterruptedException e) {
              LOG.debug(e);
            }
          }
        }
        finally {
          try {
            myTerminationCallback.take().accept(exitCode);
          }
          catch (InterruptedException e) {
            LOG.info(e);
          }
          finally {
            Thread.currentThread().setName(oldThreadName);
          }
        }
      }
    });
  }

  public void detach() {
    myWaitForThreadFuture.cancel(true);
  }

  public void setTerminationCallback(@Nonnull Consumer<Integer> r) {
    myTerminationCallback.offer(r);
  }

  public void waitFor() throws InterruptedException {
    try {
      myWaitForThreadFuture.get();
    }
    catch (ExecutionException e) {
      LOG.error(e);
    }
    catch (CancellationException ignored) {
    }
  }

  public boolean waitFor(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
    try {
      myWaitForThreadFuture.get(timeout, unit);
    }
    catch (ExecutionException e) {
      LOG.error(e);
    }
    catch (CancellationException | TimeoutException ignored) {
    }

    return myWaitForThreadFuture.isDone();
  }

}