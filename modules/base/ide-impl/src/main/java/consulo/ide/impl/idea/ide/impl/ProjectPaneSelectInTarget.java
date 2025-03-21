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

package consulo.ide.impl.idea.ide.impl;

import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.ide.projectView.impl.ProjectViewPaneImpl;
import consulo.language.psi.PsiFileSystemItem;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.view.SelectInContext;
import consulo.project.ui.view.StandardTargetWeights;
import consulo.project.ui.view.localize.ProjectUIViewLocalize;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

public class ProjectPaneSelectInTarget extends ProjectViewSelectInTarget implements DumbAware {
  public ProjectPaneSelectInTarget(Project project) {
    super(project);
  }

  @Nonnull
  @Override
  public LocalizeValue getActionText() {
    return ProjectUIViewLocalize.selectInProject();
  }

  @Override
  public boolean canSelect(PsiFileSystemItem file) {
    if (!super.canSelect(file)) return false;
    final VirtualFile vFile = file.getVirtualFile();
    return vFile != null && ProjectViewPaneImpl.canBeSelectedInProjectView(myProject, vFile);
  }

  @Override
  public boolean isSubIdSelectable(String subId, SelectInContext context) {
    return canSelect(context);
  }

  @Override
  public String getMinorViewId() {
    return ProjectViewPaneImpl.ID;
  }

  @Override
  public float getWeight() {
    return StandardTargetWeights.PROJECT_WEIGHT;
  }

  @Override
  protected boolean canWorkWithCustomObjects() {
    return false;
  }
}
