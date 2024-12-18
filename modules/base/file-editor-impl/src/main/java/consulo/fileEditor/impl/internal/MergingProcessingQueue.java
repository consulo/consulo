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
package consulo.fileEditor.impl.internal;

import consulo.application.concurrent.ApplicationConcurrency;
import consulo.project.Project;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;

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
 * @since 15/09/2023
 */
public abstract class MergingProcessingQueue<K, V> {
  private final ApplicationConcurrency myApplicationConcurrency;
  private final Project myProject;
  private final int myTickInMiliseconds;

  private Future<?> myUpdateFuture = CompletableFuture.completedFuture(null);
  private final Queue<K> myUpdateQeueu = new ConcurrentLinkedDeque<>();

  public MergingProcessingQueue(ApplicationConcurrency applicationConcurrency,
                                Project project,
                                int tickInMiliseconds) {
    myApplicationConcurrency = applicationConcurrency;
    myProject = project;
    myTickInMiliseconds = tickInMiliseconds;
  }

  public void queueAdd(K key) {
    myUpdateQeueu.add(key);

    if (myUpdateFuture.state() != Future.State.RUNNING) {
      restart();
    }
  }

  private void restart() {
    myUpdateFuture = myApplicationConcurrency.getScheduledExecutorService().schedule(() -> {
      if (myProject.isDisposed()) return;

      List<Pair<K, V>> collectedValues = new ArrayList<>(myUpdateQeueu.size());

      K it = null;
      while ((it = myUpdateQeueu.poll()) != null) {
        final K finalIt = it;

        calculateValue(myProject, it, v -> collectedValues.add(Pair.pair(finalIt, v)));
      }

      if (collectedValues.isEmpty()) {
        return;
      }

      myProject.getUIAccess().give(() -> {
        for (Pair<K, V> data : collectedValues) {
          updateValueInsideUI(myProject, data.getKey(), data.getValue());
        }
      });
    }, myTickInMiliseconds, TimeUnit.MILLISECONDS);
  }

  public void dispose() {
    myUpdateQeueu.clear();
    myUpdateFuture.cancel(false);
  }

  protected abstract void calculateValue(@Nonnull Project project, @Nonnull K key, @Nonnull Consumer<V> consumer);

  protected abstract void updateValueInsideUI(@Nonnull Project project, @Nonnull K key, @Nonnull V value);
}
