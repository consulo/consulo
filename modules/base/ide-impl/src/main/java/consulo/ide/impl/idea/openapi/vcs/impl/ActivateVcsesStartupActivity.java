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
package consulo.ide.impl.idea.openapi.vcs.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsInitObject;
import consulo.versionControlSystem.VcsStartupActivity;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

@ExtensionImpl
final class ActivateVcsesStartupActivity implements VcsStartupActivity {
  private final Project myProject;
  private final Provider<VcsDirectoryMappingStorage> myDirectoryMappingStorageProvider;

  @Inject
  ActivateVcsesStartupActivity(Project project, Provider<VcsDirectoryMappingStorage> directoryMappingStorageProvider) {
    myProject = project;
    myDirectoryMappingStorageProvider = directoryMappingStorageProvider;
  }

  @Override
  public void runActivity() {
    // init VcsDirectoryMappingStorage, and load data
    myDirectoryMappingStorageProvider.get();

    ProjectLevelVcsManagerImpl projectLevelVcsManager = (ProjectLevelVcsManagerImpl)ProjectLevelVcsManager.getInstance(myProject);
    projectLevelVcsManager.activateActiveVcses();
  }

  @Override
  public int getOrder() {
    return VcsInitObject.MAPPINGS.getOrder();
  }
}
