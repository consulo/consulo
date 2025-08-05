// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.impl.internal;

import consulo.application.util.Semaphore;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.proxy.EventDispatcher;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ModalityStateListener;
import consulo.util.collection.*;
import consulo.util.concurrent.ActionCallback;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.SystemProperties;
import consulo.util.lang.ref.Ref;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

public final class LaterInvocator {
  private static final Logger LOG = Logger.getInstance(LaterInvocator.class);

  private LaterInvocator() {
  }

  // Application modal entities
  private static final List<Object> ourModalEntities = Lists.newLockFreeCopyOnWriteList();

  // Per-project modal entities
  private static final Map<Project, List<Dialog>> projectToModalEntities = Maps.newWeakHashMap();
  private static final Map<Project, Stack<ModalityState>> projectToModalEntitiesStack = Maps.newWeakHashMap();
  private static final Stack<IdeaModalityStateEx> ourModalityStack = new Stack<>((IdeaModalityStateEx)ModalityStateImpl.NON_MODAL);
  private static final EventDispatcher<ModalityStateListener> ourModalityStateMulticaster = EventDispatcher.create(ModalityStateListener.class);

  private static final FlushQueue ourEdtQueue = new FlushQueue(SwingUtilities::invokeLater);

  public static void addModalityStateListener(@RequiredUIAccess @Nonnull ModalityStateListener listener, @Nonnull Disposable parentDisposable) {
    if (!ourModalityStateMulticaster.getListeners().contains(listener)) {
      ourModalityStateMulticaster.addListener(listener, parentDisposable);
    }
  }

  private static final ConcurrentMap<Window, IdeaModalityStateEx> ourWindowModalities = Maps.newConcurrentWeakHashMap();

  @Nonnull
  public static IdeaModalityStateEx modalityStateForWindow(@Nonnull Window window) {
    return ourWindowModalities.computeIfAbsent(window, __ -> {
      synchronized (ourModalityStack) {
        for (IdeaModalityStateEx state : ourModalityStack) {
          if (state.getModalEntities().contains(window)) {
            return state;
          }
        }
      }

      Window owner = window.getOwner();
     IdeaModalityStateEx ownerState = owner == null ? (IdeaModalityStateEx)IdeaModalityState.nonModal() : modalityStateForWindow(owner);
      return isModalDialog(window) ? ownerState.appendEntity(window) : ownerState;
    });
  }

  private static boolean isModalDialog(@Nonnull Object window) {
    return window instanceof Dialog && ((Dialog)window).isModal();
  }

  @Nonnull
  public static AsyncResult<Void> invokeLater(@Nonnull Runnable runnable, @Nonnull BooleanSupplier expired) {
    ModalityState modalityState = IdeaModalityState.defaultModalityState();
    return invokeLater(runnable, modalityState, expired);
  }

