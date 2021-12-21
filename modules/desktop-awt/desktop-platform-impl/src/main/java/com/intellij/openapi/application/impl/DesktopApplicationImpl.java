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
package com.intellij.openapi.application.impl;

import com.intellij.CommonBundle;
import com.intellij.diagnostic.LogEventException;
import com.intellij.diagnostic.ThreadDumper;
import com.intellij.ide.*;
import com.intellij.idea.StartupUtil;
import com.intellij.openapi.application.*;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.impl.CoreProgressManager;
import com.intellij.openapi.progress.impl.ProgressResult;
import com.intellij.openapi.progress.impl.ProgressRunner;
import com.intellij.openapi.progress.util.PotemkinProgress;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.project.impl.ProjectManagerImpl;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.ShutDownTracker;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.AppIcon;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Restarter;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.concurrency.AppScheduledExecutorService;
import com.intellij.util.ui.UIUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.ApplicationProperties;
import consulo.application.impl.BaseApplication;
import consulo.awt.hacking.AWTAutoShutdownHacking;
import consulo.desktop.boot.main.windows.WindowsCommandLineProcessor;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.injecting.InjectingContainerBuilder;
import consulo.logging.Logger;
import consulo.start.CommandLineArgs;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.desktop.internal.AWTUIAccessImpl;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.ref.SimpleReference;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.TestOnly;
import sun.awt.AWTAccessor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class DesktopApplicationImpl extends BaseApplication {
  private static final Logger LOG = Logger.getInstance(DesktopApplicationImpl.class);

  private final ModalityInvokator myInvokator = new ModalityInvokatorImpl();

  private final boolean myHeadlessMode;
  private final boolean myIsInternal;

  private DesktopTransactionGuardImpl myTransactionGuardImpl;

  private volatile boolean myDisposeInProgress;

  private static final String WAS_EVER_SHOWN = "was.ever.shown";

  private static final ModalityState ANY = new ModalityState() {
    @Override
    public boolean dominates(@Nonnull ModalityState anotherState) {
      return false;
    }

    @Override
    public String toString() {
      return "ANY";
    }
  };

  public DesktopApplicationImpl(boolean isHeadless, @Nonnull SimpleReference<? extends StartupProgress> splashRef) {
    super(splashRef);

    ApplicationManager.setApplication(this);

    AWTExceptionHandler.register(); // do not crash AWT on exceptions

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

          if (frame != null) AppIcon.getInstance().requestFocus(frame);
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

    if (!USE_SEPARATE_WRITE_THREAD) {
      UIUtil.invokeAndWaitIfNeeded((Runnable)() -> acquireWriteIntentLock(getClass().getName()));
    }

    NoSwingUnderWriteAction.watchForEvents(this);
  }

  private DesktopTransactionGuardImpl transactionGuard() {
    if (myTransactionGuardImpl == null) {
      myTransactionGuardImpl = new DesktopTransactionGuardImpl();
    }
    return myTransactionGuardImpl;
  }

  @Nonnull
  @Override
  protected Runnable wrapLaterInvocation(Runnable action, ModalityState state) {
    return transactionGuard().wrapLaterInvocation(action, state);
  }

  @Override
  protected void bootstrapInjectingContainer(@Nonnull InjectingContainerBuilder builder) {
    super.bootstrapInjectingContainer(builder);

    builder.bind(TransactionGuard.class).to(transactionGuard());
  }

  @RequiredUIAccess
  private boolean disposeSelf(final boolean checkCanCloseProject) {
    final ProjectManagerImpl manager = (ProjectManagerImpl)ProjectManagerEx.getInstanceEx();
    if (manager != null) {
      final boolean[] canClose = {true};
      for (final Project project : manager.getOpenProjects()) {
        try {
          CommandProcessor.getInstance().executeCommand(project, () -> {
            if (!manager.closeProject(project, true, true, checkCanCloseProject)) {
              canClose[0] = false;
            }
          }, ApplicationBundle.message("command.exit"), null);
        }
        catch (Throwable e) {
          LOG.error(e);
        }
        if (!canClose[0]) {
          return false;
        }
      }
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
  public void invokeLater(@Nonnull final Runnable runnable, @Nonnull final Condition expired) {
    invokeLater(runnable, ModalityState.defaultModalityState(), expired);
  }

  @Override
  public void invokeLater(@Nonnull final Runnable runnable, @Nonnull final ModalityState state) {
    invokeLater(runnable, state, getDisposed());
  }

  @Override
  public void invokeLater(@Nonnull final Runnable runnable, @Nonnull final ModalityState state, @Nonnull final Condition expired) {
    Runnable r = transactionGuard().wrapLaterInvocation(runnable, state);
    LaterInvocator.invokeLaterWithCallback(() -> runIntendedWriteActionOnCurrentThread(r), state, expired, null, true);
  }

  @RequiredUIAccess
  @Override
  public boolean runProcessWithProgressSynchronously(@Nonnull final Runnable process,
                                                     @Nonnull final String progressTitle,
                                                     final boolean canBeCanceled,
                                                     boolean shouldShowModalWindow,
                                                     @Nullable final Project project,
                                                     final JComponent parentComponent,
                                                     final String cancelText) {
    if (isDispatchThread() && isWriteAccessAllowed()
      // Disallow running process in separate thread from under write action.
      // The thread will deadlock trying to get read action otherwise.
    ) {
      LOG.debug("Starting process with progress from within write action makes no sense");
      try {
        ProgressManager.getInstance().runProcess(process, new EmptyProgressIndicator());
      }
      catch (ProcessCanceledException e) {
        // ok to ignore.
        return false;
      }
      return true;
    }

    CompletableFuture<ProgressWindow> progress = createProgressWindowAsyncIfNeeded(progressTitle, canBeCanceled, shouldShowModalWindow, project, parentComponent, cancelText);

    ProgressRunner<?> progressRunner = new ProgressRunner<>(process).sync().onThread(ProgressRunner.ThreadToUse.POOLED).modal().withProgress(progress);

    ProgressResult<?> result = progressRunner.submitAndGet();

    Throwable exception = result.getThrowable();
    if (!(exception instanceof ProcessCanceledException)) {
      ExceptionUtil.rethrowUnchecked(exception);
    }
    return !result.isCanceled();
  }

  @Nonnull
  public final CompletableFuture<ProgressWindow> createProgressWindowAsyncIfNeeded(@Nonnull String progressTitle,
                                                                                   boolean canBeCanceled,
                                                                                   boolean shouldShowModalWindow,
                                                                                   @Nullable Project project,
                                                                                   @Nullable JComponent parentComponent,
                                                                                   @Nullable String cancelText) {
    if (SwingUtilities.isEventDispatchThread()) {
      return CompletableFuture.completedFuture(createProgressWindow(progressTitle, canBeCanceled, shouldShowModalWindow, project, parentComponent, cancelText));
    }
    return CompletableFuture.supplyAsync(() -> createProgressWindow(progressTitle, canBeCanceled, shouldShowModalWindow, project, parentComponent, cancelText), this::invokeLater);
  }

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

    Runnable r = transactionGuard().wrapLaterInvocation(runnable, modalityState);
    LaterInvocator.invokeAndWait(() -> runIntendedWriteActionOnCurrentThread(r), modalityState);
  }

  @Override
  @Nonnull
  public ModalityState getCurrentModalityState() {
    return LaterInvocator.getCurrentModalityState();
  }

  @Override
  @Nonnull
  public ModalityState getModalityStateForComponent(@Nonnull Component c) {
    if (!isDispatchThread()) LOG.debug("please, use application dispatch thread to get a modality state");
    Window window = UIUtil.getWindow(c);
    if (window == null) return getNoneModalityState();
    return LaterInvocator.modalityStateForWindow(window);
  }

  @Override
  @Nonnull
  public ModalityState getAnyModalityState() {
    return ANY;
  }

  @Override
  @Nonnull
  public ModalityState getDefaultModalityState() {
    return isDispatchThread() ? getCurrentModalityState() : CoreProgressManager.getCurrentThreadProgressModality();
  }

  @Override
  @Nonnull
  public ModalityState getNoneModalityState() {
    return ModalityState.NON_MODAL;
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

  public void exit(final boolean force, final boolean exitConfirmed, final boolean allowListenersToCancel, final boolean restart) {
    if (!force && exiting) {
      return;
    }

    exiting = true;
    try {
      if (!force && !exitConfirmed && getDefaultModalityState() != ModalityState.NON_MODAL) {
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

          getMessageBus().syncPublisher(AppLifecycleListener.TOPIC).appClosing();
          myDisposeInProgress = true;
          doExit(allowListenersToCancel, restart);
          myDisposeInProgress = false;
        }
      };

      if (isDispatchThread()) {
        runnable.run();
      }
      else {
        invokeLater(runnable, ModalityState.NON_MODAL);
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
      public String getDoNotShowMessage() {
        return "Do not ask me again";
      }
    };

    if (hasUnsafeBgTasks || option.isToBeShown()) {
      String message = ApplicationBundle.message(hasUnsafeBgTasks ? "exit.confirm.prompt.tasks" : "exit.confirm.prompt", ApplicationNamesInfo.getInstance().getFullProductName());

      if (MessageDialogBuilder.yesNo(ApplicationBundle.message("exit.confirm.title"), message).yesText(ApplicationBundle.message("command.exit")).noText(CommonBundle.message("button.cancel"))
                  .doNotAsk(option).show() != Messages.YES) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean runWriteActionWithNonCancellableProgressInDispatchThread(@Nonnull String title,
                                                                          @Nullable Project project,
                                                                          @Nullable JComponent parentComponent,
                                                                          @Nonnull Consumer<? super ProgressIndicator> action) {
    return runEdtProgressWriteAction(title, project, parentComponent, null, action);
  }

  @Override
  public boolean runWriteActionWithCancellableProgressInDispatchThread(@Nonnull String title,
                                                                       @Nullable Project project,
                                                                       @Nullable JComponent parentComponent,
                                                                       @Nonnull Consumer<? super ProgressIndicator> action) {
    return runEdtProgressWriteAction(title, project, parentComponent, IdeBundle.message("action.stop"), action);
  }

  private boolean runEdtProgressWriteAction(@Nonnull String title,
                                            @Nullable Project project,
                                            @Nullable JComponent parentComponent,
                                            @Nullable @Nls(capitalization = Nls.Capitalization.Title) String cancelText,
                                            @Nonnull Consumer<? super ProgressIndicator> action) {
    if (!USE_SEPARATE_WRITE_THREAD) {
      // Use Potemkin progress in legacy mode; in the new model such execution will always move to a separate thread.
      return runWriteActionWithClass(action.getClass(), () -> {
        PotemkinProgress indicator = new PotemkinProgress(title, project, parentComponent, cancelText);
        indicator.runInSwingThread(() -> action.accept(indicator));
        return !indicator.isCanceled();
      });
    }

    ProgressWindow progress = createProgressWindow(title, cancelText != null, true, project, parentComponent, cancelText);

    ProgressResult<Object> result =
            new ProgressRunner<>(() -> runWriteAction(() -> action.accept(progress)))
                    .sync()
                    .onThread(ProgressRunner.ThreadToUse.WRITE)
                    .withProgress(progress)
                    .modal()
                    .submitAndGet();

    if (result.getThrowable() instanceof RuntimeException) {
      throw (RuntimeException)result.getThrowable();
    }

    return true;
  }

  @Nonnull
  private ProgressWindow createProgressWindow(@Nonnull String progressTitle,
                                              boolean canBeCanceled,
                                              boolean shouldShowModalWindow,
                                              @Nullable Project project,
                                              @Nullable JComponent parentComponent,
                                              @Nullable String cancelText) {
    ProgressWindow progress = new ProgressWindow(canBeCanceled, !shouldShowModalWindow, project, parentComponent, cancelText);
    // in case of abrupt application exit when 'ProgressManager.getInstance().runProcess(process, progress)' below
    // does not have a chance to run, and as a result the progress won't be disposed
    Disposer.register(this, progress);
    progress.setTitle(progressTitle);
    return progress;
  }

  @RequiredReadAction
  @Override
  public void assertReadAccessAllowed() {
    if (!isReadAccessAllowed()) {
      LOG.error("Read access is allowed from event dispatch thread or inside read-action only" + " (see com.intellij.openapi.application.Application.runReadAction())",
                "Current thread: " + describe(Thread.currentThread()), "; dispatch thread: " + EventQueue.isDispatchThread() + "; isDispatchThread(): " + isDispatchThread(),
                "SystemEventQueueThread: " + describe(getEventQueueThread()));
    }
  }

  private static String describe(Thread o) {
    if (o == null) return "null";
    return o + " " + System.identityHashCode(o);
  }

  private static Thread getEventQueueThread() {
    EventQueue eventQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
    return AWTAccessor.getEventQueueAccessor().getDispatchThread(eventQueue);
  }

  @RequiredUIAccess
  @Override
  public void assertIsDispatchThread() {
    if (isDispatchThread()) return;
    if (ShutDownTracker.isShutdownHookRunning()) return;
    assertIsDispatchThread("Access is allowed from event dispatch thread only.");
  }

  private void assertIsDispatchThread(@Nonnull String message) {
    if (isDispatchThread()) return;
    final Attachment dump = new Attachment("threadDump.txt", ThreadDumper.dumpThreadsToString());
    throw new LogEventException(message, " EventQueue.isDispatchThread()=" +
                                         EventQueue.isDispatchThread() +
                                         " isDispatchThread()=" +
                                         isDispatchThread() +
                                         " Toolkit.getEventQueue()=" +
                                         Toolkit.getDefaultToolkit().getSystemEventQueue() +
                                         " Current thread: " +
                                         describe(Thread.currentThread()) +
                                         " SystemEventQueueThread: " +
                                         describe(getEventQueueThread()), dump);
  }

  @RequiredUIAccess
  @Override
  public void assertIsWriteThread() {
    if (isWriteThread()) return;
    if (ShutDownTracker.isShutdownHookRunning()) return;
    assertIsIsWriteThread("Access is allowed from write thread only.");
  }

  private void assertIsIsWriteThread(@Nonnull String message) {
    if (isWriteThread()) return;
    final Attachment dump = new Attachment("threadDump.txt", ThreadDumper.dumpThreadsToString());
    throw new LogEventException(message, " EventQueue.isDispatchThread()=" +
                                         EventQueue.isDispatchThread() +
                                         " isDispatchThread()=" +
                                         isDispatchThread() +
                                         " Toolkit.getEventQueue()=" +
                                         Toolkit.getDefaultToolkit().getSystemEventQueue() +
                                         " Write Thread=" +
                                         myLock.writeThread +
                                         " Current thread: " +
                                         describe(Thread.currentThread()) +
                                         " SystemEventQueueThread: " +
                                         describe(getEventQueueThread()), dump);
  }

  @RequiredUIAccess
  @Override
  public void assertIsDispatchThread(@Nullable final JComponent component) {
    if (component == null) return;

    if (isDispatchThread()) {
      return;
    }

    if (Boolean.TRUE.equals(component.getClientProperty(WAS_EVER_SHOWN))) {
      assertIsDispatchThread();
    }
    else {
      final JRootPane root = component.getRootPane();
      if (root != null) {
        component.putClientProperty(WAS_EVER_SHOWN, Boolean.TRUE);
        assertIsDispatchThread();
      }
    }
  }

  @Override
  public void assertTimeConsuming() {
    if (myHeadlessMode || ShutDownTracker.isShutdownHookRunning()) return;
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

  @TestOnly
  public void setDisposeInProgress(boolean disposeInProgress) {
    myDisposeInProgress = disposeInProgress;
  }
}
