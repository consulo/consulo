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
package consulo.desktop.awt.application.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentProfiles;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.ApplicationProperties;
import consulo.application.impl.internal.BaseApplication;
import consulo.application.impl.internal.StampedRWLock;
import consulo.application.impl.internal.concurent.AppScheduledExecutorService;
import consulo.application.impl.internal.start.CommandLineArgs;
import consulo.application.impl.internal.start.StartupUtil;
import consulo.application.internal.AppLifecycleListener;
import consulo.application.internal.StartupProgress;
import consulo.application.localize.ApplicationLocalize;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.util.concurrent.ThreadDumper;
import consulo.awt.hacking.AWTAccessorHacking;
import consulo.awt.hacking.AWTAutoShutdownHacking;
import consulo.component.ComponentManager;
import consulo.component.internal.ComponentBinding;
import consulo.component.internal.inject.InjectingContainerBuilder;
import consulo.desktop.application.util.Restarter;
import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.desktop.awt.ui.impl.AWTUIAccessImpl;
import consulo.desktop.boot.main.windows.WindowsCommandLineProcessor;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.ide.ApplicationActivationStateManager;
import consulo.ide.impl.idea.ide.CommandLineProcessor;
import consulo.ide.impl.idea.ide.GeneralSettings;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.logging.attachment.Attachment;
import consulo.logging.attachment.AttachmentFactory;
import consulo.logging.internal.LogEventException;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.WindowManager;
import consulo.application.concurrent.coroutine.WriteLock;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.AppIcon;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.MessageDialogBuilder;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.coroutine.UIAction;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.ArrayUtil;
import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineContext;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.concurrent.coroutine.step.CompletableFutureStep;
import consulo.util.lang.ShutDownTracker;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import org.jspecify.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DesktopApplicationImpl extends BaseApplication {
    private static final Logger LOG = Logger.getInstance(DesktopApplicationImpl.class);

    private final boolean myHeadlessMode;
    private final boolean myIsInternal;

    private volatile boolean myDisposeInProgress;

    public DesktopApplicationImpl(
        ComponentBinding componentBinding,
        boolean isHeadless,
        SimpleReference<? extends StartupProgress> splashRef
    ) {
        super(componentBinding, splashRef);

        ApplicationManager.setApplication(this);

        myIsInternal = ApplicationProperties.isInternal();

        String debugDisposer = System.getProperty("idea.disposer.debug");
        Disposer.setDebugMode((myIsInternal || "on".equals(debugDisposer)) && !"off".equals(debugDisposer));

        myHeadlessMode = isHeadless;

        myDoNotSave = isHeadless;
        myGatherStatistics = LOG.isDebugEnabled() || isInternal();

        if (!isHeadless) {
            Disposer.register(this, Disposable.newDisposable(), "ui");

            StartupUtil.addExternalInstanceListener(commandLineArgs -> {
                LOG.info("ApplicationImpl.externalInstanceListener invocation");

                CommandLineProcessor.processExternalCommandLine(commandLineArgs, null).whenComplete((project, error) -> {
                    if (error == null && project != null) {
                        IdeFrame frame = WindowManager.getInstance().getIdeFrame(project);

                        if (frame != null) {
                            AppIcon.getInstance().requestFocus(frame.getWindow());
                        }
                    }
                });
            });

            WindowsCommandLineProcessor.LISTENER = (currentDirectory, commandLine) -> {
                LOG.info("Received external Windows command line: current directory " + currentDirectory + ", command line " + commandLine);
                invokeLater(() -> {
                    List<String> args = StringUtil.splitHonorQuotes(commandLine, ' ');
                    args.remove(0);   // process name
                    CommandLineProcessor.processExternalCommandLine(CommandLineArgs.parse(ArrayUtil.toStringArray(args)), currentDirectory);
                });
            };
        }

        UIUtil.invokeAndWaitIfNeeded(() -> {
            // instantiate AppDelayQueue which starts "Periodic task thread" which we'll mark busy to prevent this EDT to die
            // that thread was chosen because we know for sure it's running
            AppScheduledExecutorService service = (AppScheduledExecutorService)AppExecutorUtil.getAppScheduledExecutorService();
            Thread thread = service.getPeriodicTasksThread();
            AWTAutoShutdownHacking.notifyThreadBusy(thread); // needed for EDT not to exit suddenly
            Disposer.register(this, () -> {
                AWTAutoShutdownHacking.notifyThreadFree(thread); // allow for EDT to exit - needed for Upsource
            });
            return null;
        });

        myLock = new StampedRWLock();

        NoSwingUnderWriteAction.watchForEvents(this);
    }

    @Override
    public ComponentManager getApplication() {
        return this;
    }

    @Override
    public int getProfiles() {
        return super.getProfiles() | ComponentProfiles.AWT;
    }

    @Override
    protected void bootstrapInjectingContainer(InjectingContainerBuilder builder) {
        super.bootstrapInjectingContainer(builder);
    }

    private CompletableFuture<Boolean> disposeAllProjectsAsync(boolean checkCanClose, UIAccess uiAccess) {
        ProjectManager manager = ProjectManager.getInstance();
        Project[] projects = manager.getOpenProjects();

        // Close projects sequentially - if any vetoes, stop
        CompletableFuture<Boolean> chain = CompletableFuture.completedFuture(true);
        for (Project project : projects) {
            chain = chain.thenCompose(allClosed -> {
                if (!allClosed) {
                    return CompletableFuture.completedFuture(false);
                }
                return manager.closeProjectAsync(project, uiAccess, checkCanClose, true, true);
            });
        }
        return chain;
    }

    @Override
    public boolean isInternal() {
        return myIsInternal;
    }

    @Override
    public boolean isHeadlessEnvironment() {
        return myHeadlessMode;
    }

    @RequiredUIAccess
    @Override
    public long getIdleTime() {
        assertIsDispatchThread();
        return IdeEventQueue.getInstance().getIdleTime();
    }

    @Override
    public void exit() {
        exit(false, false);
    }

    @Override
    public void exit(boolean force, boolean exitConfirmed) {
        exit(false, exitConfirmed, true, false);
    }

    @Override
    public void restart(boolean exitConfirmed) {
        exit(false, exitConfirmed, true, true);
    }

    /*
     * There are two ways we can get an exit notification.
     *  1. From user input i.e. ExitAction
     *  2. From the native system.
     *  We should not process any quit notifications if we are handling another one
     *
     *  Note: there are possible scenarios when we get a quit notification at a moment when another
     *  quit message is shown. In that case, showing multiple messages sounds contra-intuitive as well
     */
    private static volatile boolean exiting = false;

    public void exit(
        final boolean force,
        final boolean exitConfirmed,
        final boolean allowListenersToCancel,
        final boolean restart
    ) {
        if (!force && exiting) {
            return;
        }

        exiting = true;
        try {
            // modality state is always nonModal now — this check is always false
            // keeping the structure for potential future use

            Runnable runnable = new Runnable() {
                @Override
                @RequiredUIAccess
                public void run() {
                    if (!force && !confirmExitIfNeeded(exitConfirmed)) {
                        saveAll();
                        return;
                    }

                    getMessageBus().syncPublisher(AppLifecycleListener.class).appClosing();
                    myDisposeInProgress = true;
                    doExit(allowListenersToCancel, restart);
                }
            };

            if (isDispatchThread()) {
                runnable.run();
            }
            else {
                invokeLater(runnable, Application.get().getNoneModalityState());
            }
        }
        finally {
            exiting = false;
        }
    }

    @RequiredUIAccess
    private void doExit(boolean allowListenersToCancel, boolean restart) {
        UIAccess uiAccess = UIAccess.current();

        CoroutineContext ctx = coroutineContext().copy();
        ctx.putCopyableUserData(UIAccess.KEY, uiAccess);
        CoroutineScope scope = new CoroutineScope(ctx);

        Coroutine.<Void, Void>first(UIAction.<Void, Void>apply((input, continuation) -> {
            // Step 1 (UIAction): Check canExit veto (may show dialogs)
            if (allowListenersToCancel && !canExit()) {
                myDisposeInProgress = false;
                @SuppressWarnings("unchecked")
                Continuation<Void> typed = (Continuation<Void>) continuation;
                typed.finishEarly(null);
                return null;
            }
            return null;
        })).then(CompletableFutureStep.<Void, Boolean>await(ignored -> {
            // Step 2 (CompletableFutureStep): Close all projects asynchronously
            return disposeAllProjectsAsync(allowListenersToCancel, uiAccess);
        })).then(CodeExecution.<Boolean, Boolean>apply((allClosed, continuation) -> {
            // Step 3 (CodeExecution): Check result and save settings from background
            if (!allClosed || isUnitTestMode()) {
                myDisposeInProgress = false;
                @SuppressWarnings("unchecked")
                Continuation<Boolean> typed = (Continuation<Boolean>) continuation;
                typed.finishEarly(Boolean.FALSE);
                return null;
            }
            saveSettings(uiAccess);
            return Boolean.TRUE;
        })).then(WriteLock.<Boolean, Boolean>apply(proceed -> {
            // Step 4 (WriteLock): Dispose application under write lock (off EDT)
            Disposer.dispose(DesktopApplicationImpl.this);
            return proceed;
        })).then(CodeExecution.<Boolean, Void>apply(proceed -> {
            // Step 5 (CodeExecution): Assert disposer empty and exit
            Disposer.assertIsEmpty();

            int exitCode = 0;
            if (restart && Restarter.isSupported()) {
                try {
                    exitCode = Restarter.scheduleRestart();
                }
                catch (IOException e) {
                    LOG.warn("Cannot restart", e);
                }
            }
            System.exit(exitCode);
            return null;
        })).runAsync(scope, null);
    }

    private static boolean confirmExitIfNeeded(boolean exitConfirmed) {
        final boolean hasUnsafeBgTasks = ProgressManager.getInstance().hasUnsafeProgressIndicator();
        if (exitConfirmed && !hasUnsafeBgTasks) {
            return true;
        }

        DialogWrapper.DoNotAskOption option = new DialogWrapper.DoNotAskOption() {
            @Override
            public boolean isToBeShown() {
                return GeneralSettings.getInstance().isConfirmExit() && ProjectManager.getInstance().getOpenProjects().length > 0;
            }

            @Override
            public void setToBeShown(boolean value, int exitCode) {
                GeneralSettings.getInstance().setConfirmExit(value);
            }

            @Override
            public boolean canBeHidden() {
                return !hasUnsafeBgTasks;
            }

            @Override
            public boolean shouldSaveOptionsOnCancel() {
                return false;
            }

            @Override
            public LocalizeValue getDoNotShowMessage() {
                return LocalizeValue.localizeTODO("Do not ask me again");
            }
        };

        if (hasUnsafeBgTasks || option.isToBeShown()) {
            LocalizeValue message = hasUnsafeBgTasks
                ? ApplicationLocalize.exitConfirmPromptTasks(Application.get().getName())
                : ApplicationLocalize.exitConfirmPrompt(Application.get().getName());

            if (
                MessageDialogBuilder.yesNo(ApplicationLocalize.exitConfirmTitle().get(), message.get())
                    .yesText(ApplicationLocalize.commandExit().get())
                    .noText(CommonLocalize.buttonCancel().get())
                    .doNotAsk(option)
                    .show() != Messages.YES
            ) {
                return false;
            }
        }
        return true;
    }

    @RequiredReadAction
    @Override
    public void assertReadAccessAllowed() {
        if (!isReadAccessAllowed()) {
            LOG.error(
                "Read access is allowed inside read-action only (see Application.runReadAction())",
                "Current thread: " + describe(Thread.currentThread()),
                "; dispatch thread: " + EventQueue.isDispatchThread() + "; isDispatchThread(): " + isDispatchThread(),
                "SystemEventQueueThread: " + describe(getEventQueueThread())
            );
        }
    }

    private static String describe(Thread o) {
        if (o == null) {
            return "null";
        }
        return o + " " + System.identityHashCode(o);
    }

    private static Thread getEventQueueThread() {
        return AWTAccessorHacking.getDispatchThread();
    }

    @RequiredUIAccess
    @Override
    public void assertIsDispatchThread() {
        if (isDispatchThread()) {
            return;
        }
        if (ShutDownTracker.isShutdownHookRunning()) {
            return;
        }
        assertIsDispatchThread("Access is allowed from event dispatch thread only.");
    }

    private void assertIsDispatchThread(String message) {
        if (isDispatchThread()) {
            return;
        }
        Attachment dump = AttachmentFactory.get().create("threadDump.txt", ThreadDumper.dumpThreadsToString());
        throw new LogEventException(
            message,
            " EventQueue.isDispatchThread()=" + EventQueue.isDispatchThread() +
                " isDispatchThread()=" + isDispatchThread() +
                " Toolkit.getEventQueue()=" + Toolkit.getDefaultToolkit().getSystemEventQueue() +
                " Current thread: " + describe(Thread.currentThread()) +
                " SystemEventQueueThread: " + describe(getEventQueueThread()),
            dump
        );
    }

    @Deprecated
    @Override
    public void assertIsWriteThread() {
        if (isWriteAccessAllowed()) {
            return;
        }
        if (ShutDownTracker.isShutdownHookRunning()) {
            return;
        }
        Attachment dump = AttachmentFactory.get().create("threadDump.txt", ThreadDumper.dumpThreadsToString());
        throw new LogEventException(
            "Write access is allowed inside write-action only.",
            " EventQueue.isDispatchThread()=" + EventQueue.isDispatchThread() +
                " isDispatchThread()=" + isDispatchThread() +
                " Toolkit.getEventQueue()=" + Toolkit.getDefaultToolkit().getSystemEventQueue() +
                " Current thread: " + describe(Thread.currentThread()) +
                " SystemEventQueueThread: " + describe(getEventQueueThread()),
            dump
        );
    }

    @Override
    public void assertTimeConsuming() {
        if (myHeadlessMode || ShutDownTracker.isShutdownHookRunning()) {
            return;
        }
        LOG.assertTrue(!isDispatchThread(), "This operation is time consuming and must not be called on EDT");
    }

    @Override
    public UIAccess getLastUIAccess() {
        return AWTUIAccessImpl.ourInstance;
    }

    @Override
    public boolean isActive() {
        Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();

        if (ApplicationActivationStateManager.getState().isInactive() && activeWindow != null) {
            ApplicationActivationStateManager.updateState(activeWindow);
        }

        return ApplicationActivationStateManager.getState().isActive();
    }

    @Override
    public boolean isDisposeInProgress() {
        return myDisposeInProgress || ShutDownTracker.isShutdownHookRunning();
    }

    @Override
    public boolean isRestartCapable() {
        return Restarter.isSupported();
    }

    @Override
    public boolean isSwingApplication() {
        return true;
    }

    @Override
    public boolean isCurrentWriteOnUIThread() {
        return myLock.isWriteThread() && isDispatchThread();
    }

    @TestOnly
    public void setDisposeInProgress(boolean disposeInProgress) {
        myDisposeInProgress = disposeInProgress;
    }
}
