package consulo.execution.impl.internal.configuration.fragment;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.configuration.fragment.StandardRunConfigurationFragmentGroup;
import consulo.execution.configuration.fragment.StandardRunConfigurationFragmentOrder;
import consulo.execution.configuration.fragment.TagRunConfigurationFragment;
import consulo.execution.localize.ExecutionLocalize;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
public class ShowConsoleOnStdoutRunConfigurationFragment implements TagRunConfigurationFragment<RunConfiguration> {
    @Override
    public String getId() {
        return "logs.stdout";
    }

    @Override
    public LocalizeValue getName() {
        return ExecutionLocalize.runConfigurationFragmentLogsStdout();
    }

    @Override
    public @Nullable String getParentId() {
        return StandardRunConfigurationFragmentGroup.LOGS;
    }

    @Override
    public int getOrder() {
        return StandardRunConfigurationFragmentOrder.LOGS + 20;
    }

    @Override
    public boolean isApplicable(RunConfiguration configuration) {
        return configuration instanceof RunConfigurationBase;
    }

    @Override
    public boolean isSet(RunConfiguration target) {
        return ((RunConfigurationBase) target).isShowConsoleOnStdOut();
    }

    @Override
    public void set(RunConfiguration target, boolean value) {
        ((RunConfigurationBase) target).setShowConsoleOnStdOut(value);
    }
}
