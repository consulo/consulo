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

package com.intellij.execution.impl;

import com.intellij.execution.RunnerRegistry;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.util.Comparing;
import consulo.annotations.NotLazy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

// TODO[spLeaner]: eliminate
@Singleton
@NotLazy
public class RunnerRegistryImpl extends RunnerRegistry implements Disposable {
  private final List<ProgramRunner> myRunnersOrder = new ArrayList<>();

  @Override
  public boolean hasRunner(@Nonnull final String executorId, @Nonnull final RunProfile settings) {
    final ProgramRunner[] runners = getRegisteredRunners();
    for (final ProgramRunner runner : runners) {
      if (runner.canRun(executorId, settings)) {
        return true;
      }
    }

    return false;
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

  @PostConstruct
  public void initComponent() {
    final ProgramRunner[] runners = Extensions.getExtensions(ProgramRunner.PROGRAM_RUNNER_EP);
    for (ProgramRunner runner : runners) {
      registerRunner(runner);
    }
  }

  @Override
  public synchronized void dispose() {
    while (myRunnersOrder.size() > 0) {
      final ProgramRunner runner = myRunnersOrder.get(myRunnersOrder.size() - 1);
      unregisterRunner(runner);
    }
  }

  public synchronized void registerRunner(final ProgramRunner runner) {
    if (myRunnersOrder.contains(runner)) return;
    myRunnersOrder.add(runner);
  }

  public synchronized void unregisterRunner(final ProgramRunner runner) {
    myRunnersOrder.remove(runner);
  }

  @Override
  public synchronized ProgramRunner[] getRegisteredRunners() {
    return myRunnersOrder.toArray(new ProgramRunner[myRunnersOrder.size()]);
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

}
