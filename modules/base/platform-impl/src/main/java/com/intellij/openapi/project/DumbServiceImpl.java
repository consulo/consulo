/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.project;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.PowerSaveMode;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.progress.impl.ProgressSuspender;
import com.intellij.openapi.progress.util.AbstractProgressIndicatorExBase;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.wm.AppIconScheme;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.ex.ProgressIndicatorEx;
import com.intellij.openapi.wm.ex.StatusBarEx;
import com.intellij.ui.AppIcon;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.concurrency.Semaphore;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Queue;
import com.intellij.util.io.storage.HeavyProcessLatch;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import consulo.annotations.RequiredWriteAction;
import consulo.application.AccessRule;
import consulo.application.ApplicationProperties;
import consulo.ui.RequiredUIAccess;
import consulo.ui.UIAccess;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class DumbServiceImpl extends DumbService implements Disposable, ModificationTracker {
  private static final Logger LOG = Logger.getInstance(DumbServiceImpl.class);

  private final AtomicReference<State> myState = new AtomicReference<>(State.SMART);
  private volatile Throwable myDumbStart;
  private final DumbModeListener myPublisher;
  private long myModificationCount;
  private final Set<Object> myQueuedEquivalences = new HashSet<>();
  private final Queue<DumbModeTask> myUpdatesQueue = new Queue<>(5);

  /**
   * Per-task progress indicators. Modified from EDT only.
   * The task is removed from this map after it's finished or when the project is disposed.
   */
  private final Map<DumbModeTask, ProgressIndicatorEx> myProgresses = ContainerUtil.newConcurrentMap();

  private final Queue<Runnable> myRunWhenSmartQueue = new Queue<>(5);
  private final Application myApplication;
  private final Project myProject;
  private final ThreadLocal<Integer> myAlternativeResolution = new ThreadLocal<>();
  private final StartupManager myStartupManager;

  @Inject
  public DumbServiceImpl(Application application, Project project, StartupManager startupManager) {
    myApplication = application;
    myProject = project;
    myPublisher = project.getMessageBus().syncPublisher(DUMB_MODE);
    myStartupManager = startupManager;
  }

  @Override
  public void cancelTask(@Nonnull DumbModeTask task) {
    if (ApplicationProperties.isInSandbox()) LOG.info("cancel " + task);
    ProgressIndicatorEx indicator = myProgresses.get(task);
    if (indicator != null) {
      indicator.cancel();
    }
  }

  @Override
  @RequiredWriteAction
  public void dispose() {
    myApplication.assertWriteAccessAllowed();
    myUpdatesQueue.clear();
    myQueuedEquivalences.clear();
    myRunWhenSmartQueue.clear();
    for (DumbModeTask task : new ArrayList<>(myProgresses.keySet())) {
      cancelTask(task);
      Disposer.dispose(task);
    }
  }

  @Override
  public Project getProject() {
    return myProject;
  }

  @Override
  public boolean isAlternativeResolveEnabled() {
    return myAlternativeResolution.get() != null;
  }

  @Override
  public void setAlternativeResolveEnabled(boolean enabled) {
    Integer oldValue = myAlternativeResolution.get();
    int newValue = (oldValue == null ? 0 : oldValue) + (enabled ? 1 : -1);
    assert newValue >= 0 : "Non-paired alternative resolution mode";
    myAlternativeResolution.set(newValue == 0 ? null : newValue);
  }

  @Override
  public ModificationTracker getModificationTracker() {
    return this;
  }

  @Override
  public boolean isDumb() {
    return myState.get() != State.SMART;
  }

  @TestOnly
  @RequiredWriteAction
  public void setDumb(boolean dumb, UIAccess uiAccess) {
    if (dumb) {
      myState.set(State.RUNNING_DUMB_TASKS);
      myPublisher.enteredDumbMode(uiAccess);
    }
    else {
      myState.set(State.WAITING_FOR_FINISH);
      updateFinished(uiAccess);
    }
  }

  @Override
  public void runWhenSmart(@Nonnull Runnable runnable) {
    myStartupManager.runWhenProjectIsInitialized(() -> {
      synchronized (myRunWhenSmartQueue) {
        if (isDumb()) {
          myRunWhenSmartQueue.addLast(runnable);
          return;
        }
      }

      runnable.run();
    });
  }

  @Override
  public void queueTask(@Nonnull DumbModeTask task) {
    if (LOG.isDebugEnabled()) LOG.debug("Scheduling task " + task);
    LOG.assertTrue(!myProject.isDefault(), "No indexing tasks should be created for default project: " + task);
    if (myApplication.isUnitTestMode() || myApplication.isHeadlessEnvironment()) {
      runTaskSynchronously(task);
    }
    else {
      queueAsynchronousTask(task);
    }
  }

  private static void runTaskSynchronously(@Nonnull DumbModeTask task) {
    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (indicator == null) indicator = new EmptyProgressIndicator();

    indicator.pushState();
    try (AccessToken ignored = HeavyProcessLatch.INSTANCE.processStarted("Performing indexing task")) {
      task.performInDumbMode(indicator);
    }
    finally {
      indicator.popState();
      Disposer.dispose(task);
    }
  }

  @VisibleForTesting
  void queueAsynchronousTask(@Nonnull DumbModeTask task) {
    Throwable trace = new Throwable(); // please report exceptions here to peter
    queueTaskOnEdt(task, trace);
  }

  private void queueTaskOnEdt(@Nonnull DumbModeTask task, @Nonnull Throwable trace) {
    if (!addTaskToQueue(task)) return;

    if (myState.get() == State.SMART || myState.get() == State.WAITING_FOR_FINISH) {
      enterDumbMode(myApplication.getLastUIAccess(), trace).doWhenProcessed(() -> {
        myApplication.invokeLater(() -> startBackgroundProcess(UIAccess.current()), myProject.getDisposed());
      });
    }
  }

  private boolean addTaskToQueue(@Nonnull DumbModeTask task) {
    if (!myQueuedEquivalences.add(task.getEquivalenceObject())) {
      Disposer.dispose(task);
      return false;
    }

    myProgresses.put(task, new ProgressIndicatorBase());
    Disposer.register(task, () -> {
      myProgresses.remove(task);
    });
    myUpdatesQueue.addLast(task);
    return true;
  }

  @Nonnull
  private AsyncResult<Void> enterDumbMode(@Nonnull UIAccess uiAccess, @Nonnull Throwable trace) {
    AsyncResult<Void> result = new AsyncResult<>();

    boolean wasSmart = !isDumb();
    AccessRule.writeAsync(() -> {
      synchronized (myRunWhenSmartQueue) {
        myState.set(State.SCHEDULED_TASKS);
      }
      myDumbStart = trace;
      myModificationCount++;
    }).doWhenProcessed(() -> {
      if (wasSmart) {
        myPublisher.enteredDumbMode(uiAccess);
        result.setDone();
      }
    });

    return result;
  }

  private void queueUpdateFinished(UIAccess uiAccess) {
    if (myState.compareAndSet(State.RUNNING_DUMB_TASKS, State.WAITING_FOR_FINISH)) {
      myStartupManager.runWhenProjectIsInitialized(() -> AccessRule.writeAsync(() -> updateFinished(uiAccess)));
    }
  }

  private boolean switchToSmartMode() {
    synchronized (myRunWhenSmartQueue) {
      if (!myState.compareAndSet(State.WAITING_FOR_FINISH, State.SMART)) {
        return false;
      }
    }
    myDumbStart = null;
    myModificationCount++;
    return !myProject.isDisposed();
  }

  @RequiredWriteAction
  private void updateFinished(UIAccess uiAccess) {
    if (!switchToSmartMode()) return;

    if (ApplicationProperties.isInSandbox()) LOG.info("updateFinished");

    try {
      myPublisher.exitDumbMode(uiAccess);

      FileEditorManagerEx.getInstanceEx(myProject).refreshIcons();
    }
    finally {
      // It may happen that one of the pending runWhenSmart actions triggers new dumb mode;
      // in this case we should quit processing pending actions and postpone them until the newly started dumb mode finishes.
      while (!isDumb()) {
        final Runnable runnable;
        synchronized (myRunWhenSmartQueue) {
          if (myRunWhenSmartQueue.isEmpty()) {
            break;
          }
          runnable = myRunWhenSmartQueue.pullFirst();
        }
        try {
          runnable.run();
        }
        catch (ProcessCanceledException e) {
          LOG.error("Task canceled: " + runnable, new Attachment("pce", e));
        }
        catch (Throwable e) {
          LOG.error("Error executing task " + runnable, e);
        }
      }
    }
  }

  @Override
  public void showDumbModeNotification(@Nonnull final String message) {
    UIUtil.invokeLaterIfNeeded(() -> {
      final IdeFrame ideFrame = WindowManager.getInstance().getIdeFrame(myProject);
      if (ideFrame != null) {
        StatusBarEx statusBar = (StatusBarEx)ideFrame.getStatusBar();
        statusBar.notifyProgressByBalloon(MessageType.WARNING, message, null, null);
      }
    });
  }

  @Override
  public void waitForSmartMode() {
    if (!isDumb()) {
      return;
    }

    if (myApplication.isReadAccessAllowed() || myApplication.isDispatchThread()) {
      throw new AssertionError("Don't invoke waitForSmartMode from inside read action in dumb mode");
    }

    final Semaphore semaphore = new Semaphore();
    semaphore.down();
    runWhenSmart(semaphore::up);
    while (true) {
      if (semaphore.waitFor(50)) {
        return;
      }
      ProgressManager.checkCanceled();
    }
  }

  @Override
  public JComponent wrapGently(@Nonnull JComponent dumbUnawareContent, @Nonnull Disposable parentDisposable) {
    final DumbUnawareHider wrapper = new DumbUnawareHider(dumbUnawareContent);
    wrapper.setContentVisible(!isDumb());
    getProject().getMessageBus().connect(parentDisposable).subscribe(DUMB_MODE, new DumbModeListener() {

      @RequiredUIAccess
      @Override
      public void enteredDumbMode() {
        wrapper.setContentVisible(false);
      }

      @RequiredUIAccess
      @Override
      public void exitDumbMode() {
        wrapper.setContentVisible(true);
      }
    });

    return wrapper;
  }

  @Override
  public void smartInvokeLater(@Nonnull final Runnable runnable) {
    smartInvokeLater(runnable, ModalityState.defaultModalityState());
  }

  @Override
  public void smartInvokeLater(@Nonnull final Runnable runnable, @Nonnull ModalityState modalityState) {
    myApplication.invokeLater(() -> {
      if (isDumb()) {
        runWhenSmart(() -> smartInvokeLater(runnable, modalityState));
      }
      else {
        runnable.run();
      }
    }, modalityState, myProject.getDisposed());
  }

  @Override
  public void completeJustSubmittedTasks() {
    assert myProject.isInitialized();
    if (myState.get() != State.SCHEDULED_TASKS) {
      return;
    }

    while (isDumb()) {
      showModalProgress();
    }
  }

  private void showModalProgress() {
    NoAccessDuringPsiEvents.checkCallContext();
    new Task.Modal(myProject, IdeBundle.message("progress.indexing"), false) {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        runBackgroundProcess(indicator, Application.get().getLastUIAccess());
      }

      @RequiredUIAccess
      @Override
      public void onFinished() {
        if (myState.get() != State.SMART) {
          if (myState.get() != State.WAITING_FOR_FINISH) throw new AssertionError(myState.get());

          UIAccess uiAccess = UIAccess.current();
          AccessRule.writeAsync(() -> updateFinished(uiAccess));
        }
      }
    }.queue();
  }

  private void startBackgroundProcess(@Nonnull UIAccess uiAccess) {
    try {
      ProgressManager.getInstance().run(new Task.Backgroundable(myProject, IdeBundle.message("progress.indexing"), false) {
        @Override
        public void run(@Nonnull final ProgressIndicator visibleIndicator) {
          runBackgroundProcess(visibleIndicator, uiAccess);
        }
      });
    }
    catch (Throwable e) {
      queueUpdateFinished(uiAccess);
      LOG.error("Failed to start background index update task", e);
    }
  }

  private void runBackgroundProcess(@Nonnull final ProgressIndicator visibleIndicator, @Nonnull UIAccess uiAccess) {
    if (!myState.compareAndSet(State.SCHEDULED_TASKS, State.RUNNING_DUMB_TASKS)) return;

    ProgressSuspender.markSuspendable(visibleIndicator);

    final ShutDownTracker shutdownTracker = ShutDownTracker.getInstance();
    final Thread self = Thread.currentThread();
    try {
      shutdownTracker.registerStopperThread(self);

      if (visibleIndicator instanceof ProgressIndicatorEx) {
        ((ProgressIndicatorEx)visibleIndicator).addStateDelegate(new AppIconProgress());
      }

      DumbModeTask task = null;
      while (true) {
        Pair<DumbModeTask, ProgressIndicatorEx> pair = getNextTask(task, visibleIndicator, uiAccess);
        if (pair == null) break;

        task = pair.first;
        ProgressIndicatorEx taskIndicator = pair.second;
        if (visibleIndicator instanceof ProgressIndicatorEx) {
          taskIndicator.addStateDelegate(new AbstractProgressIndicatorExBase() {
            @Override
            protected void delegateProgressChange(@Nonnull IndicatorAction action) {
              super.delegateProgressChange(action);
              action.execute((ProgressIndicatorEx)visibleIndicator);
            }
          });
        }
        try (AccessToken ignored = HeavyProcessLatch.INSTANCE.processStarted("Performing indexing tasks")) {
          runSingleTask(task, taskIndicator);
        }
      }
    }
    catch (Throwable unexpected) {
      LOG.error(unexpected);
    }
    finally {
      shutdownTracker.unregisterStopperThread(self);
    }
  }

  private void runSingleTask(final DumbModeTask task, final ProgressIndicatorEx taskIndicator) {
    if (ApplicationProperties.isInSandbox()) LOG.info("Running dumb mode task: " + task);

    // nested runProcess is needed for taskIndicator to be honored in ProgressManager.checkCanceled calls deep inside tasks
    ProgressManager.getInstance().runProcess(() -> {
      try {
        taskIndicator.checkCanceled();

        taskIndicator.setIndeterminate(true);
        taskIndicator.setText(IdeBundle.message("progress.indexing.scanning"));

        task.performInDumbMode(taskIndicator);
      }
      catch (ProcessCanceledException ignored) {
      }
      catch (Throwable unexpected) {
        LOG.error(unexpected);
      }
    }, taskIndicator);
  }

  @Nullable
  private Pair<DumbModeTask, ProgressIndicatorEx> getNextTask(@Nullable DumbModeTask prevTask, @Nonnull ProgressIndicator indicator, @Nonnull UIAccess uiAccess) {
    CompletableFuture<Pair<DumbModeTask, ProgressIndicatorEx>> result = new CompletableFuture<>();

    uiAccess.giveIfNeed(() -> {
      if (myProject.isDisposed()) {
        result.completeExceptionally(new ProcessCanceledException());
        return;
      }

      if (prevTask != null) {
        Disposer.dispose(prevTask);
      }

      if (PowerSaveMode.isEnabled() && Registry.is("pause.indexing.in.power.save.mode")) {
        indicator.setText("Indexing paused during Power Save mode...");
        runWhenPowerSaveModeChanges(() -> result.complete(pollTaskQueue(uiAccess)));
        completeWhenProjectClosed(result);
      }
      else {
        result.complete(pollTaskQueue(uiAccess));
      }
    });
    return waitForFuture(result);
  }

  @Nullable
  private Pair<DumbModeTask, ProgressIndicatorEx> pollTaskQueue(@Nonnull UIAccess uiAccess) {
    while (true) {
      if (myUpdatesQueue.isEmpty()) {
        queueUpdateFinished(uiAccess);
        return null;
      }

      DumbModeTask queuedTask = myUpdatesQueue.pullFirst();
      myQueuedEquivalences.remove(queuedTask.getEquivalenceObject());
      ProgressIndicatorEx indicator = myProgresses.get(queuedTask);
      if (indicator.isCanceled()) {
        Disposer.dispose(queuedTask);
        continue;
      }

      return Pair.create(queuedTask, indicator);
    }
  }

  @Nullable
  private static <T> T waitForFuture(Future<T> result) {
    try {
      return result.get();
    }
    catch (InterruptedException e) {
      return null;
    }
    catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (!(cause instanceof ProcessCanceledException)) {
        ExceptionUtil.rethrowAllAsUnchecked(cause);
      }
      return null;
    }
  }

  private void completeWhenProjectClosed(CompletableFuture<Pair<DumbModeTask, ProgressIndicatorEx>> result) {
    ProjectManagerAdapter listener = new ProjectManagerAdapter() {
      @Override
      public void projectClosed(Project project) {
        result.completeExceptionally(new ProcessCanceledException());
      }
    };
    ProjectManager.getInstance().addProjectManagerListener(myProject, listener);
    result.thenAccept(p -> ProjectManager.getInstance().removeProjectManagerListener(myProject, listener));
  }

  private void runWhenPowerSaveModeChanges(Runnable r) {
    MessageBusConnection connection = myProject.getMessageBus().connect();
    connection.subscribe(PowerSaveMode.TOPIC, () -> {
      r.run();
      connection.disconnect();
    });
  }

  @Override
  public long getModificationCount() {
    return myModificationCount;
  }

  @Nullable
  public Throwable getDumbModeStartTrace() {
    return myDumbStart;
  }

  private class AppIconProgress extends ProgressIndicatorBase {
    private double lastFraction;

    @Override
    public void setFraction(final double fraction) {
      if (fraction - lastFraction < 0.01d) return;
      lastFraction = fraction;
      UIUtil.invokeLaterIfNeeded(() -> AppIcon.getInstance().setProgress(myProject, "indexUpdate", AppIconScheme.Progress.INDEXING, fraction, true));
    }

    @Override
    public void finish(@Nonnull TaskInfo task) {
      if (lastFraction != 0) { // we should call setProgress at least once before
        UIUtil.invokeLaterIfNeeded(() -> {
          AppIcon appIcon = AppIcon.getInstance();
          if (appIcon.hideProgress(myProject, "indexUpdate")) {
            appIcon.requestAttention(myProject, false);
            appIcon.setOkBadge(myProject, true);
          }
        });
      }
    }
  }

  private enum State {
    /**
     * Non-dumb mode. For all other states, {@link #isDumb()} returns true.
     */
    SMART,

    /**
     * A state between entering dumb mode ({@link #queueTaskOnEdt}) and actually starting the background progress later ({@link #runBackgroundProcess}).
     * In this state, it's possible to call {@link #completeJustSubmittedTasks()} and perform all submitted the tasks modally.
     * This state can happen after {@link #SMART} or {@link #WAITING_FOR_FINISH}. Followed by {@link #RUNNING_DUMB_TASKS}.
     */
    SCHEDULED_TASKS,

    /**
     * Indicates that a background thread is currently executing dumb tasks.
     */
    RUNNING_DUMB_TASKS,

    /**
     * Set after background execution ({@link #RUNNING_DUMB_TASKS}) finishes, until the dumb mode can be exited
     * (in a write-safe context on EDT when project is initialized). If new tasks are queued at this state, it's switched to {@link #SCHEDULED_TASKS}.
     */
    WAITING_FOR_FINISH
  }
}
