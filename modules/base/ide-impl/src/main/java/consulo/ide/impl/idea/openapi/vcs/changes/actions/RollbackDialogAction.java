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
package consulo.ide.impl.idea.openapi.vcs.changes.actions;

import consulo.application.dumb.DumbAware;
import consulo.document.FileDocumentManager;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionUtil;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.RollbackChangesDialog;
import consulo.ide.impl.idea.vcsUtil.RollbackUtil;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.internal.ChangesBrowserApi;

import java.util.Arrays;

/**
 * @author yole
 */
public class RollbackDialogAction extends AnAction implements DumbAware {
  public RollbackDialogAction() {
    ActionUtil.copyFrom(this, IdeActions.CHANGES_VIEW_ROLLBACK);
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent e) {
    FileDocumentManager.getInstance().saveAllDocuments();
    Change[] changes = e.getData(VcsDataKeys.CHANGES);
    Project project = e.getData(Project.KEY);
    final ChangesBrowserApi browser = e.getData(ChangesBrowserApi.DATA_KEY);
    if (browser != null) {
      browser.setDataIsDirty(true);
    }
    RollbackChangesDialog.rollbackChanges(project, Arrays.asList(changes), true, () -> {
      if (browser != null) {
        browser.rebuildList();
        browser.setDataIsDirty(false);
      }
    });
  }

  @Override
  @RequiredUIAccess
  public void update(AnActionEvent e) {
    Change[] changes = e.getData(VcsDataKeys.CHANGES);
    Project project = e.getData(Project.KEY);
    boolean enabled = changes != null && project != null;
    e.getPresentation().setEnabled(enabled);
    if (enabled) {
      String operationName = RollbackUtil.getRollbackOperationName(project);
      e.getPresentation().setText(operationName);
      e.getPresentation().setDescription(operationName + " selected changes");
    }
  }
}
