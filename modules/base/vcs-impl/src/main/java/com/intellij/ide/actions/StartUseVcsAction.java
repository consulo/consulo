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
package com.intellij.ide.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsBundle;
import com.intellij.openapi.vcs.impl.ProjectLevelVcsManagerImpl;
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
