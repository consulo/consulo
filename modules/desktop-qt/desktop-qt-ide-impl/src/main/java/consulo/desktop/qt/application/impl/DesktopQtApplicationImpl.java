/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.qt.application.impl;

import consulo.application.concurrent.coroutine.WriteLock;
import consulo.application.impl.internal.ReadMostlyRWLock;
import consulo.application.impl.internal.UnifiedApplication;
import consulo.application.internal.AppLifecycleListener;
import consulo.application.internal.StartupProgress;
import consulo.component.internal.ComponentBinding;
import consulo.desktop.application.util.Restarter;
import consulo.desktop.qt.ui.impl.DesktopQtUIAccess;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineContext;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CallSubroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.lang.ref.SimpleReference;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtApplicationImpl extends UnifiedApplication {
    private static final Logger LOG = Logger.getInstance(DesktopQtApplicationImpl.class);

    /** An exit already under way is not started a second time - a close asked for twice would tear down twice. */
    private volatile boolean myExiting;

    public DesktopQtApplicationImpl(ComponentBinding componentBinding, SimpleReference<? extends StartupProgress> splashRef) {
        super(componentBinding, splashRef);

        // the qt frontend owns a real ui thread, so a write action can be handed to it the way the awt one
        // does. the StampedRWLock a unified application takes cannot transfer, and the write thread would
        // then block on giveAndWait while the ui thread waits for the very lock it holds
        myLock = new ReadMostlyRWLock(null);
    }

    @Override
    public UIAccess getLastUIAccess() {
        return DesktopQtUIAccess.INSTANCE;
    }

    @Override
    public void exit() {
        exit(false, false);
    }

    @Override
    public void exit(boolean force, boolean exitConfirmed) {
        exit(force, exitConfirmed, true, false);
    }

    @Override
    public void restart(boolean exitConfirmed) {
        exit(false, exitConfirmed, true, true);
    }

    /**
     * Ends the process, which a unified application leaves alone - it cannot know whether the frontend it runs
     * under owns one. The qt frontend does, and nothing else is going to end it: the thread the qt loop runs on is
     * a daemon which never returns, so the ide would tear itself down and the process stay up.
     */
    public void exit(boolean force, boolean exitConfirmed, boolean allowListenersToCancel, boolean restart) {
        if (!force && myExiting) {
            return;
        }

        myExiting = true;

        if (isDispatchThread()) {
            doExit(allowListenersToCancel, restart);
        }
        else {
            invokeLater(() -> doExit(allowListenersToCancel, restart), getNoneModalityState());
        }
    }

    @RequiredUIAccess
    private void doExit(boolean allowListenersToCancel, boolean restart) {
        getMessageBus().syncPublisher(AppLifecycleListener.class).appClosing();

        if (allowListenersToCancel && !canExit()) {
            myExiting = false;
            return;
        }

        UIAccess uiAccess = UIAccess.current();

        // one project at a time, and one which refuses to close stops the rest - the order the awt application
        // closes them in
        disposeAllProjectsAsync(allowListenersToCancel, uiAccess).whenComplete((allClosed, throwable) -> {
            if (throwable != null || !Boolean.TRUE.equals(allClosed) || isUnitTestMode()) {
                myExiting = false;
                return;
            }

            CoroutineContext context = coroutineContext().copy();
            context.putCopyableUserData(UIAccess.KEY, uiAccess);

            Coroutine.<Void, Object>first(CodeExecution.<Void, Object>apply(input -> null))
                .then(CallSubroutine.call(saveSettingsAsync()))
                .then(WriteLock.<Object, Object>apply(input -> {
                    Disposer.dispose(DesktopQtApplicationImpl.this);
                    return input;
                }))
                .then(CodeExecution.<Object, Object>apply(input -> {
                    System.exit(exitCode(restart));
                    return input;
                }))
                .runAsync(CoroutineScope.of(context), null);
        });
    }

    private static int exitCode(boolean restart) {
        if (!restart || !Restarter.isSupported()) {
            return 0;
        }

        try {
            return Restarter.scheduleRestart();
        }
        catch (IOException e) {
            LOG.warn("Cannot restart", e);
            return 0;
        }
    }

    private CompletableFuture<Boolean> disposeAllProjectsAsync(boolean checkCanClose, UIAccess uiAccess) {
        ProjectManager manager = ProjectManager.getInstance();

        CompletableFuture<Boolean> chain = CompletableFuture.completedFuture(Boolean.TRUE);
        for (Project project : manager.getOpenProjects()) {
            chain = chain.thenCompose(allClosed -> allClosed
                ? manager.closeAndDisposeAsync(project, uiAccess, checkCanClose, true, true)
                : CompletableFuture.completedFuture(Boolean.FALSE));
        }
        return chain;
    }
}
