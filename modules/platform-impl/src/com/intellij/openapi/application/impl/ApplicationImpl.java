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
import com.intellij.concurrency.IdeaForkJoinWorkerThreadFactory;
import com.intellij.concurrency.JobScheduler;
import com.intellij.diagnostic.LogEventException;
import com.intellij.diagnostic.PerformanceWatcher;
import com.intellij.diagnostic.ThreadDumper;
import com.intellij.ide.*;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.idea.ApplicationStarter;
import com.intellij.idea.StartupUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.*;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationUtil;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.components.StateStorageException;
import com.intellij.openapi.components.impl.ApplicationPathMacroManager;
import com.intellij.openapi.components.impl.PlatformComponentManagerImpl;
import com.intellij.openapi.components.impl.stores.ApplicationStoreImpl;
import com.intellij.openapi.components.impl.stores.IApplicationStore;
import com.intellij.openapi.components.impl.stores.IComponentStore;
import com.intellij.openapi.components.impl.stores.StoreUtil;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.progress.util.PotemkinProgress;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ex.ProjectEx;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.project.impl.ProjectManagerImpl;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiLock;
import com.intellij.ui.AppIcon;
import com.intellij.ui.Splash;
import com.intellij.util.*;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.concurrency.AppScheduledExecutorService;
import com.intellij.util.containers.Stack;
import com.intellij.util.io.storage.HeavyProcessLatch;
import com.intellij.util.ui.UIUtil;
import consulo.annotations.RequiredDispatchThread;
import consulo.annotations.RequiredReadAction;
import consulo.annotations.RequiredWriteAction;
import consulo.application.ex.ApplicationEx2;
import consulo.start.CommandLineArgs;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.ide.PooledThreadExecutor;
import org.picocontainer.MutablePicoContainer;
import sun.awt.AWTAccessor;
import sun.awt.AWTAutoShutdown;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ApplicationImpl extends PlatformComponentManagerImpl implements ApplicationEx2 {
  private static final Logger LOG = Logger.getInstance("#com.intellij.application.impl.ApplicationImpl");

  final ReadMostlyRWLock myLock;

  private final ModalityInvokator myInvokator = new ModalityInvokatorImpl();

  private final EventDispatcher<ApplicationListener> myDispatcher = EventDispatcher.create(ApplicationListener.class);

  private final boolean myTestModeFlag;
  private final boolean myHeadlessMode;
  private final boolean myCommandLineMode;
  private final boolean myIsInternal;

  private final Stack<Class> myWriteActionsStack = new Stack<>(); // accessed from EDT only, no need to sync
  private final TransactionGuardImpl myTransactionGuard = new TransactionGuardImpl();

  private int myInEditorPaintCounter; // EDT only
  private final long myStartTime;
  @Nullable
  private final Splash mySplash;
  private boolean myDoNotSave;
  private volatile boolean myDisposeInProgress;

  private final Disposable myLastDisposable = Disposer.newDisposable(); // will be disposed last

  private final AtomicBoolean mySaveSettingsIsInProgress = new AtomicBoolean(false);

  private final ExecutorService ourThreadExecutorsService = PooledThreadExecutor.INSTANCE;
  @SuppressWarnings("UseOfArchaicSystemPropertyAccessors")
  private static final int ourDumpThreadsOnLongWriteActionWaiting = Integer.getInteger("dump.threads.on.long.write.action.waiting", 0);

  private boolean myIsFiringLoadingEvent = false;
  private boolean myLoaded = false;
  @NonNls
  private static final String WAS_EVER_SHOWN = "was.ever.shown";

  private int myWriteStackBase;
  private volatile Thread myWriteActionThread;

  private final boolean gatherStatistics;

  private static class ActionPauses {
    private static final PausesStat WRITE = new PausesStat("Write action");
  }

  private static final ModalityState ANY = new ModalityState() {
    @Override
    public boolean dominates(@NotNull ModalityState anotherState) {
      return false;
    }

    @NonNls
    @Override
    public String toString() {
      return "ANY";
    }
  };

  @Override
  protected void bootstrapPicoContainer(@NotNull String name) {
    super.bootstrapPicoContainer(name);
    getPicoContainer().registerComponentImplementation(IComponentStore.class, ApplicationStoreImpl.class);
    getPicoContainer().registerComponentImplementation(ApplicationPathMacroManager.class);
  }

  @Override
  @NotNull
  public IApplicationStore getStateStore() {
    return (IApplicationStore)getPicoContainer().getComponentInstance(IComponentStore.class);
  }

  @Override
  public void initializeComponent(@NotNull Object component, boolean service) {
    getStateStore().initComponent(component);
  }

  static {
    IdeaForkJoinWorkerThreadFactory.setupForkJoinCommonPool();
  }

  public ApplicationImpl(boolean isInternal,
                         boolean isUnitTestMode,
                         boolean isHeadless,
                         boolean isCommandLine,
                         @NotNull String appName,
                         @Nullable Splash splash) {
    super(null);

    ApplicationManager.setApplication(this, myLastDisposable); // reset back to null only when all components already disposed

    getPicoContainer().registerComponentInstance(Application.class, this);

    AWTExceptionHandler.register(); // do not crash AWT on exceptions

    String debugDisposer = System.getProperty("idea.disposer.debug");
    Disposer.setDebugMode((isInternal || isUnitTestMode || "on".equals(debugDisposer)) && !"off".equals(debugDisposer));

    myStartTime = System.currentTimeMillis();
    mySplash = splash;

    myIsInternal = isInternal;
    myTestModeFlag = isUnitTestMode;
    myHeadlessMode = isHeadless;
    myCommandLineMode = isCommandLine;

    myDoNotSave = isUnitTestMode || isHeadless;
    gatherStatistics = LOG.isDebugEnabled() || isUnitTestMode() || isInternal();

    loadApplicationComponents();

    if (myTestModeFlag) {
      registerShutdownHook();
    }

    if (!isUnitTestMode && !isHeadless) {
      Disposer.register(this, Disposer.newDisposable(), "ui");

      StartupUtil.addExternalInstanceListener(commandLineArgs -> {
        LOG.info("ApplicationImpl.externalInstanceListener invocation");
        final Project project = CommandLineProcessor.processExternalCommandLine(commandLineArgs, null);
        final IdeFrame  frame = WindowManager.getInstance().getIdeFrame(project);

        if (frame != null) AppIcon.getInstance().requestFocus(frame);
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
      AWTAutoShutdown.getInstance().notifyThreadBusy(thread); // needed for EDT not to exit suddenly
      Disposer.register(this, () -> {
        AWTAutoShutdown.getInstance().notifyThreadFree(thread); // allow for EDT to exit - needed for Upsource
      });
      return Thread.currentThread();
    });
    myLock = new ReadMostlyRWLock(edt);

    if (isUnitTestMode) {
      ApplicationStarter.ourLoaded = true;
    }

    NoSwingUnderWriteAction.watchForEvents(this);
  }

  /**
   * Executes a {@code runnable} in an "impatient" mode.
   * In this mode any attempt to call {@link #runReadAction(Runnable)}
   * would fail (i.e. throw {@link ApplicationUtil.CannotRunReadActionException})
   * if there is a pending write action.
   */
  public void executeByImpatientReader(@NotNull Runnable runnable) throws ApplicationUtil.CannotRunReadActionException {
    if (isDispatchThread()) {
      runnable.run();
    }
    else {
      myLock.executeByImpatientReader(runnable);
    }
  }

  private void registerShutdownHook() {
    ShutDownTracker.getInstance().registerShutdownTask(() -> {
      if (isDisposed() || myDisposeInProgress) {
        return;
      }
      ShutDownTracker.invokeAndWait(isUnitTestMode(), true, new Runnable() {
        @Override
        @RequiredDispatchThread
        public void run() {
          if (ApplicationManager.getApplication() != ApplicationImpl.this) return;
          try {
            myDisposeInProgress = true;
            saveAll();
          }
          finally {
            if (!disposeSelf(true)) {
              myDisposeInProgress = false;
            }
          }
        }
      });
    });
  }

  @RequiredDispatchThread
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
    runWriteAction(() -> Disposer.dispose(ApplicationImpl.this));

    Disposer.assertIsEmpty();
    return true;
  }

  @Override
  public boolean holdsReadLock() {
    return myLock.isReadLockedByThisThread();
  }

  private void loadApplicationComponents() {
    PluginManagerCore.BUILD_NUMBER = ApplicationInfoImpl.getShadowInstance().getBuild().asString();
    PluginManagerCore.initPlugins(mySplash);
    IdeaPluginDescriptor[] plugins = PluginManagerCore.getPlugins();
    for (IdeaPluginDescriptor plugin : plugins) {
      if (!PluginManagerCore.shouldSkipPlugin(plugin)) {
        loadComponentsConfiguration(plugin.getAppComponents(), plugin, false);
      }
    }
  }

  @Override
  protected synchronized Object createComponent(@NotNull Class componentInterface) {
    Object component = super.createComponent(componentInterface);
    if (mySplash != null) {
      mySplash.showProgress("", 0.65f + getPercentageOfComponentsLoaded() * 0.35f);
    }
    return component;
  }

  @NotNull
  @Override
  protected MutablePicoContainer createPicoContainer() {
    return Extensions.getRootArea().getPicoContainer();
  }

  @Override
  public boolean isInternal() {
    return myIsInternal;
  }

  @Override
  public boolean isUnitTestMode() {
    return myTestModeFlag;
  }

  @Override
  public boolean isHeadlessEnvironment() {
    return myHeadlessMode;
  }

  @Override
  public boolean isCompilerServerMode() {
    return false;
  }

  @Override
  public boolean isCommandLine() {
    return myCommandLineMode;
  }

  @NotNull
  @Override
  public Future<?> executeOnPooledThread(@NotNull final Runnable action) {
    ReadMostlyRWLock.SuspensionId suspensionId = myLock.currentReadPrivilege();
    return ourThreadExecutorsService.submit(new Runnable() {
      @Override
      public String toString() {
        return action.toString();
      }

      @Override
      public void run() {
        try (AccessToken ignored = myLock.applyReadPrivilege(suspensionId)) {
          action.run();
        }
        catch (ProcessCanceledException e) {
          // ignore
        }
        catch (Throwable t) {
          LOG.error(t);
        }
        finally {
          Thread.interrupted(); // reset interrupted status
        }
      }
    });
  }

  @NotNull
  @Override
  public <T> Future<T> executeOnPooledThread(@NotNull final Callable<T> action) {
    ReadMostlyRWLock.SuspensionId suspensionId = myLock.currentReadPrivilege();
    return ourThreadExecutorsService.submit(new Callable<T>() {
      @Override
      public T call() {
        try (AccessToken ignored = myLock.applyReadPrivilege(suspensionId)) {
          return action.call();
        }
        catch (ProcessCanceledException e) {
          // ignore
        }
        catch (Throwable t) {
          LOG.error(t);
        }
        finally {
          Thread.interrupted(); // reset interrupted status
        }
        return null;
      }

      @Override
      public String toString() {
        return action.toString();
      }
    });
  }

  @Override
  public boolean isDispatchThread() {
    return myLock.isWriteThread();
  }

  @Override
  @NotNull
  public ModalityInvokator getInvokator() {
    return myInvokator;
  }

  @Override
  public void invokeLater(@NotNull final Runnable runnable) {
    invokeLater(runnable, getDisposed());
  }

  @Override
  public void invokeLater(@NotNull final Runnable runnable, @NotNull final Condition expired) {
    invokeLater(runnable, ModalityState.defaultModalityState(), expired);
  }

  @Override
  public void invokeLater(@NotNull final Runnable runnable, @NotNull final ModalityState state) {
    invokeLater(runnable, state, getDisposed());
  }

  @Override
  public void invokeLater(@NotNull final Runnable runnable, @NotNull final ModalityState state, @NotNull final Condition expired) {
    myInvokator.invokeLater(myTransactionGuard.wrapLaterInvocation(runnable, state), state, expired);
  }

  @Override
  public void load(@Nullable String optionsPath) throws IOException {
    load(PathManager.getConfigPath(), optionsPath == null ? PathManager.getOptionsPath() : optionsPath);
  }

  public void load(@NotNull String configPath, @NotNull String optionsPath) throws IOException {
    IApplicationStore store = getStateStore();
    store.setOptionsPath(optionsPath);
    store.setConfigPath(configPath);

    myIsFiringLoadingEvent = true;
    try {
      fireBeforeApplicationLoaded();
    }
    finally {
      myIsFiringLoadingEvent = false;
    }

    AccessToken token = HeavyProcessLatch.INSTANCE.processStarted("Loading application components");
    try {
      store.load();
    }
    catch (StateStorageException e) {
      throw new IOException(e.getMessage());
    }
    finally {
      token.finish();
    }
    myLoaded = true;

    createLocatorFile();
  }

  private static void createLocatorFile() {
    File locatorFile = new File(PathManager.getSystemPath() + "/" + ApplicationEx.LOCATOR_FILE_NAME);
    try {
      byte[] data = PathManager.getHomePath().getBytes(CharsetToolkit.UTF8_CHARSET);
      FileUtil.writeToFile(locatorFile, data);
    }
    catch (IOException e) {
      LOG.warn("can't store a location in '" + locatorFile + "'", e);
    }
  }

  @Override
  public boolean isLoaded() {
    return myLoaded;
  }

  @Override
  protected <T> T getComponentFromContainer(@NotNull final Class<T> interfaceClass) {
    if (myIsFiringLoadingEvent) {
      return null;
    }
    return super.getComponentFromContainer(interfaceClass);
  }

  private void fireBeforeApplicationLoaded() {
    for (ApplicationLoadListener listener : ApplicationLoadListener.EP_NAME.getExtensions()) {
      try {
        listener.beforeApplicationLoaded(this);
      }
      catch (Exception e) {
        LOG.error(e);
      }
    }
  }

  @Override
  public void dispose() {
    fireApplicationExiting();

    ShutDownTracker.getInstance().ensureStopperThreadsFinished();

    disposeComponents();

    AppScheduledExecutorService service = (AppScheduledExecutorService)AppExecutorUtil.getAppScheduledExecutorService();
    service.shutdownAppScheduledExecutorService();

    super.dispose();
    Disposer.dispose(myLastDisposable); // dispose it last
  }

  @RequiredDispatchThread
  @Override
  public boolean runProcessWithProgressSynchronously(@NotNull final Runnable process, @NotNull String progressTitle, boolean canBeCanceled, Project project) {
    return runProcessWithProgressSynchronously(process, progressTitle, canBeCanceled, project, null);
  }

  @RequiredDispatchThread
  @Override
  public boolean runProcessWithProgressSynchronously(@NotNull final Runnable process,
                                                     @NotNull final String progressTitle,
                                                     final boolean canBeCanceled,
                                                     @Nullable final Project project,
                                                     final JComponent parentComponent) {
    return runProcessWithProgressSynchronously(process, progressTitle, canBeCanceled, project, parentComponent, null);
  }

  @RequiredDispatchThread
  @Override
  public boolean runProcessWithProgressSynchronously(@NotNull final Runnable process,
                                                     @NotNull final String progressTitle,
                                                     final boolean canBeCanceled,
                                                     @Nullable final Project project,
                                                     final JComponent parentComponent,
                                                     final String cancelText) {
    assertIsDispatchThread();
    boolean writeAccessAllowed = isWriteAccessAllowed();
    if (writeAccessAllowed // Disallow running process in separate thread from under write action.
        // The thread will deadlock trying to get read action otherwise.
        || isHeadlessEnvironment() && !isUnitTestMode()) {
      if (writeAccessAllowed) {
        LOG.debug("Starting process with progress from within write action makes no sense");
      }
      try {
        ProgressManager.getInstance().runProcess(process, new EmptyProgressIndicator());
      }
      catch (ProcessCanceledException e) {
        // ok to ignore.
        return false;
      }
      return true;
    }

    final ProgressWindow progress = new ProgressWindow(canBeCanceled, false, project, parentComponent, cancelText);
    // in case of abrupt application exit when 'ProgressManager.getInstance().runProcess(process, progress)' below
    // does not have a chance to run, and as a result the progress won't be disposed
    Disposer.register(this, progress);

    progress.setTitle(progressTitle);

    final AtomicBoolean threadStarted = new AtomicBoolean();
    //noinspection SSBasedInspection
    SwingUtilities.invokeLater(() -> {
      executeOnPooledThread(() -> {
        try {
          ProgressManager.getInstance().runProcess(process, progress);
        }
        catch (ProcessCanceledException e) {
          progress.cancel();
          // ok to ignore.
        }
        catch (RuntimeException e) {
          progress.cancel();
          throw e;
        }
      });
      threadStarted.set(true);
    });

    progress.startBlocking();

    LOG.assertTrue(threadStarted.get());
    LOG.assertTrue(!progress.isRunning());

    return !progress.isCanceled();
  }

  @Override
  public void invokeAndWait(@NotNull Runnable runnable, @NotNull ModalityState modalityState) {
    if (isDispatchThread()) {
      runnable.run();
      return;
    }

    if (holdsReadLock()) {
      LOG.error("Calling invokeAndWait from read-action leads to possible deadlock.");
    }

    LaterInvocator.invokeAndWait(myTransactionGuard.wrapLaterInvocation(runnable, modalityState), modalityState);
  }

  @Override
  @NotNull
  public ModalityState getCurrentModalityState() {
    Object[] entities = LaterInvocator.getCurrentModalEntities();
    return entities.length > 0 ? new ModalityStateEx(entities) : getNoneModalityState();
  }

  @Override
  @NotNull
  public ModalityState getModalityStateForComponent(@NotNull Component c) {
    Window window = UIUtil.getWindow(c);
    if (window == null) return getNoneModalityState(); //?
    return LaterInvocator.modalityStateForWindow(window);
  }

  @Override
  @NotNull
  public ModalityState getAnyModalityState() {
    return ANY;
  }

  @Override
  @NotNull
  public ModalityState getDefaultModalityState() {
    if (isDispatchThread()) {
      return getCurrentModalityState();
    }
    ProgressIndicator progress = ProgressManager.getInstance().getProgressIndicator();
    return progress == null ? getNoneModalityState() : progress.getModalityState();
  }

  @Override
  @NotNull
  public ModalityState getNoneModalityState() {
    return ModalityState.NON_MODAL;
  }

  @Override
  public long getStartTime() {
    return myStartTime;
  }

  @RequiredDispatchThread
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
  public void restart() {
    restart(false);
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
        @RequiredDispatchThread
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

  @RequiredDispatchThread
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

      @NotNull
      @Override
      public String getDoNotShowMessage() {
        return "Do not ask me again";
      }
    };

    if (hasUnsafeBgTasks || option.isToBeShown()) {
      String message = ApplicationBundle
              .message(hasUnsafeBgTasks ? "exit.confirm.prompt.tasks" : "exit.confirm.prompt", ApplicationNamesInfo.getInstance().getFullProductName());

      if (MessageDialogBuilder.yesNo(ApplicationBundle.message("exit.confirm.title"), message).yesText(ApplicationBundle.message("command.exit"))
                  .noText(CommonBundle.message("button.cancel")).doNotAsk(option).show() != Messages.YES) {
        return false;
      }
    }
    return true;
  }

  private boolean canExit() {
    for (ApplicationListener applicationListener : myDispatcher.getListeners()) {
      if (!applicationListener.canExitApplication()) {
        return false;
      }
    }

    ProjectManagerEx projectManager = (ProjectManagerEx)ProjectManager.getInstance();
    Project[] projects = projectManager.getOpenProjects();
    for (Project project : projects) {
      if (!projectManager.canClose(project)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public void runReadAction(@NotNull final Runnable action) {
    if (isReadAccessAllowed()) {
      action.run();
    }
    else {
      startRead();
      try {
        action.run();
      }
      finally {
        endRead();
      }
    }
  }

  @Override
  public <T> T runReadAction(@NotNull final Computable<T> computation) {
    if (isReadAccessAllowed()) {
      return computation.compute();
    }
    startRead();
    try {
      return computation.compute();
    }
    finally {
      endRead();
    }
  }

  @Override
  public <T, E extends Throwable> T runReadAction(@NotNull ThrowableComputable<T, E> computation) throws E {
    if (isReadAccessAllowed()) {
      return computation.compute();
    }
    startRead();
    try {
      return computation.compute();
    }
    finally {
      endRead();
    }
  }

  private void startRead() {
    assertNoPsiLock();
    myLock.readLock();
  }

  private void endRead() {
    myLock.readUnlock();
  }

  public boolean runWriteActionWithProgressInDispatchThread(@NotNull String title,
                                                            @Nullable Project project,
                                                            @Nullable JComponent parentComponent,
                                                            @Nullable String cancelText,
                                                            @NotNull Consumer<ProgressIndicator> action) {
    Class<?> clazz = action.getClass();
    startWrite(clazz);
    try {
      PotemkinProgress indicator = new PotemkinProgress(title, project, parentComponent, cancelText);
      indicator.runInSwingThread(() -> action.accept(indicator));
      return !indicator.isCanceled();
    }
    finally {
      endWrite(clazz);
    }
  }

  public boolean runWriteActionWithProgressInBackgroundThread(@NotNull String title,
                                                              @Nullable Project project,
                                                              @Nullable JComponent parentComponent,
                                                              @Nullable String cancelText,
                                                              @NotNull Consumer<ProgressIndicator> action) {
    Class<?> clazz = action.getClass();
    startWrite(clazz);
    try {
      PotemkinProgress indicator = new PotemkinProgress(title, project, parentComponent, cancelText);
      indicator.runInBackground(() -> {
        assert myWriteActionThread == null;
        myWriteActionThread = Thread.currentThread();
        try {
          action.accept(indicator);
        }
        finally {
          myWriteActionThread = null;
        }
      });
      return !indicator.isCanceled();
    }
    finally {
      endWrite(clazz);
    }
  }

  @RequiredDispatchThread
  @Override
  public void runWriteAction(@NotNull final Runnable action) {
    Class<? extends Runnable> clazz = action.getClass();
    startWrite(clazz);
    try {
      action.run();
    }
    finally {
      endWrite(clazz);
    }
  }

  @RequiredDispatchThread
  @Override
  public <T> T runWriteAction(@NotNull final Computable<T> computation) {
    Class<? extends Computable> clazz = computation.getClass();
    startWrite(clazz);
    try {
      return computation.compute();
    }
    finally {
      endWrite(clazz);
    }
  }

  @RequiredDispatchThread
  @Override
  public <T, E extends Throwable> T runWriteAction(@NotNull ThrowableComputable<T, E> computation) throws E {
    Class<? extends ThrowableComputable> clazz = computation.getClass();
    startWrite(clazz);
    try {
      return computation.compute();
    }
    finally {
      endWrite(clazz);
    }
  }

  @RequiredDispatchThread
  @Override
  public boolean hasWriteAction(@NotNull Class<?> actionClass) {
    assertReadAccessAllowed();

    for (int i = myWriteActionsStack.size() - 1; i >= 0; i--) {
      Class action = myWriteActionsStack.get(i);
      if (actionClass == action || ReflectionUtil.isAssignable(actionClass, action)) return true;
    }
    return false;
  }

  @RequiredReadAction
  @Override
  public void assertReadAccessAllowed() {
    if (!isReadAccessAllowed()) {
      LOG.error("Read access is allowed from event dispatch thread or inside read-action only" +
                " (see com.intellij.openapi.application.Application.runReadAction())", "Current thread: " + describe(Thread.currentThread()),
                "; dispatch thread: " + EventQueue.isDispatchThread() + "; isDispatchThread(): " + isDispatchThread(),
                "SystemEventQueueThread: " + describe(getEventQueueThread()));
    }
  }

  @NonNls
  private static String describe(Thread o) {
    if (o == null) return "null";
    return o + " " + System.identityHashCode(o);
  }

  private static Thread getEventQueueThread() {
    EventQueue eventQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
    return AWTAccessor.getEventQueueAccessor().getDispatchThread(eventQueue);
  }

  @Override
  public boolean isReadAccessAllowed() {
    if (isDispatchThread()) {
      return myWriteActionThread == null; // no reading from EDT during background write action
    }
    return myLock.isReadLockedByThisThread() || myWriteActionThread == Thread.currentThread();
  }

  @RequiredDispatchThread
  @Override
  public void assertIsDispatchThread() {
    if (isDispatchThread()) return;
    if (ShutDownTracker.isShutdownHookRunning()) return;
    assertIsDispatchThread("Access is allowed from event dispatch thread only.");
  }

  private void assertIsDispatchThread(@NotNull String message) {
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

  @RequiredDispatchThread
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
    if (myTestModeFlag || myHeadlessMode || ShutDownTracker.isShutdownHookRunning()) return;
    LOG.assertTrue(!isDispatchThread(), "This operation is time consuming and must not be called on EDT");
  }

  @Override
  public boolean tryRunReadAction(@NotNull Runnable action) {
    //if we are inside read action, do not try to acquire read lock again since it will deadlock if there is a pending writeAction
    if (isReadAccessAllowed()) {
      action.run();
    }
    else {
      assertNoPsiLock();
      if (!myLock.tryReadLock()) return false;
      try {
        action.run();
      }
      finally {
        endRead();
      }
    }
    return true;
  }

  @Override
  public boolean isActive() {
    if (isUnitTestMode()) return true;

    Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();

    if (ApplicationActivationStateManager.getState().isInactive() && activeWindow != null) {
      ApplicationActivationStateManager.updateState(activeWindow);
    }

    return ApplicationActivationStateManager.getState().isActive();
  }

  @NotNull
  @Override
  public AccessToken acquireReadActionLock() {
    // if we are inside read action, do not try to acquire read lock again since it will deadlock if there is a pending writeAction
    return isReadAccessAllowed() ? AccessToken.EMPTY_ACCESS_TOKEN : new ReadAccessToken();
  }

  private volatile boolean myWriteActionPending;

  @Override
  public boolean isWriteActionPending() {
    return myWriteActionPending;
  }

  private void startWrite(@NotNull Class clazz) {
    if (!isWriteAccessAllowed()) {
      assertIsDispatchThread("Write access is allowed from event dispatch thread only");
    }
    HeavyProcessLatch.INSTANCE.stopThreadPrioritizing(); // let non-cancellable read actions complete faster, if present
    boolean writeActionPending = myWriteActionPending;
    if (gatherStatistics && myWriteActionsStack.isEmpty() && !writeActionPending) {
      ActionPauses.WRITE.started();
    }
    myWriteActionPending = true;
    try {
      ActivityTracker.getInstance().inc();
      fireBeforeWriteActionStart(clazz);

      if (!myLock.isWriteLocked()) {
        assertNoPsiLock();
        if (!myLock.tryWriteLock()) {
          Future<?> reportSlowWrite = ourDumpThreadsOnLongWriteActionWaiting <= 0
                                      ? null
                                      : JobScheduler.getScheduler().scheduleWithFixedDelay(() -> PerformanceWatcher.getInstance().dumpThreads("waiting", true),
                                                                                           ourDumpThreadsOnLongWriteActionWaiting,
                                                                                           ourDumpThreadsOnLongWriteActionWaiting, TimeUnit.MILLISECONDS);
          myLock.writeLock();
          if (reportSlowWrite != null) {
            reportSlowWrite.cancel(false);
          }
        }
      }
    }
    finally {
      myWriteActionPending = writeActionPending;
    }

    myWriteActionsStack.push(clazz);
    fireWriteActionStarted(clazz);
  }

  private void endWrite(Class clazz) {
    try {
      fireWriteActionFinished(clazz);
      // fire listeners before popping stack because if somebody starts write action in a listener,
      // there is a danger of unlocking the write lock before other listeners have been run (since write lock became non-reentrant).
    }
    finally {
      myWriteActionsStack.pop();
      if (gatherStatistics && myWriteActionsStack.isEmpty() && !myWriteActionPending) {
        ActionPauses.WRITE.finished("write action (" + clazz + ")");
      }
      if (myWriteActionsStack.size() == myWriteStackBase) {
        myLock.writeUnlock();
      }
      if (myWriteActionsStack.isEmpty()) {
        fireAfterWriteActionFinished(clazz);
      }
    }
  }

  @RequiredDispatchThread
  @NotNull
  @Override
  public AccessToken acquireWriteActionLock(@NotNull Class clazz) {
    return new WriteAccessToken(clazz);
  }

  private class WriteAccessToken extends AccessToken {
    @NotNull
    private final Class clazz;

    public WriteAccessToken(@NotNull Class clazz) {
      this.clazz = clazz;
      startWrite(clazz);
      markThreadNameInStackTrace();
    }

    @Override
    public void finish() {
      try {
        endWrite(clazz);
      }
      finally {
        unmarkThreadNameInStackTrace();
      }
    }

    private void markThreadNameInStackTrace() {
      String id = id();

      if (id != null) {
        final Thread thread = Thread.currentThread();
        thread.setName(thread.getName() + id);
      }
    }

    private void unmarkThreadNameInStackTrace() {
      String id = id();

      if (id != null) {
        final Thread thread = Thread.currentThread();
        String name = thread.getName();
        name = StringUtil.replace(name, id, "");
        thread.setName(name);
      }
    }

    private String id() {
      Class aClass = getClass();
      String name = aClass.getName();
      while (name == null) {
        aClass = aClass.getSuperclass();
        name = aClass.getName();
      }

      name = name.substring(name.lastIndexOf('.') + 1);
      name = name.substring(name.lastIndexOf('$') + 1);
      if (!name.equals("AccessToken")) {
        return " [" + name + "]";
      }
      return null;
    }
  }

  private class ReadAccessToken extends AccessToken {
    private ReadAccessToken() {
      startRead();
    }

    @Override
    public void finish() {
      endRead();
    }
  }

  private final boolean myExtraChecks = isUnitTestMode();

  private void assertNoPsiLock() {
    if (myExtraChecks) {
      LOG.assertTrue(!Thread.holdsLock(PsiLock.LOCK), "Thread must not hold PsiLock while performing readAction");
    }
  }

  @RequiredWriteAction
  @Override
  public void assertWriteAccessAllowed() {
    LOG.assertTrue(isWriteAccessAllowed(),
                   "Write access is allowed inside write-action only (see com.intellij.openapi.application.Application.runWriteAction())");
  }

  @Override
  public boolean isWriteAccessAllowed() {
    return isDispatchThread() && myLock.isWriteLocked() || myWriteActionThread == Thread.currentThread();
  }

  // cheaper version of isWriteAccessAllowed(). must be called from EDT
  private boolean isInsideWriteActionEDTOnly() {
    return !myWriteActionsStack.isEmpty();
  }

  @Override
  public boolean isWriteActionInProgress() {
    return myLock.isWriteLocked();
  }

  public void executeSuspendingWriteAction(@Nullable Project project, @NotNull String title, @NotNull Runnable runnable) {
    assertIsDispatchThread();
    if (!myLock.isWriteLocked()) {
      runModalProgress(project, title, runnable);
      return;
    }

    myTransactionGuard.submitTransactionAndWait(() -> {
      int prevBase = myWriteStackBase;
      myWriteStackBase = myWriteActionsStack.size();
      try (AccessToken ignored = myLock.writeSuspend()) {
        runModalProgress(project, title, () -> {
          try (AccessToken ignored1 = myLock.grantReadPrivilege()) {
            runnable.run();
          }
        });
      }
      finally {
        myWriteStackBase = prevBase;
      }
    });
  }

  private static void runModalProgress(@Nullable Project project, @NotNull String title, @NotNull Runnable runnable) {
    ProgressManager.getInstance().run(new Task.Modal(project, title, false) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        runnable.run();
      }
    });
  }

  public void editorPaintStart() {
    myInEditorPaintCounter++;
  }

  public void editorPaintFinish() {
    myInEditorPaintCounter--;
    LOG.assertTrue(myInEditorPaintCounter >= 0);
  }

  @Override
  public void addApplicationListener(@NotNull ApplicationListener l) {
    myDispatcher.addListener(l);
  }

  @Override
  public void addApplicationListener(@NotNull ApplicationListener l, @NotNull Disposable parent) {
    myDispatcher.addListener(l, parent);
  }

  @Override
  public void removeApplicationListener(@NotNull ApplicationListener l) {
    myDispatcher.removeListener(l);
  }

  private void fireApplicationExiting() {
    myDispatcher.getMulticaster().applicationExiting();
  }

  private void fireBeforeWriteActionStart(@NotNull Class action) {
    myDispatcher.getMulticaster().beforeWriteActionStart(action);
  }

  private void fireWriteActionStarted(@NotNull Class action) {
    myDispatcher.getMulticaster().writeActionStarted(action);
  }

  private void fireWriteActionFinished(@NotNull Class action) {
    myDispatcher.getMulticaster().writeActionFinished(action);
  }

  private void fireAfterWriteActionFinished(@NotNull Class action) {
    myDispatcher.getMulticaster().afterWriteActionFinished(action);
  }

  // public for testing purposes
  public void _saveSettings() {
    if (mySaveSettingsIsInProgress.compareAndSet(false, true)) {
      try {
        StoreUtil.save(getStateStore(), null);
      }
      finally {
        mySaveSettingsIsInProgress.set(false);
      }
    }
  }

  @Override
  public void saveSettings() {
    if (myDoNotSave) return;
    _saveSettings();
  }

  @Override
  public void saveAll() {
    if (myDoNotSave) return;

    FileDocumentManager.getInstance().saveAllDocuments();

    Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
    for (Project openProject : openProjects) {
      ProjectEx project = (ProjectEx)openProject;
      project.save();
    }

    saveSettings();
  }

  @Override
  public void doNotSave() {
    doNotSave(true);
  }

  @Override
  public void doNotSave(boolean value) {
    myDoNotSave = value;
  }

  @Override
  public boolean isDoNotSave() {
    return myDoNotSave;
  }

  @NotNull
  @Override
  public <T> T[] getExtensions(@NotNull final ExtensionPointName<T> extensionPointName) {
    return Extensions.getRootArea().getExtensionPoint(extensionPointName).getExtensions();
  }

  @Override
  public boolean isDisposeInProgress() {
    return myDisposeInProgress || ShutDownTracker.isShutdownHookRunning();
  }

  @Override
  public boolean isRestartCapable() {
    return Restarter.isSupported();
  }

  @TestOnly
  public void setDisposeInProgress(boolean disposeInProgress) {
    myDisposeInProgress = disposeInProgress;
  }

  @NonNls
  @Override
  public String toString() {
    return "Application" +
           (isDisposed() ? " (Disposed)" : "") +
           (isUnitTestMode() ? " (Unit test)" : "") +
           (isInternal() ? " (Internal)" : "") +
           (isHeadlessEnvironment() ? " (Headless)" : "") +
           (isCommandLine() ? " (Command line)" : "");
  }
}
