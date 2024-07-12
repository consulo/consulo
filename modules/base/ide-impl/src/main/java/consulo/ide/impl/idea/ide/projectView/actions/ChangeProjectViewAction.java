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

package consulo.ide.impl.idea.ide.projectView.actions;

import consulo.project.ui.view.ProjectView;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

public final class ChangeProjectViewAction extends AnAction {
  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getDataContext().getData(Project.KEY);
    if (project == null) {
      return;
    }
    ProjectView projectView = ProjectView.getInstance(project);
    projectView.changeView();
  }

  @Override
  @RequiredUIAccess
  public void update(AnActionEvent event){
    Presentation presentation = event.getPresentation();
    Project project = event.getDataContext().getData(Project.KEY);
    if (project == null){
      presentation.setEnabled(false);
      return;
    }
    String id = ToolWindowManager.getInstance(project).getActiveToolWindowId();
    presentation.setEnabled(ToolWindowId.PROJECT_VIEW.equals(id));
  }
}
