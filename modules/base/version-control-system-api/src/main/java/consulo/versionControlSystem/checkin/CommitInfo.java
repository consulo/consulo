/*
 * Copyright 2000-2023 JetBrains s.r.o. and contributors.
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
package consulo.versionControlSystem.checkin;

import consulo.localize.LocalizeValue;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangesUtil;
import consulo.versionControlSystem.change.CommitContext;
import consulo.versionControlSystem.change.CommitExecutor;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface CommitInfo {
    CommitContext getCommitContext();

    /**
     * Whether commit will be performed using {@link consulo.versionControlSystem.change.CommitSession#VCS_COMMIT} aka {@link CheckinEnvironment}.
     */
    boolean isVcsCommit();

    @Nullable CommitExecutor getExecutor();

    List<Change> getCommittedChanges();

    List<AbstractVcs> getAffectedVcses();

    String getCommitMessage();

    /**
     * Commit action name, without ellipsis. May contain mnemonic. Ex: 'Amend Comm&it'.
     */
    LocalizeValue getCommitActionText();

    default List<VirtualFile> getCommittedVirtualFiles() {
        return List.of(ChangesUtil.getFilesFromChanges(getCommittedChanges()));
    }
}
