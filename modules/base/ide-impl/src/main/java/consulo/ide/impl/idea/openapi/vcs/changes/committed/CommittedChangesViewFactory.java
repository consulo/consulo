/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.idea.openapi.vcs.changes.committed;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.ChangesViewContentFactory;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.ChangesViewContentProvider;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsType;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 08-Aug-22
 */
@ExtensionImpl
public class CommittedChangesViewFactory implements ChangesViewContentFactory {
  private final Project myProject;

  @Inject
  public CommittedChangesViewFactory(Project project) {
    myProject = project;
  }

  @Nonnull
  @Override
  public LocalizeValue getTabName() {
    return LocalizeValue.localizeTODO("Repository");
  }

  @Nonnull
  @Override
  public ChangesViewContentProvider create() {
    return new CommittedChangesViewManager(myProject, ProjectLevelVcsManager.getInstance(myProject));
  }

  @Override
  public boolean isAvailable() {
    final AbstractVcs[] abstractVcses = ProjectLevelVcsManager.getInstance(myProject).getAllActiveVcss();
    for (AbstractVcs vcs : abstractVcses) {
      if (vcs.getCommittedChangesProvider() != null && VcsType.centralized.equals(vcs.getType())) {
        return true;
      }
    }
    return false;
  }
}
