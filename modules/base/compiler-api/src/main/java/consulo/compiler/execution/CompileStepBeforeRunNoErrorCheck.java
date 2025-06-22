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
package consulo.compiler.execution;

import consulo.annotation.component.ExtensionImpl;
import consulo.compiler.CompilerRunner;
import consulo.component.extension.ExtensionPoint;
import consulo.dataContext.DataContext;
import consulo.execution.BeforeRunTask;
import consulo.execution.BeforeRunTaskProvider;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunProfileWithCompileBeforeLaunchOption;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

/**
 * @author Vassiliy.Kudryashov
 */
@ExtensionImpl(id = "compileBeforeRunNoErrorCheck", order = "after compileBeforeRun")
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
        return ExecutionLocalize.beforeLaunchCompileStepNoErrorCheck().get();
    }

    @Override
    public Image getIcon() {
        ExtensionPoint<CompilerRunner> point = myProject.getExtensionPoint(CompilerRunner.class);
        CompilerRunner runner = point.findFirstSafe(CompilerRunner::isAvailable);
        if (runner != null) {
            return runner.getBuildIcon();
        }
        return PlatformIconGroup.actionsCompile();
    }

    @Override
    public Image getTaskIcon(MakeBeforeRunTaskNoErrorCheck task) {
        return getIcon();
    }

    @Override
    public MakeBeforeRunTaskNoErrorCheck createTask(RunConfiguration runConfiguration) {
        return runConfiguration instanceof RunProfileWithCompileBeforeLaunchOption ? new MakeBeforeRunTaskNoErrorCheck() : null;
    }

    @Nonnull
    @Override
    @RequiredUIAccess
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
        return ExecutionLocalize.beforeLaunchCompileStepNoErrorCheck().get();
    }

    @Nonnull
    @Override
    public AsyncResult<Void> executeTaskAsync(
        UIAccess uiAccess,
        DataContext context,
        RunConfiguration configuration,
        ExecutionEnvironment env,
        MakeBeforeRunTaskNoErrorCheck task
    ) {
        return CompileStepBeforeRun.doMake(uiAccess, myProject, configuration, true);
    }
}
