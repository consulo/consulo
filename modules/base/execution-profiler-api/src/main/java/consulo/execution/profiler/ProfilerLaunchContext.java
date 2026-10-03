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
import consulo.util.dataholder.UserDataHolder;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * What a {@link ProfilerConfigurationExtension} may change in a launch. Runtimes launch in different ways, so each part is
 * offered only when the launch has it: a JVM has options, a native program has a command line, an Android app has
 * neither and offers its own launch parameters instead.
 * <p>
 * The same context is passed to the patch and to the attach of one launch; its user data carries values between them.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public interface ProfilerLaunchContext extends UserDataHolder {
    Project getProject();

    /**
     * @return the child executor of {@link DefaultProfilerExecutorGroup} the launch runs with
     */
    Executor getExecutor();

    @Nullable RunnerSettings getRunnerSettings();

    /**
     * @return the mutable environment of the launched process, or null when it cannot be changed
     */
    @Nullable Map<String, String> getEnvironment();

    /**
     * @return the mutable runtime options, such as JVM options or interpreter switches, or null when the runtime takes none
     */
    @Nullable List<String> getVmOptions();

    /**
     * @return the command line which starts the process, or null when it is not started from one
     */
    @Nullable GeneralCommandLine getCommandLine();

    /**
     * @return the runtime's own launch object of that class, such as its Java parameters, or null when the launch has none
     */
    <P> @Nullable P getLaunchParameters(Class<P> parametersClass);
}
