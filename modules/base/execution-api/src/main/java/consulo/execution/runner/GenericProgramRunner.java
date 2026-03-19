/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package consulo.execution.runner;

import consulo.execution.ExecutionManager;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.RunnerSettings;
import consulo.execution.ui.RunContentDescriptor;
import consulo.process.ExecutionException;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public abstract class GenericProgramRunner<Settings extends RunnerSettings> extends BaseProgramRunner<Settings> {
    @RequiredUIAccess
    @Override
    protected void execute(ExecutionEnvironment environment, RunProfileState state) throws ExecutionException {
        ExecutionManager.getInstance(environment.getProject()).startRunProfile((s, e) -> {
            return CompletableFuture.completedFuture(BaseProgramRunner.postProcess(environment, doExecute(state, environment)));
        }, state, environment);
    }

    protected @Nullable RunContentDescriptor doExecute(RunProfileState state, ExecutionEnvironment environment) throws ExecutionException {
        return null;
    }
}
