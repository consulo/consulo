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

import consulo.execution.RunManager;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.RunnerSettings;
import consulo.execution.ui.RunContentDescriptor;
import consulo.process.ExecutionException;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

public abstract class BaseProgramRunner<Settings extends RunnerSettings> implements ProgramRunner<Settings> {
    @RequiredUIAccess
    @Override
    public void execute(ExecutionEnvironment environment) throws ExecutionException {
        RunProfileState state = environment.getState();
        if (state == null) {
            return;
        }

        RunManager.getInstance(environment.getProject()).refreshUsagesList(environment.getRunProfile());
        
        execute(environment, state);
    }

    @RequiredUIAccess
    protected abstract void execute(ExecutionEnvironment environment, RunProfileState state) throws ExecutionException;

    static @Nullable RunContentDescriptor postProcess(ExecutionEnvironment environment, @Nullable RunContentDescriptor descriptor) {
        if (descriptor != null) {
            descriptor.setExecutionId(environment.getExecutionId());
        }

        Callback callback = environment.getCallback();
        if (callback != null) {
            callback.processStarted(descriptor);
        }
        return descriptor;
    }
}