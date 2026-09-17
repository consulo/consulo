package consulo.externalSystem.service.execution;

import consulo.configurable.ConfigurationException;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * @author Denis Zhdanov
 * @since 2013-05-23
 */
public class ExternalSystemRunConfigurationEditor extends SettingsEditor<ExternalSystemRunConfiguration> {
    private final ExternalSystemTaskSettingsControl myControl;

    public ExternalSystemRunConfigurationEditor(Project project, ProjectSystemId externalSystemId) {
        myControl = new ExternalSystemTaskSettingsControl(project, externalSystemId);
    }

    @Override
    protected void resetEditorFrom(ExternalSystemRunConfiguration s) {
        myControl.setOriginalSettings(s.getSettings());
        myControl.reset();
    }

    @Override
    protected void applyEditorTo(ExternalSystemRunConfiguration s) throws ConfigurationException {
        myControl.apply(s.getSettings());
    }

    @RequiredUIAccess
    @Override
    protected Component createUIComponent() {
        return myControl.createUIComponent();
    }

    @Override
    protected void disposeEditor() {
        myControl.disposeUIResources();
    }
}
