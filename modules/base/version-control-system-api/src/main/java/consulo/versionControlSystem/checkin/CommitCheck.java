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

import consulo.application.dumb.PossiblyDumbAware;
import consulo.application.progress.ProgressIndicator;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * Represents some check or code modification that is performed when changes are committed into VCS.
 * E.g. to enforce the code style or to check if errors are introduced by the committed changes.
 * <p>
 * This interface supersedes {@link CheckinHandler#beforeCheckin} method,
 * and will be used instead if {@link CheckinHandler} implements {@link CommitCheck}.
 * This interface is better suitable for 'non-modal' commit mode,
 * as it allows performing operations without modal progress and modal error dialogs.
 * <p>
 * Implement {@link consulo.application.dumb.DumbAware} to allow running commit check in dumb mode.
 */
public interface CommitCheck extends PossiblyDumbAware {
    enum ExecutionOrder {
        /**
         * Checks to be performed first.
         */
        EARLY,

        /**
         * Checks that can modify content about to be committed (code cleanups, formatters, etc).
         */
        MODIFICATION,

        /**
         * Checks to be performed after all modifications are finished.
         */
        LATE,

        /**
         * Slow checks that can be performed in background after the actual commit.
         * These can be handled as {@link #LATE} checks in some situations
         * (ex: an executor which {@link consulo.versionControlSystem.change.CommitExecutor#requiresSyncCommitChecks() requires sync checks}).
         */
        POST_COMMIT
    }

    ExecutionOrder getExecutionOrder();

    /**
     * Indicates if commit check should be run for the commit.
     * E.g. if the corresponding option is enabled in settings.
     * See {@link CheckinHandler#getBeforeCheckinConfigurationPanel()}.
     *
     * @return {@code true} if commit check should be run for the commit and {@code false} otherwise
     */
    @RequiredUIAccess
    boolean isEnabled();

    /**
     * Runs commit check and returns the found commit problem if any.
     * <p>
     * Method is executed on a pooled thread, inside the progress of the commit checks - use the indicator to report
     * the progress state and to check for cancellation. Switch to the UI thread explicitly for the work which needs it.
     *
     * @return a commit problem found by the commit check or {@code null} if no problems found
     */
    @Nullable CommitProblem runCheck(CommitInfo commitInfo, ProgressIndicator indicator);
}
