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
package consulo.ide.impl.idea.dvcs.ui;

import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.versionControlSystem.distributed.DvcsUtil;
import consulo.versionControlSystem.distributed.repository.Repository;
import jakarta.annotation.Nonnull;

import java.util.List;

public abstract class NewBranchAction<T extends Repository> extends DumbAwareAction {
  protected final List<T> myRepositories;
  protected final Project myProject;

  public NewBranchAction(@Nonnull Project project, @Nonnull List<T> repositories) {
    super(
      LocalizeValue.localizeTODO("New Branch"),
      LocalizeValue.localizeTODO("Create and checkout new branch"),
      PlatformIconGroup.generalAdd()
    );
    myRepositories = repositories;
    myProject = project;
  }


  @Override
  public void update(@Nonnull AnActionEvent e) {
    if (DvcsUtil.anyRepositoryIsFresh(myRepositories)) {
      e.getPresentation().setEnabled(false);
      e.getPresentation().setDescriptionValue(LocalizeValue.localizeTODO("Checkout of a new branch is not possible before the first commit"));
    }
  }

  @Override
  @RequiredUIAccess
  public abstract void actionPerformed(@Nonnull AnActionEvent e);
}
