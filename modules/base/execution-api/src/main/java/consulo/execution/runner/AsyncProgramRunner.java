/*
 * Copyright 2013-2017 consulo.io
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

import consulo.application.util.function.ThrowableComputable;
import consulo.execution.ExecutionManager;
import consulo.execution.RunProfileStarter;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.RunnerSettings;
import consulo.execution.ui.RunContentDescriptor;
import consulo.process.ExecutionException;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 05-May-17
 */
public abstract class AsyncProgramRunner<Settings extends RunnerSettings> extends BaseProgramRunner<Settings> {
    @RequiredUIAccess
    @Override
    protected final void execute(@Nonnull ExecutionEnvironment environment, @Nonnull RunProfileState state) throws ExecutionException {
        startRunProfile(environment, state, this::executeImpl);
    }

    @Nonnull
    protected abstract CompletableFuture<RunContentDescriptor> executeImpl(
        @Nonnull RunProfileState state,
        @Nonnull ExecutionEnvironment environment
    ) throws ExecutionException;

    @RequiredUIAccess
    protected static void startRunProfile(ExecutionEnvironment environment,
                                          RunProfileState state,
                                          @Nullable RunProfileStarter starter) {

        ThrowableComputable<CompletableFuture<RunContentDescriptor>, ExecutionException> func = () -> {
            CompletableFuture<RunContentDescriptor> future = starter == null
                ? CompletableFuture.completedFuture(null)
                : starter.executeAsync(state, environment);

            return future.whenComplete((runContentDescriptor, throwable) -> {
                if (throwable == null) {
                    BaseProgramRunner.postProcess(environment, runContentDescriptor);
                }
            });
        };

        ExecutionManager.getInstance(environment.getProject()).startRunProfile((s, e) -> func.get(), state, environment);
    }
}

