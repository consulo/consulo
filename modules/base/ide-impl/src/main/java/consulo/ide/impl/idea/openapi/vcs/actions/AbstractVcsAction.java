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
package consulo.ide.impl.idea.openapi.vcs.actions;

import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Presentation;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.action.VcsContext;
import consulo.versionControlSystem.impl.internal.action.VcsContextWrapper;
import jakarta.annotation.Nonnull;

import java.util.Collection;

import static consulo.ide.impl.idea.util.containers.ContainerUtil.newHashSet;
import static java.util.Collections.emptySet;

public abstract class AbstractVcsAction extends DumbAwareAction {

  @SuppressWarnings("unused") // Required for compatibility with external plugins.
  public static Collection<AbstractVcs> getActiveVcses(@Nonnull VcsContext dataContext) {
    Project project = dataContext.getProject();

    return project != null ? newHashSet(ProjectLevelVcsManager.getInstance(project).getAllActiveVcss()) : emptySet();
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    //noinspection deprecation - required for compatibility with external plugins.
    performUpdate(e.getPresentation(), VcsContextWrapper.createInstanceOn(e));
  }

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    actionPerformed(VcsContextWrapper.createCachedInstanceOn(e));
  }

  protected abstract void update(@Nonnull VcsContext vcsContext, @Nonnull Presentation presentation);

  protected abstract void actionPerformed(@Nonnull VcsContext e);

  /**
   * @deprecated Only sync update is currently supported by {@link AbstractVcsAction}.
   */
  @SuppressWarnings("unused") // Required for compatibility with external plugins.
  @Deprecated
  protected boolean forceSyncUpdate(@Nonnull AnActionEvent e) {
    return true;
  }


  /**
   * @deprecated Use {@link AbstractVcsAction#update(VcsContext, Presentation)}.
   */
  @Deprecated
  protected void performUpdate(@Nonnull Presentation presentation, @Nonnull VcsContext vcsContext) {
    update(vcsContext, presentation);
  }
}