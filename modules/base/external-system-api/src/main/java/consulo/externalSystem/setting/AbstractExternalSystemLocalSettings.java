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
package consulo.externalSystem.setting;

import consulo.annotation.access.RequiredReadAction;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.execution.ExternalTaskExecutionInfo;
import consulo.externalSystem.model.execution.ExternalTaskPojo;
import consulo.externalSystem.model.project.ExternalProjectBuildClasspathPojo;
import consulo.externalSystem.model.project.ExternalProjectPojo;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.externalSystem.util.ExternalSystemConstants;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds local project-level external system-related settings (should be kept at the '*.iws' or 'workspace.xml').
 * <p/>
 * For example, we don't want to store recent tasks list at common external system settings, hence, that data
 * is kept at user-local settings (workspace settings).
 * <p/>
 * <b>Note:</b> non-abstract sub-classes of this class are expected to be marked by {@link State} annotation configured
 * to be stored under a distinct name at a workspace file.
 *
 * @author Denis Zhdanov
 * @since 4/4/13 4:51 PM
 */
public abstract class AbstractExternalSystemLocalSettings {
  private final AtomicReference<Map<String/*tree path*/, Boolean/*expanded*/>> myExpandStates = new AtomicReference<Map<String, Boolean>>(new HashMap<String, Boolean>());
  private final AtomicReference<List<ExternalTaskExecutionInfo>> myRecentTasks = new AtomicReference<List<ExternalTaskExecutionInfo>>(new ArrayList<>());
  private final AtomicReference<Map<ExternalProjectPojo, Collection<ExternalProjectPojo>>> myAvailableProjects =
          new AtomicReference<Map<ExternalProjectPojo, Collection<ExternalProjectPojo>>>(new HashMap<>());
  private final AtomicReference<Map<String/* external project config path */, Collection<ExternalTaskPojo>>> myAvailableTasks =
          new AtomicReference<Map<String, Collection<ExternalTaskPojo>>>(new HashMap<>());
  private final AtomicReference<Map<String/* external project config path */, ExternalProjectBuildClasspathPojo>> myProjectBuildClasspath =
          new AtomicReference<Map<String, ExternalProjectBuildClasspathPojo>>(new HashMap<>());
  private final AtomicReference<Map<String/* external project config path */, Long>> myExternalConfigModificationStamps = new AtomicReference<Map<String, Long>>(new HashMap<>());

  @Nonnull
  private final ProjectSystemId myExternalSystemId;
  @Nonnull
  private final Project myProject;

  protected AbstractExternalSystemLocalSettings(@Nonnull ProjectSystemId externalSystemId, @Nonnull Project project) {
    myExternalSystemId = externalSystemId;
    myProject = project;
  }

  /**
   * Asks current settings to drop all information related to external projects which root configs are located at the given paths.
   *
   * @param linkedProjectPathsToForget target root external project paths
   */
  public void forgetExternalProjects(@Nonnull Set<String> linkedProjectPathsToForget) {
    Map<ExternalProjectPojo, Collection<ExternalProjectPojo>> projects = myAvailableProjects.get();
    for (Iterator<Map.Entry<ExternalProjectPojo, Collection<ExternalProjectPojo>>> it = projects.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<ExternalProjectPojo, Collection<ExternalProjectPojo>> entry = it.next();
      if (linkedProjectPathsToForget.contains(entry.getKey().getPath())) {
        it.remove();
      }
    }

    for (Iterator<Map.Entry<String, Collection<ExternalTaskPojo>>> it = myAvailableTasks.get().entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<String, Collection<ExternalTaskPojo>> entry = it.next();
      if (linkedProjectPathsToForget.contains(entry.getKey()) || linkedProjectPathsToForget.contains(ExternalSystemApiUtil.getRootProjectPath(entry.getKey(), myExternalSystemId, myProject))) {
        it.remove();
      }
    }

    for (Iterator<ExternalTaskExecutionInfo> it = myRecentTasks.get().iterator(); it.hasNext(); ) {
      ExternalTaskExecutionInfo taskInfo = it.next();
      String path = taskInfo.getSettings().getExternalProjectPath();
      if (linkedProjectPathsToForget.contains(path) || linkedProjectPathsToForget.contains(ExternalSystemApiUtil.getRootProjectPath(path, myExternalSystemId, myProject))) {
        it.remove();
      }
    }

    for (Iterator<Map.Entry<String, ExternalProjectBuildClasspathPojo>> it = myProjectBuildClasspath.get().entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<String, ExternalProjectBuildClasspathPojo> entry = it.next();
      if (linkedProjectPathsToForget.contains(entry.getKey()) || linkedProjectPathsToForget.contains(ExternalSystemApiUtil.getRootProjectPath(entry.getKey(), myExternalSystemId, myProject))) {
        it.remove();
      }
    }

    Map<String, Long> modificationStamps = myExternalConfigModificationStamps.get();
    for (String path : linkedProjectPathsToForget) {
      modificationStamps.remove(path);
    }
  }

  @SuppressWarnings("UnusedDeclaration")
  @Nonnull
  public Map<String, Boolean> getExpandStates() { // Necessary for the serialization.
    return myExpandStates.get();
  }

  @Nonnull
  public Map<ExternalProjectPojo, Collection<ExternalProjectPojo>> getAvailableProjects() {
    return myAvailableProjects.get();
  }

