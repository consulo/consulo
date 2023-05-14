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
package consulo.ide.newModule;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.WriteAction;
import consulo.content.base.ExcludedContentFolderTypeProvider;
import consulo.ide.moduleImport.ModuleImportContext;
import consulo.ide.moduleImport.ModuleImportProvider;
import consulo.logging.Logger;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import java.io.File;

public class NewOrImportModuleUtil {
  private static final Logger LOG = Logger.getInstance(NewOrImportModuleUtil.class);

  @Nonnull
  @RequiredReadAction
  public static Module doCreate(@Nonnull NewModuleBuilderProcessor<NewModuleWizardContext> processor,
                                @Nonnull NewModuleWizardContext context,
                                @Nonnull final Project project,
                                @Nonnull final VirtualFile baseDir) {
    return doCreate(processor, context, ModuleManager.getInstance(project).getModifiableModel(), baseDir, true);
  }

  @Nonnull
  @RequiredReadAction
  @SuppressWarnings("unchecked")
  public static Module doCreate(@Nonnull NewModuleBuilderProcessor<NewModuleWizardContext> processor,
                                @Nonnull NewModuleWizardContext context,
                                @Nonnull final ModifiableModuleModel modifiableModel,
                                @Nonnull final VirtualFile baseDir,
                                final boolean requireModelCommit) {
    String name = StringUtil.notNullize(context.getName(), baseDir.getName());

    Module newModule = modifiableModel.newModule(name, baseDir.getPath());

    ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(newModule);
    ModifiableRootModel modifiableModelForModule = moduleRootManager.getModifiableModel();
    ContentEntry contentEntry = modifiableModelForModule.addContentEntry(baseDir);

    if (context.isNewProject()) {
      contentEntry.addFolder(contentEntry.getUrl() + "/" + Project.DIRECTORY_STORE_FOLDER, ExcludedContentFolderTypeProvider.getInstance());
    }

    processor.process(context, contentEntry, modifiableModelForModule);

    WriteAction.runAndWait(modifiableModelForModule::commit);

    if (requireModelCommit) {
      WriteAction.runAndWait(modifiableModel::commit);
    }

    baseDir.refresh(true, true);
    return newModule;
  }

  @Nonnull
  @RequiredUIAccess
  public static <T extends ModuleImportContext> AsyncResult<Project> importProject(@Nonnull T context, @Nonnull ModuleImportProvider<T> importProvider) {
    final ProjectManager projectManager = ProjectManager.getInstance();
    final String projectFilePath = context.getPath();
    String projectName = context.getName();

    try {
      File projectDir = new File(projectFilePath);
      FileUtil.ensureExists(projectDir);
      File projectConfigDir = new File(projectDir, Project.DIRECTORY_STORE_FOLDER);
      FileUtil.ensureExists(projectConfigDir);

      final Project newProject = projectManager.createProject(projectName, projectFilePath);

      if (newProject == null) return AsyncResult.rejected();

      newProject.save();

      ModifiableModuleModel modifiableModel = ModuleManager.getInstance(newProject).getModifiableModel();

      importProvider.process(context, newProject, modifiableModel, module -> {
      });

      WriteAction.runAndWait(modifiableModel::commit);

      newProject.save();

      context.dispose();

      return AsyncResult.resolved(newProject);
    }
    catch (Exception e) {
      context.dispose();

      return AsyncResult.<Project>undefined().rejectWithThrowable(e);
    }
  }
}
