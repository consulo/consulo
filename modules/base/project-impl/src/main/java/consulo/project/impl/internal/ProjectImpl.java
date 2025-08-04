/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.project.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.application.Application;
import consulo.application.dumb.DumbAwareRunnable;
import consulo.application.impl.internal.BaseApplication;
import consulo.application.impl.internal.PlatformComponentManagerImpl;
import consulo.application.internal.ApplicationEx;
import consulo.application.progress.ProgressIndicator;
import consulo.application.util.TimedReference;
import consulo.component.impl.internal.ComponentBinding;
import consulo.component.internal.inject.InjectingContainerBuilder;
import consulo.component.store.internal.IComponentStore;
import consulo.component.store.internal.StorableComponent;
import consulo.component.store.internal.StoreUtil;
import consulo.logging.Logger;
import consulo.module.ModuleManager;
import consulo.module.impl.internal.ModuleManagerImpl;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.impl.internal.store.IProjectStore;
import consulo.project.internal.ProjectEx;
import consulo.project.internal.ProjectExListener;
import consulo.project.internal.ProjectManagerEx;
import consulo.project.internal.StartupManagerEx;
import consulo.project.startup.StartupManager;
import consulo.project.ui.wm.FrameTitleBuilder;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import consulo.util.io.FileUtil;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.lazy.LazyValue;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class ProjectImpl extends PlatformComponentManagerImpl implements ProjectEx, StorableComponent {
  private static final Logger LOG = Logger.getInstance(ProjectImpl.class);

  public static final String NAME_FILE = ".name";

  private final ProjectManagerEx myManager;
  @Nonnull
  private final String myDirPath;

  private final AtomicBoolean mySavingInProgress = new AtomicBoolean(false);

  private String myName;

  public static Key<Long> CREATION_TIME = Key.create("ProjectImpl.CREATION_TIME");
  public static final Key<String> CREATION_TRACE = Key.create("ProjectImpl.CREATION_TRACE");

  private Supplier<StartupManager> myStartupManagerProvider = LazyValue.notNull(() -> getInstance(StartupManager.class));
  private Supplier<ModuleManager> myModuleManagerProvider = LazyValue.notNull(() -> getInstance(ModuleManager.class));

  public ProjectImpl(@Nonnull Application application,
                     @Nonnull ProjectManager manager,
                     @Nonnull String dirPath,
                     String projectName,
                     boolean noUIThread,
                     ComponentBinding componentBinding) {
    super(application, "Project " + (projectName == null ? dirPath : projectName), ComponentScope.PROJECT, componentBinding);
    myDirPath = dirPath;

    putUserData(CREATION_TIME, System.nanoTime());

    if (application.isUnitTestMode()) {
      putUserData(CREATION_TRACE, ExceptionUtil.currentStackTrace());
    }

    if (!isDefault()) {
      if (noUIThread) {
        getStateStore().setProjectFilePathNoUI(dirPath);
      }
      else {
        getStateStore().setProjectFilePath(dirPath);
      }
    }

    myManager = (ProjectManagerEx)manager;

    myName = projectName;
  }

  @Override
  public void executeNonCancelableSection(@Nonnull Runnable runnable) {
    PlatformComponentManagerImpl application = (BaseApplication)getApplication();
    application.executeNonCancelableSection(runnable);
  }

  @Override
  public int getProfiles() {
    return myParent.getProfiles() | (isDefault() ? DEFAULT_PROJECT : REGULAR_PROJECT);
  }

  @Nullable
  public String getCreationTrace() {
    return getUserData(CREATION_TRACE);
  }

  @Override
  @Nonnull
  public Application getApplication() {
    return (Application)myParent;
  }

  @Override
  public void setProjectName(@Nonnull String projectName) {
    String name = getName();

    if (!projectName.equals(name)) {
      myName = projectName;
      myStartupManagerProvider.get().runWhenProjectIsInitialized(new DumbAwareRunnable() {
        @Override
        public void run() {
          if (isDisposed()) return;

          JFrame frame = WindowManager.getInstance().getFrame(ProjectImpl.this);
          String title = FrameTitleBuilder.getInstance().getProjectTitle(ProjectImpl.this);
          if (frame != null && title != null) {
            frame.setTitle(title);
          }
        }
      });
    }
  }

  @Override
  protected void bootstrapInjectingContainer(@Nonnull InjectingContainerBuilder builder) {
    super.bootstrapInjectingContainer(builder);

    builder.bind(Project.class).to(this);
    builder.bind(ProjectEx.class).to(this);
  }

  @Nullable
  @Override
  public IProjectStore getStateStore() {
    return (IProjectStore)super.getStateStore();
  }

  @Nonnull
  @Override
  public IComponentStore getStateStoreImpl() {
    return getInstance(IProjectStore.class);
  }

  @Override
  protected void notifyAboutInitialization(float percentOfLoad, Object component) {
    ProgressIndicator indicator = getApplication().getProgressManager().getProgressIndicator();
    if (indicator != null) {
      indicator.setText2(component.getClass().getName());
    }
  }

  @Override
  public boolean isOpen() {
    return myManager.isProjectOpened(this);
  }

  @Override
  public boolean isInitialized() {
    if (isDisposed()) {
      return false;
    }
    return isOpen() && ((StartupManagerEx)myStartupManagerProvider.get()).startupActivityPassed();
  }

  @Override
  public boolean  isModulesReady() {
    return ((ModuleManagerImpl)myModuleManagerProvider.get()).isReady();
  }

  @Override
  @Nonnull
  public String getProjectFilePath() {
    return getStateStore().getProjectFilePath();
  }

  @Override
  public VirtualFile getProjectFile() {
    return getStateStore().getProjectFile();
  }

  @Override
  public VirtualFile getBaseDir() {
    return getStateStore().getProjectBaseDir();
  }

  @Override
  public String getBasePath() {
    return getStateStore().getProjectBasePath();
  }

  @Nonnull
  @Override
  public String getName() {
    if (myName == null) {
      myName = getStateStore().getProjectName();
    }
    return myName;
  }

  @Override
  public String getPresentableUrl() {
    return getStateStore().getPresentableUrl();
  }

  @Nonnull
  @Override
  public String getLocationHash() {
    String str = getPresentableUrl();
    if (str == null) str = getName();

    final String prefix = !isDefault() ? "" : getName();
    return prefix + Integer.toHexString(str.hashCode());
  }

  @Override
  @Nullable
  public VirtualFile getWorkspaceFile() {
    return getStateStore().getWorkspaceFile();
  }

  @Override
  public void initNotLazyServices() {
    long start = System.currentTimeMillis();
//    ProfilingUtil.startCPUProfiling();
    super.initNotLazyServices();
//    ProfilingUtil.captureCPUSnapshot();
    long time = System.currentTimeMillis() - start;
    LOG.info(getNotLazyServicesCount() + " project not-lazy servicers initialized in " + time + " ms");
  }

  @Override
  public void save() {
    ApplicationEx application = (ApplicationEx)getApplication();
    if (application.isDoNotSave()) {
      // no need to save
      return;
    }

    if (!isModulesReady()) {
      LOG.warn(new Exception("Calling Project#save() but modules not initialized"));
      return;
    }

    if (!mySavingInProgress.compareAndSet(false, true)) {
      return;
    }

    try {
      if (!isDefault()) {
        String projectBasePath = getStateStore().getProjectBasePath();
        if (projectBasePath != null) {
          File projectDir = new File(projectBasePath);
          File nameFile = new File(projectDir, DIRECTORY_STORE_FOLDER + "/" + NAME_FILE);
          if (!projectDir.getName().equals(getName())) {
            try {
              FileUtil.writeToFile(nameFile, getName());
            }
            catch (IOException e) {
              LOG.error("Unable to store project name", e);
            }
          }
          else {
            FileUtil.delete(nameFile);
          }
        }
      }

      StoreUtil.save(getStateStore(), false, this);
    }
    finally {
      mySavingInProgress.set(false);
      application.getMessageBus().syncPublisher(ProjectExListener.class).saved(this);
    }
  }

  @Nonnull
  @Override
  public CompletableFuture<Void> saveAsync(@Nonnull UIAccess uiAccess) {
    return CompletableFuture.runAsync(() -> saveAsyncImpl(uiAccess));
  }

  private void saveAsyncImpl(@Nonnull UIAccess uiAccess) {
    ApplicationEx application = (ApplicationEx)getApplication();

    if (application.isDoNotSave()) {
      // no need to save
      return;
    }

    if (!mySavingInProgress.compareAndSet(false, true)) {
      return;
    }

    //HeavyProcessLatch.INSTANCE.prioritizeUiActivity();

    try {
      if (!isDefault()) {
        String projectBasePath = getStateStore().getProjectBasePath();
        if (projectBasePath != null) {
          File projectDir = new File(projectBasePath);
          File nameFile = new File(projectDir, DIRECTORY_STORE_FOLDER + "/" + NAME_FILE);
          if (!projectDir.getName().equals(getName())) {
            try {
              FileUtil.writeToFile(nameFile, getName());
            }
            catch (IOException e) {
              LOG.error("Unable to store project name", e);
            }
          }
          else {
            FileUtil.delete(nameFile);
          }
        }
      }

      StoreUtil.saveAsync(getStateStore(), uiAccess, this);
    }
    finally {
      mySavingInProgress.set(false);
      application.getMessageBus().syncPublisher(ProjectExListener.class).saved(this);
    }
  }

  @RequiredUIAccess
  @Override
  public void dispose() {
    ApplicationEx application = (ApplicationEx)getApplication();

    UIAccess.assertIsUIThread();

    assert application.isWriteAccessAllowed();  // dispose must be under write action

    // can call dispose only via consulo.ide.impl.idea.ide.impl.ProjectUtil.closeAndDispose()
    LOG.assertTrue(application.isUnitTestMode() || !myManager.isProjectOpened(this));

    LOG.assertTrue(!isDisposed());

    super.dispose();

    TimedReference.disposeTimed();
  }

  @Nullable
  @Override
  public Window getWindow() {
    return WindowManager.getInstance().getWindow(this);
  }

  @Override
  public String toString() {
    return "Project" + (isDisposed() ? " (Disposed" + (temporarilyDisposed ? " temporarily" : "") + ")" : isDefault() ? "" : " '" + myDirPath + "'") + (isDefault() ? " (Default)" : "") + " " + myName;
  }
}
