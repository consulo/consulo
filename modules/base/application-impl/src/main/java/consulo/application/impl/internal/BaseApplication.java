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
package consulo.application.impl.internal;

import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ComponentScope;
import consulo.application.AllIcons;
import consulo.application.Application;
import consulo.application.ApplicationProperties;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.concurrent.DataLock;
import consulo.application.event.ApplicationListener;
import consulo.application.event.ApplicationLoadListener;
import consulo.application.impl.internal.concurent.AppScheduledExecutorService;
import consulo.application.impl.internal.concurent.locking.BaseDataLock;
import consulo.application.impl.internal.concurent.locking.NewDataLock;
import consulo.application.impl.internal.start.StartupProgress;
import consulo.application.impl.internal.store.IApplicationStore;
import consulo.application.internal.ApplicationEx;
import consulo.application.internal.ApplicationInfo;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.application.util.concurrent.PooledThreadExecutor;
import consulo.component.ComponentManager;
import consulo.component.ProcessCanceledException;
import consulo.component.impl.internal.ComponentBinding;
import consulo.component.internal.inject.InjectingContainerBuilder;
import consulo.component.store.impl.internal.IComponentStore;
import consulo.component.store.impl.internal.StateStorageException;
import consulo.component.store.impl.internal.StoreUtil;
import consulo.container.boot.ContainerPathManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.FileDocumentManager;
import consulo.language.file.FileTypeManager;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.internal.ProjectEx;
import consulo.project.internal.ProjectManagerEx;
import consulo.proxy.EventDispatcher;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.io.FileUtil;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.SemVer;
import consulo.util.lang.ShutDownTracker;
import consulo.util.lang.function.ThrowableSupplier;
import consulo.util.lang.lazy.LazyValue;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.encoding.ApplicationEncodingManager;
import consulo.virtualFileSystem.encoding.EncodingRegistry;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2018-05-12
 */
public abstract class BaseApplication extends PlatformComponentManagerImpl implements ApplicationEx {
  private static final Logger LOG = Logger.getInstance(BaseApplication.class);

  private static final int ourDumpThreadsOnLongWriteActionWaiting = Integer.getInteger("dump.threads.on.long.write.action.waiting", 0);

  @Nonnull
  protected final SimpleReference<? extends StartupProgress> mySplashRef;

  protected final Disposable myLastDisposable = Disposable.newDisposable(); // will be disposed last
  private final EventDispatcher<ApplicationListener> myDispatcher = EventDispatcher.create(ApplicationListener.class);

  private final long myStartTime;

  protected boolean myDoNotSave;
  private boolean myLoaded;
  private volatile boolean myWriteActionPending;

  protected int myWriteStackBase;

  protected boolean myGatherStatistics;

  private final AtomicBoolean mySaveSettingsIsInProgress = new AtomicBoolean(false);

  private ProgressManager myProgressManager;

  private final BaseDataLock myLock;

  private final Supplier<SemVer> myVersionValue = LazyValue.notNull(() -> {
    ApplicationInfo instance = ApplicationInfo.getInstance();
    String majorVersion = instance.getMajorVersion();
    String minorVersion = instance.getMinorVersion();

    String version = majorVersion + "." + minorVersion + ".0";
    return SemVer.parseFromText(version);
  });

  public BaseApplication(@Nonnull ComponentBinding componentBinding, @Nonnull SimpleReference<? extends StartupProgress> splashRef) {
    super(null, "Application", ComponentScope.APPLICATION, componentBinding);
    mySplashRef = splashRef;
    myStartTime = System.currentTimeMillis();

    myLock = createLock(myDispatcher);
  }

  @Nonnull
  protected BaseDataLock createLock(EventDispatcher<ApplicationListener> dispatcher) {
    return new NewDataLock(dispatcher);
  }

  @Nonnull
  @Override
  public DataLock getLock() {
    return myLock;
  }

  @Override
  public void initNotLazyServices() {
    // reinit progress manager since, it can try call getInstance while application is disposed
    myProgressManager = getInjectingContainer().getInstance(ProgressManager.class);

    super.initNotLazyServices();
  }

  @Nonnull
  @Override
  public ProgressManager getProgressManager() {
    return myProgressManager;
  }

  @Override
  public void executeNonCancelableSection(@Nonnull Runnable runnable) {
    if (myProgressManager != null) {
      myProgressManager.executeNonCancelableSection(runnable);
    }
    else {
      runnable.run();
    }
  }

  @Nullable
  @Override
  public ProgressIndicatorProvider getProgressIndicatorProvider() {
    return myProgressManager;
  }

