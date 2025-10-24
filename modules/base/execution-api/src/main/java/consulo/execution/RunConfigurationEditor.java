/*
 * Copyright 2013-2022 consulo.io
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
import consulo.annotation.component.ServiceAPI;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2022-04-05
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface RunConfigurationEditor {
    static RunConfigurationEditor getInstance(Project project) {
        return project.getInstance(RunConfigurationEditor.class);
    }

    @RequiredUIAccess
    void editAll();

    @RequiredUIAccess
    void editOne(@Nonnull RunnerAndConfigurationSettings configuration);

    default boolean editConfiguration(Project project, RunnerAndConfigurationSettings configuration, LocalizeValue title) {
        return editConfiguration(project, configuration, title, null);
    }

    default boolean editConfiguration(@Nonnull ExecutionEnvironment environment, @Nonnull LocalizeValue title) {
        return editConfiguration(
            environment.getProject(),
            environment.getRunnerAndConfigurationSettings(),
            title,
            environment.getExecutor()
        );
    }

    @Deprecated
    @DeprecationInfo("Use consulo.execution.RunConfigurationEditor#editConfiguration(RunnerAndConfigurationSettings)")
    default boolean editConfiguration(
        Project project,
        RunnerAndConfigurationSettings configuration,
        @Nonnull LocalizeValue title,
        @Nullable Executor executor
    ) {
        return editConfiguration(project, configuration, title.get(), executor);
    }

    @Deprecated
    @DeprecationInfo("Use consulo.execution.RunConfigurationEditor#editConfiguration(RunnerAndConfigurationSettings)")
    default boolean editConfiguration(Project project, RunnerAndConfigurationSettings configuration, String title) {
        return editConfiguration(project, configuration, title, null);
    }

    @Deprecated
    @DeprecationInfo("Use consulo.execution.RunConfigurationEditor#editConfiguration(RunnerAndConfigurationSettings)")
    default boolean editConfiguration(@Nonnull ExecutionEnvironment environment, @Nonnull String title) {
        return editConfiguration(
            environment.getProject(),
            environment.getRunnerAndConfigurationSettings(),
            title,
            environment.getExecutor()
        );
    }

    @Deprecated
    @DeprecationInfo("Use consulo.execution.RunConfigurationEditor#editConfiguration(RunnerAndConfigurationSettings)")
    boolean editConfiguration(Project project, RunnerAndConfigurationSettings configuration, String title, @Nullable Executor executor);
}