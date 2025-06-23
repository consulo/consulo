/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.tasks.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.task.impl.internal.TaskManagerImpl;
import consulo.versionControlSystem.checkin.CheckinProjectPanel;
import consulo.versionControlSystem.change.CommitContext;
import consulo.versionControlSystem.checkin.CheckinHandler;
import consulo.versionControlSystem.checkin.CheckinHandlerFactory;
import consulo.task.LocalTask;
import consulo.task.Task;
import consulo.task.TaskManager;
import consulo.task.TaskRepository;
import consulo.task.impl.internal.context.WorkingContextManager;
import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import javax.swing.*;
import java.util.Date;

/**
 * @author Dmitry Avdeev
 * @since 2011-12-29
 */
@ExtensionImpl
public class TaskCheckinHandlerFactory extends CheckinHandlerFactory {

  @Nonnull
  @Override
  public CheckinHandler createHandler(final CheckinProjectPanel panel, final CommitContext commitContext) {
    return new CheckinHandler() {
      @Override
      public void checkinSuccessful() {
        final String message = panel.getCommitMessage();
        if (message != null) {
          final Project project = panel.getProject();
          final TaskManagerImpl manager = (TaskManagerImpl)TaskManager.getManager(project);
          if (manager.getState().saveContextOnCommit) {
            Task task = findTaskInRepositories(message, manager);
            if (task == null) {
              task = manager.createLocalTask(message);
            }
            final LocalTask localTask = manager.addTask(task);
            localTask.setUpdated(new Date());

            //noinspection SSBasedInspection
            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                if (!project.isDisposed()) {
                  WorkingContextManager.getInstance(project).saveContext(localTask);
                }
              }
            });
          }
        }
      }
    };
  }

  @Nullable
  private static Task findTaskInRepositories(String message, TaskManager manager) {
    TaskRepository[] repositories = manager.getAllRepositories();
    for (TaskRepository repository : repositories) {
      String id = repository.extractId(message);
      if (id == null) continue;
      LocalTask localTask = manager.findTask(id);
      if (localTask != null) return localTask;
      try {
        Task task = repository.findTask(id);
        if (task != null) {
          return task;
        }
      }
      catch (Exception ignore) {

      }
    }
    return null;
  }
}
