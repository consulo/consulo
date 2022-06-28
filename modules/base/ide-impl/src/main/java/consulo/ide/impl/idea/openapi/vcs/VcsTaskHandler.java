/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.ide.impl.idea.openapi.util.Comparing;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.project.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

@ExtensionAPI(ComponentScope.PROJECT)
public abstract class VcsTaskHandler {

  private static final String DEFAULT_PROHIBITED_SYMBOLS = " ";
  private static final ExtensionPointName<VcsTaskHandler> EXTENSION_POINT_NAME = ExtensionPointName.create(VcsTaskHandler.class);

  public static VcsTaskHandler[] getAllHandlers(final Project project) {
    VcsTaskHandler[] extensions = EXTENSION_POINT_NAME.getExtensions(project);
    List<VcsTaskHandler> handlers = ContainerUtil.filter(extensions, VcsTaskHandler::isEnabled);
    return handlers.toArray(new VcsTaskHandler[handlers.size()]);
  }

  public static class TaskInfo implements Comparable<TaskInfo> {

    private final String myBranch;
    private final Collection<String> myRepositories;
    private final boolean myRemote;

    public TaskInfo(String branch, Collection<String> repositories) {
      this(branch, repositories, false);
    }

    public TaskInfo(String branch, Collection<String> repositories, boolean remote) {
      myBranch = branch;
      myRepositories = repositories;
      myRemote = remote;
    }

    public String getName() {
      return myBranch;
    }

    public boolean isRemote() { return myRemote; }

    public Collection<String> getRepositories() {
      return myRepositories;
    }

    @Override
    public String toString() {
      return getName();
    }

    @Override
    public int compareTo(TaskInfo o) {
      return Comparing.compare(myBranch, o.myBranch);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      TaskInfo info = (TaskInfo)o;

      if (myRemote != info.myRemote) return false;
      if (!myBranch.equals(info.myBranch)) return false;
      return myRepositories.size() == info.myRepositories.size() && myRepositories.containsAll(info.myRepositories);
    }

    @Override
    public int hashCode() {
      return myBranch.hashCode();
    }
  }

  public abstract boolean isEnabled();

  public abstract TaskInfo startNewTask(@Nonnull String taskName);

  public abstract void switchToTask(TaskInfo taskInfo, @Nullable Runnable invokeAfter);

  public abstract void closeTask(@Nonnull TaskInfo taskInfo, @Nonnull TaskInfo original);

  public abstract boolean isSyncEnabled();

  /**
   * @return currently active (checked out) tasks (branches)
   */
  @Nonnull
  public abstract TaskInfo[] getCurrentTasks();

  /**
   * @return all existing tasks (branches)
   */
  public abstract TaskInfo[] getAllExistingTasks();

  /**
   * Should check prohibited symbols and constructions; name ref conflicts depended on Repository will be checked separately if needed
   *
   * @param branchName to check
   * @return true if valid
   */
  public boolean isBranchNameValid(@Nonnull String branchName) {
    return !branchName.contains(DEFAULT_PROHIBITED_SYMBOLS);
  }

  /**
   * Update branchName to valid
   *
   * @param suggestedName suggested name
   * @return new valid branchName
   */
  @Nonnull
  public String cleanUpBranchName(@Nonnull String suggestedName) {
    return suggestedName.replaceAll(DEFAULT_PROHIBITED_SYMBOLS, "-");
  }
}
