// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.application.impl;

import com.intellij.ide.IdeEventQueue;
import com.intellij.idea.ApplicationStarter;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ModalityStateListener;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.*;
import com.intellij.util.ArrayUtil;
import com.intellij.util.EventDispatcher;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.SystemProperties;
import com.intellij.util.concurrency.Semaphore;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Stack;
import com.intellij.util.ui.UIUtil;
import consulo.application.TransactionGuardEx;
import consulo.logging.Logger;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("SSBasedInspection")
public class LaterInvocator {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.application.impl.LaterInvocator");
  private static final boolean DEBUG = LOG.isDebugEnabled();

  private static final Object LOCK = new Object();

  private LaterInvocator() {
  }

  private static class RunnableInfo {
    @Nonnull
    private final Runnable runnable;
    @Nonnull
    private final ModalityState modalityState;
    @Nonnull
    private final Condition<?> expired;
    @Nullable
    private final ActionCallback callback;

    RunnableInfo(@Nonnull Runnable runnable, @Nonnull ModalityState modalityState, @Nonnull Condition<?> expired, @Nullable ActionCallback callback) {
      this.runnable = runnable;
      this.modalityState = modalityState;
      this.expired = expired;
      this.callback = callback;
    }

    void markDone() {
      if (callback != null) callback.setDone();
    }

    @Override
    @NonNls
    public String toString() {
      return "[runnable: " + runnable + "; state=" + modalityState + (expired.value(null) ? "; expired" : "") + "] ";
    }
  }

  // Application modal entities
  private static final List<Object> ourModalEntities = ContainerUtil.createLockFreeCopyOnWriteList();

  // Per-project modal entities
  private static final Map<Project, List<Dialog>> projectToModalEntities = new WeakHashMap<>();
  private static final Map<Project, Stack<ModalityState>> projectToModalEntitiesStack = new WeakHashMap<>();

  private static final Stack<ModalityStateEx> ourModalityStack = new Stack<>((ModalityStateEx)ModalityState.NON_MODAL);
  private static final List<RunnableInfo> ourSkippedItems = new ArrayList<>(); //protected by LOCK
  private static final ArrayDeque<RunnableInfo> ourQueue = new ArrayDeque<>(); //protected by LOCK
  private static final FlushQueue ourFlushQueueRunnable = new FlushQueue();

  private static final EventDispatcher<ModalityStateListener> ourModalityStateMulticaster = EventDispatcher.create(ModalityStateListener.class);

  public static void addModalityStateListener(@Nonnull ModalityStateListener listener, @Nonnull Disposable parentDisposable) {
    if (!ourModalityStateMulticaster.getListeners().contains(listener)) {
      ourModalityStateMulticaster.addListener(listener, parentDisposable);
    }
  }

  public static void removeModalityStateListener(@Nonnull ModalityStateListener listener) {
    ourModalityStateMulticaster.removeListener(listener);
  }

  private static final ConcurrentMap<Window, ModalityStateEx> ourWindowModalities = ContainerUtil.createConcurrentWeakMap();

  @Nonnull
  public static ModalityStateEx modalityStateForWindow(@Nonnull Window window) {
    return ourWindowModalities.computeIfAbsent(window, __ -> {
      for (ModalityStateEx state : ourModalityStack) {
        if (state.getModalEntities().contains(window)) {
          return state;
        }
      }

      Window owner = window.getOwner();
      ModalityStateEx ownerState = owner == null ? (ModalityStateEx)ModalityState.NON_MODAL : modalityStateForWindow(owner);
      return isModalDialog(window) ? ownerState.appendEntity(window) : ownerState;
    });
  }

  private static boolean isModalDialog(@Nonnull Object window) {
    return window instanceof Dialog && ((Dialog)window).isModal();
  }

  @Nonnull
  public static AsyncResult<Void> invokeLater(@Nonnull Runnable runnable, @Nonnull Condition<?> expired) {
    ModalityState modalityState = ModalityState.defaultModalityState();
    return invokeLater(runnable, modalityState, expired);
  }

