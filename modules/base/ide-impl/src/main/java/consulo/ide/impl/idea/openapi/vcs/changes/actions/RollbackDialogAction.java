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

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.ui.ex.action.IdeActions;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionUtil;
import consulo.document.FileDocumentManager;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.change.Change;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.ChangesBrowserBase;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.RollbackChangesDialog;
import consulo.ide.impl.idea.vcsUtil.RollbackUtil;

import java.util.Arrays;

/**
 * @author yole
 */
public class RollbackDialogAction extends AnAction implements DumbAware {
  public RollbackDialogAction() {
    ActionUtil.copyFrom(this, IdeActions.CHANGES_VIEW_ROLLBACK);
  }

  public void actionPerformed(AnActionEvent e) {
    FileDocumentManager.getInstance().saveAllDocuments();
    Change[] changes = e.getData(VcsDataKeys.CHANGES);
    Project project = e.getData(CommonDataKeys.PROJECT);
    final ChangesBrowserBase browser = e.getData(ChangesBrowserBase.DATA_KEY);
    if (browser != null) {
      browser.setDataIsDirty(true);
    }
    RollbackChangesDialog.rollbackChanges(project, Arrays.asList(changes), true, new Runnable() {
      public void run() {
        if (browser != null) {
          browser.rebuildList();
          browser.setDataIsDirty(false);
        }
      }
    });
  }

  public void update(AnActionEvent e) {
    Change[] changes = e.getData(VcsDataKeys.CHANGES);
    Project project = e.getData(CommonDataKeys.PROJECT);
    boolean enabled = changes != null && project != null;
    e.getPresentation().setEnabled(enabled);
    if (enabled) {
      String operationName = RollbackUtil.getRollbackOperationName(project);
      e.getPresentation().setText(operationName);
      e.getPresentation().setDescription(operationName + " selected changes");
    }

  }
}
