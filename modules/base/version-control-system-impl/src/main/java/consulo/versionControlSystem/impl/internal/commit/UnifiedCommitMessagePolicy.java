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

import consulo.project.Project;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.LocalChangeList;

import java.util.Objects;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class UnifiedCommitMessagePolicy {
    private final Project myProject;
    private final VcsConfiguration myConfiguration;
    private final boolean myUseLastCommitMessage;

    public UnifiedCommitMessagePolicy(Project project, boolean useLastCommitMessage) {
        myProject = project;
        myConfiguration = VcsConfiguration.getInstance(project);
        myUseLastCommitMessage = useLastCommitMessage;
    }

    public String getInitialMessage(LocalChangeList changeList) {
        String comment = changeList.getComment();
        if (comment != null && !comment.isBlank()) {
            return comment;
        }

        if (!myUseLastCommitMessage || myConfiguration.CLEAR_INITIAL_COMMIT_MESSAGE) {
            return "";
        }
        return Objects.requireNonNullElse(myConfiguration.getLastNonEmptyCommitMessage(), "");
    }

    public void saveMessage(LocalChangeList changeList, String message, boolean commit) {
        ChangeListManager.getInstance(myProject).editComment(changeList.getName(), message);
        if (commit) {
            myConfiguration.saveCommitMessage(message);
        }
    }

    public void onCommitSuccess(LocalChangeList changeList, boolean allChangesIncluded) {
        if (allChangesIncluded) {
            ChangeListManager.getInstance(myProject).editComment(changeList.getName(), "");
        }
    }
}
