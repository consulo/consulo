/*
 * Copyright 2013-2026 consulo.io
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
package consulo.application.util;

import consulo.application.concurrent.ApplicationConcurrency;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 2023-09-15
 */
public abstract class MergingProcessingQueue<K> {
  private final ApplicationConcurrency myApplicationConcurrency;
  private final int myTickInMillis;

  private Future<?> myUpdateFuture = CompletableFuture.completedFuture(null);
  private final Queue<K> myUpdateQueue = new ConcurrentLinkedDeque<>();

  public MergingProcessingQueue(ApplicationConcurrency applicationConcurrency, int tickInMillis) {
    myApplicationConcurrency = applicationConcurrency;
    myTickInMillis = tickInMillis;
  }

  public void queueAdd(K key) {
    myUpdateQueue.add(key);

    if (myUpdateFuture.state() != Future.State.RUNNING) {
      restart();
    }
  }

  private void restart() {
    myUpdateFuture = myApplicationConcurrency.getScheduledExecutorService().schedule(() -> {
      K it = null;
      while ((it = myUpdateQueue.poll()) != null) {
        process(it);
      }
    }, myTickInMillis, TimeUnit.MILLISECONDS);
  }

  public void dispose() {
    myUpdateQueue.clear();
    myUpdateFuture.cancel(false);
  }

  protected abstract void process(K key);
}
