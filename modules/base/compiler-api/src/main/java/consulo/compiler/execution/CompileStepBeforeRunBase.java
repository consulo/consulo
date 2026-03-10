/*
 * Copyright 2013-2026 consulo.io
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

import consulo.compiler.CompilerRunner;
import consulo.component.extension.ExtensionPoint;
import consulo.dataContext.DataContext;
import consulo.execution.BeforeRunTask;
import consulo.execution.BeforeRunTaskProvider;
import consulo.execution.configuration.RunConfiguration;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.concurrent.AsyncResult;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2026-02-24
 */
public abstract class CompileStepBeforeRunBase<T extends BeforeRunTask<T>> extends BeforeRunTaskProvider<T> {
    protected final Project myProject;

    protected CompileStepBeforeRunBase(Project project) {
        myProject = project;
    }

    @Nonnull
    @RequiredUIAccess
    @Override
    public AsyncResult<Void> configureTask(RunConfiguration runConfiguration, T task) {
        return AsyncResult.rejected();
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public Image getIcon(@Nonnull RunConfiguration runConfiguration) {
        ExtensionPoint<CompilerRunner> point = myProject.getExtensionPoint(CompilerRunner.class);

        DataContext context = DataContext.builder()
            .add(Project.KEY, myProject)
            .add(RunConfiguration.KEY, runConfiguration)
            .build();

        CompilerRunner.Result result = point.computeSafeIfAny(r ->
            r.checkAvailable(context) instanceof CompilerRunner.YesResult yes ? yes : null
        );

        if (result instanceof CompilerRunner.YesResult yesResult) {
            return yesResult.buildIcon();
        }
        return PlatformIconGroup.actionsCompile();
    }
}
