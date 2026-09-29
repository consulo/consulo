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
package consulo.execution.impl.internal.ui;

import consulo.annotation.component.ServiceImpl;
import consulo.configuration.editor.ConfigurationFileEditorManager;
import consulo.execution.RunConfigurationEditor;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.executor.Executor;
import consulo.execution.impl.internal.configuration.RunManagerImpl;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * @author VISTALL
 * @since 2022-04-05
 */
@Singleton
@ServiceImpl
public class RunConfigurationEditorImpl implements RunConfigurationEditor {
    private final Project myProject;

    @Inject
    public RunConfigurationEditorImpl(Project project) {
        myProject = project;
    }

    @RequiredUIAccess
    @Override
    public void editAll() {
        open(Map.of());
    }

    @RequiredUIAccess
    @Override
    public void editOne(RunnerAndConfigurationSettings configuration) {
        open(Map.of(RunConfigurationEditorProvider.RUN_CONFIGURATION_ID, configuration.getUniqueID()));
    }

    @Override
    public boolean editConfiguration(
        Project project,
        RunnerAndConfigurationSettings configuration,
        String title,
        @Nullable Executor executor
    ) {
        project.getUIAccess().give(() -> {
            RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(project);
            if (runManager.getSettings(configuration.getConfiguration()) == null) {
                runManager.addConfiguration(configuration);
            }
            editOne(configuration);
        });
        return false;
    }

    @RequiredUIAccess
    private void open(Map<String, String> params) {
        myProject.getApplication()
            .getInstance(ConfigurationFileEditorManager.class)
            .open(myProject, RunConfigurationEditorProvider.class, params);
    }
}
