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
package consulo.project.ui.wm;

import consulo.application.concurrent.ApplicationConcurrency;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2023-11-11
 */
public final class MergingQueue<V> implements Disposable {
  private final ApplicationConcurrency myApplicationConcurrency;
  private final Project myProject;
  private final int myTickInMilliseconds;
  private final Consumer<V> myValueConsumer;

  private Future<?> myUpdateFuture = CompletableFuture.completedFuture(null);
  private final Queue<V> myUpdateQueue = new ConcurrentLinkedDeque<>();

  public MergingQueue(ApplicationConcurrency applicationConcurrency,
                      Project project,
                      int tickInMilliseconds,
                      Disposable parentDisposable,
                      Consumer<V> valueConsumer) {
    myApplicationConcurrency = applicationConcurrency;
    myProject = project;
    myTickInMilliseconds = tickInMilliseconds;
    myValueConsumer = valueConsumer;

    Disposer.register(parentDisposable, this);
  }

  public void queue(V value) {
    myUpdateQueue.add(value);

    if (myUpdateFuture.isCancelled() || myUpdateFuture.isDone()) {
      restart();
    }
  }

  private void restart() {
    myApplicationConcurrency.getScheduledExecutorService().schedule(() -> {
      if (myProject.isDisposed()) return;

      List<V> collectedValues = new ArrayList<>(myUpdateQueue.size());

      V it = null;
      while ((it = myUpdateQueue.poll()) != null) {
        collectedValues.add(it);
      }

      if (collectedValues.isEmpty()) {
        return;
      }

      myProject.getUIAccess().give(() -> {
        for (V collectedValue : collectedValues) {
          myValueConsumer.accept(collectedValue);
        }
      });
    }, myTickInMilliseconds, TimeUnit.MILLISECONDS);
  }

  public void dispose() {
    myUpdateQueue.clear();
    myUpdateFuture.cancel(false);
  }
}
