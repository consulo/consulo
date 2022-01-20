/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.wm.impl;

import com.intellij.openapi.actionSystem.ActionManager;
import consulo.application.Application;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.event.ProjectManagerListener;
import consulo.ui.UIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

/**
 * @author Bas Leijdekkers
 */
@Singleton
public class WindowDressing {
  @Inject
  public WindowDressing(@Nonnull Application application, @Nonnull ActionManager actionManager) {
    application.getMessageBus().connect().subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
      @Override
      public void projectOpened(Project project, UIAccess uiAccess) {
        getWindowActionGroup(actionManager).addProject(project);
      }

      @Override
      public void projectClosed(Project project, UIAccess uiAccess) {
        getWindowActionGroup(actionManager).removeProject(project);
      }
    });
  }

  public static ProjectWindowActionGroup getWindowActionGroup(ActionManager actionManager1) {
    return (ProjectWindowActionGroup)actionManager1.getAction("OpenProjectWindows");
  }
}
