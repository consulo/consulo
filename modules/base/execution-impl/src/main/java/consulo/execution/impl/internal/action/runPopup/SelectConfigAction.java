/*
 * Copyright 2013-2025 consulo.io
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
package consulo.execution.impl.internal.action.runPopup;

import consulo.application.Application;
import consulo.execution.ExecutionTargetManager;
import consulo.execution.RunConfigurationEditor;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.ConfigurationType;
import consulo.execution.executor.Executor;
import consulo.execution.executor.ExecutorRegistry;
import consulo.execution.impl.internal.action.ExecutorAction;
import consulo.execution.impl.internal.action.RunConfigurationsComboBoxAction;
import consulo.execution.impl.internal.action.RunCurrentFileService;
import consulo.execution.internal.RunCurrentFileExecutor;
import consulo.execution.localize.ExecutionLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.util.ActionUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SelectConfigAction extends DumbAwareActionGroup implements AlwaysVisibleActionGroup {
    private final RunnerAndConfigurationSettings myConfiguration;
    private final Project myProject;
    private final String myTag;

    public SelectConfigAction(RunnerAndConfigurationSettings configuration,
                              Project project,
                              String tag) {
        myConfiguration = configuration;
        myProject = project;
        myTag = tag;
        String name = StringUtil.notNullize(configuration.getName());

        Presentation presentation = getTemplatePresentation();
        presentation.setDisabledMnemonic(true);
        presentation.setTextValue(LocalizeValue.of(name));
        presentation.setPopupGroup(true);
        presentation.setPerformGroup(true);

        ConfigurationType type = configuration.getType();
        if (type != null) {
            presentation.setDescriptionValue(ExecutionLocalize.select01(type.getConfigurationTypeDescription(), name));
        }
        updateIcon(presentation);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        RunManager.getInstance(myProject).setSelectedConfiguration(myConfiguration);

        RunConfigurationsComboBoxAction.updatePresentation(ExecutionTargetManager.getActiveTarget(myProject), myConfiguration, myProject, e.getPresentation(), e.getPlace());
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);

        Presentation presentation = e.getPresentation();

        updateIcon(presentation);

        presentation.putClientProperty(ActionFilterUtil.SEARCH_TAG, myTag);

        Application application = Application.get();

        List<Executor> executors = RunCurrentFileExecutor.getExecutors(application);

        List<AnAction> actionList = new ArrayList<>(executors.size());

        RunCurrentFileService fileService = application.getInstance(RunCurrentFileService.class);

        for (Executor executor : executors) {
            actionList.add(new ExecutorAction(ExecutorRegistry.getInstance(), executor, fileService) {
                @RequiredUIAccess
                @Override
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    RunManager.getInstance(myProject).setSelectedConfiguration(myConfiguration);
                    
                    super.actionPerformed(e);
                }

                @Nullable
                @Override
                protected RunnerAndConfigurationSettings getConfiguration(@Nonnull Project project) {
                    return myConfiguration;
                }
            });
        }

        presentation.putClientProperty(ActionUtil.INLINE_ACTIONS, actionList);
    }

    @Nonnull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        List<AnAction> actions = new ArrayList<>();
        actions.add(new DumbAwareAction(ExecutionLocalize.runConfigurationEditAction()) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                Project project = e.getRequiredData(Project.KEY);

                RunConfigurationEditor.getInstance(project).editOne(myConfiguration);
            }
        });

        if (myConfiguration.isTemporary())  {
            actions.add(new SaveConfigurationAction(myProject, myConfiguration));
        }

        actions.add(new DeleteConfigurationAction(myProject, myConfiguration));

        return actions.toArray(EMPTY_ARRAY);
    }

    private void updateIcon(Presentation presentation) {
        RunConfigurationsComboBoxAction.setConfigurationIcon(presentation, myConfiguration, myProject);
    }
}
