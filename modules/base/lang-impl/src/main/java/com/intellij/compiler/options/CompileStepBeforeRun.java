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
package com.intellij.compiler.options;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfileWithCompileBeforeLaunchOption;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;

/**
 * @author spleaner
 */
public class CompileStepBeforeRun extends BeforeRunTaskProvider<CompileStepBeforeRun.MakeBeforeRunTask> {
  /**
   * Marked for disable adding CompileStepBeforeRun
   */
  public static interface Suppressor {

  }

  private static final Logger LOG = Logger.getInstance(CompileStepBeforeRun.class);
  public static final Key<MakeBeforeRunTask> ID = Key.create("Make");

  @NonNls
  protected static final String MAKE_PROJECT_ON_RUN_KEY = "makeProjectOnRun";

  private final Project myProject;

  @Inject
  public CompileStepBeforeRun(@Nonnull final Project project) {
    myProject = project;
  }

  @Nonnull
  @Override
  public Key<MakeBeforeRunTask> getId() {
    return ID;
  }

  @Nonnull
  @Override
  public String getName() {
    return ExecutionBundle.message("before.launch.compile.step");
  }

  @Nonnull
  @Override
  public String getDescription(MakeBeforeRunTask task) {
    return ExecutionBundle.message("before.launch.compile.step");
  }

  @Override
  public Image getIcon() {
    return AllIcons.Actions.Compile;
  }

  @Override
  public Image getTaskIcon(MakeBeforeRunTask task) {
    return AllIcons.Actions.Compile;
  }

  @Override
  public MakeBeforeRunTask createTask(RunConfiguration configuration) {
    MakeBeforeRunTask task = null;

    if (!(configuration instanceof Suppressor) && configuration instanceof RunProfileWithCompileBeforeLaunchOption) {
      task = new MakeBeforeRunTask();
      if (configuration instanceof RunConfigurationBase) {
        task.setEnabled(((RunConfigurationBase)configuration).isCompileBeforeLaunchAddedByDefault());
      }
    }
    return task;
  }

  @Nonnull
  @RequiredUIAccess
  @Override
  public AsyncResult<Void> configureTask(RunConfiguration runConfiguration, MakeBeforeRunTask task) {
    return AsyncResult.rejected();
  }

  @Nonnull
  @Override
  public AsyncResult<Void> executeTaskAsync(UIAccess uiAccess, DataContext context, RunConfiguration configuration, ExecutionEnvironment env, MakeBeforeRunTask task) {
    return doMake(uiAccess, myProject, configuration, false);
  }

  static AsyncResult<Void> doMake(UIAccess uiAccess, final Project myProject, final RunConfiguration configuration, final boolean ignoreErrors) {
    if (!(configuration instanceof RunProfileWithCompileBeforeLaunchOption)) {
      return AsyncResult.rejected();
    }

    if (configuration instanceof RunConfigurationBase && ((RunConfigurationBase)configuration).excludeCompileBeforeLaunchOption()) {
      return AsyncResult.resolved();
    }

    final RunProfileWithCompileBeforeLaunchOption runConfiguration = (RunProfileWithCompileBeforeLaunchOption)configuration;
    AsyncResult<Void> result = AsyncResult.undefined();
    try {
      final CompileStatusNotification callback = (aborted, errors, warnings, compileContext) -> {
        if ((errors == 0 || ignoreErrors) && !aborted) {
          result.setDone();
        }
        else {
          result.setRejected();
        }
      };

      TransactionGuard.submitTransaction(myProject, () -> {
        CompileScope scope;
        final CompilerManager compilerManager = CompilerManager.getInstance(myProject);
        if (Comparing.equal(Boolean.TRUE.toString(), System.getProperty(MAKE_PROJECT_ON_RUN_KEY))) {
          // user explicitly requested whole-project make
          scope = compilerManager.createProjectCompileScope();
        }
        else {
          final Module[] modules = runConfiguration.getModules();
          if (modules.length > 0) {
            for (Module module : modules) {
              if (module == null) {
                LOG.error("RunConfiguration should not return null modules. Configuration=" + runConfiguration.getName() + "; class=" + runConfiguration.getClass().getName());
              }
            }
            scope = compilerManager.createModulesCompileScope(modules, true);
          }
          else {
            scope = compilerManager.createProjectCompileScope();
          }
        }

        if (!myProject.isDisposed()) {
          compilerManager.make(scope, callback);
        }
        else {
          result.setRejected();
        }
      });
    }
    catch (Exception e) {
      result.rejectWithThrowable(e);
    }

    return result;
  }

  public static class MakeBeforeRunTask extends BeforeRunTask<MakeBeforeRunTask> {
    private MakeBeforeRunTask() {
      super(ID);
      setEnabled(true);
    }
  }
}
