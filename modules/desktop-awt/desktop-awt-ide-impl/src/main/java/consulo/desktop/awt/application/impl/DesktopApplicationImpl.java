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
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.impl.internal.LaterInvocator;
import consulo.application.impl.internal.ReadMostlyRWLock;
import consulo.application.impl.internal.concurent.AppScheduledExecutorService;
import consulo.application.impl.internal.progress.CoreProgressManager;
import consulo.application.impl.internal.start.CommandLineArgs;
import consulo.application.impl.internal.start.StartupUtil;
import consulo.application.internal.AppLifecycleListener;
import consulo.application.internal.StartupProgress;
import consulo.application.localize.ApplicationLocalize;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.util.concurrent.ThreadDumper;
import consulo.application.util.function.ThrowableComputable;
import consulo.awt.hacking.AWTAccessorHacking;
import consulo.awt.hacking.AWTAutoShutdownHacking;
import consulo.component.ComponentManager;
import consulo.component.internal.ComponentBinding;
import consulo.component.internal.inject.InjectingContainerBuilder;
import consulo.desktop.application.util.Restarter;
import consulo.desktop.awt.progress.PotemkinProgress;
import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.desktop.awt.ui.impl.AWTUIAccessImpl;
import consulo.desktop.boot.main.windows.WindowsCommandLineProcessor;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.ide.ApplicationActivationStateManager;
import consulo.ide.impl.idea.ide.CommandLineProcessor;
import consulo.ide.impl.idea.ide.GeneralSettings;
import consulo.ide.localize.IdeLocalize;
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
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.AppIcon;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.MessageDialogBuilder;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.internal.EDT;
import consulo.application.concurrent.coroutine.WriteLock;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineContext;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CallSubroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.collection.ArrayUtil;
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
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class DesktopApplicationImpl extends BaseApplication {
    private static final Logger LOG = Logger.getInstance(DesktopApplicationImpl.class);

    private final ModalityInvokator myInvokator = new ModalityInvokatorImpl();

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

                CommandLineProcessor.processExternalCommandLine(commandLineArgs, null).doWhenDone(project -> {
                    IdeFrame frame = WindowManager.getInstance().getIdeFrame(project);

                    if (frame != null) {
                        AppIcon.getInstance().requestFocus(frame.getWindow());
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

        UIUtil.invokeAndWaitIfNeeded((Runnable)() -> {
            // instantiate AppDelayQueue which starts "Periodic task thread" which we'll mark busy to prevent this EDT to die
            // that thread was chosen because we know for sure it's running
            AppScheduledExecutorService service = (AppScheduledExecutorService)AppExecutorUtil.getAppScheduledExecutorService();
            Thread thread = service.getPeriodicTasksThread();
            AWTAutoShutdownHacking.notifyThreadBusy(thread); // needed for EDT not to exit suddenly
            Disposer.register(this, () -> {
                AWTAutoShutdownHacking.notifyThreadFree(thread); // allow for EDT to exit - needed for Upsource
            });
        });

        // no permanent write thread: the write-intent lock is acquired transiently around each EDT
        // event (see IdeEventQueue) and around each off-EDT write action, so any thread can write
        myLock = new ReadMostlyRWLock(null);

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
        CompletableFuture<Boolean> chain = CompletableFuture.completedFuture(Boolean.TRUE);
        for (Project project : projects) {
            chain = chain.thenCompose(allClosed -> {
                if (!allClosed) {
                    return CompletableFuture.completedFuture(Boolean.FALSE);
                }
                return manager.closeAndDisposeAsync(project, uiAccess, checkCanClose, true, true);
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

    
    public ModalityInvokator getInvokator() {
        return myInvokator;
    }

    @Override
    public void invokeLater(Runnable runnable) {
        invokeLater(runnable, getDisposed());
    }

    @Override
    public void invokeLater(Runnable runnable, BooleanSupplier expired) {
        invokeLater(runnable, IdeaModalityState.defaultModalityState(), expired);
    }

    @Override
    public void invokeLater(Runnable runnable, ModalityState state) {
        invokeLater(runnable, state, getDisposed());
    }

    @Override
    public void invokeLater(
        Runnable runnable,
        ModalityState state,
        BooleanSupplier expired
    ) {
        LaterInvocator.invokeLaterWithCallback(runnable, state, expired, null);
    }

    @RequiredUIAccess
    @Override
    public void invokeAndWait(Runnable runnable, ModalityState modalityState) {
        if (isDispatchThread()) {
            runnable.run();
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
            return;
        }

        if (holdsReadLock()) {
            throw new IllegalStateException("Calling invokeAndWait from read-action leads to possible deadlock.");
        }

        LaterInvocator.invokeAndWait(runnable, modalityState);
    }

    @Override
    public <T, E extends Throwable> T runUnlockingIntendedWrite(ThrowableComputable<T, E> action) throws E {
        if (!myLock.isWriteThread()) {
            return action.compute();
        }
        myLock.writeIntentUnlock();
        try {
            return action.compute();
        }
        finally {
            myLock.writeIntentLock();
        }
    }

    @Override
    
    public ModalityState getCurrentModalityState() {
        return LaterInvocator.getCurrentModalityState();
    }

    @Override
    
    public ModalityState getModalityStateForComponent(Component c) {
        if (!isDispatchThread()) {
            LOG.debug("please, use application dispatch thread to get a modality state");
        }
        Window window = UIUtil.getWindow(c);
        if (window == null) {
            return getNoneModalityState();
        }
        return LaterInvocator.modalityStateForWindow(window);
    }

    @Override
    
    public ModalityState getDefaultModalityState() {
        return isDispatchThread() ? getCurrentModalityState() : CoreProgressManager.getCurrentThreadProgressModality();
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
        exit(force, exitConfirmed, true, false);
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

    public void exit(boolean force, boolean exitConfirmed, boolean allowListenersToCancel, boolean restart) {
        // Re-entrancy + modality guard: skip if another exit is already running, or if the exit is not confirmed
        // while a modal dialog is active. Unlike a plain try/finally, the flag stays set across the asynchronous
        // exit sequence and is cleared only on the abort/terminal paths in doExit().
        if (!force && (exiting || (!exitConfirmed && getDefaultModalityState() != IdeaModalityState.nonModal()))) {
            return;
        }

        exiting = true;

        if (isDispatchThread()) {
            doExit(force, exitConfirmed, allowListenersToCancel, restart);
        }
        else {
            invokeLater(() -> doExit(force, exitConfirmed, allowListenersToCancel, restart), Application.get().getNoneModalityState());
        }
    }

    @RequiredUIAccess
    private void doExit(boolean force, boolean exitConfirmed, boolean allowListenersToCancel, boolean restart) {
        // Exit confirmation dialog — cancelling keeps the application running
        if (!force && !confirmExitIfNeeded(exitConfirmed)) {
            saveAll();
            exiting = false;
            return;
        }

        getMessageBus().syncPublisher(AppLifecycleListener.class).appClosing();
        myDisposeInProgress = true;

        // canExit veto (on EDT - may show dialogs)
        if (allowListenersToCancel && !canExit()) {
            myDisposeInProgress = false;
            exiting = false;
            return;
        }

        UIAccess uiAccess = UIAccess.current();

        // Close all open projects asynchronously (sequentially, honoring per-project veto)
        disposeAllProjectsAsync(allowListenersToCancel, uiAccess).whenComplete((allClosed, throwable) -> {
            if (throwable != null || !Boolean.TRUE.equals(allClosed) || isUnitTestMode()) {
                myDisposeInProgress = false;
                exiting = false;
                return;
            }

            // Final teardown - save settings, dispose application under write lock (off EDT), then exit
            CoroutineContext context = coroutineContext().copy();
            context.putCopyableUserData(UIAccess.KEY, uiAccess);
            CoroutineScope scope = CoroutineScope.of(context);

            // Save the application settings as a subroutine (the store save is itself a coroutine)
            Coroutine.<Void, Object>first(CodeExecution.<Void, Object>apply(input -> null))
                .then(CallSubroutine.call(saveSettingsAsync()))
                .then(WriteLock.<Object, Object>apply(input -> {
                    Disposer.dispose(DesktopApplicationImpl.this);
                    return input;
                }))
                .then(CodeExecution.<Object, Object>apply(input -> {
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
                    return input;
                }))
                .runAsync(scope, null);
        });
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

    @Override
    public boolean runWriteActionWithNonCancellableProgressInDispatchThread(
        LocalizeValue title,
        @Nullable ComponentManager project,
        @Nullable JComponent parentComponent,
        Consumer<? super ProgressIndicator> action
    ) {
        return runEdtProgressWriteAction(title, project, parentComponent, LocalizeValue.empty(), action);
    }

    @Override
    public boolean runWriteActionWithCancellableProgressInDispatchThread(
        LocalizeValue title,
        @Nullable ComponentManager project,
        @Nullable JComponent parentComponent,
        Consumer<? super ProgressIndicator> action
    ) {
        return runEdtProgressWriteAction(title, project, parentComponent, IdeLocalize.actionStop(), action);
    }

    private boolean runEdtProgressWriteAction(
        LocalizeValue title,
        @Nullable ComponentManager project,
        @Nullable JComponent parentComponent,
        LocalizeValue cancelText,
        Consumer<? super ProgressIndicator> action
    ) {
        // Use Potemkin progress in legacy mode; in the new model such execution will always move to a separate thread.
        return runWriteActionWithClass(action.getClass(), () -> {
            PotemkinProgress indicator = new PotemkinProgress(title.get(), (Project)project, parentComponent, cancelText);
            indicator.runInSwingThread(() -> action.accept(indicator));
            return !indicator.isCanceled();
        });
    }

    @RequiredReadAction
    @Override
    public void assertReadAccessAllowed() {
        if (!isReadAccessAllowed()) {
            LOG.error(
                "Read access is allowed from event dispatch thread or inside read-action only" +
                    " (see consulo.ide.impl.idea.openapi.application.Application.runReadAction())",
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

    @RequiredUIAccess
    @Override
    public void assertIsWriteThread() {
        if (isWriteThread()) {
            return;
        }
        if (ShutDownTracker.isShutdownHookRunning()) {
            return;
        }
        assertIsIsWriteThread("Access is allowed from write thread only.");
    }

    private void assertIsIsWriteThread(String message) {
        if (isWriteThread()) {
            return;
        }
        Attachment dump = AttachmentFactory.get().create("threadDump.txt", ThreadDumper.dumpThreadsToString());
        throw new LogEventException(
            message,
            " EventQueue.isDispatchThread()=" + EventQueue.isDispatchThread() +
                " isDispatchThread()=" + isDispatchThread() +
                " Toolkit.getEventQueue()=" + Toolkit.getDefaultToolkit().getSystemEventQueue() +
                " Write Thread=" + ((ReadMostlyRWLock) myLock).writeThread +
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
        return EDT.isEdt(((ReadMostlyRWLock) myLock).writeThread);
    }

    @TestOnly
    public void setDisposeInProgress(boolean disposeInProgress) {
        myDisposeInProgress = disposeInProgress;
    }
}
