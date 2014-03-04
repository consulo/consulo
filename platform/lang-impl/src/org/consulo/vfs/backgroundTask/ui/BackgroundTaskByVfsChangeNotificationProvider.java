/*
 * Copyright 2013 must-be.org
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
package org.consulo.vfs.backgroundTask.ui;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import com.intellij.ui.JBColor;
import org.consulo.vfs.backgroundTask.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * @author VISTALL
 * @since 23:05/06.10.13
 */
public class BackgroundTaskByVfsChangeNotificationProvider extends EditorNotifications.Provider<EditorNotificationPanel> {
  private static final Key<EditorNotificationPanel> KEY = Key.create("BackgroundTaskByVfsChange");

  private final Project myProject;
  private final EditorNotifications myNotifications;

  public BackgroundTaskByVfsChangeNotificationProvider(Project project, final EditorNotifications notifications) {
    myProject = project;
    myNotifications = notifications;
  }

  @Override
  public Key<EditorNotificationPanel> getKey() {
    return KEY;
  }

  @Nullable
  @Override
  public EditorNotificationPanel createNotificationPanel(VirtualFile file, FileEditor fileEditor) {
    final PsiFile psiFile = PsiManager.getInstance(myProject).findFile(file);
    if (psiFile == null) {
      return null;
    }

    VirtualFile virtualFile = psiFile.getVirtualFile();
    if (virtualFile == null) {
      return null;
    }
    BackgroundTaskByVfsChangeProvider changeProvider = BackgroundTaskByVfsChangeProvider.EP.forFileType(psiFile.getFileType());
    if (changeProvider == null || !changeProvider.validate(myProject, virtualFile)) {
      return null;
    }

    return createPanel(myProject, virtualFile, changeProvider);
  }

  @NotNull
  private EditorNotificationPanel createPanel(@NotNull final Project project,
                                              @NotNull final VirtualFile virtualFile,
                                              @NotNull final BackgroundTaskByVfsChangeProvider changeProvider) {
    final BackgroundTaskByVfsChangeManager backgroundTaskManager = BackgroundTaskByVfsChangeManager.getInstance(project);

    final BackgroundTaskByVfsChangeTask task = backgroundTaskManager.getTask(virtualFile);

    final EditorNotificationPanel panel = new EditorNotificationPanel() {
      @Override
      public Color getBackground() {
        return JBColor.LIGHT_GRAY;
      }
    };
    panel.setText(IdeBundle.message("background.task.editor.header", changeProvider.getName()));

    panel.createActionLabel(IdeBundle.message("background.task.button.configure"), new Runnable() {
      @Override
      public void run() {
        if (task != null) {
          BackgroundTaskByVfsParametersImpl parameters = new BackgroundTaskByVfsParametersImpl(project);
          parameters.set(task.getParameters());

          BackgroundTaskByVfsChangeDialog dialog = new BackgroundTaskByVfsChangeDialog(project, parameters);
          if (dialog.showAndGet()) {
            task.getParameters().set(parameters);
            ((BackgroundTaskByVfsChangeTaskImpl)task).parameterUpdated();

            updateNotify(task);
          }
        }
        else {
          BackgroundTaskByVfsParametersImpl parameters = new BackgroundTaskByVfsParametersImpl(project);

          changeProvider.setDefaultParameters(project, virtualFile, parameters);

          BackgroundTaskByVfsChangeDialog dialog = new BackgroundTaskByVfsChangeDialog(project, parameters);

          boolean b = dialog.showAndGet();
          if (b) {
            updateNotify(backgroundTaskManager.registerTask(virtualFile, changeProvider, parameters));
          }
        }
      }
    });

    if (task != null) {
      panel.createActionLabel(IdeBundle.message("background.task.button.cancel"), new Runnable() {
        @Override
        public void run() {
          backgroundTaskManager.cancelTask(task);

          updateNotify(task);
        }
      });
    }

    return panel;
  }

  private void updateNotify(BackgroundTaskByVfsChangeTask task) {
    VirtualFile file = task.getVirtualFilePointer().getFile();
    if (file == null) {
      return;
    }
    myNotifications.updateNotifications(file);
  }
}
