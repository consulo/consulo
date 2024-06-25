// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.impl.internal.change;

import consulo.component.ProcessCanceledException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.awt.util.Alarm;
import jakarta.annotation.Nonnull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public final class ControlledCycle {
  private static final Logger LOG = Logger.getInstance(ControlledCycle.class);

  private final Alarm mySimpleAlarm;
  private final int myRefreshInterval;
  private final Runnable myRunnable;

  private final AtomicBoolean myActive;

  public ControlledCycle(@Nonnull Project project,
                         final Supplier<Boolean> callback,
                         @Nonnull final String name,
                         final int refreshInterval) {
    myRefreshInterval = refreshInterval;
    myActive = new AtomicBoolean(false);
    myRunnable = new Runnable() {
      boolean shouldBeContinued = true;

      @Override
      public void run() {
        if (!myActive.get() || project.isDisposed()) return;
        try {
          shouldBeContinued = callback.get();
        }
        catch (ProcessCanceledException e) {
          return;
        }
        catch (RuntimeException e) {
          LOG.info(e);
        }
        if (!shouldBeContinued) {
          myActive.set(false);
        }
        else {
          mySimpleAlarm.addRequest(myRunnable, myRefreshInterval);
        }
      }
    };
    mySimpleAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, project);
  }

  public void startIfNotStarted() {
    if (myActive.compareAndSet(false, true)) {
      mySimpleAlarm.addRequest(myRunnable, myRefreshInterval);
    }
  }

  public void stop() {
    myActive.set(false);
  }
}
