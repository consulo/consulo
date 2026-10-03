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
package consulo.sandboxPlugin.ide.profiler;

import consulo.application.concurrent.ApplicationConcurrency;
import consulo.execution.attach.LocalAttachHost;
import consulo.execution.attach.XAttachHost;
import consulo.execution.profiler.ProfilerProcess;
import consulo.execution.profiler.configuration.ProfilerAttacher;
import consulo.execution.profiler.configuration.ProfilerFeature;
import consulo.platform.ProcessInfo;
import consulo.project.Project;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class SandProfilerAttacher implements ProfilerAttacher {
    private final ApplicationConcurrency myConcurrency;
    private final SandProfilerConfigurationState myState;

    public SandProfilerAttacher(ApplicationConcurrency concurrency, SandProfilerConfigurationState state) {
        myConcurrency = concurrency;
        myState = state;
    }

    @Override
    public boolean isApplicable(XAttachHost host, ProcessInfo process) {
        return host instanceof LocalAttachHost && process.getExecutableDisplayName().toLowerCase(Locale.ROOT).contains("java");
    }

    @Override
    public Set<ProfilerFeature> getFeatures(XAttachHost host, ProcessInfo process) {
        return isApplicable(host, process) ? SandProfilerProcess.FEATURES : Set.of();
    }

    @Override
    public CompletableFuture<ProfilerProcess<?>> attach(Project project, XAttachHost host, ProcessInfo process) {
        if (!isApplicable(host, process)) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("The sand profiler attaches to local Java processes only: " + process)
            );
        }

        SandTargetProcess targetProcess = new SandTargetProcess(
            process.getExecutableDisplayName() + " (" + process.getPid() + ")",
            process.getPid()
        );
        return CompletableFuture.<ProfilerProcess<?>>supplyAsync(
            () -> SandProfilerProcess.attached(project, myConcurrency, targetProcess, myState),
            myConcurrency.executor()
        );
    }
}
