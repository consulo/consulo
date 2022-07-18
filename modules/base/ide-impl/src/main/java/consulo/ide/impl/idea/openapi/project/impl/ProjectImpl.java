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
package consulo.ide.impl.idea.openapi.project.impl;

import consulo.annotation.component.ComponentScope;
import consulo.application.AccessRule;
import consulo.application.Application;
import consulo.application.dumb.DumbAwareRunnable;
import consulo.application.impl.internal.PlatformComponentManagerImpl;
import consulo.application.internal.ApplicationEx;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.component.impl.internal.BaseComponentManager;
import consulo.component.internal.inject.InjectingContainerBuilder;
import consulo.component.store.impl.internal.StoreUtil;
import consulo.ide.impl.components.impl.stores.DefaultProjectStoreImpl;
import consulo.ide.impl.components.impl.stores.IProjectStore;
import consulo.ide.impl.components.impl.stores.ProjectStoreImpl;
import consulo.ide.impl.idea.ide.startup.StartupManagerEx;
import consulo.ide.impl.idea.openapi.components.impl.ProjectPathMacroManager;
import consulo.ide.impl.idea.util.TimedReference;
import consulo.logging.Logger;
import consulo.module.impl.internal.ModuleManagerImpl;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.internal.ProjectEx;
import consulo.project.internal.ProjectExListener;
import consulo.project.internal.ProjectManagerEx;
import consulo.project.startup.StartupManager;
import consulo.project.ui.wm.FrameTitleBuilder;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import consulo.util.io.FileUtil;
import consulo.util.lang.ExceptionUtil;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProjectImpl extends PlatformComponentManagerImpl implements ProjectEx {
  private static final Logger LOG = Logger.getInstance(ProjectImpl.class);

  public static final String NAME_FILE = ".name";

  private final ProjectManagerEx myManager;
  @Nonnull
  private final String myDirPath;

  private final AtomicBoolean mySavingInProgress = new AtomicBoolean(false);

  public boolean myOptimiseTestLoadSpeed;

  private String myName;

  public static Key<Long> CREATION_TIME = Key.create("ProjectImpl.CREATION_TIME");
  public static final Key<String> CREATION_TRACE = Key.create("ProjectImpl.CREATION_TRACE");

  protected ProjectImpl(@Nonnull Application application, @Nonnull ProjectManager manager, @Nonnull String dirPath, boolean isOptimiseTestLoadSpeed, String projectName, boolean noUIThread) {
    super(application, "Project " + (projectName == null ? dirPath : projectName), ComponentScope.PROJECT);
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

    myOptimiseTestLoadSpeed = isOptimiseTestLoadSpeed;

    myManager = (ProjectManagerEx)manager;

    myName = projectName;
  }

  @Override
  public int getProfiles() {
    return ((BaseComponentManager)myParent).getProfiles();
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
      StartupManager.getInstance(this).runWhenProjectIsInitialized(new DumbAwareRunnable() {
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
    builder.bind(ProjectPathMacroManager.class).to(ProjectPathMacroManager.class).forceSingleton();

    final Class<? extends IProjectStore> storeClass = isDefault() ? DefaultProjectStoreImpl.class : ProjectStoreImpl.class;
    builder.bind(IProjectStore.class).to(storeClass).forceSingleton();
  }

  @Nonnull
  @Override
  public IProjectStore getStateStore() {
    return getInstance(IProjectStore.class);
  }

  @Override
  protected void notifyAboutInitialization(float percentOfLoad, Object component) {
    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
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
    return isOpen() && StartupManagerEx.getInstanceEx(this).startupActivityPassed();
  }

  @Override
  public boolean isModulesReady() {
    ModuleManagerImpl moduleManager = ModuleManagerImpl.getInstanceImpl(this);
    return moduleManager.isReady();
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
  public boolean isOptimiseTestLoadSpeed() {
    return myOptimiseTestLoadSpeed;
  }

  @Override
  public void setOptimiseTestLoadSpeed(final boolean optimiseTestLoadSpeed) {
    myOptimiseTestLoadSpeed = optimiseTestLoadSpeed;
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
  public AsyncResult<Void> saveAsync(@Nonnull UIAccess uiAccess) {
    return AccessRule.writeAsync(() -> saveAsyncImpl(uiAccess));
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

    application.assertIsDispatchThread();

    assert application.isWriteAccessAllowed();  // dispose must be under write action

    // can call dispose only via consulo.ide.impl.idea.ide.impl.ProjectUtil.closeAndDispose()
    LOG.assertTrue(application.isUnitTestMode() || !myManager.isProjectOpened(this));

    LOG.assertTrue(!isDisposed());

    super.dispose();

    TimedReference.disposeTimed();
  }

  @Override
  public String toString() {
    return "Project" + (isDisposed() ? " (Disposed" + (temporarilyDisposed ? " temporarily" : "") + ")" : isDefault() ? "" : " '" + myDirPath + "'") + (isDefault() ? " (Default)" : "") + " " + myName;
  }
}
