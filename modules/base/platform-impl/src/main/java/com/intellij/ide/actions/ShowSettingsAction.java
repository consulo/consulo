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

import com.intellij.CommonBundle;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.SystemInfo;
import jakarta.inject.Inject;

public class ShowSettingsAction extends AnAction implements DumbAware {
  private final ShowSettingsUtil myShowSettingsUtil;

  @Inject
  public ShowSettingsAction(ShowSettingsUtil showSettingsUtil) {
    super(CommonBundle.settingsAction(), CommonBundle.settingsActionDescription(), AllIcons.General.Settings);
    myShowSettingsUtil = showSettingsUtil;
  }

  @Override
  public void update(AnActionEvent e) {
    if (SystemInfo.isMac && e.getPlace().equals(ActionPlaces.MAIN_MENU)) {
      // It's called from Preferences in App menu.
      e.getPresentation().setVisible(false);
    }
    if (e.getPlace().equals(ActionPlaces.WELCOME_SCREEN)) {
      e.getPresentation().setText(CommonBundle.settingsTitle());
    }
  }

  public void actionPerformed(AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      project = ProjectManager.getInstance().getDefaultProject();
    }

    myShowSettingsUtil.showSettingsDialog(project);
  }
}
