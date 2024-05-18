/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.execution.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.execution.RunnerRegistry;
import consulo.execution.configuration.RunProfile;
import consulo.execution.runner.ProgramRunner;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

// TODO[spLeaner]: eliminate
@Singleton
@ServiceImpl
public class RunnerRegistryImpl extends RunnerRegistry {
  @Override
  public boolean hasRunner(@Nonnull final String executorId, @Nonnull final RunProfile settings) {
    return getRunner(executorId, settings) != null;
  }

  @Override
  public ProgramRunner getRunner(final String executorId, final RunProfile settings) {
    final ProgramRunner[] runners = getRegisteredRunners();
    for (final ProgramRunner runner : runners) {
      if (runner.canRun(executorId, settings)) {
        return runner;
      }
    }

    return null;
  }

  @Override
  @Nullable
  public ProgramRunner findRunnerById(String id) {
    ProgramRunner[] registeredRunners = getRegisteredRunners();
    for (ProgramRunner registeredRunner : registeredRunners) {
      if (Comparing.equal(id, registeredRunner.getRunnerId())) {
        return registeredRunner;
      }
    }
    return null;
  }

  @Override
  public ProgramRunner[] getRegisteredRunners() {
    return ProgramRunner.PROGRAM_RUNNER_EP.getExtensions();
  }
}