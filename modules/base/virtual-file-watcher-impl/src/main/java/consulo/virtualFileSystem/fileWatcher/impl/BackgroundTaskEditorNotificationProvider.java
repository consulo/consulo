/*
 * Copyright 2013-2016 consulo.io
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
package consulo.virtualFileSystem.fileWatcher.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.fileEditor.FileEditor;
import consulo.ide.impl.codeEditor.EditorNotificationProvider;
import consulo.ide.impl.idea.ui.EditorNotificationPanel;
import consulo.project.Project;
import consulo.ui.ex.Gray;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileWatcher.BackgroundTaskByVfsChangeManager;
import consulo.virtualFileSystem.fileWatcher.BackgroundTaskByVfsChangeProvider;
import consulo.virtualFileSystem.fileWatcher.BackgroundTaskByVfsChangeTask;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author VISTALL
 * @since 26.10.2015
 */
@ExtensionImpl
public class BackgroundTaskEditorNotificationProvider implements EditorNotificationProvider<EditorNotificationPanel>, DumbAware {
  private final Project myProject;

  @Inject
  public BackgroundTaskEditorNotificationProvider(Project project) {
    myProject = project;
  }

  @RequiredReadAction
  @Nullable
  @Override
  public EditorNotificationPanel createNotificationPanel(@Nonnull final VirtualFile file, @Nonnull FileEditor fileEditor) {
    List<BackgroundTaskByVfsChangeProvider> providers = BackgroundTaskByVfsChangeProviders.getProviders(myProject, file);
    if (providers.isEmpty()) {
      return null;
    }

    EditorNotificationPanel panel = new EditorNotificationPanel(Gray._220);

    List<BackgroundTaskByVfsChangeTask> tasks = BackgroundTaskByVfsChangeManager.getInstance(myProject).findTasks(file);
    if (!tasks.isEmpty()) {
      panel.text("Task(s): " + StringUtil.join(tasks, it -> it.getName() + (!it.isEnabled() ? " (disabled)" : ""), ", "));

      panel.createActionLabel("Force Run", () -> {
        BackgroundTaskByVfsChangeManager.getInstance(myProject).runTasks(file);
      });
    }
    else {
      panel.text("Background task(s) on file change is available");
    }

    panel.createActionLabel("Manage", () -> {
      BackgroundTaskByVfsChangeManager.getInstance(myProject).openManageDialog(file);
    });
    return panel;
  }
}