  public void setAvailableProjects(@Nonnull Map<ExternalProjectPojo, Collection<ExternalProjectPojo>> projects) {
    myAvailableProjects.set(projects);
  }

  @Nonnull
  public Map<String, Collection<ExternalTaskPojo>> getAvailableTasks() {
    return myAvailableTasks.get();
  }

  public void setAvailableTasks(@Nonnull Map<String, Collection<ExternalTaskPojo>> tasks) {
    myAvailableTasks.set(tasks);
  }

  @Nonnull
  public List<ExternalTaskExecutionInfo> getRecentTasks() {
    return myRecentTasks.get();
  }

  public void setRecentTasks(@Nonnull List<ExternalTaskExecutionInfo> tasks) {
    myRecentTasks.set(tasks);
  }

  @Nonnull
  public Map<String, Long> getExternalConfigModificationStamps() {
    return myExternalConfigModificationStamps.get();
  }

  @SuppressWarnings("UnusedDeclaration")
  public void setExternalConfigModificationStamps(@Nonnull Map<String, Long> modificationStamps) {
    // Required for IJ serialization.
    myExternalConfigModificationStamps.set(modificationStamps);
  }

  @Nonnull
  public Map<String, ExternalProjectBuildClasspathPojo> getProjectBuildClasspath() {
    return myProjectBuildClasspath.get();
  }

  @SuppressWarnings("UnusedDeclaration")
  public void setProjectBuildClasspath(@Nonnull Map<String, ExternalProjectBuildClasspathPojo> projectsBuildClasspath) {
    // Required for IJ serialization.
    myProjectBuildClasspath.set(projectsBuildClasspath);
  }

  public void fillState(@Nonnull State state) {
    state.tasksExpandState = myExpandStates.get();
    state.recentTasks = myRecentTasks.get();
    state.availableProjects = myAvailableProjects.get();
    state.availableTasks = myAvailableTasks.get();
    state.modificationStamps = myExternalConfigModificationStamps.get();
    state.projectBuildClasspath = myProjectBuildClasspath.get();
  }

  public void loadState(@Nonnull State state) {
    setIfNotNull(myExpandStates, state.tasksExpandState);
    setIfNotNull(myAvailableProjects, state.availableProjects);
    setIfNotNull(myAvailableTasks, state.availableTasks);
    setIfNotNull(myExternalConfigModificationStamps, state.modificationStamps);
    setIfNotNull(myProjectBuildClasspath, state.projectBuildClasspath);
    if (state.recentTasks != null) {
      List<ExternalTaskExecutionInfo> recentTasks = myRecentTasks.get();
      if (recentTasks != state.recentTasks) {
        recentTasks.clear();
        recentTasks.addAll(state.recentTasks);
      }
    }
    pruneOutdatedEntries();
  }

  @RequiredReadAction
  private void pruneOutdatedEntries() {
    ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(myExternalSystemId);
    assert manager != null;
    Set<String> pathsToForget = new HashSet<>();
    for (ExternalProjectPojo pojo : myAvailableProjects.get().keySet()) {
      pathsToForget.add(pojo.getPath());
    }
    for (String path : myAvailableTasks.get().keySet()) {
      pathsToForget.add(path);
    }
    for (ExternalTaskExecutionInfo taskInfo : myRecentTasks.get()) {
      pathsToForget.add(taskInfo.getSettings().getExternalProjectPath());
    }

    AbstractExternalSystemSettings<?, ?, ?> settings = manager.getSettingsProvider().apply(myProject);
    for (ExternalProjectSettings projectSettings : settings.getLinkedProjectsSettings()) {
      pathsToForget.remove(projectSettings.getExternalProjectPath());
    }
    for (Module module : ModuleManager.getInstance(myProject).getModules()) {
      String id = ExternalSystemApiUtil.getExtensionSystemOption(module, ExternalSystemConstants.EXTERNAL_SYSTEM_ID_KEY);
      if (!myExternalSystemId.toString().equals(id)) {
        continue;
      }
      pathsToForget.remove(ExternalSystemApiUtil.getExtensionSystemOption(module, ExternalSystemConstants.LINKED_PROJECT_PATH_KEY));
    }

    if (!pathsToForget.isEmpty()) {
      forgetExternalProjects(pathsToForget);
    }
  }

  private static <K, V> void setIfNotNull(@Nonnull AtomicReference<Map<K, V>> ref, @Nullable Map<K, V> candidate) {
    if (candidate == null) {
      return;
    }
    Map<K, V> map = ref.get();
    if (candidate != map) {
      map.clear();
      map.putAll(candidate);
    }
  }

  public static class State {
    public Map<String, Boolean> tasksExpandState = new HashMap<>();
    public List<ExternalTaskExecutionInfo> recentTasks = new ArrayList<>();
    public Map<ExternalProjectPojo, Collection<ExternalProjectPojo>> availableProjects = new HashMap<>();
    public Map<String/* project name */, Collection<ExternalTaskPojo>> availableTasks = new HashMap<>();

    public Map<String/* linked project path */, Long/* last config modification stamp */> modificationStamps = new HashMap<>();
    public Map<String/* linked project path */, ExternalProjectBuildClasspathPojo> projectBuildClasspath = new HashMap<>();
  }
}
