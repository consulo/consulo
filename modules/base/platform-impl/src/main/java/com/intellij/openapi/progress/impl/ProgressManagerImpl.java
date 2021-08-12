/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.openapi.progress.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.util.PingProgress;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.SystemNotifications;
import com.intellij.util.containers.ContainerUtil;
import consulo.disposer.Disposable;
import consulo.util.dataholder.Key;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.Set;
import java.util.concurrent.Future;

@Singleton
public class ProgressManagerImpl extends CoreProgressManager implements Disposable {
  private static final Key<Boolean> SAFE_PROGRESS_INDICATOR = Key.create("SAFE_PROGRESS_INDICATOR");
  private final Set<CheckCanceledHook> myHooks = ContainerUtil.newConcurrentSet();
  private final CheckCanceledHook mySleepHook = __ -> sleepIfNeededToGivePriorityToAnotherThread();

  public ProgressManagerImpl() {
  }

  @Override
  public boolean hasUnsafeProgressIndicator() {
    return super.hasUnsafeProgressIndicator() || ContainerUtil.exists(getCurrentIndicators(), ProgressManagerImpl::isUnsafeIndicator);
  }

  private static boolean isUnsafeIndicator(ProgressIndicator indicator) {
    return indicator instanceof ProgressWindow && ((ProgressWindow)indicator).getUserData(SAFE_PROGRESS_INDICATOR) == null;
  }

  /**
   * The passes progress won't count in {@link #hasUnsafeProgressIndicator()} and won't stop from application exiting.
   */
  public void markProgressSafe(@Nonnull ProgressWindow progress) {
    progress.putUserData(SAFE_PROGRESS_INDICATOR, true);
  }

  @Override
  public void executeProcessUnderProgress(@Nonnull Runnable process, ProgressIndicator progress) throws ProcessCanceledException {
    CheckCanceledHook hook = progress instanceof PingProgress && ApplicationManager.getApplication().isDispatchThread() ? p -> {
      ((PingProgress)progress).interact();
      return true;
    } : null;
    if (hook != null) addCheckCanceledHook(hook);

    try {
      super.executeProcessUnderProgress(process, progress);
    }
    finally {
      if (hook != null) removeCheckCanceledHook(hook);
    }
  }

  @TestOnly
  public static void __testWhileAlwaysCheckingCanceled(@Nonnull Runnable runnable) {
    @SuppressWarnings("InstantiatingAThreadWithDefaultRunMethod") Thread fake = new Thread("fake");
    try {
      threadsUnderCanceledIndicator.add(fake);
      runnable.run();
    }
    finally {
      threadsUnderCanceledIndicator.remove(fake);
    }
  }

  @Override
  public boolean runProcessWithProgressSynchronously(@Nonnull final Task task) {
    final long start = System.currentTimeMillis();
    final boolean result = super.runProcessWithProgressSynchronously(task);
    if (result) {
      final long end = System.currentTimeMillis();
      final Task.NotificationInfo notificationInfo = task.notifyFinished();
      long time = end - start;
      if (notificationInfo != null && time > 5000) { // show notification only if process took more than 5 secs
        final JFrame frame = WindowManager.getInstance().getFrame(task.getProject());
        if (frame != null && !frame.hasFocus()) {
          systemNotify(notificationInfo);
        }
      }
    }
    return result;
  }

  private static void systemNotify(@Nonnull Task.NotificationInfo info) {
    SystemNotifications.getInstance().notify(info.getNotificationName(), info.getNotificationTitle(), info.getNotificationText());
  }

  @Override
  @Nonnull
  public Future<?> runProcessWithProgressAsynchronously(@Nonnull Task.Backgroundable task) {
    ProgressIndicator progressIndicator = ApplicationManager.getApplication().isHeadlessEnvironment() ? new EmptyProgressIndicator() : new BackgroundableProcessIndicator(task);
    return runProcessWithProgressAsynchronously(task, progressIndicator, null);
  }

  @Override
  void notifyTaskFinished(@Nonnull Task.Backgroundable task, long elapsed) {
    final Task.NotificationInfo notificationInfo = task.notifyFinished();
    if (notificationInfo != null && elapsed > 5000) { // snow notification if process took more than 5 secs
      final Component window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
      if (window == null || notificationInfo.isShowWhenFocused()) {
        systemNotify(notificationInfo);
      }
    }
  }

  @Override
  public boolean runInReadActionWithWriteActionPriority(@Nonnull Runnable action, @Nullable ProgressIndicator indicator) {
    return ProgressIndicatorUtils.runInReadActionWithWriteActionPriority(action, indicator);
  }

  /**
   * An absolutely guru method, very dangerous, don't use unless you're desperate,
   * because hooks will be executed on every checkCanceled and can dramatically slow down everything in the IDE.
   */
  void addCheckCanceledHook(@Nonnull CheckCanceledHook hook) {
    if (myHooks.add(hook)) {
      updateShouldCheckCanceled();
    }
  }

  void removeCheckCanceledHook(@Nonnull CheckCanceledHook hook) {
    if (myHooks.remove(hook)) {
      updateShouldCheckCanceled();
    }
  }

  @Nullable
  @Override
  protected CheckCanceledHook createCheckCanceledHook() {
    if (myHooks.isEmpty()) return null;

    CheckCanceledHook[] activeHooks = myHooks.toArray(CheckCanceledHook.EMPTY_ARRAY);
    return activeHooks.length == 1 ? activeHooks[0] : indicator -> {
      boolean result = false;
      for (CheckCanceledHook hook : activeHooks) {
        if (hook.runHook(indicator)) {
          result = true; // but still continue to other hooks
        }
      }
      return result;
    };
  }

  @Override
  protected void prioritizingStarted() {
    addCheckCanceledHook(mySleepHook);
  }

  @Override
  protected void prioritizingFinished() {
    removeCheckCanceledHook(mySleepHook);
  }
}
