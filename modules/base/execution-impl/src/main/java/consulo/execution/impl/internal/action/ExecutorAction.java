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
package consulo.execution.impl.internal.action;

import consulo.application.dumb.DumbAware;
import consulo.execution.*;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.executor.Executor;
import consulo.execution.executor.ExecutorRegistry;
import consulo.execution.impl.internal.ExecutionManagerImpl;
import consulo.execution.internal.ExecutionActionValue;
import consulo.execution.internal.RunManagerEx;
import consulo.execution.runner.ExecutionEnvironmentBuilder;
import consulo.execution.runner.ProgramRunner;
import consulo.execution.runner.RunnerRegistry;
import consulo.execution.ui.RunContentDescriptor;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.startup.StartupManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public class ExecutorAction extends AnAction implements DumbAware {
    private final Executor myExecutor;
    @Nonnull
    private final ExecutorRegistry myExecutorRegistry;
    @Nonnull
    private final RunCurrentFileService myRunCurrentFileService;

    public ExecutorAction(@Nonnull ExecutorRegistry executorRegistry,
                          @Nonnull Executor executor,
                          @Nonnull RunCurrentFileService runCurrentFileService) {
        super(executor.getStartActionText(), executor.getDescription(), executor.getIcon());
        myExecutorRegistry = executorRegistry;
        myRunCurrentFileService = runCurrentFileService;
        myExecutor = executor;
        getTemplatePresentation().setVisible(false);
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull final AnActionEvent e) {
        final Presentation presentation = e.getPresentation();
        final Project project = e.getData(Project.KEY);

        if (project == null || project.isDisposed()) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        presentation.setVisible(myExecutor.isApplicable(project));
        if (!presentation.isVisible()) {
            return;
        }

        if (DumbService.getInstance(project).isDumb() || !project.isInitialized()) {
            presentation.setEnabled(false);
            return;
        }

        final RunnerAndConfigurationSettings selectedConfiguration = getConfiguration(project);
        boolean enabled;

        LocalizeValue text;
        if (selectedConfiguration != null) {
            presentation.setIcon(getInformativeIcon(project, myExecutor, selectedConfiguration));

            final ProgramRunner runner = RunnerRegistry.getInstance().getRunner(myExecutor.getId(), selectedConfiguration.getConfiguration());

            ExecutionTarget target = ExecutionTargetManager.getActiveTarget(project);
            enabled = ExecutionTargetManager.canRun(selectedConfiguration, target)
                && runner != null && !myExecutorRegistry.isStarting(project, myExecutor.getId(), runner.getRunnerId());

            if (enabled) {
                presentation.setDescriptionValue(myExecutor.getDescription());
            }
            text = ExecutionActionValue.buildWithConfiguration(myExecutor::getStartActiveText, selectedConfiguration.getName());
        }
        else {
            // don't compute current file to run if editors are not yet loaded
            if (!project.isDefault() && !StartupManager.getInstance(project).postStartupActivityPassed()) {
                presentation.setEnabled(false);
                return;
            }

            RunCurrentFileActionStatus status = myRunCurrentFileService.getRunCurrentFileActionStatus(myExecutor, e, false);
            enabled = status.enabled();
            text = status.tooltip();
            presentation.setIcon(status.icon());
        }

        presentation.setEnabled(enabled);
        presentation.setTextValue(text);
    }

    @Nonnull
    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Nonnull
    public static Image getInformativeIcon(Project project,
                                           Executor executor,
                                           RunnerAndConfigurationSettings selectedConfiguration) {
        final ExecutionManagerImpl executionManager = ExecutionManagerImpl.getInstance(project);

        List<RunContentDescriptor> runningDescriptors = executionManager.getRunningDescriptors(s -> s == selectedConfiguration);
        runningDescriptors = ContainerUtil.filter(runningDescriptors, descriptor -> {
            RunContentDescriptor contentDescriptor = executionManager.getContentManager().findContentDescriptor(executor, descriptor.getProcessHandler());
            return contentDescriptor != null && executionManager.getExecutors(contentDescriptor).contains(executor);
        });

        if (!runningDescriptors.isEmpty() && DefaultRunExecutor.EXECUTOR_ID.equals(executor.getId()) && selectedConfiguration.isSingleton()) {
            return PlatformIconGroup.actionsRestart();
        }
        if (runningDescriptors.isEmpty()) {
            return executor.getIcon();
        }

        if (runningDescriptors.size() == 1) {
            return ExecutionUtil.getIconWithLiveIndicator(executor.getIcon());
        }
        else {
            return ImageEffects.withText(executor.getIcon(), String.valueOf(runningDescriptors.size()));
        }
    }

    @Nullable
    private RunnerAndConfigurationSettings getConfiguration(@Nonnull final Project project) {
        return RunManagerEx.getInstanceEx(project).getSelectedConfiguration();
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull final AnActionEvent e) {
        final Project project = e.getData(Project.KEY);
        if (project == null || project.isDisposed()) {
            return;
        }

        RunnerAndConfigurationSettings configuration = getConfiguration(project);
        if (configuration != null) {
            ExecutionEnvironmentBuilder builder = ExecutionEnvironmentBuilder.createOrNull(myExecutor, configuration);
            if (builder == null) {
                return;
            }
            ExecutionManager.getInstance(project).restartRunProfile(builder.activeTarget().dataContext(e.getDataContext()).build());
        }
        else {
            myRunCurrentFileService.runCurrentFile(myExecutor, e);
        }
    }
}