  @Override
  protected void bootstrapInjectingContainer(@Nonnull InjectingContainerBuilder builder) {
    super.bootstrapInjectingContainer(builder);

    builder.bind(Application.class).to(this);
    builder.bind(ApplicationEx.class).to(this);
    builder.bind(ApplicationInfo.class).to(ApplicationInfo::getInstance);
    builder.bind(ContainerPathManager.class).to(ContainerPathManager::get);
    builder.bind(DataLock.class).to(this::getLock);

    builder.bind(FileTypeRegistry.class).to(FileTypeManager::getInstance);
    builder.bind(ProgressIndicatorProvider.class).to(this::getProgressManager);
    builder.bind(EncodingRegistry.class).to(ApplicationEncodingManager::getInstance);
  }

  protected void fireApplicationExiting() {
    myDispatcher.getMulticaster().applicationExiting();
  }

  @Override
  public void load(@Nullable String optionsPath) throws IOException {
    load(ContainerPathManager.get().getConfigPath(), optionsPath == null ? ContainerPathManager.get().getOptionsPath() : optionsPath);
  }

  public void load(@Nonnull String configPath, @Nonnull String optionsPath) throws IOException {
    IApplicationStore store = getStateStore();
    store.setOptionsPath(optionsPath);
    store.setConfigPath(configPath);

    fireBeforeApplicationLoaded();

    try {
      store.load();
    }
    catch (StateStorageException e) {
      throw new IOException(e.getMessage());
    }

    myLoaded = true;

    createLocatorFile();
  }

  private static void createLocatorFile() {
    ContainerPathManager containerPathManager = ContainerPathManager.get();
    File locatorFile = new File(containerPathManager.getSystemPath() + "/" + ApplicationEx.LOCATOR_FILE_NAME);
    try {
      byte[] data = containerPathManager.getHomePath().getBytes(StandardCharsets.UTF_8);
      FileUtil.writeToFile(locatorFile, data);
    }
    catch (IOException e) {
      LOG.warn("can't store a location in '" + locatorFile + "'", e);
    }
  }

  private void fireBeforeApplicationLoaded() {
    getExtensionPoint(ApplicationLoadListener.class).forEachExtensionSafe(ApplicationLoadListener::beforeApplicationLoaded);
  }

  @Override
  public void saveSettings() {
    if (myDoNotSave) return;

    if (mySaveSettingsIsInProgress.compareAndSet(false, true)) {
      try {
        StoreUtil.save(getStateStore(), false, null);
      }
      finally {
        mySaveSettingsIsInProgress.set(false);
      }
    }
  }

  @RequiredWriteAction
  @Nonnull
  @Override
  public CompletableFuture<?> saveSettingsAsync() {
    if (myDoNotSave) return CompletableFuture.completedFuture(null);

    if (mySaveSettingsIsInProgress.compareAndSet(false, true)) {
      return StoreUtil.saveAsync(getStateStore(), getLastUIAccess(), false, null).whenComplete((o, throwable) -> {
        mySaveSettingsIsInProgress.set(false);
      });
    }

    return CompletableFuture.completedFuture(null);
  }

