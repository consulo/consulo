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
package consulo.ide.impl.idea.openapi.vcs.changes;

import consulo.application.ApplicationManager;
import consulo.application.HeavyProcessLatch;
import consulo.application.util.Semaphore;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.impl.internal.StartupManagerImpl;
import consulo.project.startup.StartupManager;
import consulo.ui.ModalityState;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.change.InvokeAfterUpdateMode;
import consulo.versionControlSystem.change.VcsDirtyScopeManager;
import consulo.versionControlSystem.impl.internal.change.ChangeListScheduler;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * ChangeListManager updates scheduler.
 * Tries to zip several update requests into one (if starts and see several requests in the queue)
 * own inner synchronization
 */
public class UpdateRequestsQueue {
  private final Logger LOG = Logger.getInstance(UpdateRequestsQueue.class);
  private static final String ourHeavyLatchOptimization = "vcs.local.changes.track.heavy.latch";
  private final Project myProject;
  private ChangeListScheduler myScheduler;
  private final Runnable myDelegate;
  private final Object myLock;
  private volatile boolean myStarted;
  private volatile boolean myStopped;
  private volatile boolean myIgnoreBackgroundOperation;

  private boolean myRequestSubmitted;
  private boolean myRequestRunning;
  private final List<Runnable> myWaitingUpdateCompletionQueue;
  private final List<Semaphore> myWaitingUpdateCompletionSemaphores = new ArrayList<>();
  private final ProjectLevelVcsManager myPlVcsManager;
  //private final ScheduledSlowlyClosingAlarm mySharedExecutor;
  private final StartupManager myStartupManager;
  private final boolean myTrackHeavyLatch;

  public UpdateRequestsQueue(Project project, @Nonnull ChangeListScheduler scheduler, Runnable delegate) {
    myProject = project;
    myScheduler = scheduler;

    myTrackHeavyLatch = Boolean.parseBoolean(System.getProperty(ourHeavyLatchOptimization));

    myDelegate = delegate;
    myPlVcsManager = ProjectLevelVcsManager.getInstance(myProject);
    myStartupManager = StartupManager.getInstance(myProject);
    myLock = new Object();
    myWaitingUpdateCompletionQueue = new ArrayList<>();
    // not initialized
    myStarted = false;
    myStopped = false;
  }

  public void initialized() {
    LOG.debug("Initialized for project: " + myProject.getName());
    myStarted = true;
  }

  public boolean isStopped() {
    return myStopped;
  }

  public void schedule() {
    synchronized (myLock) {
      if (! myStarted && ApplicationManager.getApplication().isUnitTestMode()) return;

      if (! myStopped) {
        if (! myRequestSubmitted) {
          MyRunnable runnable = new MyRunnable();
          myRequestSubmitted = true;
          myScheduler.schedule(runnable, 300, TimeUnit.MILLISECONDS);
          LOG.debug("Scheduled for project: " + myProject.getName() + ", runnable: " + runnable.hashCode());
        }
      }
    }
  }

  public void pause() {
    synchronized (myLock) {
      myStopped = true;
    }
  }

  public void forceGo() {
    synchronized (myLock) {
      myStopped = false;
      myRequestSubmitted = false;
      myRequestRunning = false;
    }
    schedule();
  }

  public void go() {
    synchronized (myLock) {
      myStopped = false;
    }
    schedule();
  }

  public void stop() {
    LOG.debug("Calling stop for project: " + myProject.getName());
    List<Runnable> waiters = new ArrayList<>(myWaitingUpdateCompletionQueue.size());
    synchronized (myLock) {
      myStopped = true;
      waiters.addAll(myWaitingUpdateCompletionQueue);
      myWaitingUpdateCompletionQueue.clear();
    }
    LOG.debug("Calling runnables in stop for project: " + myProject.getName());
    // do not run under lock
    for (Runnable runnable : waiters) {
      runnable.run();
    }
    LOG.debug("Stop finished for project: " + myProject.getName());
  }

  @TestOnly
  public void waitUntilRefreshed() {
    while (true) {
      Semaphore semaphore = new Semaphore();
      synchronized (myLock) {
        if (!myRequestSubmitted && !myRequestRunning) {
          return;
        }

        if (!myRequestRunning) {
          myScheduler.submit(new MyRunnable());
        }

        semaphore.down();
        myWaitingUpdateCompletionSemaphores.add(semaphore);
      }
      if (!semaphore.waitFor(100*1000)) {
        LOG.error("Too long VCS update");
        return;
      }
    }
  }

