/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.execution.test.action;

import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.execution.test.autotest.AutoTestManager;
import consulo.execution.ui.RunContentDescriptor;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

/**
 * @author Dennis.Ushakov
 */
public class AdjustAutotestDelayActionGroup extends ActionGroup {
  public static final int MAX_DELAY = 10;
  private final DataContext myDataContext;

  public AdjustAutotestDelayActionGroup(@Nonnull JComponent parent) {
    super("Set AutoTest Delay", true);
    myDataContext = DataManager.getInstance().getDataContext(parent);
  }

  @RequiredUIAccess
  @Override
  public void update(AnActionEvent e) {
    RunContentDescriptor descriptor = myDataContext.getData(RunContentDescriptor.KEY);
    boolean visible = false;
    if (descriptor != null) {
      for (AnAction action : descriptor.getRestartActions()) {
        if (action instanceof ToggleAutoTestAction) {
          visible = ((ToggleAutoTestAction)action).isDelayApplicable();
          break;
        }
      }
    }
    e.getPresentation().setVisible(visible);
  }

  @Nonnull
  @Override
  public AnAction[] getChildren(@Nullable AnActionEvent e) {
    final AnAction[] actions = new AnAction[MAX_DELAY];
    for (int i = 0; i < MAX_DELAY; i++) {
      actions[i] = new SetAutoTestDelayAction(i + 1);
    }
    return actions;
  }

  private static class SetAutoTestDelayAction extends ToggleAction {
    private final int myDelay;

    public SetAutoTestDelayAction(int delay) {
      super(delay + "s");
      myDelay = delay * 1000;
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
      Project project = e.getData(Project.KEY);
      return project != null && AutoTestManager.getInstance(project).getDelay() == myDelay;
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
      Project project = e.getData(Project.KEY);
      if (project != null) {
        AutoTestManager.getInstance(project).setDelay(myDelay);
      }
    }
  }
}
