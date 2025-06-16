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
package consulo.ide.impl.idea.vcs.log.data;

import consulo.application.Application;
import consulo.application.internal.AbstractProgressIndicatorBase;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class VcsLogProgress implements Disposable {
  @Nonnull
  private final Object myLock = new Object();
  @Nonnull
  private final List<ProgressListener> myListeners = new ArrayList<>();
  @Nonnull
  private Set<ProgressIndicator> myTasksWithVisibleProgress = new HashSet<>();
  @Nonnull
  private Set<ProgressIndicator> myTasksWithSilentProgress = new HashSet<>();

  @Nonnull
  public ProgressIndicator createProgressIndicator() {
    return createProgressIndicator(true);
  }

  public ProgressIndicator createProgressIndicator(boolean visible) {
    if (Application.get().isHeadlessEnvironment()) {
      return new EmptyProgressIndicator();
    }
    return new VcsLogProgressIndicator(visible);
  }

  public void addProgressIndicatorListener(@Nonnull ProgressListener listener, @Nullable Disposable parentDisposable) {
    synchronized (myLock) {
      myListeners.add(listener);
      if (parentDisposable != null) {
        Disposer.register(parentDisposable, () -> removeProgressIndicatorListener(listener));
      }
      if (isRunning()) Application.get().invokeLater(listener::progressStarted);
    }
  }

  public void removeProgressIndicatorListener(@Nonnull ProgressListener listener) {
    synchronized (myLock) {
      myListeners.remove(listener);
    }
  }

  public boolean isRunning() {
    synchronized (myLock) {
      return !myTasksWithVisibleProgress.isEmpty();
    }
  }

  private void started(@Nonnull VcsLogProgressIndicator indicator) {
    synchronized (myLock) {
      if (indicator.isVisible()) {
        myTasksWithVisibleProgress.add(indicator);
        if (myTasksWithVisibleProgress.size() == 1) fireNotification(ProgressListener::progressStarted);
      }
      else {
        myTasksWithSilentProgress.add(indicator);
      }
    }
  }

  private void stopped(@Nonnull VcsLogProgressIndicator indicator) {
    synchronized (myLock) {
      if (indicator.isVisible()) {
        myTasksWithVisibleProgress.remove(indicator);
        if (myTasksWithVisibleProgress.isEmpty()) fireNotification(ProgressListener::progressStopped);
      }
      else {
        myTasksWithSilentProgress.remove(indicator);
      }
    }
  }

  private void fireNotification(@Nonnull Consumer<ProgressListener> action) {
    synchronized (myLock) {
      List<ProgressListener> list = ContainerUtil.newArrayList(myListeners);
      Application.get().invokeLater(() -> list.forEach(action));
    }
  }

  @Override
  public void dispose() {
    synchronized (myLock) {
      for (ProgressIndicator indicator : myTasksWithVisibleProgress) {
        indicator.cancel();
      }
      for (ProgressIndicator indicator : myTasksWithSilentProgress) {
        indicator.cancel();
      }
    }
  }

  private class VcsLogProgressIndicator extends AbstractProgressIndicatorBase {
    private final boolean myVisible;

    private VcsLogProgressIndicator(boolean visible) {
      myVisible = visible;
    }

    @Override
    public synchronized void start() {
      super.start();
      started(this);
    }

    @Override
    public synchronized void stop() {
      super.stop();
      stopped(this);
    }

    public boolean isVisible() {
      return myVisible;
    }
  }

  public interface ProgressListener {
    void progressStarted();

    void progressStopped();
  }
}
