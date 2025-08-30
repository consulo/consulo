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
package consulo.versionControlSystem.log.impl.internal.ui.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.ScrollingUtil;
import consulo.versionControlSystem.log.VcsLogUi;
import consulo.versionControlSystem.log.impl.internal.ui.VcsLogGraphTable;
import consulo.versionControlSystem.log.impl.internal.ui.VcsLogUiImpl;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;
import jakarta.annotation.Nonnull;

@ActionImpl(id = "Vcs.Log.ShowTooltip", shortcutFrom = @ActionRef(id = IdeActions.ACTION_QUICK_JAVADOC))
public class ShowCommitTooltipAction extends DumbAwareAction {
    public ShowCommitTooltipAction() {
        super(VersionControlSystemLogLocalize.actionShowCommitTooltipText(), VersionControlSystemLogLocalize.actionShowCommitTooltipDescription());
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(
            e.hasData(Project.KEY) && e.getData(VcsLogUi.KEY) instanceof VcsLogUiImpl vcsLogUi
                && vcsLogUi.getTable().getSelectedRowCount() == 1
        );
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        VcsLogGraphTable table = ((VcsLogUiImpl) e.getRequiredData(VcsLogUi.KEY)).getTable();
        int row = table.getSelectedRow();
        if (ScrollingUtil.isVisible(table, row)) {
            table.showTooltip(row);
        }
    }
}
