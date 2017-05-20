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
package consulo.backgroundTaskByVfsChange;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author VISTALL
 * @since 22:46/06.10.13
 */
public abstract class BackgroundTaskByVfsChangeManager {
  @NotNull
  public static BackgroundTaskByVfsChangeManager getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, BackgroundTaskByVfsChangeManager.class);
  }

  public interface Listener {
    void taskAdded(@NotNull BackgroundTaskByVfsChangeTask task);

    void taskChanged(@NotNull BackgroundTaskByVfsChangeTask task);

    void taskCanceled(@NotNull BackgroundTaskByVfsChangeTask task);
  }

  public abstract static class ListenerAdapter implements Listener {
    @Override
    public void taskAdded(@NotNull BackgroundTaskByVfsChangeTask task) {
      taskChanged(task);
    }

    @Override
    public void taskCanceled(@NotNull BackgroundTaskByVfsChangeTask task) {
      taskChanged(task);
    }
  }

  public static final Topic<Listener> TOPIC =
          Topic.create("background.task.change.listener", Listener.class);


  public abstract void openManageDialog(@NotNull VirtualFile virtualFile);

  @NotNull
  public abstract List<BackgroundTaskByVfsChangeTask> findTasks(@NotNull VirtualFile virtualFile);

  @NotNull
  public abstract List<BackgroundTaskByVfsChangeTask> findEnabledTasks(@NotNull VirtualFile virtualFile);

  @NotNull
  public abstract BackgroundTaskByVfsChangeTask[] getTasks();

  public abstract void runTasks(@NotNull VirtualFile virtualFile);

  public abstract boolean cancelTask(@NotNull BackgroundTaskByVfsChangeTask task);

  public abstract void registerTask(@NotNull BackgroundTaskByVfsChangeTask task);
}
