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
import consulo.application.progress.ProgressIndicator;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.task.event.TaskListener;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.VcsTaskHandler;
import consulo.versionControlSystem.change.LocalChangeList;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class TaskManager {

  public static TaskManager getManager(@Nonnull Project project) {
    return project.getComponent(TaskManager.class);
  }

  public enum VcsOperation {
    CREATE_BRANCH,
    CREATE_CHANGELIST,
    DO_NOTHING
  }

  /**
   * Queries all configured task repositories.
   * Operation may be blocked for a while.
   *
   * @param query text search
   * @return up-to-date issues retrieved from repositories
   * @see #getCachedIssues()
   */
  public abstract List<Task> getIssues(@Nullable String query);

  public abstract List<Task> getIssues(@Nullable String query, boolean forceRequest);

  /**
   * Most arguments have the same meaning as the ones in {@link TaskRepository#getIssues(String, int, int, boolean, ProgressIndicator)}.
   *
   * @param query        optional pattern to filter tasks. One use case is the text entered in "Open Task" dialog.
   * @param offset       first issue, that should be returned by server. It's safe to use 0, if your server doesn't support pagination.
   *                     Or you could calculate it as {@code pageSize * (page - 1)} if it does.
   * @param limit        maximum number of issues returned in one response. You can interpret it as page size.
   * @param withClosed   whether to include closed issues. Downloaded issues will be filtered by {@link Task#isClosed()} anyway, but
   *                     filtering on server side can give more useful results in single request.
   * @param indicator    progress indicator to interrupt long-running requests.
   * @param forceRequest whether to download issues anew or use already cached ones.
   * @return tasks collected from all active repositories
   */
  public List<Task> getIssues(@Nullable String query, int offset, int limit, boolean withClosed, @Nonnull ProgressIndicator indicator, boolean forceRequest) {
    return Collections.emptyList();
  }

  /**
   * Returns already cached issues.
   *
   * @return cached issues.
   */
  public abstract List<Task> getCachedIssues();

  public abstract List<Task> getCachedIssues(boolean withClosed);

  @Nullable
  public abstract Task updateIssue(@Nonnull String id);

  public abstract List<LocalTask> getLocalTasks();

  public abstract List<LocalTask> getLocalTasks(boolean withClosed);

  public abstract LocalTask addTask(Task issue);

  public abstract LocalTask createLocalTask(String summary);

  public abstract LocalTask activateTask(@Nonnull Task task, boolean clearContext);

  @Nonnull
  public abstract LocalTask getActiveTask();

  @Nullable
  public abstract LocalTask findTask(String id);

  /**
   * Update issue cache asynchronously
   *
   * @param onComplete callback to be invoked after updating
   */
  public abstract void updateIssues(@Nullable Runnable onComplete);

  public abstract boolean isVcsEnabled();

  public abstract AbstractVcs getActiveVcs();

  public abstract boolean isLocallyClosed(LocalTask localTask);

  @Nullable
  public abstract LocalTask getAssociatedTask(LocalChangeList list);

  public abstract void trackContext(LocalChangeList changeList);

  public abstract void disassociateFromTask(LocalChangeList changeList);

  public abstract void removeTask(LocalTask task);

  public abstract void addTaskListener(@Nonnull TaskListener listener, @Nonnull Disposable parentDisposable);

  public abstract TaskRepository[] getAllRepositories();

  public abstract void addRepository(@Nonnull TaskRepository repository);

  public abstract boolean testConnection(TaskRepository repository);

  public abstract void setTrackContextForNewChangelist(boolean value);

  public abstract boolean isTrackContextForNewChangelist();

  @Nonnull
  public String suggestBranchName(@Nonnull Task task) {
    String name = constructDefaultBranchName(task);
    if (task.isIssue()) {
      return name.replace(' ', '-');
    }
    List<String> words = StringUtil.getWordsIn(name);
    String[] strings = ArrayUtil.toStringArray(words);
    return StringUtil.join(strings, 0, Math.min(2, strings.length), "-");
  }

  @Nonnull
  public abstract String constructDefaultBranchName(@Nonnull Task task);

  public abstract void addBranches(LocalTask task, VcsTaskHandler.TaskInfo[] info, boolean original);
}
