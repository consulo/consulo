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

import consulo.versionControlSystem.distributed.DvcsUtil;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.application.AllIcons;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import java.util.List;

public abstract class NewBranchAction<T extends Repository> extends DumbAwareAction {
  protected final List<T> myRepositories;
  protected final Project myProject;

  public NewBranchAction(@Nonnull Project project, @Nonnull List<T> repositories) {
    super("New Branch", "Create and checkout new branch", AllIcons.General.Add);
    myRepositories = repositories;
    myProject = project;
  }


  @Override
  public void update(AnActionEvent e) {
    if (DvcsUtil.anyRepositoryIsFresh(myRepositories)) {
      e.getPresentation().setEnabled(false);
      e.getPresentation().setDescription("Checkout of a new branch is not possible before the first commit");
    }
  }

  @Override
  public abstract void actionPerformed(AnActionEvent e);
}
