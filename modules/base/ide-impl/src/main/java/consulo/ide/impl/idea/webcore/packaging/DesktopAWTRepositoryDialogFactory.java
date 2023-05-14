/*
 * Copyright 2013-2023 consulo.io
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
package consulo.ide.impl.idea.webcore.packaging;

import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import consulo.repository.ui.PackageManagementService;
import consulo.repository.ui.RepositoryDialogFactory;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 07/01/2023
 */
@Singleton
@ServiceImpl
public class DesktopAWTRepositoryDialogFactory implements RepositoryDialogFactory {
  private final Project myProject;

  @Inject
  public DesktopAWTRepositoryDialogFactory(Project project) {
    myProject = project;
  }

  @Override
  @RequiredUIAccess
  public void showManagePackagesDialogAsync(@Nonnull PackageManagementService service, @Nullable PackageManagementService.Listener listener) {
    ManagePackagesDialog dialog = new ManagePackagesDialog(myProject, service, listener);
    dialog.showAsync();
  }
}
