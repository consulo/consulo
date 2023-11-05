/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.progress.impl;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.progress.CoreProgressManager;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorListener;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.application.progress.ProgressManager;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import consulo.application.internal.ProgressIndicatorEx;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import java.util.Set;

/**
 * @author peter
 */
public class ProgressSuspender implements AutoCloseable {
  private static final Key<ProgressSuspender> PROGRESS_SUSPENDER = Key.create("PROGRESS_SUSPENDER");

  private final Object myLock = new Object();
  private static final Application ourApp = ApplicationManager.getApplication();
  @Nonnull
  private final String mySuspendedText;
  @Nullable
  private String myTempReason;
  private final ProgressSuspenderListener myPublisher;
  private volatile boolean mySuspended;
  private final CoreProgressManager.CheckCanceledHook myHook = this::freezeIfNeeded;
  private final Set<ProgressIndicator> myProgresses = ContainerUtil.newConcurrentSet();
  private boolean myClosed;

  private ProgressSuspender(@Nonnull ProgressIndicatorEx progress, @Nonnull String suspendedText) {
    mySuspendedText = suspendedText;
    assert progress.isRunning();
    assert ProgressIndicatorProvider.getGlobalProgressIndicator() == progress;
    myPublisher = ApplicationManager.getApplication().getMessageBus().syncPublisher(ProgressSuspenderListener.class);

    attachToProgress(progress);

    progress.addListener(new ProgressIndicatorListener() {
      @Override
      public void canceled() {
        resumeProcess();
      }
    });

    myPublisher.suspendableProgressAppeared(this);
  }

  @Override
  public void close() {
    synchronized (myLock) {
      myClosed = true;
      mySuspended = false;
      ((ProgressManagerImpl)ProgressManager.getInstance()).removeCheckCanceledHook(myHook);
    }
    for (ProgressIndicator progress : myProgresses) {
      ((UserDataHolder)progress).putUserData(PROGRESS_SUSPENDER, null);
    }
  }

  public static ProgressSuspender markSuspendable(@Nonnull ProgressIndicator indicator, @Nonnull String suspendedText) {
    return new ProgressSuspender((ProgressIndicatorEx)indicator, suspendedText);
  }

  @Nullable
  public static ProgressSuspender getSuspender(@Nonnull ProgressIndicator indicator) {
    return indicator instanceof UserDataHolder ? ((UserDataHolder)indicator).getUserData(PROGRESS_SUSPENDER) : null;
  }

  /**
   * Associates an additional progress indicator with this suspender, so that its {@code #checkCanceled} can later block the calling thread.
   */
  public void attachToProgress(@Nonnull ProgressIndicatorEx progress) {
    myProgresses.add(progress);
    ((UserDataHolder)progress).putUserData(PROGRESS_SUSPENDER, this);
  }

  @Nonnull
  public String getSuspendedText() {
    synchronized (myLock) {
      return myTempReason != null ? myTempReason : mySuspendedText;
    }
  }

  public boolean isSuspended() {
    return mySuspended;
  }

  /**
   * @param reason if provided, is displayed in the UI instead of suspended text passed into constructor until the progress is resumed
   */
  public void suspendProcess(@Nullable String reason) {
    synchronized (myLock) {
      if (mySuspended || myClosed) return;

      mySuspended = true;
      myTempReason = reason;

      ((ProgressManagerImpl)ProgressManager.getInstance()).addCheckCanceledHook(myHook);
    }

    myPublisher.suspendedStatusChanged(this);
  }

  public void resumeProcess() {
    synchronized (myLock) {
      if (!mySuspended) return;

      mySuspended = false;
      myTempReason = null;

      ((ProgressManagerImpl)ProgressManager.getInstance()).removeCheckCanceledHook(myHook);

      myLock.notifyAll();
    }

    myPublisher.suspendedStatusChanged(this);
  }

  private boolean freezeIfNeeded(@Nullable ProgressIndicator current) {
    if (current == null || !myProgresses.contains(current) || ourApp.isReadAccessAllowed()) {
      return false;
    }

    synchronized (myLock) {
      while (mySuspended) {
        try {
          myLock.wait(10000);
        }
        catch (InterruptedException ignore) {
        }
      }

      return true;
    }
  }

}
