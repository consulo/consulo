// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.impl.internal;

import consulo.application.util.Semaphore;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.Lists;
import consulo.util.collection.Maps;
import consulo.util.collection.Stack;
import consulo.util.concurrent.ActionCallback;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.SystemProperties;
import consulo.util.lang.ref.Ref;
import org.jetbrains.annotations.TestOnly;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class ModalityContext {
    private static final Logger LOG = Logger.getInstance(ModalityContext.class);

    private final List<Object> myModalEntities = Lists.newLockFreeCopyOnWriteList();

    private final Map<Project, List<Dialog>> myProjectToModalEntities = Maps.newWeakHashMap();
    private final Map<Project, Stack<ModalityState>> myProjectToModalEntitiesStack = Maps.newWeakHashMap();
    private final Stack<IdeaModalityStateEx> myModalityStack = new Stack<>((IdeaModalityStateEx) ModalityStateImpl.NON_MODAL);

    private final ConcurrentMap<Window, IdeaModalityStateEx> myWindowModalities = Maps.newConcurrentWeakHashMap();

    private final FlushQueue myQueue;

    final AtomicBoolean myFlusherScheduled = new AtomicBoolean(false);
    private final AtomicLong mySubmittedCount = new AtomicLong(0);

    ModalityContext(Consumer<? super Runnable> flushExecutor) {
        myQueue = new FlushQueue(this, flushExecutor);
    }

    public IdeaModalityStateEx modalityStateForWindow(Window window) {
        return myWindowModalities.computeIfAbsent(window, __ -> {
            synchronized (myModalityStack) {
                for (IdeaModalityStateEx state : myModalityStack) {
                    if (state.getModalEntities().contains(window)) {
                        return state;
                    }
                }
            }

            Window owner = window.getOwner();
            IdeaModalityStateEx ownerState =
                owner == null ? (IdeaModalityStateEx) IdeaModalityState.nonModal() : modalityStateForWindow(owner);
            return isModalDialog(window) ? ownerState.appendEntity(window) : ownerState;
        });
    }

    private static boolean isModalDialog(Object window) {
        return window instanceof Dialog dialog && dialog.isModal();
    }

    public AsyncResult<Void> invokeLater(Runnable runnable, BooleanSupplier expired) {
        ModalityState modalityState = IdeaModalityState.defaultModalityState();
        return invokeLater(runnable, modalityState, expired);
    }

    public AsyncResult<Void> invokeLater(Runnable runnable, ModalityState modalityState) {
        return invokeLater(runnable, modalityState, () -> false);
    }

    public AsyncResult<Void> invokeLater(Runnable runnable, ModalityState modalityState, BooleanSupplier expired) {
        AsyncResult<Void> callback = AsyncResult.undefined();
        invokeLaterWithCallback(runnable, modalityState, expired, callback);
        return callback;
    }

    public void invokeLaterWithCallback(
        Runnable runnable,
        ModalityState modalityState,
        BooleanSupplier expired,
        @Nullable ActionCallback callback
    ) {
        if (expired.getAsBoolean()) {
            if (callback != null) {
                callback.setRejected();
            }
            return;
        }
        FlushQueue.RunnableInfo runnableInfo = new FlushQueue.RunnableInfo(runnable, modalityState, expired, callback);
        myQueue.push(runnableInfo);
        requestFlush();
    }

    public void invokeAndWait(Runnable runnable, ModalityState modalityState) {
        UIAccess.assetIsNotUIThread();

        Semaphore semaphore = new Semaphore();
        semaphore.down();
        Ref<Throwable> exception = Ref.create();
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
            public String toString() {
                return "InvokeAndWait[" + runnable + "]";
            }
        };
        invokeLaterWithCallback(runnable1, modalityState, () -> false, null);
        semaphore.waitFor();
        if (exception.isNotNull()) {
            Throwable cause = exception.get();
            if (SystemProperties.getBooleanProperty("invoke.later.wrap.error", true)) {
                throw new RuntimeException(cause);
            }
            else {
                ExceptionUtil.rethrow(cause);
            }
        }
    }

    @RequiredUIAccess
    public void enterModal(Object modalEntity) {
        IdeaModalityStateEx state = getCurrentModalityState().appendEntity(modalEntity);
        if (isModalDialog(modalEntity)) {
            List<Object> currentEntities = state.getModalEntities();
            state = modalityStateForWindow((Window) modalEntity);
            state.forceModalEntities(currentEntities);
        }
        enterModal(modalEntity, state);
    }

    @RequiredUIAccess
    public void enterModal(Object modalEntity, IdeaModalityStateEx appendedState) {
        UIAccess.assertIsUIThread();

        if (LOG.isDebugEnabled()) {
            LOG.debug("enterModal:" + modalEntity);
        }

        LaterInvocator.fireBeforeModalityStateChanged(true, modalEntity);

        myModalEntities.add(modalEntity);
        synchronized (myModalityStack) {
            myModalityStack.push(appendedState);
        }

        reincludeSkippedItemsAndRequestFlush();
    }

    @RequiredUIAccess
    public void enterModal(@Nullable Project project, Dialog dialog) {
        UIAccess.assertIsUIThread();

        if (project == null) {
            enterModal(dialog);
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("enterModal:" + dialog.getName() + " ; for project: " + project.getName());
        }

        LaterInvocator.fireBeforeModalityStateChanged(true, dialog);

        List<Dialog> modalEntitiesList = myProjectToModalEntities.getOrDefault(project, Lists.newLockFreeCopyOnWriteList());
        myProjectToModalEntities.put(project, modalEntitiesList);
        modalEntitiesList.add(dialog);

        Stack<ModalityState> modalEntitiesStack = myProjectToModalEntitiesStack.getOrDefault(project, new Stack<>(IdeaModalityState.nonModal()));
        myProjectToModalEntitiesStack.put(project, modalEntitiesStack);
        modalEntitiesStack.push(new IdeaModalityStateEx(myModalEntities));
    }

    @RequiredUIAccess
    public void leaveModal(@Nullable Project project, Dialog dialog) {
        UIAccess.assertIsUIThread();

        if (LOG.isDebugEnabled()) {
            LOG.debug("leaveModal:" + dialog.getName() + " ; for project: " + (project == null ? null : project.getName()));
        }

        LaterInvocator.fireBeforeModalityStateChanged(false, dialog);

        int index = myModalEntities.indexOf(dialog);

        if (index != -1) {
            removeModality(dialog, index);
        }
        else if (project != null) {
            List<Dialog> dialogs = myProjectToModalEntities.get(project);
            int perProjectIndex = dialogs.indexOf(dialog);
            LOG.assertTrue(perProjectIndex >= 0);
            dialogs.remove(perProjectIndex);
            Stack<ModalityState> states = myProjectToModalEntitiesStack.get(project);
            states.remove(perProjectIndex + 1);
            for (int i = 1; i < states.size(); i++) {
                ((IdeaModalityStateEx) states.get(i)).removeModality(dialog);
            }
        }

        reincludeSkippedItemsAndRequestFlush();
    }

    @RequiredUIAccess
    private void removeModality(Object modalEntity, int index) {
        UIAccess.assertIsUIThread();

        myModalEntities.remove(index);
        synchronized (myModalityStack) {
            myModalityStack.remove(index + 1);
            for (int i = 1; i < myModalityStack.size(); i++) {
                myModalityStack.get(i).removeModality(modalEntity);
            }
        }
    }

    @RequiredUIAccess
    public void leaveModal(Object modalEntity) {
        UIAccess.assertIsUIThread();

        if (LOG.isDebugEnabled()) {
            LOG.debug("leaveModal:" + modalEntity);
        }

        LaterInvocator.fireBeforeModalityStateChanged(false, modalEntity);

        int index = myModalEntities.indexOf(modalEntity);
        LOG.assertTrue(index >= 0);
        removeModality(modalEntity, index);

        reincludeSkippedItemsAndRequestFlush();
    }

    @TestOnly
    @RequiredUIAccess
    public void leaveAllModals() {
        while (!myModalEntities.isEmpty()) {
            leaveModal(myModalEntities.get(myModalEntities.size() - 1));
        }
        LOG.assertTrue(getCurrentModalityState() == IdeaModalityState.nonModal(), getCurrentModalityState());
        reincludeSkippedItemsAndRequestFlush();
    }

    @RequiredUIAccess
    public Object[] getCurrentModalEntities() {
        UIAccess.assertIsUIThread();

        return ArrayUtil.toObjectArray(myModalEntities);
    }

    @RequiredUIAccess
    public IdeaModalityStateEx getCurrentModalityState() {
        UIAccess.assertIsUIThread();

        synchronized (myModalityStack) {
            return myModalityStack.peek();
        }
    }

    @RequiredUIAccess
    public boolean isInModalContextForProject(@Nullable Project project) {
        UIAccess.assertIsUIThread();

        if (myModalEntities.isEmpty()) {
            return false;
        }

        List<Dialog> modalEntitiesForProject = myProjectToModalEntities.get(project);

        return modalEntitiesForProject == null || modalEntitiesForProject.isEmpty();
    }

    @RequiredUIAccess
    public boolean isInModalContext() {
        return isInModalContextForProject(null);
    }

    void requestFlush() {
        mySubmittedCount.incrementAndGet();
        while (myFlusherScheduled.compareAndSet(false, true)) {
            long submittedCount = mySubmittedCount.get();

            if (myQueue.mayHaveItems()) {
                myQueue.scheduleFlush();
                return;
            }

            myFlusherScheduled.set(false);

            // If a requestFlush was called by somebody else (because queues were modified) but we have not really scheduled anything
            // then we've missed `mayHaveItems` `true` value because of race.
            // Another run of `requestFlush` will get the correct `mayHaveItems` because
            // `mayHaveItems` is mutated strictly before SUBMITTED_COUNT which we've observe below
            if (submittedCount == mySubmittedCount.get()) {
                break;
            }
        }
    }

    /**
     * There might be some requests in the queue, but the flush might not be scheduled yet, and an event queue which
     * reports itself empty at that moment would stop dispatching too early.
     */
    public boolean ensureFlushRequested() {
        if (myQueue.getNextEvent(false) != null) {
            myQueue.scheduleFlush();
            return true;
        }
        return false;
    }

    private void reincludeSkippedItemsAndRequestFlush() {
        myQueue.reincludeSkippedItems();
        requestFlush();
    }

    public void purgeExpiredItems() {
        myQueue.purgeExpiredItems();
        requestFlush();
    }
}
