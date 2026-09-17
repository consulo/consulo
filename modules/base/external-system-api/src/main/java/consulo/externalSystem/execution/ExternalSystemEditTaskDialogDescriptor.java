/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.externalSystem.execution;

import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import consulo.externalSystem.service.execution.ExternalSystemTaskSettingsControl;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogValue;
import org.jspecify.annotations.Nullable;

/**
 * Edits the task execution settings of a single before-run task.
 */
public class ExternalSystemEditTaskDialogDescriptor extends DialogDescriptor {
    private static final Logger LOG = Logger.getInstance(ExternalSystemEditTaskDialogDescriptor.class);

    private final ExternalSystemTaskExecutionSettings myTaskExecutionSettings;

    private final ExternalSystemTaskSettingsControl myControl;

    public ExternalSystemEditTaskDialogDescriptor(
        LocalizeValue title,
        Project project,
        ExternalSystemTaskExecutionSettings taskExecutionSettings,
        ProjectSystemId externalSystemId
    ) {
        super(title);
        myTaskExecutionSettings = taskExecutionSettings;
        myControl = new ExternalSystemTaskSettingsControl(project, externalSystemId);
        myControl.setOriginalSettings(taskExecutionSettings);
    }

    @Override
    @RequiredUIAccess
    public Component createCenterComponent(Disposable uiDisposable) {
        Component component = myControl.createUIComponent();
        myControl.reset();
        return component;
    }

    @Override
    @RequiredUIAccess
    public boolean canHandle(AnAction action, @Nullable DialogValue value) {
        if (!isDefaultAction(action)) {
            return super.canHandle(action, value);
        }

        try {
            myControl.apply(myTaskExecutionSettings);
        }
        catch (ConfigurationException e) {
            LOG.error(e);
            return false;
        }
        return true;
    }
}
