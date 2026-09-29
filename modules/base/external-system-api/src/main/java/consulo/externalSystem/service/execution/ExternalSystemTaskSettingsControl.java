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
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.externalSystem.model.project.ExternalProjectPojo;
import consulo.externalSystem.setting.AbstractExternalSystemLocalSettings;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.process.cmd.ParametersListUtil;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.TextBox;
import consulo.ui.TextBoxWithExpandAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.LabeledBuilder;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
    private FileChooserTextBoxBuilder.@Nullable Controller myProjectPathField;
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

        FileChooserTextBoxBuilder projectPathBuilder = FileChooserTextBoxBuilder.create(myProject);
        projectPathBuilder.fileChooserDescriptor(projectPathChooserDescriptor);
        projectPathBuilder.dialogTitle(title);
        projectPathBuilder.firstActions(new ChooseRegisteredProjectAction());
        myProjectPathField = projectPathBuilder.build();
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
            myProjectPathField.getComponent()
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

        myProjectPathField.setValue("");
        myTasksBox.setValue("");
        myVmOptionsBox.setValue("");
        myScriptParametersBox.setValue("");

        if (myOriginalSettings == null) {
            return;
        }

        myProjectPathField.setValue(StringUtil.notNullize(myOriginalSettings.getExternalProjectPath()));
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
            normalizePath(myProjectPathField.getValue()),
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

        String projectPath = myProjectPathField.getValue();
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

    private class ChooseRegisteredProjectAction extends DumbAwareAction {
        private ChooseRegisteredProjectAction() {
            super(
                ExternalSystemLocalize.runConfigurationTooltipChooseRegisteredProject(myExternalSystemId.getDisplayName()),
                LocalizeValue.empty(),
                myExternalSystemId.getIcon()
            );
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(myExternalSystemId);
            if (manager == null) {
                return;
            }

            AbstractExternalSystemLocalSettings settings = manager.getLocalSettingsProvider().apply(myProject);
            Map<ExternalProjectPojo, Collection<ExternalProjectPojo>> projects = settings.getAvailableProjects();
            List<ExternalProjectPojo> rootProjects = new ArrayList<>(projects.keySet());
            ContainerUtil.sort(rootProjects);

            List<ExternalProjectPojo> items = new ArrayList<>();
            for (ExternalProjectPojo rootProject : rootProjects) {
                items.add(rootProject);

                Collection<ExternalProjectPojo> subProjects = projects.get(rootProject);
                if (subProjects != null) {
                    List<ExternalProjectPojo> sortedSubProjects = new ArrayList<>(subProjects);
                    ContainerUtil.sort(sortedSubProjects);
                    for (ExternalProjectPojo subProject : sortedSubProjects) {
                        if (!subProject.equals(rootProject)) {
                            items.add(subProject);
                        }
                    }
                }
            }

            String title = ExternalSystemLocalize.runConfigurationTitleChooseRegisteredProject(myExternalSystemId.getDisplayName()).get();
            ListPopup popup = JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<>(title, items) {
                @Override
                public String getTextFor(ExternalProjectPojo value) {
                    return value.getName();
                }

                @Override
                public Image getIconFor(ExternalProjectPojo value) {
                    return myExternalSystemId.getIcon();
                }

                @Override
                @RequiredUIAccess
                public PopupStep<?> onChosen(ExternalProjectPojo selectedValue, boolean finalChoice) {
                    FileChooserTextBoxBuilder.Controller projectPathField = myProjectPathField;
                    if (projectPathField != null) {
                        projectPathField.setValue(selectedValue.getPath());
                    }
                    return FINAL_CHOICE;
                }
            });
            popup.showUnderneathOf(e);
        }
    }
}
