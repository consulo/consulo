/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.externalSystem.service.execution;

import consulo.configurable.ConfigurationException;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.localize.ExternalSystemLocalize;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import consulo.externalSystem.ui.ExternalSystemUiAware;
import consulo.externalSystem.ui.awt.ExternalProjectPathField;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.process.cmd.ParametersListUtil;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.TextBox;
import consulo.ui.TextBoxWithExpandAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.LabeledBuilder;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import static consulo.externalSystem.util.ExternalSystemApiUtil.normalizePath;

/**
 * Editor of the task execution settings of a run configuration. Unlike the settings pages, this one is driven by
 * {@link consulo.execution.configuration.ui.SettingsEditor}, which hands a different settings object on every reset, so the settings
 * are set through {@link #setOriginalSettings} instead of being fixed at construction.
 *
 * @author Denis Zhdanov
 * @since 2013-05-23
 */
public class ExternalSystemTaskSettingsControl {
    private final ProjectSystemId myExternalSystemId;

    private final Project myProject;

    private @Nullable Component myComponent;
    private @Nullable ExternalProjectPathField myProjectPathField;
    private @Nullable TextBox myTasksBox;
    private @Nullable TextBoxWithExpandAction myVmOptionsBox;
    private @Nullable TextBoxWithExpandAction myScriptParametersBox;

    private @Nullable ExternalSystemTaskExecutionSettings myOriginalSettings;

    public ExternalSystemTaskSettingsControl(Project project, ProjectSystemId externalSystemId) {
        myProject = project;
        myExternalSystemId = externalSystemId;
    }

    public void setOriginalSettings(@Nullable ExternalSystemTaskExecutionSettings originalSettings) {
        myOriginalSettings = originalSettings;
    }

    @RequiredUIAccess
    public Component createUIComponent() {
        Component component = myComponent;
        if (component != null) {
            return component;
        }

        ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(myExternalSystemId);
        FileChooserDescriptor projectPathChooserDescriptor = null;
        if (manager instanceof ExternalSystemUiAware extSysUiAware) {
            projectPathChooserDescriptor = extSysUiAware.getExternalProjectConfigDescriptor();
        }
        if (projectPathChooserDescriptor == null) {
            projectPathChooserDescriptor = FileChooserDescriptorFactory.createSingleLocalFileDescriptor();
        }
        String title = ExternalSystemLocalize.settingsLabelSelectProject(myExternalSystemId.getDisplayName().get()).get();

        myProjectPathField = new ExternalProjectPathField(myProject, myExternalSystemId, projectPathChooserDescriptor, title);
        myTasksBox = TextBox.create();
        myVmOptionsBox = TextBoxWithExpandAction.create(
            PlatformIconGroup.actionsShow(),
            ExternalSystemLocalize.runConfigurationSettingsLabelVmoptions().get(),
            ParametersListUtil.DEFAULT_LINE_PARSER,
            ParametersListUtil.DEFAULT_LINE_JOINER
        );
        myScriptParametersBox = TextBoxWithExpandAction.create(
            PlatformIconGroup.actionsShow(),
            ExternalSystemLocalize.runConfigurationSettingsLabelScriptParameters().get(),
            ParametersListUtil.DEFAULT_LINE_PARSER,
            ParametersListUtil.DEFAULT_LINE_JOINER
        );

        VerticalLayout layout = VerticalLayout.create();
        layout.add(LabeledBuilder.filled(
            ExternalSystemLocalize.runConfigurationSettingsLabelProject(myExternalSystemId.getDisplayName()),
            TargetAWT.wrap(myProjectPathField)
        ));
        layout.add(LabeledBuilder.filled(ExternalSystemLocalize.runConfigurationSettingsLabelTasks(), myTasksBox));
        layout.add(LabeledBuilder.filled(ExternalSystemLocalize.runConfigurationSettingsLabelVmoptions(), myVmOptionsBox));
        layout.add(LabeledBuilder.filled(
            ExternalSystemLocalize.runConfigurationSettingsLabelScriptParameters(),
            myScriptParametersBox
        ));

        myComponent = layout;
        return layout;
    }

    @RequiredUIAccess
    public void reset() {
        if (myProjectPathField == null || myTasksBox == null || myVmOptionsBox == null || myScriptParametersBox == null) {
            return;
        }

        myProjectPathField.setText("");
        myTasksBox.setValue("");
        myVmOptionsBox.setValue("");
        myScriptParametersBox.setValue("");

        if (myOriginalSettings == null) {
            return;
        }

        myProjectPathField.setText(StringUtil.notNullize(myOriginalSettings.getExternalProjectPath()));
        myTasksBox.setValue(StringUtil.join(myOriginalSettings.getTaskNames(), " "));
        myVmOptionsBox.setValue(StringUtil.notNullize(myOriginalSettings.getVmOptions()));
        myScriptParametersBox.setValue(StringUtil.notNullize(myOriginalSettings.getScriptParameters()));
    }

    @RequiredUIAccess
    public boolean isModified() {
        if (myOriginalSettings == null
            || myProjectPathField == null
            || myTasksBox == null
            || myVmOptionsBox == null
            || myScriptParametersBox == null) {
            return false;
        }

        return !Comparing.equal(
            normalizePath(myProjectPathField.getText()),
            normalizePath(myOriginalSettings.getExternalProjectPath())
        )
            || !Comparing.equal(
            normalizePath(myTasksBox.getValue()),
            normalizePath(StringUtil.join(myOriginalSettings.getTaskNames(), " "))
        )
            || !Comparing.equal(normalizePath(myVmOptionsBox.getValue()), normalizePath(myOriginalSettings.getVmOptions()))
            || !Comparing.equal(
            normalizePath(myScriptParametersBox.getValue()),
            normalizePath(myOriginalSettings.getScriptParameters())
        );
    }

    @RequiredUIAccess
    public void apply(ExternalSystemTaskExecutionSettings settings) throws ConfigurationException {
        if (myProjectPathField == null || myTasksBox == null || myVmOptionsBox == null || myScriptParametersBox == null) {
            return;
        }

        String projectPath = myProjectPathField.getText();
        if (myOriginalSettings == null) {
            throw new ConfigurationException(String.format(
                "Can't store external task settings into run configuration. Reason: target run configuration is undefined. Tasks: '%s', "
                    + "external project: '%s', vm options: '%s', script parameters: '%s'",
                myTasksBox.getValue(), projectPath, myVmOptionsBox.getValue(), myScriptParametersBox.getValue()
            ));
        }

        settings.setExternalProjectPath(projectPath);
        settings.setTaskNames(StringUtil.split(StringUtil.notNullize(myTasksBox.getValue()), " "));
        settings.setVmOptions(myVmOptionsBox.getValue());
        settings.setScriptParameters(myScriptParametersBox.getValue());
    }

    public void disposeUIResources() {
        myComponent = null;
        myProjectPathField = null;
        myTasksBox = null;
        myVmOptionsBox = null;
        myScriptParametersBox = null;
    }
}
