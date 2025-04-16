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
package consulo.ide.impl.idea.vcs.log.ui;

import consulo.ide.impl.idea.vcs.log.data.VcsLogDataImpl;
import consulo.ui.ex.JBColor;
import consulo.util.lang.function.Condition;
import consulo.util.lang.function.Conditions;
import consulo.versionControlSystem.log.*;
import consulo.versionControlSystem.log.util.VcsLogUtil;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class CurrentBranchHighlighter implements VcsLogHighlighter {
    private static final JBColor CURRENT_BRANCH_BG = new JBColor(new Color(228, 250, 255), new Color(63, 71, 73));
    private static final String HEAD = "HEAD";
    @Nonnull
    private final VcsLogData myLogData;
    @Nonnull
    private final VcsLogUi myLogUi;
    @Nonnull
    private final Map<VirtualFile, Condition<CommitId>> myConditions = new HashMap<>();
    @Nullable
    private String mySingleFilteredBranch;

    public CurrentBranchHighlighter(@Nonnull VcsLogData logData, @Nonnull VcsLogUi logUi) {
        myLogData = logData;
        myLogUi = logUi;
    }

    @Nonnull
    @Override
    public VcsCommitStyle getStyle(@Nonnull VcsShortCommitDetails details, boolean isSelected) {
        if (isSelected || !myLogUi.isHighlighterEnabled(CurrentBranchHighlighterFactory.ID)) {
            return VcsCommitStyle.DEFAULT;
        }
        Condition<CommitId> condition = myConditions.get(details.getRoot());
        if (condition == null) {
            VcsLogProvider provider = myLogData.getLogProvider(details.getRoot());
            String currentBranch = provider.getCurrentBranch(details.getRoot());
            if (!HEAD.equals(mySingleFilteredBranch) && currentBranch != null && !(currentBranch.equals(mySingleFilteredBranch))) {
                condition = ((VcsLogDataImpl)myLogData).getContainingBranchesGetter()
                    .getContainedInBranchCondition(currentBranch, details.getRoot());
                myConditions.put(details.getRoot(), condition);
            }
            else {
                condition = Conditions.alwaysFalse();
            }
        }
        if (condition != null && condition.value(new CommitId(details.getId(), details.getRoot()))) {
            return VcsCommitStyleFactory.background(CURRENT_BRANCH_BG);
        }
        return VcsCommitStyle.DEFAULT;
    }

    @Override
    public void update(@Nonnull VcsLogDataPack dataPack, boolean refreshHappened) {
        VcsLogBranchFilter branchFilter = dataPack.getFilters().getBranchFilter();
        mySingleFilteredBranch = branchFilter == null ? null : VcsLogUtil.getSingleFilteredBranch(branchFilter, dataPack.getRefs());
        myConditions.clear();
    }
}
