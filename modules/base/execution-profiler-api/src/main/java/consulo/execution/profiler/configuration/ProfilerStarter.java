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
package consulo.execution.profiler.configuration;

import consulo.execution.configuration.RunProfile;
import consulo.project.Project;

/**
 * Says where a configuration can launch processes under the profiler: it decides whether its "Profile with" entry is
 * shown and enabled.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public interface ProfilerStarter {
    /**
     * @return whether the run profile can be started under this configuration
     */
    boolean canRun(RunProfile profile);

    /**
     * @return whether the configuration is usable in the project at all, for example because its runtime is set up
     */
    boolean isApplicable(Project project);
}
