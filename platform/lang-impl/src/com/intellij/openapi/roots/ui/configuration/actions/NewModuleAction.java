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
package com.intellij.openapi.roots.ui.configuration.actions;

import com.intellij.ide.util.newProjectWizard.AddModuleWizard;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ProjectBuilder;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ui.configuration.DefaultModulesProvider;
import com.intellij.openapi.roots.ui.configuration.ModulesConfigurator;
import com.intellij.openapi.vfs.VirtualFile;
import org.consulo.ide.eap.EarlyAccessProgramManager;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.ide.impl.ui.CreateProjectOrModuleDialog;

import java.util.List;

/**
 * @author Eugene Zhuravlev
 *         Date: Jan 5, 2004
 */
public class NewModuleAction extends AnAction implements DumbAware {
  public NewModuleAction() {
    super(ProjectBundle.message("module.new.action"), ProjectBundle.message("module.new.action.description"), null);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Project project = getEventProject(e);
    if (project == null) {
      return;
    }
    Object dataFromContext = prepareDataFromContext(e);

    final VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);

    boolean eapState = EarlyAccessProgramManager.getInstance().getState(CreateProjectOrModuleDialog.EapDescriptor.class);
    Module moduleBySimpleWay = eapState ? CreateProjectOrModuleDialog.showAndCreate(project) : createModuleBySimpleWay(project, virtualFile);
    if (moduleBySimpleWay != null) {
      processCreatedModule(moduleBySimpleWay, dataFromContext);
    }
  }

  public static Module createModuleBySimpleWay(Project project, VirtualFile virtualFile) {
    final ModuleManager moduleManager = ModuleManager.getInstance(project);
    FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(false, true, false, false, false, false) {
      @Override
      public boolean isFileSelectable(VirtualFile file) {
        if (!super.isFileSelectable(file)) {
          return false;
        }
        for (Module module : moduleManager.getModules()) {
          VirtualFile moduleDir = module.getModuleDir();
          if (moduleDir != null && moduleDir.equals(file)) {
            return false;
          }
        }
        return true;
      }
    };
    fileChooserDescriptor.setTitle(ProjectBundle.message("choose.module.home"));

    VirtualFile moduleDir = FileChooser.chooseFile(fileChooserDescriptor, project, virtualFile != null && virtualFile.isDirectory() ? virtualFile : null);

    if (moduleDir == null) {
      return null;
    }

    final ModifiableModuleModel modifiableModel = moduleManager.getModifiableModel();

    Module newModule = modifiableModel.newModule(moduleDir.getNameWithoutExtension(), moduleDir.getPath());

    ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(newModule);

    final ModifiableRootModel moduleRootManagerModifiableModel = moduleRootManager.getModifiableModel();
    moduleRootManagerModifiableModel.addContentEntry(moduleDir);
    new WriteAction<Object>() {
      @Override
      protected void run(Result<Object> result) throws Throwable {
        moduleRootManagerModifiableModel.commit();

        modifiableModel.commit();
      }
    }.execute();
    return newModule;
  }

  @Nullable
  public Module createModuleFromWizard(Project project, @Nullable Object dataFromContext, AddModuleWizard wizard) {
    final ProjectBuilder builder = wizard.getProjectBuilder();
    if (builder instanceof ModuleBuilder) {
      final ModuleBuilder moduleBuilder = (ModuleBuilder)builder;
      if (moduleBuilder.getName() == null) {
        moduleBuilder.setName(wizard.getProjectName());
      }
      if (moduleBuilder.getModuleDirPath() == null) {
        moduleBuilder.setModuleDirPath(wizard.getModuleFilePath());
      }
    }
    if (!builder.validate(project, project)) {
      return null;
    }
    Module module;
    if (builder instanceof ModuleBuilder) {
      module = ((ModuleBuilder)builder).commitModule(project, null);
      if (module != null) {
        processCreatedModule(module, dataFromContext);
      }
      return module;
    }
    else {
      List<Module> modules = builder.commit(project, null, new DefaultModulesProvider(project));
      if (builder.isOpenProjectSettingsAfter()) {
        ModulesConfigurator.showDialog(project, null, null);
      }
      module = modules == null || modules.isEmpty() ? null : modules.get(0);
    }
    project.save();
    return module;
  }

  @Nullable
  protected Object prepareDataFromContext(final AnActionEvent e) {
    return null;
  }

  protected void processCreatedModule(final Module module, @Nullable final Object dataFromContext) {
  }

  @Override
  public void update(AnActionEvent e) {
    super.update(e);
    e.getPresentation().setEnabled(getEventProject(e) != null);
  }
}
