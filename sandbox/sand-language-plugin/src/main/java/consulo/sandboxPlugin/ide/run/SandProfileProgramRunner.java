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
package consulo.sandboxPlugin.ide.run;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.configuration.RunProfile;
import consulo.execution.executor.Executor;
import consulo.execution.executor.ExecutorGroup;
import consulo.execution.executor.ExecutorRegistry;
import consulo.execution.runner.DefaultProgramRunner;
import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionImpl
public class SandProfileProgramRunner extends DefaultProgramRunner {
    private final ExecutorRegistry myExecutorRegistry;

    @Inject
    public SandProfileProgramRunner(ExecutorRegistry executorRegistry) {
        myExecutorRegistry = executorRegistry;
    }

    @Override
    public String getRunnerId() {
        return "SandProfileRunner";
    }

    @Override
    public boolean canRun(String executorId, RunProfile profile) {
        Executor executor = myExecutorRegistry.getExecutorById(executorId);
        if (executor == null || !(ExecutorGroup.getGroupIfProxy(executor) instanceof SandExecutorGroup executorGroup)) {
            return false;
        }
        SandExecutorSettings settings = executorGroup.getRegisteredSettings(executorId);
        return settings != null && settings.canRun(profile);
    }
}
