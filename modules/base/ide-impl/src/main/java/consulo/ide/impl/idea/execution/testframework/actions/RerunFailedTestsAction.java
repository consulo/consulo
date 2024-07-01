/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.execution.testframework.actions;

import consulo.dataContext.DataManager;
import consulo.execution.ExecutionManager;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.test.action.AbstractRerunFailedTestsAction;
import consulo.execution.ui.RunContentDescriptor;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;

import jakarta.annotation.Nonnull;
import javax.swing.*;

public class RerunFailedTestsAction extends AnAction {
  @RequiredUIAccess
  @Override
  public void update(AnActionEvent e) {
    e.getPresentation().setEnabled(getAction(e, false));
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    getAction(e, true);
  }

  private static boolean getAction(@Nonnull AnActionEvent e, boolean execute) {
    Project project = e.getData(Project.KEY);
    if (project == null) {
      return false;
    }

    RunContentDescriptor contentDescriptor = ExecutionManager.getInstance(project).getContentManager().getSelectedContent();
    if (contentDescriptor == null) {
      return false;
    }

    JComponent component = contentDescriptor.getComponent();
    if (component == null) {
      return false;
    }

    ExecutionEnvironment environment = DataManager.getInstance().getDataContext(component).getData(ExecutionEnvironment.KEY);
    if (environment == null) {
      return false;
    }

    AnAction[] actions = contentDescriptor.getRestartActions();
    if (actions.length == 0) {
      return false;
    }

    for (AnAction action : actions) {
      if (action instanceof AbstractRerunFailedTestsAction) {
        if (execute) {                                                                         
          ((AbstractRerunFailedTestsAction)action).execute(e, environment);
        }
        return true;
      }
    }
    return false;
  }
}
