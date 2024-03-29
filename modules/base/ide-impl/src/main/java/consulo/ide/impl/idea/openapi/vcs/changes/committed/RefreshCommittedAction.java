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
package consulo.ide.impl.idea.openapi.vcs.changes.committed;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.ChangesViewContentManager;

/**
 * @author yole
 */
public class RefreshCommittedAction extends AnAction implements DumbAware {
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    CommittedChangesPanel panel = ChangesViewContentManager.getInstance(project).getActiveComponent(CommittedChangesPanel.class);
    assert panel != null;
    if (panel.isInLoad()) return;
    if (panel.getRepositoryLocation() != null) {
      panel.refreshChanges(false);
    }
    else {
      RefreshIncomingChangesAction.doRefresh(project);
    }
  }

  public void update(final AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    if (project != null) {
      CommittedChangesPanel panel = ChangesViewContentManager.getInstance(project).getActiveComponent(CommittedChangesPanel.class);
      e.getPresentation().setEnabled(panel != null && (! panel.isInLoad()));
    }
    else {
      e.getPresentation().setEnabled(false);
    }
  }
}
