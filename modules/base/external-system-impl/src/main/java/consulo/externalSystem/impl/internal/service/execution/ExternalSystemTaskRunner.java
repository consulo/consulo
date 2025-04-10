/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.externalSystem.impl.internal.service.execution;

import consulo.annotation.component.ExtensionImpl;
import consulo.externalSystem.service.execution.ExternalSystemRunConfiguration;
import consulo.process.ExecutionException;
import consulo.execution.ExecutionResult;
import consulo.execution.configuration.RunProfile;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.GenericProgramRunner;
import consulo.execution.runner.RunContentBuilder;
import consulo.execution.ui.RunContentDescriptor;
import consulo.externalSystem.util.ExternalSystemConstants;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Denis Zhdanov
 * @since 5/26/13 11:20 PM
 */
@ExtensionImpl
public class ExternalSystemTaskRunner extends GenericProgramRunner {

  @Nonnull
  @Override
  public String getRunnerId() {
    return ExternalSystemConstants.RUNNER_ID;
  }

  @Override
  public boolean canRun(@Nonnull String executorId, @Nonnull RunProfile profile) {
    return profile instanceof ExternalSystemRunConfiguration && DefaultRunExecutor.EXECUTOR_ID.equals(executorId);
  }

  @Nullable
  @Override
  protected RunContentDescriptor doExecute(@Nonnull RunProfileState state, @Nonnull ExecutionEnvironment env) throws ExecutionException {
    ExecutionResult executionResult = state.execute(env.getExecutor(), this);
    return executionResult == null ? null : new RunContentBuilder(executionResult, env).showRunContent(env.getContentToReuse());
  }
}
