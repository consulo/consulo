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
package consulo.execution.action;

import consulo.application.AllIcons;
import consulo.application.dumb.DumbAware;
import consulo.execution.ExecutionManager;
import consulo.execution.executor.Executor;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.ui.RunContentDescriptor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import jakarta.annotation.Nonnull;

public class CloseAction extends AnAction implements DumbAware {
  private RunContentDescriptor myContentDescriptor;
  private final Project myProject;
  private Executor myExecutor;

  public CloseAction(Executor executor, RunContentDescriptor contentDescriptor, Project project) {
    myExecutor = executor;
    myContentDescriptor = contentDescriptor;
    myProject = project;
    copyFrom(ActionManager.getInstance().getAction(IdeActions.ACTION_CLOSE));
    final Presentation templatePresentation = getTemplatePresentation();
    templatePresentation.setIcon(AllIcons.Actions.Cancel);
    templatePresentation.setTextValue(ExecutionLocalize.closeTabActionName());
    templatePresentation.setDescriptionValue(LocalizeValue.empty());
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final RunContentDescriptor contentDescriptor = getContentDescriptor();
    if (contentDescriptor == null) {
      return;
    }
    final boolean removedOk = ExecutionManager.getInstance(myProject).getContentManager().removeRunContent(getExecutor(), contentDescriptor);
    if (removedOk) {
      myContentDescriptor = null;
      myExecutor = null;
    }
  }

  public RunContentDescriptor getContentDescriptor() {
    return myContentDescriptor;
  }

  public Executor getExecutor() {
    return myExecutor;
  }

  @Override
  public void update(AnActionEvent e) {
    e.getPresentation().setEnabled(myContentDescriptor != null);
  }
}
