/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.ChangesViewContentManager;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.versionControlSystem.RepositoryLocation;

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 4/23/12
 * Time: 4:53 PM
 */
public class ClearCommittedAction extends AnAction implements DumbAware {
  public ClearCommittedAction() {
    super("Clear", "Clears cached revisions", PlatformIconGroup.generalRemove());
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    CommittedChangesPanel panel = ChangesViewContentManager.getInstance(project).getActiveComponent(CommittedChangesPanel.class);
    assert panel != null;
    if (panel.isInLoad()) return;
    if (panel.getRepositoryLocation() == null) {
      panel.clearCaches();
    }
  }

  @Override
  @RequiredUIAccess
  public void update(final AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    if (project != null) {
      CommittedChangesPanel panel = ChangesViewContentManager.getInstance(project).getActiveComponent(CommittedChangesPanel.class);
      RepositoryLocation rl = panel == null ? null : panel.getRepositoryLocation();
      e.getPresentation().setVisible(rl == null);
      e.getPresentation().setEnabled(panel != null && (! panel.isInLoad()));
    }
    else {
      e.getPresentation().setVisible(false);
      e.getPresentation().setEnabled(false);
    }
  }
}
