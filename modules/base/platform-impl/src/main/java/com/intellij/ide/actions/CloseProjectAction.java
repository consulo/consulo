/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import com.intellij.ide.RecentProjectsManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import consulo.start.WelcomeFrameManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.UIAccess;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;

public class CloseProjectAction extends AnAction implements DumbAware {
  private WelcomeFrameManager myWelcomeFrameManager;
  private final ProjectManager myProjectManager;
  private final RecentProjectsManager myRecentProjectsManager;

  @Inject
  public CloseProjectAction(WelcomeFrameManager welcomeFrameManager, ProjectManager projectManager, RecentProjectsManager recentProjectsManager) {
    myWelcomeFrameManager = welcomeFrameManager;
    myProjectManager = projectManager;
    myRecentProjectsManager = recentProjectsManager;
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent event) {
    Project project = event.getRequiredData(CommonDataKeys.PROJECT);

    myProjectManager.closeAndDisposeAsync(project, UIAccess.current()).doWhenDone(() -> {
      myRecentProjectsManager.updateLastProjectPath();
      myWelcomeFrameManager.showIfNoProjectOpened();
    });
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    Project project = event.getData(CommonDataKeys.PROJECT);
    presentation.setEnabled(project != null);
  }
}