  @Nonnull
  public static AsyncResult<Void> invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState modalityState) {
    return invokeLater(runnable, modalityState, Conditions.FALSE);
  }

  @Nonnull
  public static AsyncResult<Void> invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState modalityState, @Nonnull Condition<?> expired) {
    AsyncResult<Void> callback = new AsyncResult<>();
    invokeLaterWithCallback(runnable, modalityState, expired, callback);
    return callback;
  }

  public static void invokeLaterWithCallback(@Nonnull Runnable runnable, @Nonnull ModalityState modalityState, @Nonnull Condition<?> expired, @Nullable ActionCallback callback) {
    if (expired.value(null)) {
      if (callback != null) {
        callback.setRejected();
      }
      return;
    }
    RunnableInfo runnableInfo = new RunnableInfo(runnable, modalityState, expired, callback);
    synchronized (LOCK) {
      ourQueue.add(runnableInfo);
    }
    requestFlush();
  }

  public static void invokeAndWait(@Nonnull final Runnable runnable, @Nonnull ModalityState modalityState) {
    LOG.assertTrue(!isDispatchThread());

    final Semaphore semaphore = new Semaphore();
    semaphore.down();
    final Ref<Throwable> exception = Ref.create();
    Runnable runnable1 = new Runnable() {
      @Override
      public void run() {
        try {
          runnable.run();
        }
        catch (Throwable e) {
          exception.set(e);
        }
        finally {
          semaphore.up();
        }
      }

      @Override
      @NonNls
      public String toString() {
        return "InvokeAndWait[" + runnable + "]";
      }
    };
    invokeLaterWithCallback(runnable1, modalityState, Conditions.FALSE, null);
    semaphore.waitFor();
    if (!exception.isNull()) {
      Throwable cause = exception.get();
      if (SystemProperties.getBooleanProperty("invoke.later.wrap.error", true)) {
        // wrap everything to keep the current thread stacktrace
        // also TC ComparisonFailure feature depends on this
        throw new RuntimeException(cause);
      }
      else {
        ExceptionUtil.rethrow(cause);
      }
    }
  }

  public static void enterModal(@Nonnull Object modalEntity) {
    ModalityStateEx state = getCurrentModalityState().appendEntity(modalEntity);
    if (isModalDialog(modalEntity)) {
      List<Object> currentEntities = state.getModalEntities();
      state = modalityStateForWindow((Window)modalEntity);
      state.forceModalEntities(currentEntities);
    }
    enterModal(modalEntity, state);
  }

  public static void enterModal(@Nonnull Object modalEntity, @Nonnull ModalityStateEx appendedState) {
    LOG.assertTrue(isDispatchThread(), "enterModal() should be invoked in event-dispatch thread");

    if (LOG.isDebugEnabled()) {
      LOG.debug("enterModal:" + modalEntity);
    }

    ourModalityStateMulticaster.getMulticaster().beforeModalityStateChanged(true);

    ourModalEntities.add(modalEntity);
    ourModalityStack.push(appendedState);

    TransactionGuardEx guard = ApplicationStarter.isLoaded() ? (TransactionGuardEx)TransactionGuard.getInstance() : null;
    if (guard != null) {
      guard.enteredModality(appendedState);
    }

    reincludeSkippedItems();
    requestFlush();
  }

  public static void enterModal(Project project, Dialog dialog) {
    LOG.assertTrue(isDispatchThread(), "enterModal() should be invoked in event-dispatch thread");

    if (LOG.isDebugEnabled()) {
      LOG.debug("enterModal:" + dialog.getName() + " ; for project: " + project.getName());
    }

    if (project == null) {
      enterModal(dialog);
      return;
    }

    ourModalityStateMulticaster.getMulticaster().beforeModalityStateChanged(true);

    List<Dialog> modalEntitiesList = projectToModalEntities.getOrDefault(project, ContainerUtil.createLockFreeCopyOnWriteList());
    projectToModalEntities.put(project, modalEntitiesList);
    modalEntitiesList.add(dialog);

    Stack<ModalityState> modalEntitiesStack = projectToModalEntitiesStack.getOrDefault(project, new Stack<>(ModalityState.NON_MODAL));
    projectToModalEntitiesStack.put(project, modalEntitiesStack);
    modalEntitiesStack.push(new ModalityStateEx(ArrayUtil.toObjectArray(ourModalEntities)));
  }


  public static void leaveModal(Project project, Dialog dialog) {
    LOG.assertTrue(isDispatchThread(), "leaveModal() should be invoked in event-dispatch thread");

    if (LOG.isDebugEnabled()) {
      LOG.debug("leaveModal:" + dialog.getName() + " ; for project: " + project.getName());
    }

    ourModalityStateMulticaster.getMulticaster().beforeModalityStateChanged(false);

    int index = ourModalEntities.indexOf(dialog);

    if (index != -1) {
      ourModalEntities.remove(index);
      ourModalityStack.remove(index + 1);
      for (int i = 1; i < ourModalityStack.size(); i++) {
        ourModalityStack.get(i).removeModality(dialog);
      }
    }
    else if (project != null) {
      List<Dialog> dialogs = projectToModalEntities.get(project);
      int perProjectIndex = dialogs.indexOf(dialog);
      LOG.assertTrue(perProjectIndex >= 0);
      dialogs.remove(perProjectIndex);
      Stack<ModalityState> states = projectToModalEntitiesStack.get(project);
      states.remove(perProjectIndex + 1);
      for (int i = 1; i < states.size(); i++) {
        ((ModalityStateEx)states.get(i)).removeModality(dialog);
      }
    }

    reincludeSkippedItems();
    requestFlush();
  }

  private static void reincludeSkippedItems() {
    synchronized (LOCK) {
      for (int i = ourSkippedItems.size() - 1; i >= 0; i--) {
        ourQueue.addFirst(ourSkippedItems.get(i));
      }
      ourSkippedItems.clear();
    }
  }

  public static void leaveModal(@Nonnull Object modalEntity) {
    LOG.assertTrue(isDispatchThread(), "leaveModal() should be invoked in event-dispatch thread");

    if (LOG.isDebugEnabled()) {
      LOG.debug("leaveModal:" + modalEntity);
    }

    ourModalityStateMulticaster.getMulticaster().beforeModalityStateChanged(false);

    int index = ourModalEntities.indexOf(modalEntity);
    LOG.assertTrue(index >= 0);
    ourModalEntities.remove(index);
    ourModalityStack.remove(index + 1);
    for (int i = 1; i < ourModalityStack.size(); i++) {
      ourModalityStack.get(i).removeModality(modalEntity);
    }

    reincludeSkippedItems();
    requestFlush();
  }

  @TestOnly
  public static void leaveAllModals() {
    while (!ourModalEntities.isEmpty()) {
      leaveModal(ourModalEntities.get(ourModalEntities.size() - 1));
    }
    LOG.assertTrue(getCurrentModalityState() == ModalityState.NON_MODAL, getCurrentModalityState());
    reincludeSkippedItems();
    requestFlush();
  }

  @Nonnull
  private static Object[] getCurrentModalEntitiesForProject(Project project) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (project == null || !ourModalEntities.isEmpty()) {
      return ArrayUtil.toObjectArray(ourModalEntities);
    }
    return ArrayUtil.toObjectArray(projectToModalEntities.get(project));
  }

  @Nonnull
  public static Object[] getCurrentModalEntities() {
    return getCurrentModalEntitiesForProject(null);
  }

  @Nonnull
  public static ModalityStateEx getCurrentModalityState() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    return ourModalityStack.peek();
  }

  public static boolean isInModalContextForProject(final Project project) {
    LOG.assertTrue(isDispatchThread());

    if (ourModalEntities.isEmpty()) return false;

    List<Dialog> modalEntitiesForProject = getModalEntitiesForProject(project);

    return modalEntitiesForProject == null || modalEntitiesForProject.isEmpty();
  }

  private static List<Dialog> getModalEntitiesForProject(Project project) {
    return projectToModalEntities.get(project);
  }

  public static boolean isInModalContext() {
    return isInModalContextForProject(null);
  }

  private static boolean isDispatchThread() {
    return ApplicationManager.getApplication().isDispatchThread();
  }

  private static void requestFlush() {
    if (FLUSHER_SCHEDULED.compareAndSet(false, true)) {
      SwingUtilities.invokeLater(ourFlushQueueRunnable);
    }
  }

  /**
   * There might be some requests in the queue, but ourFlushQueueRunnable might not be scheduled yet. In these circumstances
   * {@link EventQueue#peekEvent()} default implementation would return null, and {@link UIUtil#dispatchAllInvocationEvents()} would
   * stop processing events too early and lead to spurious test failures.
   *
   * @see IdeEventQueue#peekEvent()
   */
  public static boolean ensureFlushRequested() {
    if (getNextEvent(false) != null) {
      SwingUtilities.invokeLater(ourFlushQueueRunnable);
      return true;
    }
    return false;
  }

  @Nullable
  private static RunnableInfo getNextEvent(boolean remove) {
    synchronized (LOCK) {
      ModalityState currentModality = getCurrentModalityState();

      while (!ourQueue.isEmpty()) {
        RunnableInfo info = ourQueue.getFirst();

        if (info.expired.value(null)) {
          ourQueue.removeFirst();
          info.markDone();
          continue;
        }

        if (!currentModality.dominates(info.modalityState)) {
          if (remove) {
            ourQueue.removeFirst();
          }
          return info;
        }
        ourSkippedItems.add(ourQueue.removeFirst());
      }

      return null;
    }
  }

  private static final AtomicBoolean FLUSHER_SCHEDULED = new AtomicBoolean(false);

  private static class FlushQueue implements Runnable {
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
    private RunnableInfo myLastInfo;

    @Override
    public void run() {
      FLUSHER_SCHEDULED.set(false);
      long startTime = System.currentTimeMillis();
      while (true) {
        if (!runNextEvent()) {
          return;
        }
        if (System.currentTimeMillis() - startTime > 5) {
          requestFlush();
          return;
        }
      }
    }

    private boolean runNextEvent() {
      final RunnableInfo lastInfo = getNextEvent(true);
      myLastInfo = lastInfo;

      if (lastInfo != null) {
        try {
          doRun(lastInfo);
          lastInfo.markDone();
        }
        catch (ProcessCanceledException ignored) {
        }
        catch (Throwable t) {
          LOG.error(t);
        }
        finally {
          if (!DEBUG) myLastInfo = null;
        }
      }
      return lastInfo != null;
    }

    // Extracted to have a capture point
    private static void doRun(RunnableInfo info) {
      info.runnable.run();
    }

    @Override
    public String toString() {
      return "LaterInvocator.FlushQueue" + (myLastInfo == null ? "" : " lastInfo=" + myLastInfo);
    }
  }

  @TestOnly
  public static Collection<RunnableInfo> getLaterInvocatorQueue() {
    synchronized (LOCK) {
      // used by leak hunter as root, so we must not copy it here to another list
      // to avoid walking over obsolete queue
      return Collections.unmodifiableCollection(ourQueue);
    }
  }

  public static void purgeExpiredItems() {
    synchronized (LOCK) {
      reincludeSkippedItems();

      List<RunnableInfo> alive = new ArrayList<>(ourQueue.size());
      for (RunnableInfo info : ourQueue) {
        if (info.expired.value(null)) {
          info.markDone();
        }
        else {
          alive.add(info);
        }
      }
      if (alive.size() < ourQueue.size()) {
        ourQueue.clear();
        ourQueue.addAll(alive);
      }
    }
  }

  @TestOnly
  public static void dispatchPendingFlushes() {
    if (!isDispatchThread()) throw new IllegalStateException("Must call from EDT");

    Semaphore semaphore = new Semaphore();
    semaphore.down();
    invokeLaterWithCallback(semaphore::up, ModalityState.any(), Conditions.FALSE, null);
    while (!semaphore.isUp()) {
      UIUtil.dispatchAllInvocationEvents();
    }
  }
}
