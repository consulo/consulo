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
package consulo.ui.ex.awt.util;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.ModalityState;
import consulo.ui.ex.UiActivity;
import consulo.ui.ex.UiActivityMonitor;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.update.Activatable;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.util.collection.ContainerUtil;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Use this class to postpone task execution and optionally merge identical tasks. This is needed e.g. to reflect in UI status of some
 * background activity: it doesn't make sense and would be inefficient to update UI 1000 times per second, so it's better to postpone 'update UI'
 * task execution for e.g. 500ms and if new updates are added during this period they can be simply ignored.
 * <p>
 * <p/>
 * Create instance of this class and use {@link #queue(Update)} method to add new tasks.
 */
public class MergingUpdateQueue implements Runnable, Disposable, Activatable {
  public static final JComponent ANY_COMPONENT = new JComponent() {
  };

  private volatile boolean myActive;
  private volatile boolean mySuspended;

  private final Map<Integer, Map<Update, Update>> myScheduledUpdates = new TreeMap<>();

  private final Alarm myWaiterForMerge;

  private volatile boolean myFlushing;

  private final String myName;
  private int myMergingTimeSpan;
  private JComponent myModalityStateComponent;
  private final boolean myExecuteInDispatchThread;
  private boolean myPassThrough;
  private boolean myDisposed;

  private UiNotifyConnector myUiNotifyConnector;
  private boolean myRestartOnAdd;

  private boolean myTrackUiActivity;
  private UiActivity myUiActivity;

  public MergingUpdateQueue(@NonNls @Nonnull String name, int mergingTimeSpan, boolean isActive, @Nullable JComponent modalityStateComponent) {
    this(name, mergingTimeSpan, isActive, modalityStateComponent, null);
  }

  public MergingUpdateQueue(@NonNls @Nonnull String name, int mergingTimeSpan, boolean isActive, @Nullable JComponent modalityStateComponent, @Nullable Disposable parent) {
    this(name, mergingTimeSpan, isActive, modalityStateComponent, parent, null);
  }

  public MergingUpdateQueue(@NonNls @Nonnull String name,
                            int mergingTimeSpan,
                            boolean isActive,
                            @Nullable JComponent modalityStateComponent,
                            @Nullable Disposable parent,
                            @Nullable JComponent activationComponent) {
    this(name, mergingTimeSpan, isActive, modalityStateComponent, parent, activationComponent, true);
  }

  public MergingUpdateQueue(@NonNls @Nonnull String name,
                            int mergingTimeSpan,
                            boolean isActive,
                            @Nullable JComponent modalityStateComponent,
                            @Nullable Disposable parent,
                            @Nullable JComponent activationComponent,
                            boolean executeInDispatchThread) {
    this(name, mergingTimeSpan, isActive, modalityStateComponent, parent, activationComponent, executeInDispatchThread ? Alarm.ThreadToUse.SWING_THREAD : Alarm.ThreadToUse.POOLED_THREAD);
  }

  /**
   * @param name                   name of this queue, used only for debugging purposes
   * @param mergingTimeSpan        time (in milliseconds) for which execution of tasks will be postponed
   * @param isActive               if {@code true} the queue will execute tasks otherwise it'll just collect them and execute only after {@link #activate()} is called
   * @param modalityStateComponent makes sense only if {@code thread} is {@linkplain Alarm.ThreadToUse#SWING_THREAD SWING_THREAD}, in that
   *                               case the tasks will be processed in {@link ModalityState} corresponding the given component
   * @param parent                 if not {@code null} the queue will be disposed when the given parent is disposed
   * @param activationComponent    if not {@code null} the tasks will be processing only when the given component is showing
   * @param thread                 specifies on which thread the tasks are executed
   */
  public MergingUpdateQueue(@NonNls @Nonnull String name,
                            int mergingTimeSpan,
                            boolean isActive,
                            @Nullable JComponent modalityStateComponent,
                            @Nullable Disposable parent,
                            @Nullable JComponent activationComponent,
                            @Nonnull Alarm.ThreadToUse thread) {
    myMergingTimeSpan = mergingTimeSpan;
    myModalityStateComponent = modalityStateComponent;
    myName = name;
    Application app = ApplicationManager.getApplication();
    myPassThrough = app == null || app.isUnitTestMode();
    myExecuteInDispatchThread = thread == Alarm.ThreadToUse.SWING_THREAD;

    if (parent != null) {
      Disposer.register(parent, this);
    }

    myWaiterForMerge = createAlarm(thread, myExecuteInDispatchThread ? null : this);

    if (isActive) {
      showNotify();
    }

    if (activationComponent != null) {
      setActivationComponent(activationComponent);
    }
  }

  private static Alarm createAlarm(@Nonnull Alarm.ThreadToUse thread, @Nullable Disposable parent) {
    return parent == null ? new Alarm(thread) : new Alarm(thread, parent);
  }

  public void setMergingTimeSpan(int timeSpan) {
    myMergingTimeSpan = timeSpan;
    if (myActive) {
      restartTimer();
    }
  }

  public void cancelAllUpdates() {
    synchronized (myScheduledUpdates) {
      for (Update each : getAllScheduledUpdates()) {
        try {
          each.setRejected();
        }
        catch (ProcessCanceledException ignored) {
        }
      }
      myScheduledUpdates.clear();
      finishActivity();
    }
  }

  @Nonnull
  private List<Update> getAllScheduledUpdates() {
    return ContainerUtil.concat(myScheduledUpdates.values(), map -> map.keySet());
  }

  public final boolean isPassThrough() {
    return myPassThrough;
  }

  /**
   * @param passThrough if {@code true} the tasks won't be postponed but executed immediately instead (this is default mode for tests)
   */
  public final void setPassThrough(boolean passThrough) {
    myPassThrough = passThrough;
  }

  public void activate() {
    showNotify();
  }

  public void deactivate() {
    hideNotify();
  }

  public void suspend() {
    mySuspended = true;
  }

  public void resume() {
    mySuspended = false;
    restartTimer();
  }

  @Override
  public void hideNotify() {
    if (!myActive) {
      return;
    }

    myActive = false;

    finishActivity();

    clearWaiter();
  }

  @Override
  public void showNotify() {
    if (myActive) {
      return;
    }

    myActive = true;
    restartTimer();
    flush();
  }

  public void restartTimer() {
    restart(myMergingTimeSpan);
  }

  private void restart(int mergingTimeSpanMillis) {
    if (!myActive) return;

    clearWaiter();

    if (myExecuteInDispatchThread) {
      myWaiterForMerge.addRequest(this, mergingTimeSpanMillis, getMergerModalityState());
    }
    else {
      myWaiterForMerge.addRequest(this, mergingTimeSpanMillis);
    }
  }

  @Override
  public void run() {
    if (mySuspended) return;
    flush();
  }

  public void flush() {
    synchronized (myScheduledUpdates) {
      if (myScheduledUpdates.isEmpty()) {
        finishActivity();
        return;
      }
    }
    if (myFlushing) {
      return;
    }
    if (!isModalityStateCorrect()) {
      return;
    }

    myFlushing = true;
    Runnable toRun = () -> {
      try {
        List<Update> all;

        synchronized (myScheduledUpdates) {
          all = getAllScheduledUpdates();
          myScheduledUpdates.clear();
        }

        for (Update each : all) {
          each.setProcessed();
        }

        execute(all.toArray(new Update[0]));
      }
      finally {
        myFlushing = false;
        if (isEmpty()) {
          finishActivity();
        }
      }
    };

    if (myExecuteInDispatchThread) {
      Application application = Application.get();
      if(application.isUnifiedApplication()) {
        application.getLastUIAccess().giveAndWaitIfNeed(toRun);
      }
      else {
        UIUtil.invokeAndWaitIfNeeded(toRun);
      }
    }
    else {
      toRun.run();
    }
  }

  public void setModalityStateComponent(JComponent modalityStateComponent) {
    myModalityStateComponent = modalityStateComponent;
  }

  protected boolean isModalityStateCorrect() {
    if (!myExecuteInDispatchThread) return true;
    if (myModalityStateComponent == ANY_COMPONENT) return true;

    consulo.ui.ModalityState current = ApplicationManager.getApplication().getCurrentModalityState();
    ModalityState modalityState = getModalityState();
    return !current.dominates(modalityState);
  }

  public boolean isSuspended() {
    return mySuspended;
  }

  private static boolean isExpired(@Nonnull Update each) {
    return each.isDisposed() || each.isExpired();
  }

  protected void execute(@Nonnull Update[] update) {
    for (Update each : update) {
      if (isExpired(each)) {
        each.setRejected();
        continue;
      }

      if (each.executeInWriteAction()) {
        ApplicationManager.getApplication().runWriteAction(() -> execute(each));
      }
      else {
        execute(each);
      }
    }
  }

  private void execute(@Nonnull Update each) {
    if (myDisposed) {
      each.setRejected();
    }
    else {
      each.run();
    }
  }

  /**
   * Adds a task to be executed.
   */
  public void queue(@Nonnull Update update) {
    if (myDisposed) return;

    if (myTrackUiActivity) {
      startActivity();
    }

    if (myPassThrough) {
      update.run();
      finishActivity();
      return;
    }

    boolean active = myActive;
    synchronized (myScheduledUpdates) {
      try {
        if (eatThisOrOthers(update)) {
          return;
        }

        if (active && myScheduledUpdates.isEmpty()) {
          restartTimer();
        }
        put(update);

        if (myRestartOnAdd) {
          restartTimer();
        }
      }
      finally {
        if (isEmpty()) {
          finishActivity();
        }
      }
    }
  }

  private boolean eatThisOrOthers(@Nonnull Update update) {
    Map<Update, Update> updates = myScheduledUpdates.get(update.getPriority());
    if (updates != null && updates.containsKey(update)) {
      return false;
    }

    for (Update eachInQueue : getAllScheduledUpdates()) {
      if (eachInQueue.canEat(update)) {
        return true;
      }
      if (update.canEat(eachInQueue)) {
        myScheduledUpdates.get(eachInQueue.getPriority()).remove(eachInQueue);
        eachInQueue.setRejected();
      }
    }
    return false;
  }

  public final void run(@Nonnull Update update) {
    execute(new Update[]{update});
  }

  private void put(@Nonnull Update update) {
    Map<Update, Update> updates = myScheduledUpdates.computeIfAbsent(update.getPriority(), __ -> new LinkedHashMap<>());
    Update existing = updates.remove(update);
    if (existing != null && existing != update) {
      existing.setProcessed();
      existing.setRejected();
    }
    updates.put(update, update);
  }

  public boolean isActive() {
    return myActive;
  }

  @Override
  public void dispose() {
    myDisposed = true;
    myActive = false;
    finishActivity();
    clearWaiter();
  }

  private void clearWaiter() {
    myWaiterForMerge.cancelAllRequests();
  }

  @Override
  public String toString() {
    synchronized (myScheduledUpdates) {
      return myName + " active=" + myActive + " scheduled=" + getAllScheduledUpdates().size();
    }
  }

  @Nullable
  private ModalityState getMergerModalityState() {
    return myModalityStateComponent == ANY_COMPONENT ? null : getModalityState();
  }

  @Nonnull
  public ModalityState getModalityState() {
    if (myModalityStateComponent == null) {
      return Application.get().getNoneModalityState();
    }
    return Application.get().getModalityStateForComponent(myModalityStateComponent);
  }

  public void setActivationComponent(@Nonnull JComponent c) {
    if (myUiNotifyConnector != null) {
      Disposer.dispose(myUiNotifyConnector);
    }

    UiNotifyConnector connector = new UiNotifyConnector(c, this);
    Disposer.register(this, connector);
    myUiNotifyConnector = connector;
  }

  public MergingUpdateQueue setRestartTimerOnAdd(boolean restart) {
    myRestartOnAdd = restart;
    return this;
  }

  public boolean isEmpty() {
    synchronized (myScheduledUpdates) {
      return myScheduledUpdates.isEmpty();
    }
  }

  public void sendFlush() {
    restart(0);
  }

  public boolean isFlushing() {
    return myFlushing;
  }

  public void setTrackUiActivity(boolean trackUiActivity) {
    if (myTrackUiActivity && !trackUiActivity) {
      finishActivity();
    }

    myTrackUiActivity = trackUiActivity;
  }

  private void startActivity() {
    if (!myTrackUiActivity) return;

    UiActivityMonitor.getInstance().addActivity(getActivityId(), getModalityState());
  }

  private void finishActivity() {
    if (!myTrackUiActivity) return;

    UiActivityMonitor.getInstance().removeActivity(getActivityId());
  }

  @Nonnull
  private UiActivity getActivityId() {
    if (myUiActivity == null) {
      myUiActivity = new UiActivity.AsyncBgOperation("UpdateQueue:" + myName + hashCode());
    }

    return myUiActivity;
  }
}
