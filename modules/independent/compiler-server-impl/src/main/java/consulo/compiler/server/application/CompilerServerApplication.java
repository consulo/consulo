/*
 * Copyright 2013-2016 consulo.io
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
package consulo.compiler.server.application;

import com.intellij.core.CoreFileTypeRegistry;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.*;
import com.intellij.openapi.application.ex.ApplicationUtil;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.components.StateStorageException;
import com.intellij.openapi.components.impl.ApplicationPathMacroManager;
import com.intellij.openapi.components.impl.ComponentManagerImpl;
import com.intellij.openapi.components.impl.stores.ApplicationStoreImpl;
import com.intellij.openapi.components.impl.stores.IApplicationStore;
import com.intellij.openapi.components.impl.stores.IComponentStore;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.project.impl.ProjectManagerImpl;
import com.intellij.openapi.util.*;
import com.intellij.util.io.storage.HeavyProcessLatch;
import consulo.annotations.RequiredDispatchThread;
import consulo.annotations.RequiredReadAction;
import consulo.annotations.RequiredWriteAction;
import consulo.application.ex.ApplicationEx2;
import consulo.ui.UIAccess;
import org.picocontainer.MutablePicoContainer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 11:26/12.08.13
 */
public class CompilerServerApplication extends ComponentManagerImpl implements ApplicationEx2 {
  public static final Logger LOGGER = Logger.getInstance(CompilerServerApplication.class);

  private static class ExecutorServiceHolder {
    private static final ExecutorService ourThreadExecutorsService = createServiceImpl();

    private static ThreadPoolExecutor createServiceImpl() {
      return new ThreadPoolExecutor(10, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new ThreadFactory() {
        @Nonnull
        @Override
        @SuppressWarnings({"HardCodedStringLiteral"})
        public Thread newThread(@Nonnull Runnable r) {
          return new Thread(r, "CompilerServerApplication pooled thread");
        }
      });
    }
  }

  public static CompilerServerApplication createApplication() {
    final CompilerServerApplication app = new CompilerServerApplication();
    ApplicationManager.setApplication(app, new Getter<FileTypeRegistry>() {
                                        @Override
                                        public FileTypeRegistry get() {
                                          return new CoreFileTypeRegistry();
                                        }
                                      }, app
    );
    return app;
  }

  private boolean myDisposeInProgress;

  public CompilerServerApplication() {
    super(null);

    ApplicationManager.setApplication(this, Disposer.newDisposable());

    getPicoContainer().registerComponentInstance(Application.class, this);

    loadApplicationComponents();

    registerShutdownHook();
  }

  private void loadApplicationComponents() {
    PluginManagerCore.initPlugins((message, progress) -> {
    });

    final IdeaPluginDescriptor[] plugins = PluginManagerCore.getPlugins();
    for (IdeaPluginDescriptor plugin : plugins) {
      if (PluginManagerCore.shouldSkipPlugin(plugin)) continue;
      loadComponentsConfiguration(plugin.getAppComponents(), plugin, false);
    }
  }

  @Override
  protected void bootstrapPicoContainer(@Nonnull String name) {
    super.bootstrapPicoContainer(name);
    getPicoContainer().registerComponentImplementation(IComponentStore.class, ApplicationStoreImpl.class);
    getPicoContainer().registerComponentImplementation(ApplicationPathMacroManager.class);
  }

  @Override
  public void initializeFromStateStore(Object component, boolean service) {
    getStateStore().initComponent(component);
  }

  @Nonnull
  @Override
  public IApplicationStore getStateStore() {
    return (IApplicationStore)getPicoContainer().getComponentInstance(IComponentStore.class);
  }

  @Override
  public void executeSuspendingWriteAction(@Nullable Project project, @Nonnull String title, @Nonnull Runnable runnable) {
    runWriteAction(runnable);
  }

  @Override
  public void executeByImpatientReader(@Nonnull Runnable runnable) throws ApplicationUtil.CannotRunReadActionException {

  }

  @Override
  public boolean runWriteActionWithProgressInDispatchThread(@Nonnull String title,
                                                            @Nullable Project project,
                                                            @Nullable JComponent parentComponent,
                                                            @Nullable String cancelText,
                                                            @Nonnull Consumer<ProgressIndicator> action) {
    return true;
  }

  @Nonnull
  @Override
  protected MutablePicoContainer createPicoContainer() {
    return Extensions.getRootArea().getPicoContainer();
  }

  @Override
  public synchronized void dispose() {
    ShutDownTracker.getInstance().ensureStopperThreadsFinished();

    disposeComponents();

    ExecutorServiceHolder.ourThreadExecutorsService.shutdownNow();

    super.dispose();
  }

