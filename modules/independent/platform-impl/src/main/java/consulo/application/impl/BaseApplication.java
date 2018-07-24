/*
 * Copyright 2013-2018 consulo.io
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
package consulo.application.impl;

import com.google.inject.Binder;
import com.intellij.concurrency.JobScheduler;
import com.intellij.diagnostic.PerformanceWatcher;
import com.intellij.icons.AllIcons;
import com.intellij.ide.ActivityTracker;
import com.intellij.ide.ApplicationLoadListener;
import com.intellij.ide.StartupProgress;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.*;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationUtil;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import com.intellij.openapi.application.impl.ReadMostlyRWLock;
import com.intellij.openapi.components.*;
import com.intellij.openapi.components.impl.ApplicationPathMacroManager;
import com.intellij.openapi.components.impl.PlatformComponentManagerImpl;
import com.intellij.openapi.components.impl.ServiceManagerImpl;
import com.intellij.openapi.components.impl.stores.ApplicationStoreImpl;
import com.intellij.openapi.components.impl.stores.IApplicationStore;
import com.intellij.openapi.components.impl.stores.IComponentStore;
import com.intellij.openapi.components.impl.stores.StoreUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ex.ProjectEx;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.util.EventDispatcher;
import com.intellij.util.PausesStat;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.concurrency.AppScheduledExecutorService;
import com.intellij.util.containers.Stack;
import com.intellij.util.io.storage.HeavyProcessLatch;
import consulo.annotations.RequiredDispatchThread;
import consulo.annotations.RequiredWriteAction;
import consulo.application.ApplicationProperties;
import consulo.application.ex.ApplicationEx2;
import consulo.ui.image.Image;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.ide.PooledThreadExecutor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author VISTALL
 * @since 2018-05-12
 */
public abstract class BaseApplication extends PlatformComponentManagerImpl implements ApplicationEx2 {
  private class ReadAccessToken extends AccessToken {
    private ReadAccessToken() {
      startRead();
    }

    @Override
    public void finish() {
      endRead();
    }
  }

  private class WriteAccessToken extends AccessToken {
    @Nonnull
    private final Class clazz;

