// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt.util;

import consulo.annotation.DeprecationInfo;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.ui.ModalityState;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@Deprecated
@DeprecationInfo("Use UIAccessScheduler")
public final class SingleAlarm extends Alarm {
  private final Runnable task;
  private final int delay;
  private final ModalityState myModalityState;

  public SingleAlarm(@Nonnull Runnable task, int delay) {
    this(task, delay, ThreadToUse.SWING_THREAD, Application.get().getNoneModalityState(), null);
  }

  public SingleAlarm(@Nonnull Runnable task, int delay, @Nonnull Disposable parentDisposable) {
    this(task, delay, Alarm.ThreadToUse.SWING_THREAD, parentDisposable);
  }

  public SingleAlarm(@Nonnull Runnable task, int delay, @Nonnull ModalityState modalityState, @Nonnull Disposable parentDisposable) {
    this(task, delay, Alarm.ThreadToUse.SWING_THREAD, modalityState, parentDisposable);
  }

  public SingleAlarm(@Nonnull Runnable task, int delay, @Nonnull ThreadToUse threadToUse, @Nonnull Disposable parentDisposable) {
    this(task, delay, threadToUse, threadToUse == ThreadToUse.SWING_THREAD ? Application.get().getNoneModalityState() : null, parentDisposable);
  }

  private SingleAlarm(@Nonnull Runnable task, int delay, @Nonnull ThreadToUse threadToUse, ModalityState modalityState, @Nullable Disposable parentDisposable) {
    super(threadToUse, parentDisposable);

    this.task = task;
    this.delay = delay;
    if (threadToUse == ThreadToUse.SWING_THREAD && modalityState == null) {
      throw new IllegalArgumentException("modalityState must be not null if threadToUse == ThreadToUse.SWING_THREAD");
    }
    myModalityState = modalityState;
  }

  public void request() {
    request(false);
  }

  public void request(boolean forceRun) {
    if (isEmpty() && !isDisposed()) {
      addRequest(forceRun ? 0 : delay);
    }
  }

  public void cancel() {
    cancelAllRequests();
  }

  public void cancelAndRequest() {
    if (!isDisposed()) {
      cancel();
      addRequest(delay);
    }
  }

  private void addRequest(int delay) {
    _addRequest(task, delay, myModalityState);
  }
}