  private void freeSemaphores() {
    synchronized (myLock) {
      for (Semaphore semaphore : myWaitingUpdateCompletionSemaphores) {
        semaphore.up();
      }
      myWaitingUpdateCompletionSemaphores.clear();
    }
  }

  public void invokeAfterUpdate(@Nonnull Runnable afterUpdate,
                                @Nonnull InvokeAfterUpdateMode mode,
                                @Nullable String title,
                                @Nullable Consumer<VcsDirtyScopeManager> dirtyScopeManagerFiller,
                                @Nullable ModalityState state) {
    LOG.debug("invokeAfterUpdate for project: " + myProject.getName());
    CallbackData data = CallbackData.create(myProject, mode, afterUpdate, title, state);

    if (dirtyScopeManagerFiller != null) {
      VcsDirtyScopeManagerProxy managerProxy = new VcsDirtyScopeManagerProxy();

      dirtyScopeManagerFiller.accept(managerProxy);
      if (!myProject.isDisposed()) {
        managerProxy.callRealManager(VcsDirtyScopeManager.getInstance(myProject));
      }
    }

    synchronized (myLock) {
      if (! myStopped) {
        myWaitingUpdateCompletionQueue.add(data.getCallback());
        schedule();
      }
    }
    // do not run under lock; stopped cannot be switched into not stopped - can check without lock
    if (myStopped) {
      LOG.debug("invokeAfterUpdate: stopped, invoke right now for project: " + myProject.getName());
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          if (!myProject.isDisposed()) {
            afterUpdate.run();
          }
        }
      });
      return;
    }
    // invoke progress if needed
    data.getWrapperStarter().run();
    LOG.debug("invokeAfterUpdate: exit for project: " + myProject.getName());
  }

  // true = do not execute
  private boolean checkHeavyOperations() {
    if (myIgnoreBackgroundOperation) return false;
    return myPlVcsManager.isBackgroundVcsOperationRunning() || myTrackHeavyLatch && HeavyProcessLatch.INSTANCE.isRunning();
  }

  // true = do not execute
  private boolean checkLifeCycle() {
    return !myStarted || !((StartupManagerImpl)myStartupManager).startupActivityPassed();
  }

  private class MyRunnable implements Runnable {
    public void run() {
      List<Runnable> copy = new ArrayList<>(myWaitingUpdateCompletionQueue.size());
      try {
        synchronized (myLock) {
          if (!myRequestSubmitted) return;

          LOG.assertTrue(!myRequestRunning);
          myRequestRunning = true;
          if (myStopped) {
            myRequestSubmitted = false;
            LOG.debug("MyRunnable: STOPPED, project: " + myProject.getName() + ", runnable: " + hashCode());
            return;
          }

          if (checkLifeCycle() || checkHeavyOperations()) {
            LOG.debug("MyRunnable: reschedule, project: " + myProject.getName() + ", runnable: " + hashCode());
            myRequestSubmitted = false;
            // try again after time
            schedule();
            return;
          }

          copy.addAll(myWaitingUpdateCompletionQueue);
          myRequestSubmitted = false;
        }

        LOG.debug("MyRunnable: INVOKE, project: " + myProject.getName() + ", runnable: " + hashCode());
        myDelegate.run();
        LOG.debug("MyRunnable: invokeD, project: " + myProject.getName() + ", runnable: " + hashCode());
      }
      finally {
        synchronized (myLock) {
          myRequestRunning = false;
          LOG.debug("MyRunnable: delete executed, project: " + myProject.getName() + ", runnable: " + hashCode());
          if (! copy.isEmpty()) {
            myWaitingUpdateCompletionQueue.removeAll(copy);
          }

          if (! myWaitingUpdateCompletionQueue.isEmpty() && ! myRequestSubmitted && ! myStopped) {
            LOG.error("No update task to handle request(s)");
          }
        }
        // do not run under lock
        for (Runnable runnable : copy) {
          runnable.run();
        }
        freeSemaphores();
        LOG.debug("MyRunnable: Runnables executed, project: " + myProject.getName() + ", runnable: " + hashCode());
      }
    }

    @Override
    public String toString() {
      return "UpdateRequestQueue delegate: "+myDelegate;
    }
  }

  public void setIgnoreBackgroundOperation(boolean ignoreBackgroundOperation) {
    myIgnoreBackgroundOperation = ignoreBackgroundOperation;
  }
}
