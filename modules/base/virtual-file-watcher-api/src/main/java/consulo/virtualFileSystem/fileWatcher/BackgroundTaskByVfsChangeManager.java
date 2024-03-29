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
package consulo.virtualFileSystem.fileWatcher;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import java.util.List;

/**
 * @author VISTALL
 * @since 22:46/06.10.13
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class BackgroundTaskByVfsChangeManager {
  @Nonnull
  public static BackgroundTaskByVfsChangeManager getInstance(@Nonnull Project project) {
    return project.getInstance(BackgroundTaskByVfsChangeManager.class);
  }

  /**
   * Create task without adding to list
   */
  @Nonnull
  public abstract BackgroundTaskByVfsChangeTask createTask(@Nonnull BackgroundTaskByVfsChangeProvider provider,
                                                           @Nonnull VirtualFile virtualFile,
                                                           @Nonnull String name);

  @RequiredUIAccess
  public abstract void openManageDialog(@Nonnull VirtualFile virtualFile);

  @Nonnull
  public abstract List<BackgroundTaskByVfsChangeTask> findTasks(@Nonnull VirtualFile virtualFile);

  @Nonnull
  public abstract List<BackgroundTaskByVfsChangeTask> findEnabledTasks(@Nonnull VirtualFile virtualFile);

  @Nonnull
  public abstract BackgroundTaskByVfsChangeTask[] getTasks();

  public abstract void runTasks(@Nonnull VirtualFile virtualFile);

  public abstract boolean removeTask(@Nonnull BackgroundTaskByVfsChangeTask task);

  public abstract void registerTask(@Nonnull BackgroundTaskByVfsChangeTask task);
}
