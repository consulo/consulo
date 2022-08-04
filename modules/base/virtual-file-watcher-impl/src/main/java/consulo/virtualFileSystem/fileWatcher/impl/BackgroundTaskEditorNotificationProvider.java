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
import consulo.fileEditor.EditorNotificationBuilder;
import consulo.fileEditor.EditorNotificationProvider;
import consulo.fileEditor.FileEditor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.color.RGBColor;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileWatcher.BackgroundTaskByVfsChangeManager;
import consulo.virtualFileSystem.fileWatcher.BackgroundTaskByVfsChangeProvider;
import consulo.virtualFileSystem.fileWatcher.BackgroundTaskByVfsChangeTask;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 26.10.2015
 */
@ExtensionImpl
public class BackgroundTaskEditorNotificationProvider implements EditorNotificationProvider, DumbAware {
  private final Project myProject;

  @Inject
  public BackgroundTaskEditorNotificationProvider(Project project) {
    myProject = project;
  }

  @Nonnull
  @Override
  public String getId() {
    return "background-task";
  }

  @RequiredReadAction
  @Nullable
  @Override
  public EditorNotificationBuilder buildNotification(@Nonnull VirtualFile file, @Nonnull FileEditor fileEditor, @Nonnull Supplier<EditorNotificationBuilder> builderFactory) {
    List<BackgroundTaskByVfsChangeProvider> providers = BackgroundTaskByVfsChangeProviders.getProviders(myProject, file);
    if (providers.isEmpty()) {
      return null;
    }

    EditorNotificationBuilder builder = builderFactory.get();
    builder.withBackgroundColor(new RGBColor(220, 220, 220));

    List<BackgroundTaskByVfsChangeTask> tasks = BackgroundTaskByVfsChangeManager.getInstance(myProject).findTasks(file);
    if (!tasks.isEmpty()) {
      builder.withText(LocalizeValue.localizeTODO("Task(s): " + StringUtil.join(tasks, it -> it.getName() + (!it.isEnabled() ? " (disabled)" : ""), ", ")));

      builder.withAction(LocalizeValue.localizeTODO("Force Run"), () -> BackgroundTaskByVfsChangeManager.getInstance(myProject).runTasks(file));
    }
    else {
      builder.withText(LocalizeValue.localizeTODO("Background task(s) on file change is available"));
    }

    builder.withAction(LocalizeValue.localizeTODO("Manage"), () -> BackgroundTaskByVfsChangeManager.getInstance(myProject).openManageDialog(file));
    return builder;
  }
}
