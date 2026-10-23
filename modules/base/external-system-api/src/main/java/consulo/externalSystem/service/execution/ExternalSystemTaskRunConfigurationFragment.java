package consulo.externalSystem.service.execution;

import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.externalSystem.localize.ExternalSystemLocalize;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * External project, tasks, VM options and script parameters of an {@link ExternalSystemRunConfiguration}.
 */
final class ExternalSystemTaskRunConfigurationFragment implements RunConfigurationFragment<RunConfiguration> {
    private final ProjectSystemId myExternalSystemId;

    ExternalSystemTaskRunConfigurationFragment(ProjectSystemId externalSystemId) {
        myExternalSystemId = externalSystemId;
    }

    @Override
    public String getId() {
        return "externalSystemTask";
    }

    @Override
    public LocalizeValue getName() {
        return ExternalSystemLocalize.runConfigurationSettingsLabelTasks();
    }

    @Override
    public boolean isRemovable() {
        return false;
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<RunConfiguration> createEditor(Project project, Disposable parent) {
        ExternalSystemTaskSettingsControl control = new ExternalSystemTaskSettingsControl(project, myExternalSystemId);
        Disposer.register(parent, control::disposeUIResources);

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                control.addRows(layout);
            }

            @RequiredUIAccess
            @Override
            public void reset(RunConfiguration target) {
                control.setOriginalSettings(((ExternalSystemRunConfiguration) target).getSettingsCopy());
                control.reset();
            }

            @RequiredUIAccess
            @Override
            public void apply(RunConfiguration target) throws ConfigurationException {
                ExternalSystemRunConfiguration configuration = (ExternalSystemRunConfiguration) target;
                ExternalSystemTaskExecutionSettings settings = configuration.getSettingsCopy();
                control.apply(settings);
                configuration.setSettings(settings);
            }
        };
    }
}
