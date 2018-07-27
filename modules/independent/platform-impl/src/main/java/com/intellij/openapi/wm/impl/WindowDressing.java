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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import consulo.annotations.NotLazy;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Bas Leijdekkers
 */
@Singleton
@NotLazy
public class WindowDressing {

  private final Project myProject;

  @Inject
  public WindowDressing(@Nonnull Project project) {
    myProject = project;

    project.getMessageBus().connect().subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
      @Override
      public void projectOpened(Project project) {
        if (myProject == project) {
          WindowDressing.this.projectOpened();
        }
      }

      @Override
      public void projectClosed(Project project) {
        if (myProject == project) {
          WindowDressing.this.projectClosed();
        }
      }
    });
  }

  private void projectOpened() {
    getWindowActionGroup().addProject(myProject);
  }

  private void projectClosed() {
    getWindowActionGroup().removeProject(myProject);
  }

  public static ProjectWindowActionGroup getWindowActionGroup() {
    return (ProjectWindowActionGroup)ActionManager.getInstance().getAction("OpenProjectWindows");
  }
}
