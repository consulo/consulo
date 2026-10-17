/*
 * Copyright 2013-2026 consulo.io
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
package consulo.versionControlSystem.impl.internal.commit;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangesUtil;
import consulo.versionControlSystem.change.CommitContext;
import consulo.versionControlSystem.change.CommitExecutor;
import consulo.versionControlSystem.checkin.CommitInfo;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class UnifiedCommitInfo implements CommitInfo {
    private final CommitContext myCommitContext;
    private final @Nullable CommitExecutor myExecutor;
    private final List<Change> myCommittedChanges;
    private final List<AbstractVcs> myAffectedVcses;
    private final String myCommitMessage;
    private final LocalizeValue myCommitActionText;
    private final boolean myPostCommitCheck;

    public UnifiedCommitInfo(Project project,
                             CommitContext commitContext,
                             @Nullable CommitExecutor executor,
                             List<Change> committedChanges,
                             String commitMessage,
                             LocalizeValue commitActionText) {
        this(commitContext,
            executor,
            List.copyOf(committedChanges),
            List.copyOf(ChangesUtil.getAffectedVcses(committedChanges, project)),
            commitMessage,
            commitActionText,
            false);
    }

    private UnifiedCommitInfo(CommitContext commitContext,
                              @Nullable CommitExecutor executor,
                              List<Change> committedChanges,
                              List<AbstractVcs> affectedVcses,
                              String commitMessage,
                              LocalizeValue commitActionText,
                              boolean postCommitCheck) {
        myCommitContext = commitContext;
        myExecutor = executor;
        myCommittedChanges = committedChanges;
        myAffectedVcses = affectedVcses;
        myCommitMessage = commitMessage;
        myCommitActionText = commitActionText;
        myPostCommitCheck = postCommitCheck;
    }

    public UnifiedCommitInfo asPostCommit() {
        return new UnifiedCommitInfo(myCommitContext, myExecutor, myCommittedChanges, myAffectedVcses, myCommitMessage, myCommitActionText, true);
    }

    public boolean isPostCommitCheck() {
        return myPostCommitCheck;
    }

    @Override
    public CommitContext getCommitContext() {
        return myCommitContext;
    }

    @Override
    public boolean isVcsCommit() {
        return myExecutor == null;
    }

    @Override
    public @Nullable CommitExecutor getExecutor() {
        return myExecutor;
    }

    @Override
    public List<Change> getCommittedChanges() {
        return myCommittedChanges;
    }

    @Override
    public List<AbstractVcs> getAffectedVcses() {
        return myAffectedVcses;
    }

    @Override
    public String getCommitMessage() {
        return myCommitMessage;
    }

    @Override
    public LocalizeValue getCommitActionText() {
        return myCommitActionText;
    }
}
