/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.change;

import consulo.component.ProcessCanceledException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.awt.util.Alarm;
import jakarta.annotation.Nonnull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

public class ControlledCycle {
  private static final Logger LOG = Logger.getInstance(ControlledCycle.class);

  private final Alarm mySimpleAlarm;
  // this interval is also to check for not initialized paths, so it is rather small
  private static final int ourRefreshInterval = 10000;
  private int myRefreshInterval;
  private final Runnable myRunnable;

  private final AtomicBoolean myActive;

  public ControlledCycle(final Project project, final BooleanSupplier callback, @Nonnull final String name, final int refreshInterval) {
    myRefreshInterval = (refreshInterval <= 0) ? ourRefreshInterval : refreshInterval;
    myActive = new AtomicBoolean(false);
    myRunnable = new Runnable() {
      boolean shouldBeContinued = true;
      @Override
      public void run() {
        if (! myActive.get() || project.isDisposed()) return;
        try {
          shouldBeContinued = callback.getAsBoolean();
        } catch (ProcessCanceledException e) {
          return;
        } catch (RuntimeException e) {
          LOG.info(e);
        }
        if (! shouldBeContinued) {
          myActive.set(false);
        } else {
          mySimpleAlarm.addRequest(myRunnable, myRefreshInterval);
        }
      }
    };
    mySimpleAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, project);
  }

  public boolean startIfNotStarted(final int refreshInterval) {
    final boolean refreshIntervalChanged = (refreshInterval > 0) && refreshInterval != myRefreshInterval;
    if (refreshIntervalChanged) {
      mySimpleAlarm.cancelAllRequests();
    }
    if (refreshInterval > 0) {
      myRefreshInterval = refreshInterval;
    }

    final boolean wasSet = myActive.compareAndSet(false, true);
    if (wasSet || refreshIntervalChanged) {
      mySimpleAlarm.addRequest(myRunnable, myRefreshInterval);
    }
    return wasSet;
  }

  public void stop() {
    myActive.set(false);
  }
}
