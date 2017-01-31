/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.ide.util.projectWizard;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.RecentProjectsManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.projectImport.ProjectImportBuilder;
import com.intellij.util.SystemProperties;
import com.intellij.util.containers.ContainerUtil;
import consulo.moduleImport.LegacyModuleImportProvider;
import consulo.moduleImport.ModuleImportContext;
import consulo.moduleImport.ModuleImportProvider;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class WizardContext extends UserDataHolderBase implements Disposable {
  /**
   * a project where the module should be added, can be null => the wizard creates a new project
   */
  @Nullable
  private final Project myProject;
  private String myProjectFileDirectory;
  private String myProjectName;
  private String myCompilerOutputDirectory;

  private ModuleImportProvider<?> myImportProvider;
  private final List<Listener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

  private final Map<ModuleImportProvider, ModuleImportContext> myModuleImportContexts = new THashMap<>();

  public interface Listener {
    void buttonsUpdateRequested();

    void nextStepRequested();
  }

  public WizardContext(@Nullable Project project) {
    myProject = project;
  }

  public void initModuleImportContext(@NotNull ModuleImportProvider<?> provider) {
    ModuleImportContext context = provider.createContext();

    Disposer.register(this, context);

    if (myModuleImportContexts.put(provider, context) != null) {
      throw new IllegalArgumentException();
    }
  }

  @NotNull
  public ModuleImportContext getModuleImportContext(@NotNull ModuleImportProvider<?> provider) {
    return Objects.requireNonNull(myModuleImportContexts.get(provider));
  }

  @Nullable
  public Project getProject() {
    return myProject;
  }

  @NotNull
  public String getProjectFileDirectory() {
    if (myProjectFileDirectory != null) {
      return myProjectFileDirectory;
    }
    final String lastProjectLocation = RecentProjectsManager.getInstance().getLastProjectCreationLocation();
    if (lastProjectLocation != null) {
      return lastProjectLocation.replace('/', File.separatorChar);
    }
    final String userHome = SystemProperties.getUserHome();
    //noinspection HardCodedStringLiteral
    String productName = ApplicationNamesInfo.getInstance().getLowercaseProductName();
    return userHome.replace('/', File.separatorChar) + File.separator + productName.replace(" ", "") + "Projects";
  }

  public boolean isProjectFileDirectorySet() {
    return myProjectFileDirectory != null;
  }

  public void setProjectFileDirectory(String projectFileDirectory) {
    myProjectFileDirectory = projectFileDirectory;
  }

  public String getCompilerOutputDirectory() {
    return myCompilerOutputDirectory;
  }

  public void setCompilerOutputDirectory(final String compilerOutputDirectory) {
    myCompilerOutputDirectory = compilerOutputDirectory;
  }

  public String getProjectName() {
    return myProjectName;
  }

  public void setProjectName(String projectName) {
    myProjectName = projectName;
  }

  public boolean isCreatingNewProject() {
    return myProject == null;
  }

  public Icon getStepIcon() {
    return null;
  }

  public void requestWizardButtonsUpdate() {
    for (Listener listener : myListeners) {
      listener.buttonsUpdateRequested();
    }
  }

  public void requestNextStep() {
    for (Listener listener : myListeners) {
      listener.nextStepRequested();
    }
  }

  public void addContextListener(Listener listener) {
    myListeners.add(listener);
  }

  public void removeContextListener(Listener listener) {
    myListeners.remove(listener);
  }

  @Nullable
  @Deprecated
  public ProjectBuilder getProjectBuilder() {
    if (myImportProvider == null) {
      return null;
    }
    if (myImportProvider instanceof LegacyModuleImportProvider) {
      return ((LegacyModuleImportProvider)myImportProvider).getProvider().getBuilder();
    }
    throw new IllegalArgumentException();
  }

  @Nullable
  public ModuleImportProvider<?> getImportProvider() {
    return myImportProvider;
  }

  public void setImportProvider(@Nullable final ModuleImportProvider<?> projectBuilder) {
    myImportProvider = projectBuilder;
  }

  @Deprecated
  public void setProjectBuilder(@Nullable final ProjectImportBuilder projectBuilder) {
    myImportProvider = new LegacyModuleImportProvider(projectBuilder.getProvider());
  }

  public String getPresentationName() {
    return myProject == null ? IdeBundle.message("project.new.wizard.project.identification") : IdeBundle.message("project.new.wizard.module.identification");
  }

  @Override
  public void dispose() {
    myModuleImportContexts.clear();
  }
}
