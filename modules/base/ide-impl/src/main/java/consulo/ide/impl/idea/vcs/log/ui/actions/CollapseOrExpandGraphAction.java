/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import consulo.ide.impl.idea.vcs.log.data.MainVcsLogUiProperties;
import consulo.ide.impl.idea.vcs.log.data.VcsLogUiProperties;
import consulo.ide.impl.idea.vcs.log.graph.PermanentGraph;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogInternalDataKeys;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogUiImpl;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.versionControlSystem.log.VcsLogUi;
import consulo.versionControlSystem.log.util.VcsLogUtil;
import jakarta.annotation.Nonnull;

abstract class CollapseOrExpandGraphAction extends DumbAwareAction {
    @Nonnull
    private final LocalizeValue myLinearBranchesText;
    @Nonnull
    private final LocalizeValue myLinearBranchesDescription;
    @Nonnull
    private final LocalizeValue myMergesText;
    @Nonnull
    private final LocalizeValue myMergesDescription;

    protected CollapseOrExpandGraphAction(
        @Nonnull LocalizeValue linearBranchesText,
        @Nonnull LocalizeValue linearBranchesDescription,
        @Nonnull LocalizeValue mergesText,
        @Nonnull LocalizeValue mergesDescription
    ) {
        super(linearBranchesText, linearBranchesDescription);
        myLinearBranchesText = linearBranchesText;
        myLinearBranchesDescription = linearBranchesDescription;
        myMergesText = mergesText;
        myMergesDescription = mergesDescription;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        VcsLogUtil.triggerUsage(e);

        VcsLogUi ui = e.getRequiredData(VcsLogUi.KEY);
        executeAction((VcsLogUiImpl) ui);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        VcsLogUi ui = e.getData(VcsLogUi.KEY);
        VcsLogUiProperties properties = e.getData(VcsLogInternalDataKeys.LOG_UI_PROPERTIES);

        if (ui != null && ui.areGraphActionsEnabled() && properties != null && !properties.exists(MainVcsLogUiProperties.BEK_SORT_TYPE)) {
            e.getPresentation().setEnabled(ui.getFilterUi().getFilters().getDetailsFilters().isEmpty());

            boolean isMerges = properties.get(MainVcsLogUiProperties.BEK_SORT_TYPE) == PermanentGraph.SortType.LinearBek;
            e.getPresentation().setTextValue(isMerges ? myMergesText : myLinearBranchesText);
            e.getPresentation().setDescriptionValue(isMerges ? myMergesDescription : myLinearBranchesDescription);
        }
        else {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setTextValue(myLinearBranchesText);
            e.getPresentation().setDescriptionValue(myLinearBranchesDescription);
        }
    }

    protected abstract void executeAction(@Nonnull VcsLogUiImpl vcsLogUi);
}