  @RequiredWriteAction
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
      if (project.isInitialized()) {
        project.save();
      }
    }

    saveSettings();
  }

  @Override
  public CompletableFuture<?> saveAllAsync() {
    if (myDoNotSave) return CompletableFuture.completedFuture(null);

    return myLock.writeAsync(() -> FileDocumentManager.getInstance().saveAllDocuments())
                 .thenComposeAsync(o -> {
                   Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
                   List<CompletableFuture<?>> futures = new ArrayList<>(openProjects.length + 1);
                   for (Project openProject : openProjects) {
                     if (openProject.isDisposed()) {
                       // debug for https://github.com/consulo/consulo/issues/296
                       LOG.error("Project is disposed: " + openProject.getName() + ", isInitialized: " + openProject.isInitialized());
                       continue;
                     }

                     ProjectEx project = (ProjectEx)openProject;
                     if (project.isInitialized()) {
                       futures.add(project.saveAsync());
                     }
                   }
                   futures.add(saveSettingsAsync());
                   return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
                 }, myLock.writeExecutor());
  }

  @Override
  public void doNotSave(boolean value) {
    myDoNotSave = value;
  }

  @Override
  public boolean isDoNotSave() {
    return myDoNotSave;
  }

  @Override
  public boolean isLoaded() {
    return myLoaded;
  }

  @Nonnull
  @Override
  public Future<?> executeOnPooledThread(@Nonnull final Runnable action) {
    return PooledThreadExecutor.getInstance().submit(new RunnableAsCallable(action));
  }

  @Nonnull
  @Override
  public <T> Future<T> executeOnPooledThread(@Nonnull final Callable<T> action) {
    return PooledThreadExecutor.getInstance().submit(new Callable<T>() {
      @Override
      public T call() {
        try {
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
    ProjectManagerEx projectManager = (ProjectManagerEx)ProjectManager.getInstance();
    Project[] projects = projectManager.getOpenProjects();
    for (Project project : projects) {
      if (!projectManager.canClose(project)) {
        return false;
      }
    }

    return true;
  }

  @RequiredUIAccess
  @Override
  public void dispose() {
    Application.get().assertIsDispatchThread();

    fireApplicationExiting();

    ShutDownTracker.getInstance().ensureStopperThreadsFinished();

    ApplicationConcurrency concurrency = getInstance(ApplicationConcurrency.class);

    super.dispose();

    AppScheduledExecutorService service = (AppScheduledExecutorService)concurrency.getScheduledExecutorService();
    service.shutdownAppScheduledExecutorService();

    Disposer.dispose(myLastDisposable); // dispose it last
  }

  @Override
  protected void notifyAboutInitialization(float percentOfLoad, Object component) {
    StartupProgress progress = mySplashRef.get();
    if (progress != null) {
      float progress1 = 0.65f + percentOfLoad * 0.35f;
      progress.showProgress("", progress1);
    }
  }

  @Nullable
  @Override
  protected IApplicationStore getStateStore() {
    return (IApplicationStore)super.getStateStore();
  }

  @Override
  @Nonnull
  public IComponentStore getStateStoreImpl() {
    return getInstance(IApplicationStore.class);
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return ApplicationProperties.isInSandbox() ? AllIcons.Icon16_Sandbox : AllIcons.Icon16;
  }

  @Nonnull
  @Override
  public Image getBigIcon() {
    return ApplicationProperties.isInSandbox() ? PlatformIconGroup.consulobigsandbox() : PlatformIconGroup.consulobig();
  }

  @Nonnull
  @Override
  public SemVer getVersion() {
    return myVersionValue.get();
  }

  @Override
  public boolean tryRunReadAction(@Nonnull Runnable action) {
    return myLock.tryReadSync(action);
  }

  @RequiredUIAccess
  @Override
  public void executeSuspendingWriteAction(@Nullable ComponentManager project, @Nonnull String title, @Nonnull Runnable runnable) {
    throw new UnsupportedOperationException();
  }

  private static void runModalProgress(@Nullable ComponentManager project, @Nonnull String title, @Nonnull Runnable runnable) {
    new Task.Modal(project, title, false) {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        runnable.run();
      }
    }.queue();
  }

  @Override
  public boolean isInImpatientReader() {
    return false;
  }

  @Override
  public boolean isWriteActionPending() {
    return myWriteActionPending;
  }

  @Override
  public boolean isWriteActionInProgress() {
    return myLock.isWriteActionInProgress();
  }

  @Override
  public boolean holdsReadLock() {
    return myLock.isReadAccessAllowed();
  }

  @RequiredUIAccess
  @Override
  public void runWriteAction(@Nonnull final Runnable action) {
    if (isDispatchThread()) {
      LOG.error("Calling write sync action inside ui thread");
    }

    if (isWriteAccessAllowed()) {
      myLock.runWriteActionUnsafe(() -> {
        action.run();
        return null;
      });
      return;
    }

    try {
      getLock().writeAsync(() -> {
        action.run();
        return null;
      }).get();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @RequiredUIAccess
  @Override
  public <T, E extends Throwable> T runWriteAction(@Nonnull ThrowableSupplier<T, E> computation) throws E {
    if (isDispatchThread()) {
      LOG.error("Calling write sync action inside ui thread");
    }

    if (isWriteAccessAllowed()) {
      return myLock.runWriteActionUnsafe(computation);
    }

    try {
      return myLock.writeAsync(computation::get).get();
    }
    catch (Throwable e) {
      ExceptionUtil.rethrow(e);
      return null;
    }
  }

  @Override
  public String toString() {
    return "Application" +
      (isDisposed() ? " (Disposed)" : "") +
      (ApplicationProperties.isInSandbox() ? " (Sandbox)" : "") +
      (isHeadlessEnvironment() ? " (Headless)" : "") +
      (isCommandLine() ? " (Command line)" : "");
  }
}
