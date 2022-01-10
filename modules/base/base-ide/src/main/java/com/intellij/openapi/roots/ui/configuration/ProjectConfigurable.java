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

package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.project.ex.ProjectEx;
import com.intellij.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureElement;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import consulo.compiler.CompilerConfiguration;
import consulo.disposer.Disposable;
import consulo.ide.settings.impl.ProjectStructureSettingsUtil;
import consulo.ide.ui.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.ProjectLocalize;
import consulo.preferences.internal.ConfigurableWeight;
import consulo.roots.ui.configuration.ModulesConfigurator;
import consulo.roots.ui.configuration.ProjectStructureElementConfigurable;
import consulo.ui.Component;
import consulo.ui.HtmlLabel;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * @author Eugene Zhuravlev
 * Date: Dec 15, 2003
 */
public class ProjectConfigurable extends ProjectStructureElementConfigurable<Project> implements Configurable, ConfigurableWeight {
  public static final String ID = "project";

  private final Project myProject;

  private TextBox myProjectName;

  private VerticalLayout myLayout;

  private boolean myFreeze = false;

  private final GeneralProjectSettingsElement mySettingsElement;

  private FileChooserTextBoxBuilder.Controller myCompilerPathController;

  public ProjectConfigurable(Project project) {
    myProject = project;
    mySettingsElement = new GeneralProjectSettingsElement();
    // todo final ProjectStructureDaemonAnalyzer daemonAnalyzer = context.getDaemonAnalyzer();
    // todo myModulesConfigurator.addAllModuleChangeListener(moduleRootModel -> daemonAnalyzer.queueUpdate(mySettingsElement));
  }

  @Override
  public ProjectStructureElement getProjectStructureElement() {
    return mySettingsElement;
  }

  @Nonnull
  @RequiredUIAccess
  @Override
  public Component createOptionsPanel(@Nonnull Disposable parentUIDisposable) {
    init(parentUIDisposable);
    return myLayout;
  }

  @RequiredUIAccess
  private void init(Disposable parentUIDisposable) {
    myLayout = VerticalLayout.create();

    myLayout.add(HtmlLabel.create(LocalizeValue.localizeTODO("<html><body><b>Project name:</b></body></html>")));

    myProjectName = TextBox.create().withVisibleLength(40);

    myLayout.add(DockLayout.create().left(myProjectName));

    FileChooserTextBoxBuilder builder = FileChooserTextBoxBuilder.create(myProject);
    builder.uiDisposable(parentUIDisposable);
    builder.fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFolderDescriptor());

    myCompilerPathController = builder.build();
    myCompilerPathController.getComponent().addValueListener(event -> {
      if (myFreeze) return;
      getModulesConfigurator().processModuleCompilerOutputChanged(getCompilerOutputUrl());
    });

    myLayout.add(HtmlLabel.create(ProjectLocalize.projectCompilerOutput()));
    myLayout.add(myCompilerPathController);
  }

  private ModulesConfigurator getModulesConfigurator() {
    ProjectStructureSettingsUtil util = (ProjectStructureSettingsUtil)ShowSettingsUtil.getInstance();
    return util.getModulesModel(myProject);
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    myLayout = null;
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    myFreeze = true;
    try {

      final String compilerOutput = getModulesConfigurator().getCompilerOutputUrl();
      if (compilerOutput != null) {
        myCompilerPathController.setValue(FileUtil.toSystemDependentName(VfsUtil.urlToPath(compilerOutput)));
      }
      if (myProjectName != null) {
        myProjectName.setValue(myProject.getName(), false);
      }
    }
    finally {
      myFreeze = false;
    }

    // todo myContext.getDaemonAnalyzer().queueUpdate(mySettingsElement);
  }


  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    final CompilerConfiguration configuration = CompilerConfiguration.getInstance(myProject);

    if (myProjectName != null && StringUtil.isEmptyOrSpaces(myProjectName.getValue())) {
      throw new ConfigurationException("Please, specify project name!");
    }

    getModulesConfigurator().setCompilerOutputUrl(getCompilerOutputUrl());

    ApplicationManager.getApplication().runWriteAction(() -> {
      // set the output path first so that handlers of RootsChanged event sent after JDK is set
      // would see the updated path
      String canonicalPath = myCompilerPathController.getValue();
      if (canonicalPath != null && canonicalPath.length() > 0) {
        try {
          canonicalPath = FileUtil.resolveShortWindowsName(canonicalPath);
        }
        catch (IOException e) {
          //file doesn't exist yet
        }
        canonicalPath = FileUtil.toSystemIndependentName(canonicalPath);
        configuration.setCompilerOutputUrl(VfsUtil.pathToUrl(canonicalPath));
      }
      else {
        configuration.setCompilerOutputUrl(null);
      }
      if (myProjectName != null) {
        ((ProjectEx)myProject).setProjectName(consulo.util.lang.StringUtil.notNullize(myProjectName.getValue()));
      }
    });
  }


  @Override
  public void setDisplayName(final String name) {
    //do nothing
  }

  @Override
  public Project getEditableObject() {
    return myProject;
  }

  @Override
  public String getDisplayName() {
    return ProjectBundle.message("project.roots.project.display.name");
  }

  @RequiredUIAccess
  @Override
  @SuppressWarnings({"SimplifiableIfStatement"})
  public boolean isModified() {
    final String compilerOutput = CompilerConfiguration.getInstance(myProject).getCompilerOutputUrl();
    if (!Comparing.strEqual(FileUtil.toSystemIndependentName(VfsUtil.urlToPath(compilerOutput)), FileUtil.toSystemIndependentName(myCompilerPathController.getValue()))) {
      return true;
    }
    if (myProjectName != null) {
      if (!myProjectName.getValueOrError().trim().equals(myProject.getName())) return true;
    }

    return false;
  }

  public String getCompilerOutputUrl() {
    return VfsUtil.pathToUrl(myCompilerPathController.getValue().trim());
  }

  @Override
  public int getConfigurableWeight() {
    return Integer.MAX_VALUE - 1;
  }
}
