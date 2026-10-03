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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.profiler.configuration.ProfilerConfigurationState;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import org.jspecify.annotations.Nullable;

/**
 * Starts processes under a profiler: changes how a run configuration launches its process, then attaches to the started
 * process.
 * <p>
 * The plugin which launches a run configuration finds the configuration of the launch with
 * {@code DefaultProfilerExecutorGroup.getInstance().getRegisteredSettings(executorId)}, and calls the extensions which are
 * applicable to the run configuration and enabled for that configuration.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ProfilerConfigurationExtension {
    /**
     * @return whether this extension knows how to launch the run configuration
     */
    boolean isApplicableFor(RunConfigurationBase configuration);

    /**
     * @return whether this extension handles launches with the profiler configuration
     */
    boolean isEnabledFor(RunConfigurationBase configuration, ProfilerConfigurationState state);

    /**
     * Changes the launch before the process starts, for example by adding agent options or environment variables.
     */
    void patch(RunConfigurationBase configuration, ProfilerConfigurationState state, ProfilerLaunchContext context)
        throws ExecutionException;

    /**
     * Called once the process started.
     *
     * @return the profiled process to show as a profiling session, or null when nothing is profiled
     */
    @Nullable ProfilerProcess<?> attachToProcess(
        RunConfigurationBase configuration,
        ProcessHandler handler,
        ProfilerConfigurationState state,
        ProfilerLaunchContext context
    );
}