  private void registerShutdownHook() {
    ShutDownTracker.getInstance(); // Necessary to avoid creating an instance while already shutting down.

    ShutDownTracker.getInstance().registerShutdownTask(new Runnable() {
      @Override
      public void run() {
        if (isDisposed() || isDisposeInProgress()) {
          return;
        }
        ShutDownTracker.invokeAndWait(isUnitTestMode(), true, new Runnable() {
          @Override
          public void run() {
            if (ApplicationManager.getApplication() != CompilerServerApplication.this) return;
              myDisposeInProgress = true;
              if (!disposeSelf(true)) {
                myDisposeInProgress = false;
              }
          }
        });
      }
    });
  }

  private boolean disposeSelf(final boolean checkCanCloseProject) {
    final CommandProcessor commandProcessor = CommandProcessor.getInstance();
    final boolean[] canClose = {true};
    for (final Project project : ProjectManagerEx.getInstanceEx().getOpenProjects()) {
      try {
        commandProcessor.executeCommand(project, new Runnable() {
          @Override
          public void run() {
            final ProjectManagerImpl manager = (ProjectManagerImpl)ProjectManagerEx.getInstanceEx();
            if (!manager.closeProject(project, true, true, checkCanCloseProject)) {
              canClose[0] = false;
            }
          }
        }, ApplicationBundle.message("command.exit"), null);
      }
      catch (Throwable e) {
        CompilerServerApplication.LOGGER.error(e);
      }
      if (!canClose[0]) {
        return false;
      }
    }
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        Disposer.dispose(CompilerServerApplication.this);
      }
    });

    Disposer.assertIsEmpty();
    return true;
  }

  @Override
  public boolean isInternal() {
    return false;
  }

  @Override
  public void runReadAction(@Nonnull Runnable action) {
    try {
      action.run();
    }
    catch (Exception e) {
      CompilerServerApplication.LOGGER.error(e);
    }
  }

  @Override
  public <T> T runReadAction(@Nonnull Computable<T> computation) {
    try {
      return computation.compute();
    }
    catch (Exception e) {
      CompilerServerApplication.LOGGER.error(e);
      return null;
    }
  }

  @Override
  public <T, E extends Throwable> T runReadAction(@Nonnull ThrowableComputable<T, E> computation) throws E {
    return computation.compute();
  }

  @RequiredDispatchThread
  @Override
  public void runWriteAction(@Nonnull Runnable action) {
    try {
      action.run();
    }
    catch (Exception e) {
      CompilerServerApplication.LOGGER.error(e);
    }
  }

  @RequiredDispatchThread
  @Override
  public <T> T runWriteAction(@Nonnull Computable<T> computation) {
    try {
      return computation.compute();
    }
    catch (Exception e) {
      CompilerServerApplication.LOGGER.error(e);
      return null;
    }
  }

  @RequiredDispatchThread
  @Override
  public <T, E extends Throwable> T runWriteAction(@Nonnull ThrowableComputable<T, E> computation) throws E {
    return computation.compute();
  }

  @RequiredDispatchThread
  @Override
  public boolean hasWriteAction(@javax.annotation.Nullable Class<?> actionClass) {
    return true;
  }

  @RequiredReadAction
  @Override
  public void assertReadAccessAllowed() {
  }

  @RequiredWriteAction
  @Override
  public void assertWriteAccessAllowed() {
  }

  @RequiredDispatchThread
  @Override
  public void assertIsDispatchThread() {
  }

  @Override
  public void addApplicationListener(@Nonnull ApplicationListener listener) {
  }

  @Override
  public void addApplicationListener(@Nonnull ApplicationListener listener, @Nonnull Disposable parent) {
  }

  @Override
  public void removeApplicationListener(@Nonnull ApplicationListener listener) {
  }

  @RequiredDispatchThread
  @Override
  public void saveAll() {
  }

  @Override
  public void saveSettings() {
  }

  @Override
  public void exit() {
  }

  @Override
  public boolean isWriteAccessAllowed() {
    return true;
  }

  @Override
  public boolean isReadAccessAllowed() {
    return true;
  }

  @Override
  public boolean isDispatchThread() {
    return true;
  }

  @Override
  public boolean isWriteThread() {
    return true;
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable) {
    try {
      runnable.run();
    }
    catch (Exception e) {
      CompilerServerApplication.LOGGER.error(e);
    }
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull Condition expired) {
    try {
      runnable.run();
    }
    catch (Exception e) {
      CompilerServerApplication.LOGGER.error(e);
    }
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState state) {
    try {
      runnable.run();
    }
    catch (Exception e) {
      CompilerServerApplication.LOGGER.error(e);
    }
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState state, @Nonnull Condition expired) {
    try {
      runnable.run();
    }
    catch (Exception e) {
      CompilerServerApplication.LOGGER.error(e);
    }
  }

  @Override
  public void invokeAndWait(@Nonnull Runnable runnable, @Nonnull ModalityState modalityState) {
    try {
      runnable.run();
    }
    catch (Exception e) {
      CompilerServerApplication.LOGGER.error(e);
    }
  }

  @Nonnull
  @Override
  public ModalityState getCurrentModalityState() {
    return ModalityState.NON_MODAL;
  }

  @Nonnull
  @Override
  public ModalityState getModalityStateForComponent(@Nonnull Component c) {
    return ModalityState.NON_MODAL;
  }

  @Nonnull
  @Override
  public ModalityState getDefaultModalityState() {
    return ModalityState.NON_MODAL;
  }

  @Nonnull
  @Override
  public ModalityState getNoneModalityState() {
    return ModalityState.NON_MODAL;
  }

  @Nonnull
  @Override
  public ModalityState getAnyModalityState() {
    return ModalityState.NON_MODAL;
  }

  @Override
  public long getStartTime() {
    return 0;
  }

  @RequiredDispatchThread
  @Override
  public long getIdleTime() {
    return 0;
  }

  @Override
  public boolean isHeadlessEnvironment() {
    return true;
  }

  @Override
  public boolean isCompilerServerMode() {
    return true;
  }

  @Override
  public boolean isCommandLine() {
    return false;
  }

  @Nonnull
  @Override
  public Future<?> executeOnPooledThread(@Nonnull Runnable action) {
    return ExecutorServiceHolder.ourThreadExecutorsService.submit(action);
  }

  @Nonnull
  @Override
  public <T> Future<T> executeOnPooledThread(@Nonnull Callable<T> action) {
    return ExecutorServiceHolder.ourThreadExecutorsService.submit(action);
  }

  @Override
  public boolean isDisposeInProgress() {
    return myDisposeInProgress || ShutDownTracker.isShutdownHookRunning();
  }

  @Override
  public boolean isRestartCapable() {
    return false;
  }

  @Override
  public void restart() {
  }

  @Override
  public boolean isActive() {
    return true;
  }

  @Nonnull
  @Override
  public consulo.ui.image.Image getIcon() {
    return null;
  }

  @Nonnull
  @Override
  public AccessToken acquireReadActionLock() {
    return AccessToken.EMPTY_ACCESS_TOKEN;
  }

  @RequiredDispatchThread
  @Nonnull
  @Override
  public AccessToken acquireWriteActionLock(@Nonnull Class marker) {
    return AccessToken.EMPTY_ACCESS_TOKEN;
  }

  @Override
  public void load(String path) throws IOException {
    getStateStore().setOptionsPath(path);
    getStateStore().setConfigPath(PathManager.getConfigPath());

    AccessToken accessToken = HeavyProcessLatch.INSTANCE.processStarted("app store load");
    try {
      getStateStore().load();
    }
    catch (StateStorageException e) {
      throw new IOException(e.getMessage());
    }
    finally {
      accessToken.finish();
    }
  }


  @Override
  public boolean isLoaded() {
    return true;
  }

  @Override
  public boolean holdsReadLock() {
    return false;
  }

  @Override
  public boolean isWriteActionInProgress() {
    return false;
  }

  @Override
  public boolean isWriteActionPending() {
    return false;
  }

  @Override
  public void doNotSave() {
  }

  @Override
  public void doNotSave(boolean value) {
  }

  @Override
  public boolean isDoNotSave() {
    return true;
  }

  @Override
  public void exit(boolean force, boolean exitConfirmed) {

  }

  @Override
  public void restart(boolean force) {
  }

  @RequiredDispatchThread
  @Override
  public boolean runProcessWithProgressSynchronously(@Nonnull Runnable process,
                                                     @Nonnull String progressTitle,
                                                     boolean canBeCanceled,
                                                     Project project) {
    process.run();
    return true;
  }

  @RequiredDispatchThread
  @Override
  public boolean runProcessWithProgressSynchronously(@Nonnull Runnable process,
                                                     @Nonnull String progressTitle,
                                                     boolean canBeCanceled,
                                                     @javax.annotation.Nullable Project project,
                                                     JComponent parentComponent) {
    process.run();
    return true;
  }

  @RequiredDispatchThread
  @Override
  public boolean runProcessWithProgressSynchronously(@Nonnull Runnable process,
                                                     @Nonnull String progressTitle,
                                                     boolean canBeCanceled,
                                                     @Nullable Project project,
                                                     JComponent parentComponent,
                                                     String cancelText) {
    process.run();
    return true;
  }

  @RequiredDispatchThread
  @Override
  public void assertIsDispatchThread(@javax.annotation.Nullable JComponent component) {
  }

  @Override
  public void assertTimeConsuming() {
  }

  @Override
  public boolean tryRunReadAction(@Nonnull Runnable action) {
    action.run();
    return true;
  }

  @Nonnull
  @Override
  public UIAccess getLastUIAccess() {
    return UIAccess.get();
  }

  @Nonnull
  @Override
  public <T> T[] getExtensions(final ExtensionPointName<T> extensionPointName) {
    return Extensions.getRootArea().getExtensionPoint(extensionPointName).getExtensions();
  }
}
