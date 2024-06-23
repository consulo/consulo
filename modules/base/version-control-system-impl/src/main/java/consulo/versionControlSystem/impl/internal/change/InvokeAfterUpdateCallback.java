// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.change;

import consulo.application.ApplicationManager;
import consulo.application.impl.internal.progress.ProgressIndicatorUtils;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.application.util.Semaphore;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.change.InvokeAfterUpdateMode;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nullable;

final class InvokeAfterUpdateCallback {
  private final static Logger LOG = Logger.getInstance(InvokeAfterUpdateCallback.class);

  interface Callback {
    void startProgress();

    void endProgress();

    void handleStoppedQueue();
  }

  @Nonnull
  public static Callback create(@Nonnull Project project,
                                @Nonnull InvokeAfterUpdateMode mode,
                                @Nonnull Runnable afterUpdate,
                                @Nullable @Nls String title) {
    if (mode.isSilent()) {
      return new SilentCallback(project, afterUpdate, mode.isCallbackOnAwt());
    }
    else {
      return new ProgressCallback(project, afterUpdate, mode.isSynchronous(), mode.isCancellable(), title);
    }
  }

  private abstract static class CallbackBase implements Callback {
    protected final Project myProject;
    private final Runnable myAfterUpdate;

    CallbackBase(@Nonnull Project project, @Nonnull Runnable afterUpdate) {
      myProject = project;
      myAfterUpdate = afterUpdate;
    }

    protected final void invokeCallback() {
      LOG.debug("changes update finished for project " + myProject.getName());
      if (!myProject.isDisposed()) myAfterUpdate.run();
    }
  }

  private static class SilentCallback extends CallbackBase {
    private final boolean myCallbackOnAwt;

    SilentCallback(@Nonnull Project project,
                   @Nonnull Runnable afterUpdate,
                   boolean callbackOnAwt) {
      super(project, afterUpdate);
      myCallbackOnAwt = callbackOnAwt;
    }

    @Override
    public void startProgress() {
    }

    @Override
    public void endProgress() {
      scheduleCallback();
    }

    @Override
    public void handleStoppedQueue() {
      scheduleCallback();
    }

    private void scheduleCallback() {
      if (myCallbackOnAwt) {
        ApplicationManager.getApplication().invokeLater(this::invokeCallback);
      }
      else {
        ApplicationManager.getApplication().executeOnPooledThread(this::invokeCallback);
      }
    }
  }

  private static class ProgressCallback extends CallbackBase {
    private final boolean mySynchronous;
    private final boolean myCanBeCancelled;
    private final @Nls String myTitle;

    @Nonnull
    private final Semaphore mySemaphore = new Semaphore(1);

    ProgressCallback(@Nonnull Project project,
                     @Nonnull Runnable afterUpdate,
                     boolean synchronous,
                     boolean canBeCancelled,
                     @Nullable @Nls String title) {
      super(project, afterUpdate);
      mySynchronous = synchronous;
      myCanBeCancelled = canBeCancelled;
      myTitle = title;
    }

    @Override
    public void startProgress() {
      if (mySynchronous) {
        String dialogTitle = VcsBundle.message("change.list.manager.wait.lists.synchronization.modal",
                                               myTitle, myTitle != null ? 1 : 0);
        new ModalWaiter(myProject, dialogTitle, myCanBeCancelled).queue();
      }
      else {
        String progressTitle = VcsBundle.message("change.list.manager.wait.lists.synchronization.background",
                                                 myTitle, myTitle != null ? 1 : 0);
        new BackgroundableWaiter(myProject, progressTitle, myCanBeCancelled).queue();
      }
    }

    @Override
    public void endProgress() {
      mySemaphore.up();
    }

    @Override
    public void handleStoppedQueue() {
      ApplicationManager.getApplication().invokeLater(this::invokeCallback);
    }

    private void awaitSemaphore(@Nonnull ProgressIndicator indicator) {
      indicator.setIndeterminate(true);
      indicator.setText2(VcsBundle.message("commit.wait.util.synched.text"));
      ProgressIndicatorUtils.awaitWithCheckCanceled(mySemaphore, indicator);
    }

    private class ModalWaiter extends Task.Modal {
      ModalWaiter(@Nonnull Project project, @Nonnull String title, boolean canBeCancelled) {
        super(project, title, canBeCancelled);
        setCancelText(VcsBundle.message("button.skip"));
      }

      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        awaitSemaphore(indicator);
      }

      @Override
      public void onFinished() {
        invokeCallback();
      }
    }

    private class BackgroundableWaiter extends Task.Backgroundable {
      BackgroundableWaiter(@Nonnull Project project, @Nonnull String title, boolean canBeCancelled) {
        super(project, title, canBeCancelled);
      }

      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        awaitSemaphore(indicator);
      }

      @Override
      public void onSuccess() {
        invokeCallback();
      }

      @Override
      public boolean isHeadless() {
        return false;
      }
    }
  }
}
