/*
 * Copyright 2013-2023 consulo.io
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
package consulo.ui.impl;

import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.UIAccessScheduler;

import java.util.List;
import java.util.concurrent.*;

/**
 * @author VISTALL
 * @since 14/09/2023
 */
public abstract class BaseUIAccessScheduler extends AbstractExecutorService implements UIAccessScheduler {
  private final ScheduledExecutorService myScheduledExecutorService;

  public BaseUIAccessScheduler( ScheduledExecutorService scheduledExecutorService) {
    myScheduledExecutorService = scheduledExecutorService;
  }

  
  @Override
  public ScheduledFuture<?> schedule(Runnable command, ModalityState modalityState, long delay, TimeUnit unit) {
    return myScheduledExecutorService.schedule(() -> runWithModalityState(command, modalityState), delay, unit);
  }

  public abstract void runWithModalityState(Runnable runnable, ModalityState modalityState);

  
  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    return myScheduledExecutorService.schedule(wrap(command), delay, unit);
  }

  
  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable,
                                         long delay,
                                         TimeUnit unit) {
    return myScheduledExecutorService.schedule(() -> uiAccess().<V>giveAndWaitIfNeed(() -> {
      try {
        return callable.call();
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }), delay, unit);
  }

  
  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
                                                long initialDelay,
                                                long period,
                                                TimeUnit unit) {
    return myScheduledExecutorService.scheduleAtFixedRate(wrap(command), initialDelay, period, unit);
  }

  
  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
                                                   long initialDelay,
                                                   long delay,
                                                   TimeUnit unit) {
    return myScheduledExecutorService.scheduleWithFixedDelay(wrap(command), initialDelay, delay, unit);
  }

  private Runnable wrap(Runnable command) {
    return () -> uiAccess().execute(command);
  }

  @Override
  public void execute(Runnable command) {
    uiAccess().execute(command);
  }

  @Override
  public void shutdown() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Runnable> shutdownNow() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isShutdown() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isTerminated() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    throw new UnsupportedOperationException();
  }

  
  protected abstract UIAccess uiAccess();
}
