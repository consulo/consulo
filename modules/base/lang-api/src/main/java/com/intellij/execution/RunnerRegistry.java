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
package com.intellij.execution;

import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.components.ServiceManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class RunnerRegistry {
  public static RunnerRegistry getInstance() {
    return ServiceManager.getService(RunnerRegistry.class);
  }

  public abstract boolean hasRunner(@Nonnull final String executorId, @Nonnull final RunProfile settings);

  @Nullable
  public abstract ProgramRunner getRunner(final String executorId, final RunProfile settings);

  public abstract ProgramRunner[] getRegisteredRunners();

  @Nullable
  public abstract ProgramRunner findRunnerById(String id);
}
