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
package consulo.ide.impl.idea.openapi.wm.impl;

import consulo.application.dumb.DumbAware;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @ author Bas Leijdekkers
 * This class is programmatically instantiated and registered when opening and closing projects
 * and thus not registered in plugin.xml
 */
@SuppressWarnings({"ComponentNotRegistered"})
public class ProjectWindowAction extends ToggleAction implements DumbAware {

  private ProjectWindowAction myPrevious;
  private ProjectWindowAction myNext;
  @Nonnull
  private final String myProjectName;
  @Nonnull
  private final String myProjectLocation;

  public ProjectWindowAction(@Nonnull String projectName, @Nonnull String projectLocation, ProjectWindowAction previous) {
    super();
    myProjectName = projectName;
    myProjectLocation = projectLocation;
    if (previous != null) {
      myPrevious = previous;
      myNext = previous.myNext;
      myNext.myPrevious = this;
      myPrevious.myNext = this;
    } else {
      myPrevious = this;
      myNext = this;
    }
    getTemplatePresentation().setText(projectName, false);
  }

  public void dispose() {
    if (myPrevious == this) {
      assert myNext == this;
      return;
    }
    if (myNext == this) {
      assert false;
      return;
    }
    myPrevious.myNext = myNext;
    myNext.myPrevious = myPrevious;
  }

  public ProjectWindowAction getPrevious() {
    return myPrevious;
  }

  public ProjectWindowAction getNext() {
    return myNext;
  }

  @Nonnull
  public String getProjectLocation() {
    return myProjectLocation;
  }

  @Nonnull
  public String getProjectName() {
    return myProjectName;
  }

  @Nullable
  private Project findProject() {
    final Project[] projects = ProjectManager.getInstance().getOpenProjects();
    for (Project project : projects) {
      if (myProjectLocation.equals(project.getPresentableUrl())) {
        return project;
      }
    }
    return null;
  }

  @Override
  public boolean isSelected(AnActionEvent e) {
    // show check mark for active and visible project frame
    final Project project = e.getData(Project.KEY);
    return project != null && myProjectLocation.equals(project.getPresentableUrl());
  }

  @Override
  public void setSelected(@Nullable AnActionEvent e, boolean selected) {
    if (!selected) {
      return;
    }
    final Project project = findProject();
    if (project == null) {
      return;
    }
    final JFrame projectFrame = (JFrame)TargetAWT.to(WindowManager.getInstance().getWindow(project));
    final int frameState = projectFrame.getExtendedState();
    if ((frameState & Frame.ICONIFIED) == Frame.ICONIFIED) {
      // restore the frame if it is minimized
      projectFrame.setExtendedState(frameState ^ Frame.ICONIFIED);
    }
    projectFrame.toFront();
    IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(projectFrame);
    //ProjectUtil.focusProjectWindow(project, true);
  }

  @Override
  public String toString() {
    return getTemplatePresentation().getText() + " previous: " + myPrevious.getTemplatePresentation().getText() + " next: " + myNext.getTemplatePresentation().getText();
  }
}