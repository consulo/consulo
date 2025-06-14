/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
import consulo.dataContext.DataManager;
import consulo.execution.ExecutionManager;
import consulo.execution.ExecutionUtil;
import consulo.execution.executor.ExecutorRegistry;
import consulo.execution.impl.internal.ExecutionManagerImpl;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ui.RunContentDescriptor;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.process.ProcessHandler;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

public class FakeRerunAction extends AnAction implements DumbAware {
    public FakeRerunAction() {
        super(CommonLocalize.actionRerun(), CommonLocalize.actionRerun(), PlatformIconGroup.actionsRestart());
    }

    @Override
    @RequiredUIAccess
    public void update(@Nonnull AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        ExecutionEnvironment environment = getEnvironment(event);
        if (environment != null) {
            presentation.setTextValue(ExecutionLocalize.rerunConfigurationActionName(environment.getRunProfile().getName()));
            presentation.setIcon(
                ExecutionManagerImpl.isProcessRunning(getDescriptor(event))
                    ? PlatformIconGroup.actionsRestart()
                    : environment.getExecutor().getIcon()
            );
            presentation.setEnabled(isEnabled(event));
            return;
        }

        presentation.setEnabled(false);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent event) {
        ExecutionEnvironment environment = getEnvironment(event);
        if (environment != null) {
            ExecutionUtil.restart(environment);
        }
    }

    @Nullable
    protected RunContentDescriptor getDescriptor(AnActionEvent event) {
        return event.getData(RunContentDescriptor.KEY);
    }

    @Nullable
    protected ExecutionEnvironment getEnvironment(@Nonnull AnActionEvent event) {
        ExecutionEnvironment environment = event.getData(ExecutionEnvironment.KEY);
        if (environment == null) {
            Project project = event.getData(Project.KEY);
            RunContentDescriptor contentDescriptor = project == null ? null
                : ExecutionManager.getInstance(project).getContentManager().getSelectedContent();
            if (contentDescriptor != null) {
                JComponent component = contentDescriptor.getComponent();
                if (component != null) {
                    environment = DataManager.getInstance().getDataContext(component).getData(ExecutionEnvironment.KEY);
                }
            }
        }
        return environment;
    }

    protected boolean isEnabled(AnActionEvent event) {
        RunContentDescriptor descriptor = getDescriptor(event);
        ProcessHandler processHandler = descriptor == null ? null : descriptor.getProcessHandler();
        ExecutionEnvironment environment = getEnvironment(event);
        return environment != null
            && !ExecutorRegistry.getInstance().isStarting(environment)
            && !(processHandler != null && processHandler.isProcessTerminating());
    }
}