  @Nonnull
  public static AsyncResult<Void> invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState modalityState) {
    return invokeLater(runnable, modalityState, () -> false);
  }

  @Nonnull
  public static AsyncResult<Void> invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState modalityState, @Nonnull BooleanSupplier expired) {
    AsyncResult<Void> callback = AsyncResult.undefined();
    invokeLaterWithCallback(runnable, modalityState, expired, callback);
    return callback;
  }

  public static void invokeLaterWithCallback(@Nonnull Runnable runnable,
                                             @Nonnull ModalityState modalityState,
                                             @Nonnull BooleanSupplier expired,
                                             @Nullable ActionCallback callback) {
    if (expired.getAsBoolean()) {
      if (callback != null) {
        callback.setRejected();
      }
      return;
    }
    FlushQueue.RunnableInfo runnableInfo = new FlushQueue.RunnableInfo(runnable, modalityState, expired, callback);
    getRunnableQueue().push(runnableInfo);
    requestFlush();
  }

  public static void invokeAndWait(@Nonnull final Runnable runnable, @Nonnull ModalityState modalityState) {
    UIAccess.assetIsNotUIThread();

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
    invokeLaterWithCallback(runnable1, modalityState, () -> false, null);
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
    IdeaModalityStateEx state = getCurrentModalityState().appendEntity(modalEntity);
    if (isModalDialog(modalEntity)) {
      List<Object> currentEntities = state.getModalEntities();
      state = modalityStateForWindow((Window)modalEntity);
      state.forceModalEntities(currentEntities);
    }
    enterModal(modalEntity, state);
  }

  public static void enterModal(@Nonnull Object modalEntity, @Nonnull IdeaModalityStateEx appendedState) {
    assertIsDispatchThread();

    if (LOG.isDebugEnabled()) {
      LOG.debug("enterModal:" + modalEntity);
    }

    ourModalityStateMulticaster.getMulticaster().beforeModalityStateChanged(true, modalEntity);

    ourModalEntities.add(modalEntity);
    synchronized (ourModalityStack) {
      ourModalityStack.push(appendedState);
    }

    reincludeSkippedItemsAndRequestFlush();
  }

  public static void enterModal(Project project, @Nonnull Dialog dialog) {
    assertIsDispatchThread();

    if (LOG.isDebugEnabled()) {
      LOG.debug("enterModal:" + dialog.getName() + " ; for project: " + project.getName());
    }

    if (project == null) {
      enterModal(dialog);
      return;
    }

    ourModalityStateMulticaster.getMulticaster().beforeModalityStateChanged(true, dialog);

    List<Dialog> modalEntitiesList = projectToModalEntities.getOrDefault(project, Lists.newLockFreeCopyOnWriteList());
    projectToModalEntities.put(project, modalEntitiesList);
    modalEntitiesList.add(dialog);

    Stack<ModalityState> modalEntitiesStack = projectToModalEntitiesStack.getOrDefault(project, new Stack<>(IdeaModalityState.nonModal()));
    projectToModalEntitiesStack.put(project, modalEntitiesStack);
    modalEntitiesStack.push(new IdeaModalityStateEx(ourModalEntities));
  }

  public static void leaveModal(Project project, @Nonnull Dialog dialog) {
    assertIsDispatchThread();

    if (LOG.isDebugEnabled()) {
      LOG.debug("leaveModal:" + dialog.getName() + " ; for project: " + project.getName());
    }

    ourModalityStateMulticaster.getMulticaster().beforeModalityStateChanged(false, dialog);

    int index = ourModalEntities.indexOf(dialog);

    if (index != -1) {
      removeModality(dialog, index);
    }
    else if (project != null) {
      List<Dialog> dialogs = projectToModalEntities.get(project);
      int perProjectIndex = dialogs.indexOf(dialog);
      LOG.assertTrue(perProjectIndex >= 0);
      dialogs.remove(perProjectIndex);
      Stack<ModalityState> states = projectToModalEntitiesStack.get(project);
      states.remove(perProjectIndex + 1);
      for (int i = 1; i < states.size(); i++) {
        ((IdeaModalityStateEx)states.get(i)).removeModality(dialog);
      }
    }

    reincludeSkippedItemsAndRequestFlush();
  }

  private static void removeModality(@Nonnull Object modalEntity, int index) {
    assertIsDispatchThread();

    ourModalEntities.remove(index);
    synchronized (ourModalityStack) {
      ourModalityStack.remove(index + 1);
      for (int i = 1; i < ourModalityStack.size(); i++) {
        ourModalityStack.get(i).removeModality(modalEntity);
      }
    }
  }

  public static void leaveModal(@Nonnull Object modalEntity) {
    assertIsDispatchThread();

    if (LOG.isDebugEnabled()) {
      LOG.debug("leaveModal:" + modalEntity);
    }

    ourModalityStateMulticaster.getMulticaster().beforeModalityStateChanged(false, modalEntity);

    int index = ourModalEntities.indexOf(modalEntity);
    LOG.assertTrue(index >= 0);
    removeModality(modalEntity, index);

    reincludeSkippedItemsAndRequestFlush();
  }

  @TestOnly
  public static void leaveAllModals() {
    while (!ourModalEntities.isEmpty()) {
      leaveModal(ourModalEntities.get(ourModalEntities.size() - 1));
    }
    LOG.assertTrue(getCurrentModalityState() == IdeaModalityState.nonModal(), getCurrentModalityState());
    reincludeSkippedItemsAndRequestFlush();
  }

  @Nonnull
  public static Object [] getCurrentModalEntities() {
    assertIsDispatchThread();

    return ArrayUtil.toObjectArray(ourModalEntities);
  }

  @Nonnull
  public static IdeaModalityStateEx getCurrentModalityState() {
    assertIsDispatchThread();

    synchronized (ourModalityStack) {
      return ourModalityStack.peek();
    }
  }

  public static boolean isInModalContextForProject(final Project project) {
    assertIsDispatchThread();

    if (ourModalEntities.isEmpty()) return false;

    List<Dialog> modalEntitiesForProject = projectToModalEntities.get(project);

    return modalEntitiesForProject == null || modalEntitiesForProject.isEmpty();
  }

  public static boolean isInModalContext() {
    return isInModalContextForProject(null);
  }

  @RequiredUIAccess
  private static void assertIsDispatchThread() {
    UIAccess.assertIsUIThread();
  }

  @Nonnull
  private static FlushQueue getRunnableQueue() {
    return ourEdtQueue;
  }

  static void requestFlush() {
    SUBMITTED_COUNT.incrementAndGet();
    while (FLUSHER_SCHEDULED.compareAndSet(false, true)) {
      long submittedCount = SUBMITTED_COUNT.get();

      FlushQueue firstQueue = getRunnableQueue();
      if (firstQueue.mayHaveItems()) {
        firstQueue.scheduleFlush();
        return;
      }

      FLUSHER_SCHEDULED.set(false);

      // If a requestFlush was called by somebody else (because queues were modified) but we have not really scheduled anything
      // then we've missed `mayHaveItems` `true` value because of race.
      // Another run of `requestFlush` will get the correct `mayHaveItems` because
      // `mayHaveItems` is mutated strictly before SUBMITTED_COUNT which we've observe below
      if (submittedCount == SUBMITTED_COUNT.get()) {
        break;
      }
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
    if (ourEdtQueue.getNextEvent(false) != null) {
      ourEdtQueue.scheduleFlush();
      return true;
    }
    return false;
  }

  static final AtomicBoolean FLUSHER_SCHEDULED = new AtomicBoolean(false);

  private static final AtomicLong SUBMITTED_COUNT = new AtomicLong(0);

  private static void reincludeSkippedItemsAndRequestFlush() {
    ourEdtQueue.reincludeSkippedItems();
    requestFlush();
  }

  public static void purgeExpiredItems() {
    ourEdtQueue.purgeExpiredItems();
    requestFlush();
  }
}
