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
package consulo.ide.impl.idea.openapi.vcs.checkout;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.application.dumb.DumbAware;
import consulo.versionControlSystem.checkout.CheckoutProvider;
import consulo.versionControlSystem.ProjectLevelVcsManager;

public class CheckoutAction extends AnAction implements DumbAware {
  private final CheckoutProvider myProvider;

  public CheckoutAction(final CheckoutProvider provider) {
    myProvider = provider;
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    project = (project == null) ? ProjectManager.getInstance().getDefaultProject() : project;
    myProvider.doCheckout(project, ProjectLevelVcsManager.getInstance(project).getCompositeCheckoutListener());
  }

  @Override
  @RequiredUIAccess
  public void update(AnActionEvent e) {
    super.update(e);
    e.getPresentation().setText(myProvider.getVcsName(), true);
  }
}
