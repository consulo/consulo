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
package consulo.execution.profiler;

import consulo.execution.configuration.RunnerSettings;
import consulo.execution.executor.Executor;
import consulo.process.cmd.GeneralCommandLine;
import consulo.project.Project;
import consulo.util.dataholder.UserDataHolderBase;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A {@link ProfilerLaunchContext} filled by the runtime that launches the process. Without an explicit environment, the
 * command line's environment is offered.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public class SimpleProfilerLaunchContext extends UserDataHolderBase implements ProfilerLaunchContext {
    private final Project myProject;
    private final Executor myExecutor;
    private final @Nullable RunnerSettings myRunnerSettings;
    private final List<Object> myLaunchParameters = new ArrayList<>();
    private @Nullable Map<String, String> myEnvironment;
    private @Nullable List<String> myVmOptions;
    private @Nullable GeneralCommandLine myCommandLine;

    public SimpleProfilerLaunchContext(Project project, Executor executor, @Nullable RunnerSettings runnerSettings) {
        myProject = project;
        myExecutor = executor;
        myRunnerSettings = runnerSettings;
    }

    public SimpleProfilerLaunchContext withEnvironment(Map<String, String> environment) {
        myEnvironment = environment;
        return this;
    }

    public SimpleProfilerLaunchContext withVmOptions(List<String> vmOptions) {
        myVmOptions = vmOptions;
        return this;
    }

    public SimpleProfilerLaunchContext withCommandLine(GeneralCommandLine commandLine) {
        myCommandLine = commandLine;
        return this;
    }

    public SimpleProfilerLaunchContext withLaunchParameters(Object parameters) {
        myLaunchParameters.add(parameters);
        return this;
    }

    @Override
    public Project getProject() {
        return myProject;
    }

    @Override
    public Executor getExecutor() {
        return myExecutor;
    }

    @Override
    public @Nullable RunnerSettings getRunnerSettings() {
        return myRunnerSettings;
    }

    @Override
    public @Nullable Map<String, String> getEnvironment() {
        Map<String, String> environment = myEnvironment;
        if (environment != null) {
            return environment;
        }
        GeneralCommandLine commandLine = myCommandLine;
        return commandLine == null ? null : commandLine.getEnvironment();
    }

    @Override
    public @Nullable List<String> getVmOptions() {
        return myVmOptions;
    }

    @Override
    public @Nullable GeneralCommandLine getCommandLine() {
        return myCommandLine;
    }

    @Override
    public <P> @Nullable P getLaunchParameters(Class<P> parametersClass) {
        for (Object parameters : myLaunchParameters) {
            if (parametersClass.isInstance(parameters)) {
                return parametersClass.cast(parameters);
            }
        }
        GeneralCommandLine commandLine = myCommandLine;
        if (commandLine != null && parametersClass.isInstance(commandLine)) {
            return parametersClass.cast(commandLine);
        }
        return null;
    }
}
