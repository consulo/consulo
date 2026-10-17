/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.versionControlSystem.distributed.push;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.progress.ProgressIndicator;
import consulo.localize.LocalizeValue;
import consulo.project.Project;

import java.util.List;

/**
 * Interface for any checkers that should be called right before push operation starts.
 * All implemented handlers will be called on a background thread one by one (in unspecified order)
 * with cancelable progress indicator.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface PrePushHandler {
    /**
     * Handler's decision of whether a push must be performed or canceled.
     */
    enum Result {
        /**
         * Push is allowed.
         */
        OK,
        /**
         * Push is not allowed. The Push Dialog won't be closed.
         */
        ABORT,
        /**
         * Push is not allowed. The Push Dialog will be closed immediately.
         */
        ABORT_AND_CLOSE
    }

    /**
     * Presentable name used in dialogs, UI, etc.
     *
     * @return presentable name of this handler
     */
    LocalizeValue getPresentableName();

    /**
     * Check synchronously if the push operation should be performed or canceled for specified {@link PushInfo}s.
     *
     * @param pushDetails information about the repository, source and target branches, and commits to be pushed
     * @param indicator   progress indicator to cancel this handler if necessary
     * @return handler's decision on whether the push must be performed or canceled
     */
    Result handle(Project project, List<PushInfo> pushDetails, ProgressIndicator indicator);
}
