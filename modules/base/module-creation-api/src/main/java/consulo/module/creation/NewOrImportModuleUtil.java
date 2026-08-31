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
package consulo.module.creation;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.WriteAction;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.application.concurrent.coroutine.WriteLock;
import consulo.content.base.ExcludedContentFolderTypeProvider;
import consulo.logging.Logger;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.creation.importing.ModuleImportContext;
import consulo.module.creation.importing.ModuleImportProvider;
import consulo.module.creation.scratch.NewModuleBuilderProcessor;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.UIAccess;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CallSubroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.dataholder.Key;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;

import java.io.File;
import java.io.IOException;

public class NewOrImportModuleUtil {
  private static final Logger LOG = Logger.getInstance(NewOrImportModuleUtil.class);

  private static final Key<Project> IMPORT_PROJECT = Key.create("NewOrImportModuleUtil.importProject");
  private static final Key<ModifiableModuleModel> IMPORT_MODULE_MODEL = Key.create("NewOrImportModuleUtil.importModuleModel");

  @RequiredReadAction
  public static Module doCreate(NewModuleBuilderProcessor<NewModuleWizardContext> processor,
                                NewModuleWizardContext context,
                                Project project,
                                VirtualFile baseDir) {
    return doCreate(processor, context, ModuleManager.getInstance(project).getModifiableModel(), baseDir, true);
  }

  
  @RequiredReadAction
  @SuppressWarnings("unchecked")
  public static Module doCreate(NewModuleBuilderProcessor<NewModuleWizardContext> processor,
                                NewModuleWizardContext context,
                                ModifiableModuleModel modifiableModel,
                                VirtualFile baseDir,
                                boolean requireModelCommit) {
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

  public static <T extends ModuleImportContext> Coroutine<Void, Project> importProject(T context, ModuleImportProvider<T> importProvider, UIAccess uiAccess) {
    return Coroutine.<Void, Object>first(CodeExecution.<Void, Object>apply((input, c) -> {
        String projectFilePath = context.getPath();
        String projectName = context.getName();

        try {
          File projectDir = new File(projectFilePath);
          FileUtil.ensureExists(projectDir);
          FileUtil.ensureExists(new File(projectDir, Project.DIRECTORY_STORE_FOLDER));
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }

        Project newProject = ProjectManager.getInstance().createProject(projectName, projectFilePath);

        if (newProject == null) {
          throw new IllegalStateException("Project not initialized");
        }

        c.putCopyableUserData(IMPORT_PROJECT, newProject);
        return null;
      }))
      // Save the freshly created project as a subroutine
      .then(CallSubroutine.call(c -> c.getCopyableUserData(IMPORT_PROJECT).saveAsync(uiAccess)))
      // Create the module model under a read action
      .then(ReadLock.<Object, Object>apply((o, c) -> {
        Project newProject = c.getCopyableUserData(IMPORT_PROJECT);
        ModifiableModuleModel modifiableModel = ModuleManager.getInstance(newProject).getModifiableModel();
        c.putCopyableUserData(IMPORT_MODULE_MODEL, modifiableModel);
        return o;
      }))
      // Run the import provider as a subroutine
      .then(CallSubroutine.<Object, Object>call(c ->
        importProvider.process(context, c.getCopyableUserData(IMPORT_PROJECT), c.getCopyableUserData(IMPORT_MODULE_MODEL), module -> {
        })))
      // Commit the module model under a write action
      .then(WriteLock.<Object, Object>apply((o, c) -> {
        c.getCopyableUserData(IMPORT_MODULE_MODEL).commit();
        return o;
      }))
      // Save again as a subroutine
      .then(CallSubroutine.call(c -> c.getCopyableUserData(IMPORT_PROJECT).saveAsync(uiAccess)))
      .then(CodeExecution.<Object, Project>apply((o, c) -> {
        context.dispose();
        return c.getCopyableUserData(IMPORT_PROJECT);
      }));
  }
}
