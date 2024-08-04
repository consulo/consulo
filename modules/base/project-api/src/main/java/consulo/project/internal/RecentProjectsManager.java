/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.project.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.project.Project;
import consulo.project.ProjectGroup;
import consulo.ui.ex.action.AnAction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.List;

@ServiceAPI(ComponentScope.APPLICATION)
public abstract class RecentProjectsManager {
  @Nonnull
  public static RecentProjectsManager getInstance() {
    return Application.get().getInstance(RecentProjectsManager.class);
  }

  @Nullable
  public abstract String getLastProjectCreationLocation();

  public abstract void setLastProjectCreationLocation(@Nullable String lastProjectLocation);

  public abstract void updateProjectModuleExtensions(@Nonnull Project project);

  public abstract void updateLastProjectPath();

  public abstract String getLastProjectPath();

  public abstract void removePath(@Nullable String path);

  /**
   * @param addClearListItem whether the "Clear List" action should be added to the end of the list.
   */
  public abstract AnAction[] getRecentProjectsActions(boolean addClearListItem);

  public AnAction[] getRecentProjectsActions(boolean addClearListItem, boolean useGroups) {
    return getRecentProjectsActions(addClearListItem);
  }

  public List<ProjectGroup> getGroups() {
    return Collections.emptyList();
  }

  public void addGroup(ProjectGroup group) {
  }

  public void removeGroup(ProjectGroup group) {
  }

  public boolean hasPath(String path) {
    return false;
  }
}