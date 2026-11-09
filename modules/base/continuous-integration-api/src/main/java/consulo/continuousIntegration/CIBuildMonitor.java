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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;

import java.util.concurrent.CompletableFuture;

/**
 * Follows builds in the Run tool window: polls the state of the build, streams its log and stops it on request. Works
 * for every {@link CIServer}, so providers do not need their own console.
 *
 * @since 2026-11-09
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface CIBuildMonitor {
    static CIBuildMonitor getInstance(Project project) {
        return project.getInstance(CIBuildMonitor.class);
    }

    /**
     * Starts the job and shows the build until it is finished. Can be called from any thread.
     *
     * @return the started (or queued) build, see {@link CIServer#runBuild(CIJob, CIRunRequest)}
     */
    CompletableFuture<CIBuild> runBuild(CIServer server, CIJob job, CIRunRequest request);

    /**
     * Shows an existing build: follows it while it runs, or prints its log when it is finished. A build which is
     * already shown is brought to front. Can be called from any thread.
     */
    void showBuild(CIServer server, CIJob job, CIBuild build);
}
