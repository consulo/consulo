/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.vcs.log.ui.actions;

import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.ui.ex.awt.ScrollingUtil;
import consulo.ide.impl.idea.vcs.log.VcsLogDataKeys;
import consulo.ide.impl.idea.vcs.log.VcsLogUi;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogUiImpl;
import consulo.ide.impl.idea.vcs.log.ui.frame.VcsLogGraphTable;
import javax.annotation.Nonnull;

public class ShowCommitTooltipAction extends DumbAwareAction {
  public ShowCommitTooltipAction() {
    super("Show Commit Tooltip", "Show tooltip for currently selected commit in the Log", null);
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    VcsLogUi ui = e.getData(VcsLogDataKeys.VCS_LOG_UI);
    if (project == null || ui == null) {
      e.getPresentation().setEnabledAndVisible(false);
    }
    else {
      e.getPresentation().setEnabledAndVisible(ui instanceof VcsLogUiImpl && ((VcsLogUiImpl)ui).getTable().getSelectedRowCount() == 1);
    }
  }

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    VcsLogGraphTable table = ((VcsLogUiImpl)e.getRequiredData(VcsLogDataKeys.VCS_LOG_UI)).getTable();
    int row = table.getSelectedRow();
    if (ScrollingUtil.isVisible(table, row)) {
      table.showTooltip(row);
    }
  }
}
