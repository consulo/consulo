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
package com.intellij.platform;

import com.intellij.ide.GeneralSettings;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.roots.impl.ExcludedContentFolderTypeProvider;

import java.io.File;

/**
 * @author yole
 */
public class NewDirectoryProjectAction extends AnAction implements DumbAware {
  @Override
  public void actionPerformed(final AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    NewDirectoryProjectDialog dlg = new NewDirectoryProjectDialog(project);
    dlg.show();
    if (dlg.getExitCode() != DialogWrapper.OK_EXIT_CODE) return;
    generateProject(project, dlg);
  }

  @Nullable
  protected Project generateProject(Project project, NewDirectoryProjectDialog dlg) {
    final File location = new File(dlg.getNewProjectLocation());
    final int childCount = location.exists() ? location.list().length : 0;
    if (!location.exists() && !location.mkdirs()) {
      Messages.showErrorDialog(project, "Cannot create directory '" + location + "'", "Create Project");
      return null;
    }

    final VirtualFile baseDir = ApplicationManager.getApplication().runWriteAction(new Computable<VirtualFile>() {
      @Override
      public VirtualFile compute() {
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(location);
      }
    });
    baseDir.refresh(false, true);

    if (childCount > 0) {
      int rc = Messages.showYesNoDialog(project,
                                        "The directory '" + location +
                                        "' is not empty. Would you like to create a project from existing sources instead?",
                                        "Create New Project", Messages.getQuestionIcon());
      if (rc == 0) {
        return PlatformProjectOpenProcessor.getInstance().doOpenProject(baseDir, null, false);
      }
    }

    GeneralSettings.getInstance().setLastProjectCreationLocation(location.getParent());
    return PlatformProjectOpenProcessor.doOpenProject(baseDir, null, false, -1, new Consumer<Project>() {
      @Override
      public void consume(final Project project) {
        new WriteAction<Object>() {
          @Override
          protected void run(Result<Object> result) throws Throwable {
            createModule(project, baseDir);
          }
        }.execute();
      }
    }, false);
  }

  private static void createModule(final Project project, final VirtualFile baseDir) {
    ModuleManager moduleManager = ModuleManager.getInstance(project);

    ModifiableModuleModel modifiableModel = moduleManager.getModifiableModel();
    Module newModule = modifiableModel.newModule(baseDir.getName(), baseDir.getPath());

    ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(newModule);
    ModifiableRootModel modifiableModelForModule = moduleRootManager.getModifiableModel();
    ContentEntry contentEntry = modifiableModelForModule.addContentEntry(baseDir);
    contentEntry.addFolder(baseDir.getUrl() + "/" + Project.DIRECTORY_STORE_FOLDER, ExcludedContentFolderTypeProvider.getInstance());
    modifiableModelForModule.commit();

    modifiableModel.commit();
  }
}
