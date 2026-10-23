package consulo.execution.impl.internal.configuration.fragment;

import consulo.annotation.component.ExtensionImpl;
import consulo.disposer.Disposable;
import consulo.execution.configuration.EnvironmentAware;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.fragment.EnvironmentFragmentState;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.execution.configuration.fragment.StandardRunConfigurationFragmentOrder;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.ui.awt.EnvironmentVariablesTextFieldWithBrowseButton;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.Map;

@ExtensionImpl
public class EnvironmentRunConfigurationFragment implements RunConfigurationFragment<RunConfiguration> {
    @Override
    public String getId() {
        return "environment";
    }

    @Override
    public LocalizeValue getName() {
        return ExecutionLocalize.runConfigurationFragmentEnvironment();
    }

    @Override
    public LocalizeValue getGroup() {
        return ExecutionLocalize.runConfigurationFragmentGroupOperatingSystem();
    }

    @Override
    public int getOrder() {
        return StandardRunConfigurationFragmentOrder.ENVIRONMENT;
    }

    @Override
    public boolean isApplicable(RunConfiguration configuration) {
        return configuration instanceof EnvironmentAware;
    }

    @Override
    public Class<EnvironmentFragmentState> getStateClass() {
        return EnvironmentFragmentState.class;
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<RunConfiguration> createEditor(Project project, Disposable parent) {
        EnvironmentVariablesTextFieldWithBrowseButton box = new EnvironmentVariablesTextFieldWithBrowseButton();

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                LocalizeValue label = LocalizeValue.join(ExecutionLocalize.environmentVariablesComponentTitle(), LocalizeValue.colon());
                layout.addLabeled(label, box.getComponent());
            }

            @RequiredUIAccess
            @Override
            public void reset(RunConfiguration target) {
                EnvironmentAware environment = (EnvironmentAware) target;
                box.setEnvs(environment.getEnvs());
                box.setPassParentEnvs(environment.isPassParentEnvs());
            }

            @RequiredUIAccess
            @Override
            public void apply(RunConfiguration target) {
                EnvironmentAware environment = (EnvironmentAware) target;
                environment.setEnvs(box.getEnvs());
                environment.setPassParentEnvs(box.isPassParentEnvs());
            }
        };
    }

    @Override
    public void applyHidden(RunConfiguration target) {
        EnvironmentAware environment = (EnvironmentAware) target;
        environment.setEnvs(Map.of());
        environment.setPassParentEnvs(true);
    }
}
