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

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.ide.CompositeSelectInTarget;
import consulo.ide.impl.idea.ide.projectView.ProjectView;
import consulo.ide.impl.idea.openapi.util.Comparing;
import consulo.project.ui.view.SelectInContext;
import consulo.project.ui.view.SelectInTarget;
import consulo.project.ui.wm.ToolWindowId;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * @author yole
 */
@ExtensionImpl
public class ProjectViewSelectInGroupTarget implements CompositeSelectInTarget, DumbAware {
  @Override
  @Nonnull
  public Collection<SelectInTarget> getSubTargets(SelectInContext context) {
    return ProjectView.getInstance(context.getProject()).getSelectInTargets();
  }

  @Override
  public boolean canSelect(SelectInContext context) {
    ProjectView projectView = ProjectView.getInstance(context.getProject());
    Collection<SelectInTarget> targets = projectView.getSelectInTargets();
    for (SelectInTarget projectViewTarget : targets) {
      if (projectViewTarget.canSelect(context)) return true;
    }
    return false;
  }

  @Override
  public void selectIn(final SelectInContext context, final boolean requestFocus) {
    ProjectView projectView = ProjectView.getInstance(context.getProject());
    Collection<SelectInTarget> targets = projectView.getSelectInTargets();
    Collection<SelectInTarget> targetsToCheck = new LinkedHashSet<>();
    String currentId = projectView.getCurrentViewId();
    for (SelectInTarget projectViewTarget : targets) {
      if (Comparing.equal(currentId, projectViewTarget.getMinorViewId())) {
        targetsToCheck.add(projectViewTarget);
        break;
      }
    }
    targetsToCheck.addAll(targets);
    for (SelectInTarget target : targetsToCheck) {
      if (context.selectIn(target, requestFocus)) break;
    }
  }

  @Override
  public String getToolWindowId() {
    return ToolWindowId.PROJECT_VIEW;
  }

  @Override
  public String getMinorViewId() {
    return null;
  }

  @Override
  public float getWeight() {
    return 0;
  }

  @Override
  public String toString() {
    return "Project View";
  }
}
