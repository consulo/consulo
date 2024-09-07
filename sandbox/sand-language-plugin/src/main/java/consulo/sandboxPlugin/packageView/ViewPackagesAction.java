/*
 * Copyright 2013-2021 consulo.io
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
package consulo.sandboxPlugin.packageView;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.project.Project;
import consulo.repository.ui.RepositoryDialogFactory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.IdeActions;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 30/05/2021
 */
@ActionImpl(id = "ViewPackagesAction", parents = @ActionParentRef(@ActionRef(id = IdeActions.TOOLS_MENU)))
public class ViewPackagesAction extends DumbAwareAction {
  private final SandPackageManagementService myManagementService;

  @Inject
  public ViewPackagesAction(SandPackageManagementService managementService) {
    myManagementService = managementService;
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    if (project == null) {
      return;
    }

    RepositoryDialogFactory dialogFactory = project.getInstance(RepositoryDialogFactory.class);

    dialogFactory.showManagePackagesDialogAsync(myManagementService, null);
  }
}
