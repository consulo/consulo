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
package consulo.ide.impl.idea.execution.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.impl.internal.configuration.ProjectRunConfigurationManager;
import consulo.project.Project;
import consulo.project.startup.PostStartupActivity;
import consulo.ui.UIAccess;

import jakarta.annotation.Nonnull;
import java.util.List;

@ExtensionImpl
public class ProjectRunConfigurationManagerStartupActivity implements PostStartupActivity, DumbAware {

  @Override
  public void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
    ProjectRunConfigurationManager manager = project.getInstance(ProjectRunConfigurationManager.class);

    // just initialize it project run manager (load shared runs)

    RunManager runManager = RunManager.getInstance(project);
    RunnerAndConfigurationSettings selectedConfiguration = runManager.getSelectedConfiguration();
    if (selectedConfiguration == null) {
      List<RunnerAndConfigurationSettings> settings = runManager.getAllSettings();
      if (!settings.isEmpty()) {
        runManager.setSelectedConfiguration(settings.get(0));
      }
    }
  }
}
