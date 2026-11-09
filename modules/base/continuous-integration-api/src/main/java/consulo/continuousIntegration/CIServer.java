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

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Client of one CI server, created by {@link CIServerType#createServer}. Every call is asynchronous and must not block
 * the calling thread.
 *
 * @since 2026-11-09
 */
public interface CIServer {
    CIServerType<?> getType();

    CIServerOptions getOptions();

    /**
     * Presentable name, for example the host of the server or {@code owner/repo}.
     */
    String getName();

    @Nullable
    String getWebUrl();

    Set<CICapability> getCapabilities();

    default boolean hasCapability(CICapability capability) {
        return getCapabilities().contains(capability);
    }

    /**
     * Optional part of the client, for example {@link consulo.continuousIntegration.support.CIBuildStagesSupport}.
     */
    default <T> @Nullable T getSupport(Class<T> supportClass) {
        return supportClass.isInstance(this) ? supportClass.cast(this) : null;
    }

    CompletableFuture<?> testConnection();

    /**
     * Children of a folder, or top level jobs and folders when {@code parent} is {@code null}.
     */
    CompletableFuture<List<CIJob>> loadJobs(@Nullable CIJob parent);

    CompletableFuture<CIJob> loadJob(CIJob job);

    /**
     * Newest first.
     */
    CompletableFuture<List<CIBuild>> loadBuilds(CIJob job, int limit);

    CompletableFuture<CIBuild> loadBuild(CIBuild build);

    CompletableFuture<List<CIParameterDefinition>> loadParameters(CIJob job);

    /**
     * Starts the job. The future completes with the started build, or with a {@link CIBuildStatus#QUEUED} build when
     * the server has not started it yet; reload it with {@link #loadBuild(CIBuild)} until it runs.
     */
    CompletableFuture<CIBuild> runBuild(CIJob job, CIRunRequest request);

    CompletableFuture<?> stopBuild(CIBuild build);

    /**
     * Log of the build, or of one of its stages when {@code stage} is not {@code null}, from {@code offset}.
     */
    CompletableFuture<CILogChunk> loadLog(CIBuild build, @Nullable CIBuildStage stage, long offset);
}
