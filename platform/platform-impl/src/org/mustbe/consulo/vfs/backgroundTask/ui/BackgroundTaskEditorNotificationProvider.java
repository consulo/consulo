/*
 * Copyright 2013-2015 must-be.org
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
package org.mustbe.consulo.vfs.backgroundTask.ui;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.Gray;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.RequiredReadAction;
import org.mustbe.consulo.editor.notifications.EditorNotificationProvider;
import org.mustbe.consulo.vfs.backgroundTask.BackgroundTaskByVfsChangeManager;
import org.mustbe.consulo.vfs.backgroundTask.BackgroundTaskByVfsChangeProvider;
import org.mustbe.consulo.vfs.backgroundTask.BackgroundTaskByVfsChangeProviders;
import org.mustbe.consulo.vfs.backgroundTask.BackgroundTaskByVfsChangeTask;

import java.awt.*;
import java.util.List;

/**
 * @author VISTALL
 * @since 26.10.2015
 */
public class BackgroundTaskEditorNotificationProvider implements EditorNotificationProvider<EditorNotificationPanel>, DumbAware {
  private static final Key<EditorNotificationPanel> KEY = Key.create("BackgroundTaskEditorProvider");
  private final Project myProject;

  public BackgroundTaskEditorNotificationProvider(Project project) {
    myProject = project;
  }

  @NotNull
  @Override
  public Key<EditorNotificationPanel> getKey() {
    return KEY;
  }

  @RequiredReadAction
  @Nullable
  @Override
  public EditorNotificationPanel createNotificationPanel(@NotNull final VirtualFile file, @NotNull FileEditor fileEditor) {
    List<BackgroundTaskByVfsChangeProvider> providers = BackgroundTaskByVfsChangeProviders.getProviders(myProject, file);
    if (providers.isEmpty()) {
      return null;
    }


    EditorNotificationPanel panel = new EditorNotificationPanel() {
      @Override
      public Color getBackground() {
        return Gray._220;
      }
    };

    List<BackgroundTaskByVfsChangeTask> tasks = BackgroundTaskByVfsChangeManager.getInstance(myProject).findTasks(file);
    if (!tasks.isEmpty()) {
      panel.text("Task(s): " + StringUtil.join(tasks, new Function<BackgroundTaskByVfsChangeTask, String>() {
        @Override
        public String fun(BackgroundTaskByVfsChangeTask backgroundTaskByVfsChangeTask) {
          return backgroundTaskByVfsChangeTask.getName() + (!backgroundTaskByVfsChangeTask.isEnabled() ? " (disabled)" : "");
        }
      }, ", "));

      panel.createActionLabel("Force Run", new Runnable() {
        @Override
        public void run() {
          BackgroundTaskByVfsChangeManager.getInstance(myProject).runTasks(file);
        }
      });
    }
    else {
      panel.text("Background task(s) on file change is available");
    }

    panel.createActionLabel("Manage", new Runnable() {
      @Override
      public void run() {
        BackgroundTaskByVfsChangeManager.getInstance(myProject).openManageDialog(file);
      }
    });
    return panel;
  }
}
