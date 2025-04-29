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
package consulo.execution;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.dataContext.DataContext;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Vladislav.Kaznacheev
 * @since 2007-07-04
 */
@ExtensionAPI(ComponentScope.PROJECT)
public abstract class BeforeRunTaskProvider<T extends BeforeRunTask> {
    @Nonnull
    public abstract Key<T> getId();

    @Nonnull
    public abstract String getName();

    @Nullable
    public abstract Image getIcon();

    @Nonnull
    public String getDescription(T task) {
        return getName();
    }

    @Nullable
    public Image getTaskIcon(T task) {
        return getIcon();
    }

    public boolean isConfigurable() {
        return false;
    }

    /**
     * @return <code>true</code> if at most one task may be configured
     */
    public boolean isSingleton() {
        return false;
    }

    /**
     * @return 'before run' task for the configuration or null, if the task from this provider is not applicable to the specified configuration
     */
    @Nullable
    public abstract T createTask(RunConfiguration runConfiguration);

    /**
     * @return <code>true</code> if task configuration is changed
     */
    @Nonnull
    @RequiredUIAccess
    public abstract AsyncResult<Void> configureTask(RunConfiguration runConfiguration, T task);

    public boolean canExecuteTask(RunConfiguration configuration, T task) {
        return true;
    }

    @Deprecated
    @DeprecationInfo("See executeTaskAsync()")
    public boolean executeTask(DataContext context, RunConfiguration configuration, ExecutionEnvironment env, T task) {
        throw new AbstractMethodError();
    }

    @Nonnull
    @SuppressWarnings("deprecation")
    public AsyncResult<Void> executeTaskAsync(
        UIAccess uiAccess,
        DataContext context,
        RunConfiguration configuration,
        ExecutionEnvironment env,
        T task
    ) {
        return executeTask(context, configuration, env, task) ? AsyncResult.resolved() : AsyncResult.rejected();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T extends BeforeRunTask> BeforeRunTaskProvider<T> getProvider(Project project, Key<T> key) {
        return project.getExtensionPoint(BeforeRunTaskProvider.class).findFirstSafe(provider -> provider.getId() == key);
    }
}