    public WriteAccessToken(@Nonnull Class clazz) {
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

  private static class ActionPauses {
    private static final PausesStat WRITE = new PausesStat("Write action");
  }

  private static final Logger LOG = Logger.getInstance(BaseApplication.class);

  private static final int ourDumpThreadsOnLongWriteActionWaiting = Integer.getInteger("dump.threads.on.long.write.action.waiting", 0);

  @Nonnull
  protected final Ref<? extends StartupProgress> mySplashRef;

  protected final Disposable myLastDisposable = Disposer.newDisposable(); // will be disposed last
  private final EventDispatcher<ApplicationListener> myDispatcher = EventDispatcher.create(ApplicationListener.class);

  private final ExecutorService myThreadExecutorsService = PooledThreadExecutor.INSTANCE;

  protected final Stack<Class> myWriteActionsStack = new Stack<>();

  private final long myStartTime;

  protected ReadMostlyRWLock myLock;

  private boolean myIsFiringLoadingEvent;
  protected boolean myDoNotSave;
  private boolean myLoaded;
  private volatile boolean myWriteActionPending;

  protected int myWriteStackBase;
  protected volatile Thread myWriteActionThread;

  protected boolean myGatherStatistics;

  private final AtomicBoolean mySaveSettingsIsInProgress = new AtomicBoolean(false);

  public BaseApplication(ComponentManager parent, @Nonnull Ref<? extends StartupProgress> splashRef) {
    super(parent);
    mySplashRef = splashRef;
    myStartTime = System.currentTimeMillis();

    Extensions.setRootArea(this);

    Disposer.register(() -> {
      Extensions.setRootArea(null);
    }, this);
    ApplicationManager.setApplication(this, myLastDisposable); // reset back to null only when all components already disposed
  }

  protected void initPlugins() {
    PluginManagerCore.BUILD_NUMBER = ApplicationInfoImpl.getShadowInstance().getBuild().asString();
    PluginManagerCore.initPlugins(mySplashRef.get());
  }

  @Nonnull
  @Override
  protected ComponentConfig[] selectComponentConfigs(IdeaPluginDescriptor descriptor) {
    return descriptor.getAppComponents();
  }

  @Override
  public String getAreaId() {
    return ExtensionAreas.APPLICATION;
  }

  @Nonnull
  @Override
  protected ExtensionPointName<ServiceDescriptor> getServiceEpName() {
    return ServiceManagerImpl.APP_SERVICES;
  }

  protected void fireApplicationExiting() {
    myDispatcher.getMulticaster().applicationExiting();
  }

  protected void fireBeforeWriteActionStart(@Nonnull Class action) {
    myDispatcher.getMulticaster().beforeWriteActionStart(action);
  }

  protected void fireWriteActionStarted(@Nonnull Class action) {
    myDispatcher.getMulticaster().writeActionStarted(action);
  }

  protected void fireWriteActionFinished(@Nonnull Class action) {
    myDispatcher.getMulticaster().writeActionFinished(action);
  }

  protected void fireAfterWriteActionFinished(@Nonnull Class action) {
    myDispatcher.getMulticaster().afterWriteActionFinished(action);
  }

  @Override
  public void load(@Nullable String optionsPath) throws IOException {
    load(PathManager.getConfigPath(), optionsPath == null ? PathManager.getOptionsPath() : optionsPath);
  }

  public void load(@Nonnull String configPath, @Nonnull String optionsPath) throws IOException {
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
  public void saveSettings() {
    if (myDoNotSave) return;
    _saveSettings();
  }

  // public for testing purposes
  public void _saveSettings() {
    if (mySaveSettingsIsInProgress.compareAndSet(false, true)) {
      HeavyProcessLatch.INSTANCE.prioritizeUiActivity();

      try {
        StoreUtil.save(getStateStore(), null);
      }
      finally {
        mySaveSettingsIsInProgress.set(false);
      }
    }
  }

  @RequiredDispatchThread
  @Override
  public void saveAll() {
    if (myDoNotSave) return;

    FileDocumentManager.getInstance().saveAllDocuments();

    Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
    for (Project openProject : openProjects) {
      if (openProject.isDisposed()) {
        // debug for https://github.com/consulo/consulo/issues/296
        LOG.error("Project is disposed: " + openProject.getName() + ", isInitialized: " + openProject.isInitialized());
        continue;
      }

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

  @Nonnull
  @Override
  public <T> T[] getExtensions(@Nonnull final ExtensionPointName<T> extensionPointName) {
    return Extensions.getRootArea().getExtensionPoint(extensionPointName).getExtensions();
  }

  @Override
  public boolean isLoaded() {
    return myLoaded;
  }

  @Nonnull
  @Override
  public Future<?> executeOnPooledThread(@Nonnull final Runnable action) {
    ReadMostlyRWLock.SuspensionId suspensionId = myLock.currentReadPrivilege();
    return myThreadExecutorsService.submit(new Runnable() {
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

  @Nonnull
  @Override
  public <T> Future<T> executeOnPooledThread(@Nonnull final Callable<T> action) {
    ReadMostlyRWLock.SuspensionId suspensionId = myLock.currentReadPrivilege();
    return myThreadExecutorsService.submit(new Callable<T>() {
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
  public long getStartTime() {
    return myStartTime;
  }

  @Override
  public void addApplicationListener(@Nonnull ApplicationListener l) {
    myDispatcher.addListener(l);
  }

  @Override
  public void addApplicationListener(@Nonnull ApplicationListener l, @Nonnull Disposable parent) {
    myDispatcher.addListener(l, parent);
  }

  @Override
  public void removeApplicationListener(@Nonnull ApplicationListener l) {
    myDispatcher.removeListener(l);
  }

  protected boolean canExit() {
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
  public void dispose() {
    fireApplicationExiting();

    ShutDownTracker.getInstance().ensureStopperThreadsFinished();

    disposeComponents();

    AppScheduledExecutorService service = (AppScheduledExecutorService)AppExecutorUtil.getAppScheduledExecutorService();
    service.shutdownAppScheduledExecutorService();

    super.dispose();
    Disposer.dispose(myLastDisposable); // dispose it last
  }

  @Override
  protected synchronized Object createComponent(@Nonnull Class componentInterface) {
    Object component = super.createComponent(componentInterface);
    StartupProgress progress = mySplashRef.get();
    if (progress != null) {
      progress.showProgress("", 0.65f + getPercentageOfComponentsLoaded() * 0.35f);
    }
    return component;
  }

  @Override
  protected void bootstrapBinder(String name, Binder binder) {
    super.bootstrapBinder(name, binder);

    binder.bind(Application.class).toInstance(this);
    binder.bind(ApplicationEx.class).toInstance(this);
    binder.bind(ApplicationEx2.class).toInstance(this);
    binder.bind(IComponentStore.class).to(ApplicationStoreImpl.class);
    binder.bind(PathMacroManager.class).to(ApplicationPathMacroManager.class);
    binder.bind(TransactionGuard.class).to(TransactionGuardImpl.class);
  }

  @Override
  @Nonnull
  public IApplicationStore getStateStore() {
    return (IApplicationStore)getInjector().getInstance(IComponentStore.class);
  }

  @Override
  public void initializeFromStateStore(@Nonnull Object component, boolean service) {
    getStateStore().initComponent(component);
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return ApplicationProperties.isInSandbox() ? AllIcons.Icon16_Sandbox : AllIcons.Icon16;
  }

  @Nonnull
  @Override
  public AccessToken acquireReadActionLock() {
    // if we are inside read action, do not try to acquire read lock again since it will deadlock if there is a pending writeAction
    return isReadAccessAllowed() ? AccessToken.EMPTY_ACCESS_TOKEN : new ReadAccessToken();
  }

  @Override
  public void runReadAction(@Nonnull final Runnable action) {
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
  public <T> T runReadAction(@Nonnull final Computable<T> computation) {
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
  public <T, E extends Throwable> T runReadAction(@Nonnull ThrowableComputable<T, E> computation) throws E {
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
  public boolean tryRunReadAction(@Nonnull Runnable action) {
    //if we are inside read action, do not try to acquire read lock again since it will deadlock if there is a pending writeAction
    if (isReadAccessAllowed()) {
      action.run();
    }
    else {
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

  private void startRead() {
    myLock.readLock();
  }

  private void endRead() {
    myLock.readUnlock();
  }

  @Override
  public boolean isWriteActionPending() {
    return myWriteActionPending;
  }

  @RequiredWriteAction
  @Override
  public void assertWriteAccessAllowed() {
    LOG.assertTrue(isWriteAccessAllowed(), "Write access is allowed inside write-action only (see com.intellij.openapi.application.Application.runWriteAction())");
  }

  @Override
  public boolean holdsReadLock() {
    return myLock.isReadLockedByThisThread();
  }

  /**
   * Executes a {@code runnable} in an "impatient" mode.
   * In this mode any attempt to call {@link #runReadAction(Runnable)}
   * would fail (i.e. throw {@link ApplicationUtil.CannotRunReadActionException})
   * if there is a pending write action.
   */
  @Override
  public void executeByImpatientReader(@Nonnull Runnable runnable) throws ApplicationUtil.CannotRunReadActionException {
    if (isDispatchThread()) {
      runnable.run();
    }
    else {
      myLock.executeByImpatientReader(runnable);
    }
  }

  @Override
  public boolean isWriteActionInProgress() {
    return myLock.isWriteLocked();
  }

  protected void assertWriteActionStart() {
    if (!isWriteAccessAllowed()) {
      throw new IllegalArgumentException("Can't start write action from current thread. Thread: " + Thread.currentThread().getName());
    }
  }

  protected void startWrite(@Nonnull Class clazz) {
    assertWriteActionStart();

    HeavyProcessLatch.INSTANCE.stopThreadPrioritizing(); // let non-cancellable read actions complete faster, if present
    boolean writeActionPending = myWriteActionPending;
    if (myGatherStatistics && myWriteActionsStack.isEmpty() && !writeActionPending) {
      ActionPauses.WRITE.started();
    }
    myWriteActionPending = true;
    try {
      ActivityTracker.getInstance().inc();
      fireBeforeWriteActionStart(clazz);

      if (!myLock.isWriteLocked()) {
        if (!myLock.tryWriteLock()) {
          Future<?> reportSlowWrite = ourDumpThreadsOnLongWriteActionWaiting <= 0
                                      ? null
                                      : JobScheduler.getScheduler().scheduleWithFixedDelay(() -> PerformanceWatcher.getInstance().dumpThreads("waiting", true), ourDumpThreadsOnLongWriteActionWaiting,
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

  protected void endWrite(Class clazz) {
    try {
      fireWriteActionFinished(clazz);
      // fire listeners before popping stack because if somebody starts write action in a listener,
      // there is a danger of unlocking the write lock before other listeners have been run (since write lock became non-reentrant).
    }
    finally {
      myWriteActionsStack.pop();
      if (myGatherStatistics && myWriteActionsStack.isEmpty() && !myWriteActionPending) {
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
  @Nonnull
  @Override
  public AccessToken acquireWriteActionLock(@Nonnull Class clazz) {
    return new WriteAccessToken(clazz);
  }

  @RequiredDispatchThread
  @Override
  public void runWriteAction(@Nonnull final Runnable action) {
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
  public <T> T runWriteAction(@Nonnull final Computable<T> computation) {
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
  public <T, E extends Throwable> T runWriteAction(@Nonnull ThrowableComputable<T, E> computation) throws E {
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
  public boolean hasWriteAction(@Nonnull Class<?> actionClass) {
    assertReadAccessAllowed();

    for (int i = myWriteActionsStack.size() - 1; i >= 0; i--) {
      Class action = myWriteActionsStack.get(i);
      if (actionClass == action || ReflectionUtil.isAssignable(actionClass, action)) return true;
    }
    return false;
  }

  @Override
  public boolean isWriteThread() {
    return myLock.isWriteThread();
  }

  @NonNls
  @Override
  public String toString() {
    return "Application" +
           (isDisposed() ? " (Disposed)" : "") +
           (ApplicationProperties.isInSandbox() ? " (Sandbox)" : "") +
           (isHeadlessEnvironment() ? " (Headless)" : "") +
           (isCommandLine() ? " (Command line)" : "");
  }
}
