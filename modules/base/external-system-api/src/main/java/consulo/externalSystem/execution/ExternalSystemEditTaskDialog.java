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

import consulo.externalSystem.localize.ExternalSystemLocalize;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import consulo.externalSystem.service.execution.ExternalSystemTaskSettingsControl;
import consulo.externalSystem.ui.awt.PaintAwarePanel;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

public class ExternalSystemEditTaskDialog extends DialogWrapper {

    @Nonnull
    private final ExternalSystemTaskExecutionSettings myTaskExecutionSettings;
    @Nonnull
    private final ExternalSystemTaskSettingsControl myControl;
    @Nullable
    private JComponent contentPane;

    public ExternalSystemEditTaskDialog(@Nonnull Project project,
                                        @Nonnull ExternalSystemTaskExecutionSettings taskExecutionSettings,
                                        @Nonnull ProjectSystemId externalSystemId) {
        super(project, true);
        myTaskExecutionSettings = taskExecutionSettings;

        setTitle(ExternalSystemLocalize.tasksEditTaskTitle(externalSystemId.getDisplayName()));
        myControl = new ExternalSystemTaskSettingsControl(project, externalSystemId);
        myControl.setOriginalSettings(taskExecutionSettings);
        setModal(true);
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        if (contentPane == null) {
            contentPane = new PaintAwarePanel();
            myControl.fillUi(getDisposable(), (PaintAwarePanel) contentPane, 0);
            myControl.reset();
        }
        return contentPane;
    }

    @RequiredUIAccess
    @Override
    public JComponent getPreferredFocusedComponent() {
        return null;
    }

    @Override
    protected void dispose() {
        super.dispose();
        myControl.disposeUIResources();
    }

    @Override
    protected void doOKAction() {
        myControl.apply(myTaskExecutionSettings);
        super.doOKAction();
    }
}
