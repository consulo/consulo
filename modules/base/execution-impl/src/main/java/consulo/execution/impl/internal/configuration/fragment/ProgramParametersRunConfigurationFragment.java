package consulo.execution.impl.internal.configuration.fragment;

import consulo.annotation.component.ExtensionImpl;
import consulo.disposer.Disposable;
import consulo.execution.configuration.ProgramParametersAware;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.fragment.ProgramParametersFragmentState;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.execution.configuration.fragment.StandardRunConfigurationFragmentOrder;
import consulo.execution.localize.ExecutionLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.process.cmd.ParametersListUtil;
import consulo.project.Project;
import consulo.ui.TextBoxWithExpandAction;
import consulo.ui.annotation.RequiredUIAccess;

@ExtensionImpl
public class ProgramParametersRunConfigurationFragment implements RunConfigurationFragment<RunConfiguration> {
    @Override
    public String getId() {
        return "programParameters";
    }

    @Override
    public LocalizeValue getName() {
        return ExecutionLocalize.runConfigurationFragmentProgramParameters();
    }

    @Override
    public int getOrder() {
        return StandardRunConfigurationFragmentOrder.PROGRAM_PARAMETERS;
    }

    @Override
    public boolean isRemovable() {
        return false;
    }

    @Override
    public boolean isApplicable(RunConfiguration configuration) {
        return configuration instanceof ProgramParametersAware;
    }

    @Override
    public Class<ProgramParametersFragmentState> getStateClass() {
        return ProgramParametersFragmentState.class;
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<RunConfiguration> createEditor(Project project, Disposable parent) {
        TextBoxWithExpandAction box = TextBoxWithExpandAction.create(
            PlatformIconGroup.actionsShow(),
            "",
            ParametersListUtil.DEFAULT_LINE_PARSER,
            ParametersListUtil.DEFAULT_LINE_JOINER
        );

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addLabeled(ExecutionLocalize.runConfigurationProgramParameters(), box);
            }

            @RequiredUIAccess
            @Override
            public void reset(RunConfiguration target) {
                String value = ((ProgramParametersAware) target).getProgramParameters();
                box.setValue(value == null ? "" : value);
            }

            @RequiredUIAccess
            @Override
            public void apply(RunConfiguration target) {
                ((ProgramParametersAware) target).setProgramParameters(box.getValue());
            }
        };
    }
}
