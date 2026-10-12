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
package consulo.sandboxPlugin.ide.debug;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.ExecutionResult;
import consulo.execution.configuration.RunProfile;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XDebuggerManager;
import consulo.execution.runner.DefaultProgramRunner;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ui.RunContentDescriptor;
import consulo.process.ExecutionException;
import consulo.sandboxPlugin.ide.run.SandConfiguration;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-12
 */
@ExtensionImpl
public class SandDebugRunner extends DefaultProgramRunner {
    @Override
    public String getRunnerId() {
        return "SandDebugRunner";
    }

    @Override
    public boolean canRun(String executorId, RunProfile profile) {
        return DefaultDebugExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof SandConfiguration;
    }

    @Override
    protected @Nullable RunContentDescriptor doExecute(RunProfileState state, ExecutionEnvironment env) throws ExecutionException {
        ExecutionResult executionResult = state.execute(env.getExecutor(), this);
        if (executionResult == null) {
            return null;
        }

        XDebugSession session = XDebuggerManager.getInstance(env.getProject())
            .startSession(env, debugSession -> new SandDebugProcess(debugSession, executionResult));
        return session.getRunContentDescriptor();
    }
}
