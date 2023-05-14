/*
 * Copyright 2013-2022 consulo.io
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

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.AsyncFileListener;
import consulo.virtualFileSystem.event.VFileContentChangeEvent;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.fileWatcher.BackgroundTaskByVfsChangeManager;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 01-Aug-22
 */
@ExtensionImpl
public class BackgroundTaskByVfsChangeFileListener implements AsyncFileListener {
  private final Provider<ProjectManager> myProjectManager;

  @Inject
  public BackgroundTaskByVfsChangeFileListener(Provider<ProjectManager> projectManager) {
    myProjectManager = projectManager;
  }

  @Nullable
  @Override
  public ChangeApplier prepareChange(@Nonnull List<? extends VFileEvent> events) {
    return new ChangeApplier() {
      private List<Runnable> myTasks = new ArrayList<>();

      @Override
      public void beforeVfsChange() {
        for (VFileEvent event : events) {
          if (!(event instanceof VFileContentChangeEvent)) {
            continue;
          }

          for (Project project : myProjectManager.get().getOpenProjects()) {
            BackgroundTaskByVfsChangeManager vfsChangeManager = BackgroundTaskByVfsChangeManager.getInstance(project);

            VirtualFile file = event.getFile();
            if(!vfsChangeManager.findEnabledTasks(file).isEmpty()) {
              myTasks.add(() -> vfsChangeManager.runTasks(file));
            }
          }
        }
      }

      @Override
      public void afterVfsChange() {
        for (Runnable task : myTasks) {
          task.run();
        }
      }
    };
  }
}
