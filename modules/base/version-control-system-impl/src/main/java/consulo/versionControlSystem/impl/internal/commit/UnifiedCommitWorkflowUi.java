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

import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.checkin.CheckinHandler;
import consulo.versionControlSystem.checkin.CommitProblem;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public interface UnifiedCommitWorkflowUi {
    LocalChangeList getChangeList();

    List<Change> getDisplayedChanges();

    List<Change> getIncludedChanges();

    List<VirtualFile> getIncludedUnversionedFiles();

    @RequiredUIAccess
    void includeChanges(Collection<Change> changes);

    @RequiredUIAccess
    CompletableFuture<?> refreshChangesBeforeCommit();

    String getCommitMessage();

    @RequiredUIAccess
    void setCommitMessage(String commitMessage);

    @RequiredUIAccess
    void startBeforeCommitChecks();

    @RequiredUIAccess
    void endBeforeCommitChecks(CheckinHandler.ReturnResult result, @Nullable CommitProblem problem);

    @RequiredUIAccess
    void deactivate(boolean committed);
}
