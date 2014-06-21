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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import org.consulo.ide.eap.EarlyAccessProgramManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.ide.impl.NewProjectOrModuleDialog;
import org.mustbe.consulo.ide.impl.NewProjectOrModuleDialogWithSetup;

import java.io.File;

/**
 * @author yole
 */
public class NewDirectoryProjectAction extends AnAction implements DumbAware {
  @Override
  public void actionPerformed(final AnActionEvent e) {
    NewProjectOrModuleDialog dialog;
    Project project = e.getProject();

    if (EarlyAccessProgramManager.getInstance().getState(NewProjectOrModuleDialogWithSetup.EapDescriptor.class)) {
      dialog = new NewProjectOrModuleDialogWithSetup(project, null);
    }
    else {
      dialog = new NewDirectoryProjectDialog(project);
    }

    if (dialog.showAndGet()) {
      generateProject(project, dialog);
    }
  }

  @Nullable
  protected Project generateProject(Project project, @NotNull final NewProjectOrModuleDialog dialog) {
    final File location = new File(dialog.getLocationText());
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
      int rc = Messages.showYesNoDialog(project, "The directory '" + location +
                                                 "' is not empty. Would you like to create a project from existing sources instead?", "Create New Project",
                                        Messages.getQuestionIcon());
      if (rc == Messages.YES) {
        return PlatformProjectOpenProcessor.getInstance().doOpenProject(baseDir, null, false);
      }
    }

    GeneralSettings.getInstance().setLastProjectCreationLocation(location.getParent());
    return PlatformProjectOpenProcessor.doOpenProject(baseDir, null, false, -1, new Consumer<Project>() {
      @Override
      public void consume(final Project project) {
        dialog.doCreate(project, baseDir);
      }
    });
  }
}
