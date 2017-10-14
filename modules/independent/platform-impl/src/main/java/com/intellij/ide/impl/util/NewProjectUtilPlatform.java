/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.ide.impl.util;

import com.intellij.ide.GeneralSettings;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.annotations.RequiredReadAction;
import consulo.annotations.RequiredWriteAction;
import consulo.ide.newProject.NewModuleBuilderProcessor;
import consulo.ide.newProject.NewProjectPanel;
import consulo.roots.impl.ExcludedContentFolderTypeProvider;
import org.jetbrains.annotations.NotNull;

public class NewProjectUtilPlatform {
  public static void closePreviousProject(final Project projectToClose) {
    Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
    if (openProjects.length > 0) {
      int exitCode = ProjectUtil.confirmOpenNewProject(true);
      if (exitCode == GeneralSettings.OPEN_PROJECT_SAME_WINDOW) {
        ProjectUtil.closeAndDispose(projectToClose != null ? projectToClose : openProjects[openProjects.length - 1]);
      }
    }
  }

  @NotNull
  @RequiredReadAction
  public static Module doCreate(@NotNull NewProjectPanel projectPanel, @NotNull final Project project, @NotNull final VirtualFile baseDir) {
    return doCreate(projectPanel, ModuleManager.getInstance(project).getModifiableModel(), baseDir, true);
  }

  @NotNull
  public static Module doCreate(@NotNull NewProjectPanel projectPanel, @NotNull final ModifiableModuleModel modifiableModel, @NotNull final VirtualFile baseDir, final boolean requireModelCommit) {
    return new WriteAction<Module>() {
      @Override
      protected void run(Result<Module> result) throws Throwable {
        result.setResult(doCreateImpl(projectPanel, modifiableModel, baseDir, requireModelCommit));
      }
    }.execute().getResultObject();
  }

  @SuppressWarnings("unchecked")
  @NotNull
  @RequiredWriteAction
  private static Module doCreateImpl(@NotNull NewProjectPanel projectPanel, @NotNull final ModifiableModuleModel modifiableModel, @NotNull final VirtualFile baseDir, boolean requireModelCommit) {
    String name = StringUtil.notNullize(projectPanel.getNameText(), baseDir.getName());

    Module newModule = modifiableModel.newModule(name, baseDir.getPath());

    ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(newModule);
    ModifiableRootModel modifiableModelForModule = moduleRootManager.getModifiableModel();
    ContentEntry contentEntry = modifiableModelForModule.addContentEntry(baseDir);

    if (!projectPanel.isModuleCreation()) {
      contentEntry.addFolder(contentEntry.getUrl() + "/" + Project.DIRECTORY_STORE_FOLDER, ExcludedContentFolderTypeProvider.getInstance());
    }

    NewModuleBuilderProcessor processor = projectPanel.getProcessor();
    if (processor != null) {
      processor.setupModule(projectPanel.getConfigurationPanel(), contentEntry, modifiableModelForModule);
    }

    modifiableModelForModule.commit();

    if (requireModelCommit) {
      modifiableModel.commit();
    }

    baseDir.refresh(true, true);
    return newModule;
  }
}
