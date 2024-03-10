// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.vcs.impl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.impl.internal.progress.CoreProgressManager;
import consulo.application.impl.internal.progress.StandardProgressIndicatorBase;
import consulo.application.internal.BackgroundTaskUtil;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.application.util.concurrent.QueueProcessor;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.TimeoutUtil;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.VcsInitObject;
import consulo.versionControlSystem.VcsStartupActivity;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Predicate;

@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public final class VcsInitialization {
  private static final Logger LOG = Logger.getInstance(VcsInitialization.class);

  @Nonnull
  public static VcsInitialization getInstance(Project project) {
    return project.getInstance(VcsInitialization.class);
  }

  private final Object myLock = new Object();

  @Nonnull
  private final Project myProject;

  private enum Status {
    PENDING,
    RUNNING_INIT,
    RUNNING_POST,
    FINISHED

  }

  // guarded by myLock
  private Status myStatus = Status.PENDING;
  private final List<VcsStartupActivity> myInitActivities = new ArrayList<>();

  private final List<VcsStartupActivity> myPostActivities = new ArrayList<>();
  private volatile Future<?> myFuture;

  private final ProgressIndicator myIndicator = new StandardProgressIndicatorBase();

  @Inject
  VcsInitialization(@Nonnull Project project) {
    myProject = project;
  }

  protected void startInitialization() {
    myFuture = ((CoreProgressManager)ProgressManager.getInstance()).runProcessWithProgressAsynchronously(new Task.Backgroundable(myProject,
                                                                                                                                 VcsBundle.message(
                                                                                                                                   "impl.vcs.initialization")) {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        execute();
      }
    }, myIndicator, null);
  }

  void add(@Nonnull VcsInitObject vcsInitObject, @Nonnull Runnable runnable) {
    if (myProject.isDefault()) return;
    boolean wasScheduled = scheduleActivity(vcsInitObject, runnable);
    if (!wasScheduled) {
      BackgroundTaskUtil.executeOnPooledThread(myProject, runnable);
    }
  }

  private boolean scheduleActivity(@Nonnull VcsInitObject vcsInitObject, @Nonnull Runnable runnable) {
    synchronized (myLock) {
      ProxyVcsStartupActivity activity = new ProxyVcsStartupActivity(vcsInitObject, runnable);
      if (isInitActivity(activity)) {
        if (myStatus == Status.PENDING) {
          myInitActivities.add(activity);
          return true;
        }
        else {
          LOG.warn(String.format("scheduling late initialization: %s", activity));
          return false;
        }
      }
      else {
        if (myStatus == Status.PENDING || myStatus == Status.RUNNING_INIT) {
          myPostActivities.add(activity);
          return true;
        }
        else {
          if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("scheduling late post activity: %s", activity));
          }
          return false;
        }
      }
    }
  }

  private void execute() {
    LOG.assertTrue(!myProject.isDefault());
    try {
      runInitStep(Status.PENDING, Status.RUNNING_INIT, it -> isInitActivity(it), myInitActivities);
      runInitStep(Status.RUNNING_INIT, Status.RUNNING_POST, it -> !isInitActivity(it), myPostActivities);
    }
    finally {
      synchronized (myLock) {
        myStatus = Status.FINISHED;
      }
    }
  }

  private void runInitStep(@Nonnull Status current,
                           @Nonnull Status next,
                           @Nonnull Predicate<VcsStartupActivity> extensionFilter,
                           @Nonnull List<VcsStartupActivity> pendingActivities) {
    List<VcsStartupActivity> extensionList = myProject.getExtensionList(VcsStartupActivity.class);
    List<VcsStartupActivity> epActivities = ContainerUtil.filter(extensionList, extensionFilter);

    List<VcsStartupActivity> activities = new ArrayList<>();
    synchronized (myLock) {
      assert myStatus == current;
      myStatus = next;

      activities.addAll(epActivities);
      activities.addAll(pendingActivities);
      pendingActivities.clear();
    }

    runActivities(activities);
  }

  private void runActivities(@Nonnull List<VcsStartupActivity> activities) {
    Future<?> future = myFuture;
    if (future != null && future.isCancelled()) return;

    Collections.sort(activities, Comparator.comparingInt(VcsStartupActivity::getOrder));

    for (VcsStartupActivity activity : activities) {
      ProgressManager.checkCanceled();
      if (LOG.isDebugEnabled()) {
        LOG.debug(String.format("running activity: %s", activity));
      }

      QueueProcessor.runSafely(activity::runActivity);
    }
  }

  protected void cancelBackgroundInitialization() {
    myIndicator.cancel();

    // do not leave VCS initialization run in background when the project is closed
    Future<?> future = myFuture;
    LOG.debug(String.format("cancelBackgroundInitialization() future=%s from %s with write access=%s",
                            future,
                            Thread.currentThread(),
                            myProject.getApplication().isWriteAccessAllowed()));
    if (future != null) {
      future.cancel(false);
      if (myProject.getApplication().isWriteAccessAllowed()) {
        // dispose happens without prior project close (most likely light project case in tests)
        // get out of write action and wait there
        SwingUtilities.invokeLater(this::waitNotRunning);
      }
      else {
        waitNotRunning();
      }
    }
  }

  private void waitNotRunning() {
    boolean success = waitFor(status -> status == Status.PENDING || status == Status.FINISHED);
    if (!success) {
      LOG.warn("Failed to wait for VCS initialization cancellation for project " + myProject, new Throwable());
    }
  }

  @TestOnly
  void waitFinished() {
    boolean success = waitFor(status -> status == Status.FINISHED);
    if (!success) {
      LOG.error("Failed to wait for VCS initialization completion for project " + myProject, new Throwable());
    }
  }

  private boolean waitFor(@Nonnull Predicate<? super Status> predicate) {
    if (myProject.isDefault()) throw new IllegalArgumentException();
    // have to wait for task completion to avoid running it in background for closed project
    long start = System.currentTimeMillis();
    while (System.currentTimeMillis() < start + 10000) {
      synchronized (myLock) {
        if (predicate.test(myStatus)) {
          return true;
        }
      }
      TimeoutUtil.sleep(10);
    }
    return false;
  }

  private static boolean isInitActivity(@Nonnull VcsStartupActivity activity) {
    return activity.getOrder() < VcsInitObject.AFTER_COMMON.getOrder();
  }

  private static final class ProxyVcsStartupActivity implements VcsStartupActivity {
    @Nonnull
    private final Runnable myRunnable;
    private final int myOrder;

    private ProxyVcsStartupActivity(@Nonnull VcsInitObject vcsInitObject, @Nonnull Runnable runnable) {
      myOrder = vcsInitObject.getOrder();
      myRunnable = runnable;
    }

    @Override
    public void runActivity() {
      myRunnable.run();
    }

    @Override
    public int getOrder() {
      return myOrder;
    }

    @Override
    public String toString() {
      return String.format("ProxyVcsStartupActivity{runnable=%s, order=%s}", myRunnable, myOrder); //NON-NLS
    }
  }
}
