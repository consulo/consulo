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

import com.intellij.icons.AllIcons;
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
import com.intellij.openapi.wm.impl.welcomeScreen.NewWelcomeScreen;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import consulo.annotations.RequiredDispatchThread;
import consulo.ide.impl.NewProjectOrModuleDialog;

import java.io.File;

/**
 * @author yole
 */
public class NewProjectAction extends AnAction implements DumbAware {
  @RequiredDispatchThread
  @Override
  public void actionPerformed(final AnActionEvent e) {
    Project project = e.getProject();
    NewProjectOrModuleDialog dialog = new NewProjectOrModuleDialog(project, null);

    if (dialog.showAndGet()) {
      generateProject(project, dialog);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    if (NewWelcomeScreen.isNewWelcomeScreen(e)) {
      e.getPresentation().setIcon(AllIcons.Welcome.CreateNewProject);
    }
  }

  @Nullable
  @RequiredDispatchThread
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
      int rc = Messages.showYesNoDialog(project, "The directory '" + location + "' is not empty. Continue?", "Create New Project", Messages.getQuestionIcon());
      if (rc == Messages.NO) {
        return null;
      }
    }

    GeneralSettings.getInstance().setLastProjectCreationLocation(location.getParent());
    return PlatformProjectOpenProcessor.doOpenProject(baseDir, null, false, -1, new Consumer<Project>() {
      @Override
      @RequiredDispatchThread
      public void consume(final Project project) {
        dialog.doCreate(project, baseDir);
      }
    });
  }
}
