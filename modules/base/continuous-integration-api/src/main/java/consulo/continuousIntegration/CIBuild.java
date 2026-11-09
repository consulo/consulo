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

import java.time.Duration;
import java.time.Instant;

/**
 * One build of a job. Immutable snapshot, reload it with {@link CIServer#loadBuild(CIBuild)}.
 *
 * @param id                provider id, unique within a server (Jenkins build URL, GitHub run id)
 * @param jobId             {@link CIJob#id()} of the job
 * @param number            presentable number, for example {@code 42}
 * @param displayName       presentable name, for example {@code #42}
 * @param webUrl            page of the build in a browser
 * @param estimatedDuration expected duration of a running build, for progress
 * @since 2026-11-09
 */
public record CIBuild(
    String id,
    String jobId,
    String number,
    String displayName,
    @Nullable String webUrl,
    CIBuildStatus status,
    @Nullable Instant startTime,
    @Nullable Duration duration,
    @Nullable Duration estimatedDuration,
    @Nullable String branch,
    @Nullable String revision,
    @Nullable String description
) {
    public static Builder builder(String id, String jobId) {
        return new Builder(id, jobId);
    }

    public Builder toBuilder() {
        return new Builder(id, jobId)
            .number(number)
            .displayName(displayName)
            .webUrl(webUrl)
            .status(status)
            .startTime(startTime)
            .duration(duration)
            .estimatedDuration(estimatedDuration)
            .branch(branch)
            .revision(revision)
            .description(description);
    }

    public static final class Builder {
        private final String myId;
        private final String myJobId;
        private String myNumber = "";
        private String myDisplayName = "";
        private @Nullable String myWebUrl;
        private CIBuildStatus myStatus = CIBuildStatus.UNKNOWN;
        private @Nullable Instant myStartTime;
        private @Nullable Duration myDuration;
        private @Nullable Duration myEstimatedDuration;
        private @Nullable String myBranch;
        private @Nullable String myRevision;
        private @Nullable String myDescription;

        private Builder(String id, String jobId) {
            myId = id;
            myJobId = jobId;
        }

        public Builder number(String number) {
            myNumber = number;
            return this;
        }

        public Builder displayName(String displayName) {
            myDisplayName = displayName;
            return this;
        }

        public Builder webUrl(@Nullable String webUrl) {
            myWebUrl = webUrl;
            return this;
        }

        public Builder status(CIBuildStatus status) {
            myStatus = status;
            return this;
        }

        public Builder startTime(@Nullable Instant startTime) {
            myStartTime = startTime;
            return this;
        }

        public Builder duration(@Nullable Duration duration) {
            myDuration = duration;
            return this;
        }

        public Builder estimatedDuration(@Nullable Duration estimatedDuration) {
            myEstimatedDuration = estimatedDuration;
            return this;
        }

        public Builder branch(@Nullable String branch) {
            myBranch = branch;
            return this;
        }

        public Builder revision(@Nullable String revision) {
            myRevision = revision;
            return this;
        }

        public Builder description(@Nullable String description) {
            myDescription = description;
            return this;
        }

        public CIBuild build() {
            String displayName = myDisplayName.isEmpty() && !myNumber.isEmpty() ? "#" + myNumber : myDisplayName;
            return new CIBuild(
                myId,
                myJobId,
                myNumber,
                displayName,
                myWebUrl,
                myStatus,
                myStartTime,
                myDuration,
                myEstimatedDuration,
                myBranch,
                myRevision,
                myDescription
            );
        }
    }
}
