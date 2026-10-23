package consulo.execution.impl.internal.configuration.fragment;

import consulo.annotation.component.ExtensionImpl;
import consulo.content.platform.PlatformTargetComboBox;
import consulo.content.platform.PlatformTargetManager;
import consulo.disposer.Disposable;
import consulo.execution.configuration.PlatformAwareRunProfile;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.fragment.PlatformTargetFragmentState;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.execution.configuration.fragment.StandardRunConfigurationFragmentOrder;
import consulo.execution.localize.ExecutionLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

@ExtensionImpl
public class PlatformTargetRunConfigurationFragment implements RunConfigurationFragment<RunConfiguration> {
    @Override
    public String getId() {
        return "platformTarget";
    }

    @Override
    public LocalizeValue getName() {
        return ExecutionLocalize.runConfigurationFragmentRunOn();
    }

    @Override
    public int getOrder() {
        return StandardRunConfigurationFragmentOrder.PLATFORM_TARGET;
    }

    @Override
    public boolean isRemovable() {
        return false;
    }

    @Override
    public boolean isApplicable(RunConfiguration configuration) {
        return configuration instanceof PlatformAwareRunProfile;
    }

    @Override
    public Class<PlatformTargetFragmentState> getStateClass() {
        return PlatformTargetFragmentState.class;
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<RunConfiguration> createEditor(Project project, Disposable parent) {
        PlatformTargetComboBox box = new PlatformTargetComboBox(project.getApplication().getInstance(PlatformTargetManager.class));

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addLabeled(ExecutionLocalize.labelRunOn(), box.getComponent());
            }

            @RequiredUIAccess
            @Override
            public void reset(RunConfiguration target) {
                box.setTargetId(((PlatformAwareRunProfile) target).getPlatformTargetId());
            }

            @RequiredUIAccess
            @Override
            public void apply(RunConfiguration target) {
                ((PlatformAwareRunProfile) target).setPlatformTargetId(box.getTargetId());
            }
        };
    }
}
