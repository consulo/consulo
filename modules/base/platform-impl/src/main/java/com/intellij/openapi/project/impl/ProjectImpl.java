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
package com.intellij.openapi.project.impl;

import com.intellij.ide.startup.StartupManagerEx;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.ServiceDescriptor;
import com.intellij.openapi.components.impl.ProjectPathMacroManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.impl.ExtensionAreaId;
import com.intellij.openapi.module.impl.ModuleManagerImpl;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAwareRunnable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerAdapter;
import com.intellij.openapi.project.ex.ProjectEx;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.FrameTitleBuilder;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.util.TimedReference;
import consulo.application.AccessRule;
import consulo.components.impl.PlatformComponentManagerImpl;
import consulo.components.impl.stores.DefaultProjectStoreImpl;
import consulo.components.impl.stores.IProjectStore;
import consulo.components.impl.stores.ProjectStoreImpl;
import consulo.components.impl.stores.StoreUtil;
import consulo.container.plugin.ComponentConfig;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginListenerDescriptor;
import consulo.injecting.InjectingContainerBuilder;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProjectImpl extends PlatformComponentManagerImpl implements ProjectEx {
  private static final Logger LOG = Logger.getInstance(ProjectImpl.class);
  private static final ExtensionPointName<ServiceDescriptor> PROJECT_SERVICES = ExtensionPointName.create("com.intellij.projectService");

  public static final String NAME_FILE = ".name";

  private final ProjectManagerEx myManager;
  @Nonnull
  private final String myDirPath;

  private MyProjectManagerListener myProjectManagerListener;

  private final AtomicBoolean mySavingInProgress = new AtomicBoolean(false);

  public boolean myOptimiseTestLoadSpeed;

  private String myName;

  public static Key<Long> CREATION_TIME = Key.create("ProjectImpl.CREATION_TIME");
  public static final Key<String> CREATION_TRACE = Key.create("ProjectImpl.CREATION_TRACE");

  private final List<ProjectComponent> myProjectComponents = new CopyOnWriteArrayList<>();

  protected ProjectImpl(@Nonnull Application application, @Nonnull ProjectManager manager, @Nonnull String dirPath, boolean isOptimiseTestLoadSpeed, String projectName, boolean noUIThread) {
    super(application, "Project " + (projectName == null ? dirPath : projectName), ExtensionAreaId.PROJECT);
    myDirPath = dirPath;

    putUserData(CREATION_TIME, System.nanoTime());

    if (application.isUnitTestMode()) {
      putUserData(CREATION_TRACE, DebugUtil.currentStackTrace());
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

  @Nullable
  public String getCreationTrace() {
    return getUserData(CREATION_TRACE);
  }

  @Override
  @Nonnull
  public Application getApplication() {
    return (Application)myParent;
  }

  @Nullable
  @Override
  protected ExtensionPointName<ServiceDescriptor> getServiceExtensionPointName() {
    return PROJECT_SERVICES;
  }

  @Nonnull
  @Override
  protected List<ComponentConfig> getComponentConfigs(PluginDescriptor ideaPluginDescriptor) {
    return ideaPluginDescriptor.getProjectComponents();
  }

  @Nonnull
  @Override
  protected List<PluginListenerDescriptor> getPluginListenerDescriptors(PluginDescriptor pluginDescriptor) {
    return pluginDescriptor.getProjectListeners();
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
    return getComponent(IProjectStore.class);
  }

  @Override
  protected void notifyAboutInitialization(float percentOfLoad, Object component) {
    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (indicator != null) {
      indicator.setText2(getComponentName(component));
    }

    if (component instanceof ProjectComponent) {
      myProjectComponents.add((ProjectComponent)component);
    }
  }

  @Override
  public boolean isOpen() {
    return myManager.isProjectOpened(this);
  }

  @Override
  public boolean isInitialized() {
    if(isDisposed()) {
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
    if(myName == null) {
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
  public void initNotLazyServices(ProgressIndicator progressIndicator) {
    long start = System.currentTimeMillis();
//    ProfilingUtil.startCPUProfiling();
    super.initNotLazyServices(progressIndicator);
//    ProfilingUtil.captureCPUSnapshot();
    long time = System.currentTimeMillis() - start;
    LOG.info(getNotLazyServicesCount() + " project components initialized in " + time + " ms");

    myProjectManagerListener = new MyProjectManagerListener();
    myManager.addProjectManagerListener(this, myProjectManagerListener);
  }

  @Override
  public void save() {
    ApplicationEx application = (ApplicationEx)getApplication();
    if (application.isDoNotSave()) {
      // no need to save
      return;
    }

    if(!isModulesReady()) {
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
      application.getMessageBus().syncPublisher(ProjectSaved.TOPIC).saved(this);
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
      application.getMessageBus().syncPublisher(ProjectSaved.TOPIC).saved(this);
    }
  }

  @RequiredUIAccess
  @Override
  public void dispose() {
    ApplicationEx application = (ApplicationEx)getApplication();

    assert application.isWriteAccessAllowed();  // dispose must be under write action

    // can call dispose only via com.intellij.ide.impl.ProjectUtil.closeAndDispose()
    LOG.assertTrue(application.isUnitTestMode() || !myManager.isProjectOpened(this));

    LOG.assertTrue(!isDisposed());
    if (myProjectManagerListener != null) {
      myManager.removeProjectManagerListener(this, myProjectManagerListener);
    }

    myProjectManagerListener = null;

    super.dispose();

    TimedReference.disposeTimed();
  }

  private void projectOpened() {
    for (ProjectComponent component : myProjectComponents) {
      try {
        component.projectOpened();
      }
      catch (Throwable e) {
        LOG.error(component.toString(), e);
      }
    }
  }

  private void projectClosed() {
    List<ProjectComponent> components = new ArrayList<>(myProjectComponents);
    Collections.reverse(components);
    for (ProjectComponent component : components) {
      try {
        component.projectClosed();
      }
      catch (Throwable e) {
        LOG.error(e);
      }
    }
  }

  private class MyProjectManagerListener extends ProjectManagerAdapter {
    @Override
    public void projectOpened(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
      LOG.assertTrue(project == ProjectImpl.this);
      ProjectImpl.this.projectOpened();
    }

    @Override
    public void projectClosed(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
      LOG.assertTrue(project == ProjectImpl.this);
      ProjectImpl.this.projectClosed();
    }
  }

  @Override
  public String toString() {
    return "Project" +
           (isDisposed() ? " (Disposed" + (temporarilyDisposed ? " temporarily" : "") + ")" : isDefault() ? "" : " '" + myDirPath + "'") +
           (isDefault() ? " (Default)" : "") +
           " " + myName;
  }
}
