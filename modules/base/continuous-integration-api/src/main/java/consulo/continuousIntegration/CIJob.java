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

import org.jspecify.annotations.Nullable;

/**
 * Job, pipeline, build configuration, workflow, or a folder of them. Immutable snapshot, reload it with
 * {@link CIServer#loadJob(CIJob)}.
 *
 * @param id            provider id, unique within a server (Jenkins job URL, GitHub workflow id)
 * @param name          short name
 * @param displayName   presentable name
 * @param fullName      name with all parent folders, for example {@code folder/job}
 * @param webUrl        page of the job in a browser
 * @param buildable     the job can be started now (not disabled, not a folder)
 * @param status        state of the last build, {@link CIBuildStatus#UNKNOWN} for folders
 * @param healthScore   0..100 (Jenkins weather), -1 when unknown
 * @param hasParameters the job takes parameters, so starting it shows a dialog
 * @since 2026-11-09
 */
public record CIJob(
    String id,
    String name,
    String displayName,
    String fullName,
    @Nullable String webUrl,
    CIJobKind kind,
    boolean buildable,
    CIBuildStatus status,
    boolean queued,
    int healthScore,
    @Nullable String description,
    boolean hasParameters,
    @Nullable CIBuild lastBuild
) {
    public static Builder builder(String id, String name) {
        return new Builder(id, name);
    }

    public Builder toBuilder() {
        return new Builder(id, name)
            .displayName(displayName)
            .fullName(fullName)
            .webUrl(webUrl)
            .kind(kind)
            .buildable(buildable)
            .status(status)
            .queued(queued)
            .healthScore(healthScore)
            .description(description)
            .hasParameters(hasParameters)
            .lastBuild(lastBuild);
    }

    public static final class Builder {
        private final String myId;
        private final String myName;
        private String myDisplayName = "";
        private String myFullName = "";
        private @Nullable String myWebUrl;
        private CIJobKind myKind = CIJobKind.JOB;
        private boolean myBuildable = true;
        private CIBuildStatus myStatus = CIBuildStatus.UNKNOWN;
        private boolean myQueued;
        private int myHealthScore = -1;
        private @Nullable String myDescription;
        private boolean myHasParameters;
        private @Nullable CIBuild myLastBuild;

        private Builder(String id, String name) {
            myId = id;
            myName = name;
        }

        public Builder displayName(String displayName) {
            myDisplayName = displayName;
            return this;
        }

        public Builder fullName(String fullName) {
            myFullName = fullName;
            return this;
        }

        public Builder webUrl(@Nullable String webUrl) {
            myWebUrl = webUrl;
            return this;
        }

        public Builder kind(CIJobKind kind) {
            myKind = kind;
            return this;
        }

        public Builder buildable(boolean buildable) {
            myBuildable = buildable;
            return this;
        }

        public Builder status(CIBuildStatus status) {
            myStatus = status;
            return this;
        }

        public Builder queued(boolean queued) {
            myQueued = queued;
            return this;
        }

        public Builder healthScore(int healthScore) {
            myHealthScore = healthScore;
            return this;
        }

        public Builder description(@Nullable String description) {
            myDescription = description;
            return this;
        }

        public Builder hasParameters(boolean hasParameters) {
            myHasParameters = hasParameters;
            return this;
        }

        public Builder lastBuild(@Nullable CIBuild lastBuild) {
            myLastBuild = lastBuild;
            return this;
        }

        public CIJob build() {
            return new CIJob(
                myId,
                myName,
                myDisplayName.isEmpty() ? myName : myDisplayName,
                myFullName.isEmpty() ? myName : myFullName,
                myWebUrl,
                myKind,
                myBuildable && myKind == CIJobKind.JOB,
                myStatus,
                myQueued,
                myHealthScore,
                myDescription,
                myHasParameters,
                myLastBuild
            );
        }
    }
}
