// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.impl.internal.memory.ui;

import consulo.disposer.Disposable;
import consulo.execution.debug.frame.XSuspendContext;
import consulo.ui.ex.awt.util.Alarm;
import jakarta.annotation.Nonnull;

public class SingleAlarmWithMutableDelay {
    private final Alarm myAlarm;
    private final Task myTask;

    private volatile int myDelayMillis;

    public SingleAlarmWithMutableDelay(@Nonnull Task task, @Nonnull Disposable parentDisposable) {
        myAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, parentDisposable);
        myTask = task;
    }

    public void setDelay(int millis) {
        myDelayMillis = millis;
    }

    public void cancelAndRequest(@Nonnull XSuspendContext suspendContext) {
        cancelAndAddRequest(suspendContext, myDelayMillis);
    }

    public void cancelAndRequestImmediate(@Nonnull XSuspendContext suspendContext) {
        cancelAndAddRequest(suspendContext, 0);
    }

    public void cancelAllRequests() {
        myAlarm.cancelAllRequests();
    }

    private void cancelAndAddRequest(@Nonnull XSuspendContext suspendContext, int delayMillis) {
        if (!myAlarm.isDisposed()) {
            cancelAllRequests();
            myAlarm.addRequest(() -> myTask.run(suspendContext), delayMillis);
        }
    }

    @FunctionalInterface
    public interface Task {
        void run(@Nonnull XSuspendContext suspendContext);
    }
}
