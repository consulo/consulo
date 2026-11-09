/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.task;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.task.event.TaskListener;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.VcsTaskHandler;
import consulo.versionControlSystem.change.LocalChangeList;

import org.jspecify.annotations.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Dmitry Avdeev
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class TaskManager {

  public static TaskManager getManager(Project project) {
    return project.getComponent(TaskManager.class);
  }

  public enum VcsOperation {
    CREATE_BRANCH,
    CREATE_CHANGELIST,
    DO_NOTHING
  }

  /**
   * Searches all configured task repositories; issues found are also put into the cache.
   *
   * @param query        optional pattern to filter tasks, passed to {@link consulo.task.issue.IssueTracker#search}
   * @param offset       first issue to return
   * @param limit        maximum number of issues per repository
   * @param withClosed   whether to include closed issues
   * @param forceRequest whether to also ask repositories that failed before (and report their failure)
   * @return tasks collected from all active repositories
   */
  public abstract CompletableFuture<List<Task>> searchIssues(@Nullable String query, int offset, int limit, boolean withClosed, boolean forceRequest);

  /**
   * Returns already cached issues.
   *
   * @return cached issues.
   */
  public abstract List<Task> getCachedIssues();

  public abstract List<Task> getCachedIssues(boolean withClosed);

  public abstract CompletableFuture<@Nullable Task> refreshIssue(String id);

  public abstract List<LocalTask> getLocalTasks();

  public abstract List<LocalTask> getLocalTasks(boolean withClosed);

  public abstract LocalTask addTask(Task issue);

  public abstract LocalTask createLocalTask(String summary);

  public abstract LocalTask activateTask(Task task, boolean clearContext);

  
  public abstract LocalTask getActiveTask();

  public abstract @Nullable LocalTask findTask(String id);

  /**
   * Update issue cache asynchronously
   *
   * @param onComplete callback to be invoked after updating
   */
  public abstract void updateIssues(@Nullable Runnable onComplete);

  public abstract boolean isVcsEnabled();

  public abstract AbstractVcs getActiveVcs();

  public abstract boolean isLocallyClosed(LocalTask localTask);

  public abstract @Nullable LocalTask getAssociatedTask(LocalChangeList list);

  public abstract void trackContext(LocalChangeList changeList);

  public abstract void disassociateFromTask(LocalChangeList changeList);

  public abstract void removeTask(LocalTask task);

  public abstract void addTaskListener(TaskListener listener, Disposable parentDisposable);

  public abstract TaskRepository[] getAllRepositories();

  public abstract void addRepository(TaskRepository repository);

  public abstract CompletableFuture<?> testConnection(TaskRepository repository);

  public abstract void refreshDetectedRepositories();

  public abstract void setTrackContextForNewChangelist(boolean value);

  public abstract boolean isTrackContextForNewChangelist();

  
  public String suggestBranchName(Task task) {
    String name = constructDefaultBranchName(task);
    if (task.isIssue()) {
      return name.replace(' ', '-');
    }
    List<String> words = StringUtil.getWordsIn(name);
    String[] strings = ArrayUtil.toStringArray(words);
    return StringUtil.join(strings, 0, Math.min(2, strings.length), "-");
  }

  
  public abstract String constructDefaultBranchName(Task task);

  public abstract void addBranches(LocalTask task, VcsTaskHandler.TaskInfo[] info, boolean original);
}
