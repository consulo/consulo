/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.compiler.options;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileWithCompileBeforeLaunchOption;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;

/**
 * User: Vassiliy.Kudryashov
 */
public class CompileStepBeforeRunNoErrorCheck extends BeforeRunTaskProvider<CompileStepBeforeRunNoErrorCheck.MakeBeforeRunTaskNoErrorCheck> {

  public static class MakeBeforeRunTaskNoErrorCheck extends BeforeRunTask<MakeBeforeRunTaskNoErrorCheck> {
    private MakeBeforeRunTaskNoErrorCheck() {
      super(ID);
    }
  }

  public static final Key<MakeBeforeRunTaskNoErrorCheck> ID = Key.create("MakeNoErrorCheck");
  @Nonnull
  private final Project myProject;

  @Inject
  public CompileStepBeforeRunNoErrorCheck(@Nonnull Project project) {
    myProject = project;
  }

  @Nonnull
  @Override
  public Key<MakeBeforeRunTaskNoErrorCheck> getId() {
    return ID;
  }

  @Nonnull
  @Override
  public String getDescription(MakeBeforeRunTaskNoErrorCheck task) {
    return ExecutionBundle.message("before.launch.compile.step.no.error.check");
  }

  @Override
  public Image getIcon() {
    return AllIcons.Actions.Compile;
  }

  @Override
  public Image getTaskIcon(MakeBeforeRunTaskNoErrorCheck task) {
    return AllIcons.Actions.Compile;
  }

  @Override
  public MakeBeforeRunTaskNoErrorCheck createTask(RunConfiguration runConfiguration) {
    return runConfiguration instanceof RunProfileWithCompileBeforeLaunchOption ? new MakeBeforeRunTaskNoErrorCheck() : null;
  }

  @Nonnull
  @RequiredUIAccess
  @Override
  public AsyncResult<Void> configureTask(RunConfiguration runConfiguration, MakeBeforeRunTaskNoErrorCheck task) {
    return AsyncResult.rejected();
  }

  @Override
  public boolean isConfigurable() {
    return false;
  }

  @Nonnull
  @Override
  public String getName() {
    return ExecutionBundle.message("before.launch.compile.step.no.error.check");
  }

  @Nonnull
  @Override
  public AsyncResult<Void> executeTaskAsync(UIAccess uiAccess, DataContext context, RunConfiguration configuration, ExecutionEnvironment env, MakeBeforeRunTaskNoErrorCheck task) {
    return CompileStepBeforeRun.doMake(uiAccess, myProject, configuration, true);
  }
}
