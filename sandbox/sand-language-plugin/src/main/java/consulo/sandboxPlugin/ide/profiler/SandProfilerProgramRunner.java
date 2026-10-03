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

import consulo.annotation.component.ExtensionImpl;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.execution.configuration.RunProfile;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.executor.Executor;
import consulo.execution.executor.ExecutorGroup;
import consulo.execution.executor.ExecutorRegistry;
import consulo.execution.profiler.DefaultProfilerExecutorGroup;
import consulo.execution.profiler.ProfilerExecutorSettings;
import consulo.execution.profiler.ProfilerToolWindowManager;
import consulo.execution.runner.DefaultProgramRunner;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ui.RunContentDescriptor;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.project.Project;
import consulo.sandboxPlugin.ide.run.SandConfiguration;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionImpl
public class SandProfilerProgramRunner extends DefaultProgramRunner {
    private final ExecutorRegistry myExecutorRegistry;
    private final ApplicationConcurrency myConcurrency;

    @Inject
    public SandProfilerProgramRunner(ExecutorRegistry executorRegistry, ApplicationConcurrency concurrency) {
        myExecutorRegistry = executorRegistry;
        myConcurrency = concurrency;
    }

    @Override
    public String getRunnerId() {
        return "SandProfilerRunner";
    }

    @Override
    public boolean canRun(String executorId, RunProfile profile) {
        if (!(profile instanceof SandConfiguration)) {
            return false;
        }

        Executor executor = myExecutorRegistry.getExecutorById(executorId);
        if (executor == null || !(ExecutorGroup.getGroupIfProxy(executor) instanceof DefaultProfilerExecutorGroup profilerGroup)) {
            return false;
        }

        ProfilerExecutorSettings settings = profilerGroup.getRegisteredSettings(executorId);
        return settings != null && settings.getState() instanceof SandProfilerConfigurationState && settings.canRun(profile);
    }

    @Override
    protected @Nullable RunContentDescriptor doExecute(RunProfileState state, ExecutionEnvironment environment) throws ExecutionException {
        RunContentDescriptor descriptor = super.doExecute(state, environment);
        if (descriptor == null || !(state instanceof SandProfilerRunState profilerState)) {
            return descriptor;
        }

        ProcessHandler processHandler = descriptor.getProcessHandler();
        if (processHandler == null) {
            return descriptor;
        }

        Project project = environment.getProject();
        SandProfilerProcess process = SandProfilerProcess.launched(
            project,
            myConcurrency,
            new SandTargetProcess(descriptor.getDisplayName(), 0),
            profilerState.getConfiguration(),
            processHandler
        );
        ProfilerToolWindowManager.getInstance(project).addProfilerProcessTab(process, true);
        return descriptor;
    }
}
