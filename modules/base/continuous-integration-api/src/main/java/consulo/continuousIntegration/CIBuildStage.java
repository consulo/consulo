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
 * Part of a build: a Jenkins pipeline stage, a job of a GitHub workflow run, a step.
 *
 * @param parentId {@link #id()} of the parent stage, {@code null} for top level stages
 * @param hasLog   {@link CIServer#loadLog(CIBuild, CIBuildStage, long)} returns the log of this stage
 * @since 2026-11-09
 */
public record CIBuildStage(
    String id,
    String name,
    @Nullable String parentId,
    CIBuildStatus status,
    @Nullable Instant startTime,
    @Nullable Duration duration,
    boolean hasLog,
    @Nullable String webUrl
) {
}
