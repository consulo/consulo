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
package consulo.ide.impl.idea.openapi.vcs.update;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.openapi.project.ProjectReloadState;
import consulo.ide.impl.idea.openapi.vcs.VcsBundle;
import consulo.ide.impl.idea.openapi.vcs.changes.committed.CommittedChangesCache;
import consulo.ide.impl.idea.openapi.vcs.ex.ProjectLevelVcsManagerEx;
import consulo.project.Project;
import consulo.project.startup.PostStartupActivity;
import consulo.ui.UIAccess;

import javax.annotation.Nonnull;

@ExtensionImpl
public class RestoreUpdateTreeStartUpActivity implements PostStartupActivity, DumbAware {
  @Override
  public void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
    RestoreUpdateTree restoreUpdateTree = RestoreUpdateTree.getInstance(project);
    UpdateInfo updateInfo = restoreUpdateTree.myUpdateInfo;

    if (updateInfo != null && !updateInfo.isEmpty() && ProjectReloadState.getInstance(project).isAfterAutomaticReload()) {
      ActionInfo actionInfo = updateInfo.getActionInfo();
      if (actionInfo != null) {
        ProjectLevelVcsManagerEx.getInstanceEx(project).showUpdateProjectInfo(updateInfo.getFileInformation(), VcsBundle.message("action.display.name.update"), actionInfo, false);

        CommittedChangesCache.getInstance(project).refreshIncomingChangesAsync();
      }
      restoreUpdateTree.myUpdateInfo = null;
    }
    else {
      restoreUpdateTree.myUpdateInfo = null;
    }
  }
}
