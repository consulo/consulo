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
package consulo.task.impl.internal.versionControlSystem;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.task.LocalTask;
import consulo.task.Task;
import consulo.task.TaskManager;
import consulo.task.TaskRepository;
import consulo.task.impl.internal.TaskManagerImpl;
import consulo.task.impl.internal.context.WorkingContextManager;
import consulo.versionControlSystem.change.CommitContext;
import consulo.versionControlSystem.checkin.CheckinHandler;
import consulo.versionControlSystem.checkin.CheckinHandlerFactory;
import consulo.versionControlSystem.checkin.CheckinProjectPanel;
import org.jspecify.annotations.Nullable;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

/**
 * @author Dmitry Avdeev
 * @since 2011-12-29
 */
@ExtensionImpl
public class TaskCheckinHandlerFactory extends CheckinHandlerFactory {
    
    @Override
    public CheckinHandler createHandler(final CheckinProjectPanel panel, CommitContext commitContext) {
        return new CheckinHandler() {
            @Override
            public void checkinSuccessful() {
                String message = panel.getCommitMessage();
                Project project = panel.getProject();
                TaskManagerImpl manager = (TaskManagerImpl) TaskManager.getManager(project);
                if (manager.getState().saveContextOnCommit) {
                    findTaskInRepositories(message, manager).whenComplete((found, error) -> {
                        Task task = found == null ? manager.createLocalTask(message) : found;
                        LocalTask localTask = manager.addTask(task);
                        localTask.setUpdated(new Date());

                        project.getApplication().invokeLater(
                            () -> WorkingContextManager.getInstance(project).saveContext(localTask),
                            project.getDisposed()
                        );
                    });
                }
            }
        };
    }

    private static CompletableFuture<@Nullable Task> findTaskInRepositories(String message, TaskManager manager) {
        CompletableFuture<@Nullable Task> result = CompletableFuture.completedFuture(null);
        for (TaskRepository repository : manager.getAllRepositories()) {
            String id = repository.extractId(message);
            if (id == null) {
                continue;
            }
            result = result.thenCompose(found -> {
                if (found != null) {
                    return CompletableFuture.completedFuture(found);
                }
                LocalTask localTask = manager.findTask(id);
                if (localTask != null) {
                    return CompletableFuture.completedFuture(localTask);
                }
                return repository.getIssueTracker().findIssue(id).exceptionally(error -> null);
            });
        }
        return result;
    }
}
