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
package consulo.continuousIntegration;

import consulo.continuousIntegration.localize.ContinuousIntegrationLocalize;
import consulo.localize.LocalizeValue;

/**
 * State of a build, or the derived state of a job (state of its last build).
 *
 * @since 2026-11-09
 */
public enum CIBuildStatus {
    SUCCESS,
    UNSTABLE,
    FAILURE,
    ABORTED,
    NOT_BUILT,
    RUNNING,
    QUEUED,
    UNKNOWN;

    public boolean isFinished() {
        return this == SUCCESS || this == UNSTABLE || this == FAILURE || this == ABORTED || this == NOT_BUILT;
    }

    public boolean isActive() {
        return this == RUNNING || this == QUEUED;
    }

    public boolean isSuccessful() {
        return this == SUCCESS;
    }

    public LocalizeValue getPresentableName() {
        return switch (this) {
            case SUCCESS -> ContinuousIntegrationLocalize.statusSuccess();
            case UNSTABLE -> ContinuousIntegrationLocalize.statusUnstable();
            case FAILURE -> ContinuousIntegrationLocalize.statusFailure();
            case ABORTED -> ContinuousIntegrationLocalize.statusAborted();
            case NOT_BUILT -> ContinuousIntegrationLocalize.statusNotBuilt();
            case RUNNING -> ContinuousIntegrationLocalize.statusRunning();
            case QUEUED -> ContinuousIntegrationLocalize.statusQueued();
            case UNKNOWN -> ContinuousIntegrationLocalize.statusUnknown();
        };
    }
}
