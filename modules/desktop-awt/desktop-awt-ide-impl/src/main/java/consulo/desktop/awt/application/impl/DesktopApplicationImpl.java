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
import consulo.application.impl.internal.*;
import consulo.application.impl.internal.concurent.AppScheduledExecutorService;
import consulo.application.impl.internal.progress.CoreProgressManager;
import consulo.application.impl.internal.start.CommandLineArgs;
import consulo.application.impl.internal.start.StartupUtil;
import consulo.application.internal.StartupProgress;
import consulo.application.localize.ApplicationLocalize;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.util.concurrent.ThreadDumper;
import consulo.awt.hacking.AWTAccessorHacking;
import consulo.awt.hacking.AWTAutoShutdownHacking;
import consulo.component.ComponentManager;
import consulo.component.impl.internal.ComponentBinding;
import consulo.component.internal.inject.InjectingContainerBuilder;
import consulo.desktop.application.util.Restarter;
import consulo.desktop.awt.progress.PotemkinProgress;
import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.desktop.awt.ui.impl.AWTUIAccessImpl;
import consulo.desktop.boot.main.windows.WindowsCommandLineProcessor;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.ide.AppLifecycleListener;
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
import consulo.project.internal.ProjectManagerEx;
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
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.ShutDownTracker;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;
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
        @Nonnull SimpleReference<? extends StartupProgress> splashRef
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
                    final IdeFrame frame = WindowManager.getInstance().getIdeFrame(project);

                    if (frame != null) {
                        AppIcon.getInstance().requestFocus(frame.getWindow());
                    }
                });
            });

            WindowsCommandLineProcessor.LISTENER = (currentDirectory, commandLine) -> {
                LOG.info("Received external Windows command line: current directory " + currentDirectory + ", command line " + commandLine);
                invokeLater(() -> {
                    final List<String> args = StringUtil.splitHonorQuotes(commandLine, ' ');
                    args.remove(0);   // process name
                    CommandLineProcessor.processExternalCommandLine(CommandLineArgs.parse(ArrayUtil.toStringArray(args)), currentDirectory);
                });
            };
        }

        Thread edt = UIUtil.invokeAndWaitIfNeeded(() -> {
            // instantiate AppDelayQueue which starts "Periodic task thread" which we'll mark busy to prevent this EDT to die
            // that thread was chosen because we know for sure it's running
            AppScheduledExecutorService service = (AppScheduledExecutorService)AppExecutorUtil.getAppScheduledExecutorService();
            Thread thread = service.getPeriodicTasksThread();
            AWTAutoShutdownHacking.notifyThreadBusy(thread); // needed for EDT not to exit suddenly
            Disposer.register(this, () -> {
                AWTAutoShutdownHacking.notifyThreadFree(thread); // allow for EDT to exit - needed for Upsource
            });
            return Thread.currentThread();
        });

        myLock = new ReadMostlyRWLock(edt);

        UIUtil.invokeAndWaitIfNeeded((Runnable)() -> acquireWriteIntentLock(getClass().getName()));

        NoSwingUnderWriteAction.watchForEvents(this);
    }

    @Override
    public int getProfiles() {
        return super.getProfiles() | ComponentProfiles.AWT;
    }

    @Override
    protected void bootstrapInjectingContainer(@Nonnull InjectingContainerBuilder builder) {
        super.bootstrapInjectingContainer(builder);
    }

    @RequiredUIAccess
    private boolean disposeSelf(final boolean checkCanCloseProject) {
        final ProjectManagerEx manager = ProjectManagerEx.getInstanceEx();
        final boolean[] canClose = {true};
        boolean wantSaveSettingsAgain = false;
        for (final Project project : manager.getOpenProjects()) {
            try {
                CommandProcessor.getInstance().newCommand()
                    .project(project)
                    .name(ApplicationLocalize.commandExit())
                    .run(() -> {
                        if (!manager.closeProject(project, true, true, checkCanCloseProject)) {
                            canClose[0] = false;
                        }
                    });
                wantSaveSettingsAgain = true;
            }
            catch (Throwable e) {
                LOG.error(e);
            }
            if (!canClose[0]) {
                return false;
            }
        }

        if (wantSaveSettingsAgain) {
            saveSettings();
        }

        runWriteAction(() -> Disposer.dispose(DesktopApplicationImpl.this));

        Disposer.assertIsEmpty();
        return true;
    }

    @Override
    public boolean isInternal() {
        return myIsInternal;
    }

    @Override
    public boolean isHeadlessEnvironment() {
        return myHeadlessMode;
    }

    @Nonnull
    public ModalityInvokator getInvokator() {
        return myInvokator;
    }

    @Override
    public void invokeLater(@Nonnull final Runnable runnable) {
        invokeLater(runnable, getDisposed());
    }

    @Override
    public void invokeLater(@Nonnull final Runnable runnable, @Nonnull final BooleanSupplier expired) {
        invokeLater(runnable, IdeaModalityState.defaultModalityState(), expired);
    }

    @Override
    public void invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState state) {
        invokeLater(runnable, state, getDisposed());
    }

    @Override
    public void invokeLater(
        @Nonnull final Runnable runnable,
        @Nonnull final ModalityState state,
        @Nonnull final BooleanSupplier expired
    ) {
        LaterInvocator.invokeLaterWithCallback(() -> runIntendedWriteActionOnCurrentThread(runnable), state, expired, null);
    }

    @RequiredUIAccess
    @Override
    public void invokeAndWait(@Nonnull Runnable runnable, @Nonnull ModalityState modalityState) {
        if (isDispatchThread()) {
            runnable.run();
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            runIntendedWriteActionOnCurrentThread(runnable);
            return;
        }

        if (holdsReadLock()) {
            throw new IllegalStateException("Calling invokeAndWait from read-action leads to possible deadlock.");
        }

        LaterInvocator.invokeAndWait(() -> runIntendedWriteActionOnCurrentThread(runnable), modalityState);
    }

    @Override
    @Nonnull
    public ModalityState getCurrentModalityState() {
        return LaterInvocator.getCurrentModalityState();
    }

    @Override
    @Nonnull
    public ModalityState getModalityStateForComponent(@Nonnull Component c) {
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
    @Nonnull
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
    public void exit(boolean force, final boolean exitConfirmed) {
        exit(false, exitConfirmed, true, false);
    }

    @Override
    public void restart(final boolean exitConfirmed) {
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
            if (!force && !exitConfirmed && getDefaultModalityState() != IdeaModalityState.nonModal()) {
                return;
            }

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
                    myDisposeInProgress = false;
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
    private boolean doExit(boolean allowListenersToCancel, boolean restart) {
        saveSettings();

        if (allowListenersToCancel && !canExit()) {
            return false;
        }

        final boolean success = disposeSelf(allowListenersToCancel);
        if (!success || isUnitTestMode()) {
            return false;
        }

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
        return true;
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

            @Nonnull
            @Override
            @NonNls
            public LocalizeValue getDoNotShowMessage() {
                return LocalizeValue.localizeTODO("Do not ask me again");
            }
        };

        if (hasUnsafeBgTasks || option.isToBeShown()) {
            String message = hasUnsafeBgTasks
                ? ApplicationLocalize.exitConfirmPromptTasks(Application.get().getName()).get()
                : ApplicationLocalize.exitConfirmPrompt(Application.get().getName()).get();

            if (
                MessageDialogBuilder.yesNo(ApplicationLocalize.exitConfirmTitle().get(), message)
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
        @Nonnull String title,
        @Nullable ComponentManager project,
        @Nullable JComponent parentComponent,
        @Nonnull Consumer<? super ProgressIndicator> action
    ) {
        return runEdtProgressWriteAction(title, project, parentComponent, LocalizeValue.of(), action);
    }

    @Override
    public boolean runWriteActionWithCancellableProgressInDispatchThread(
        @Nonnull String title,
        @Nullable ComponentManager project,
        @Nullable JComponent parentComponent,
        @Nonnull Consumer<? super ProgressIndicator> action
    ) {
        return runEdtProgressWriteAction(title, project, parentComponent, IdeLocalize.actionStop(), action);
    }

    private boolean runEdtProgressWriteAction(
        @Nonnull String title,
        @Nullable ComponentManager project,
        @Nullable JComponent parentComponent,
        @Nonnull LocalizeValue cancelText,
        @Nonnull Consumer<? super ProgressIndicator> action
    ) {
        // Use Potemkin progress in legacy mode; in the new model such execution will always move to a separate thread.
        return runWriteActionWithClass(action.getClass(), () -> {
            PotemkinProgress indicator = new PotemkinProgress(title, (Project)project, parentComponent, cancelText);
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

    private void assertIsDispatchThread(@Nonnull String message) {
        if (isDispatchThread()) {
            return;
        }
        final Attachment dump = AttachmentFactory.get().create("threadDump.txt", ThreadDumper.dumpThreadsToString());
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

    private void assertIsIsWriteThread(@Nonnull String message) {
        if (isWriteThread()) {
            return;
        }
        final Attachment dump = AttachmentFactory.get().create("threadDump.txt", ThreadDumper.dumpThreadsToString());
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

    @Nonnull
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
