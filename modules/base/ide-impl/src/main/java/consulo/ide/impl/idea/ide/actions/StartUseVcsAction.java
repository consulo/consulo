/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.actions;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.ui.ex.action.Presentation;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsBundle;
import consulo.ide.impl.idea.openapi.vcs.impl.ProjectLevelVcsManagerImpl;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nullable;

public class StartUseVcsAction extends AnAction implements DumbAware {
  public StartUseVcsAction() {
    super(VcsBundle.message("action.enable.version.control.integration.text"));
  }

  @Override
  @RequiredUIAccess
  public void update(final AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    boolean enabled = isEnabled(project);

    final Presentation presentation = e.getPresentation();
    presentation.setEnabledAndVisible(enabled);
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(final AnActionEvent e) {
    Project project = e.getRequiredData(CommonDataKeys.PROJECT);
    if (!isEnabled(project)) {
      return;
    }

    final StartUseVcsDialog dialog = new StartUseVcsDialog(project);
    if (dialog.showAndGet()) {
      AbstractVcs vcs = dialog.getSelectedVcs();
      vcs.enableIntegration();
    }
  }

  private static boolean isEnabled(@Nullable Project project) {
    if (project == null) return false;
    ProjectLevelVcsManagerImpl manager = (ProjectLevelVcsManagerImpl)ProjectLevelVcsManager.getInstance(project);
    return manager.haveVcses() && !manager.hasAnyMappings();
  }
}
