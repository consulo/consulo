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
package consulo.versionControlSystem.impl.internal.update;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.project.internal.ProjectReloadState;
import consulo.project.startup.PostStartupActivity;
import consulo.ui.UIAccess;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.impl.internal.change.commited.CommittedChangesCache;
import consulo.versionControlSystem.internal.ProjectLevelVcsManagerEx;
import consulo.versionControlSystem.update.ActionInfo;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class RestoreUpdateTreeStartUpActivity implements PostStartupActivity, DumbAware {
  @Override
  public void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
    RestoreUpdateTree restoreUpdateTree = RestoreUpdateTree.getInstance(project);
    UpdateInfo updateInfo = restoreUpdateTree.myUpdateInfo;

    if (updateInfo != null && !updateInfo.isEmpty() && ProjectReloadState.getInstance(project).isAfterAutomaticReload()) {
      ActionInfo actionInfo = updateInfo.getActionInfo();
      if (actionInfo != null) {
        ProjectLevelVcsManager projectLevelVcsManager = ProjectLevelVcsManagerEx.getInstanceEx(project);

        projectLevelVcsManager.showUpdateProjectInfo(updateInfo.getFileInformation(),
                                         VcsBundle.message("action.display.name.update"),
                                         actionInfo,
                                         false);

        CommittedChangesCache.getInstance(project).refreshIncomingChangesAsync();
      }
      restoreUpdateTree.myUpdateInfo = null;
    }
    else {
      restoreUpdateTree.myUpdateInfo = null;
    }
  }
}
