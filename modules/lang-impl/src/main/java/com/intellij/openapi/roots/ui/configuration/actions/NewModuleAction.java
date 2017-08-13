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
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.ui.configuration.DefaultModulesProvider;
import com.intellij.openapi.roots.ui.configuration.ModulesConfigurator;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.annotations.RequiredDispatchThread;
import consulo.ide.newProject.NewProjectDialog;
import consulo.ide.newProject.actions.NewProjectAction;
import consulo.moduleImport.ModuleImportContext;
import consulo.moduleImport.ModuleImportProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
  @RequiredDispatchThread
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = getEventProject(e);
    if (project == null) {
      return;
    }
    final VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);

    VirtualFile moduleDir = selectModuleDirectory(project, virtualFile);
    if(moduleDir == null) {
      return;
    }

    NewProjectDialog dialog = new NewProjectDialog(project, moduleDir);
    if (dialog.showAndGet()) {
      NewProjectAction.doCreate(dialog.getProjectPanel(), project, moduleDir);
    }
  }

  private static VirtualFile selectModuleDirectory(Project project, VirtualFile virtualFile) {
    final ModuleManager moduleManager = ModuleManager.getInstance(project);
    FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(false, true, false, false, false, false) {
      @Override
      @RequiredDispatchThread
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

    return FileChooser.chooseFile(fileChooserDescriptor, project, virtualFile != null && virtualFile.isDirectory() ? virtualFile : null);
  }

  @Nullable
  @SuppressWarnings("unchecked")
  public static Module createModuleFromWizard(Project project, @Nullable Object dataFromContext, AddModuleWizard wizard) {
    final ModuleImportProvider importProvider = wizard.getImportProvider();
    if (importProvider instanceof ModuleBuilder) {
      final ModuleBuilder moduleBuilder = (ModuleBuilder)importProvider;
      if (moduleBuilder.getName() == null) {
        moduleBuilder.setName(wizard.getProjectName());
      }
      if (moduleBuilder.getModuleDirPath() == null) {
        moduleBuilder.setModuleDirPath(wizard.getModuleDirPath());
      }
    }
    if (!importProvider.validate(project, project)) {
      return null;
    }
    Module module;
    if (importProvider instanceof ModuleBuilder) {
      module = ((ModuleBuilder)importProvider).commitModule(project, null);
      return module;
    }
    else {
      ModuleImportContext context = wizard.getWizardContext().getModuleImportContext(importProvider);
      List<Module> modules = importProvider.commit(context, project, null, DefaultModulesProvider.createForProject(project), null);
      if (context.isOpenProjectSettingsAfter()) {
        ModulesConfigurator.showDialog(project, null, null);
      }
      module = modules.isEmpty() ? null : modules.get(0);
    }
    project.save();
    return module;
  }

  @RequiredDispatchThread
  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    e.getPresentation().setEnabled(e.getProject() != null);
  }
}
