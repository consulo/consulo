package consulo.execution.impl.internal.configuration.fragment;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentType;
import consulo.execution.configuration.fragment.StandardRunConfigurationFragmentGroup;
import consulo.execution.configuration.fragment.StandardRunConfigurationFragmentOrder;
import consulo.execution.impl.internal.compound.CompoundRunConfiguration;
import consulo.execution.localize.ExecutionLocalize;
import consulo.localize.LocalizeValue;

/**
 * The "Logs" section: log files, console output file and when to show the console.
 */
@ExtensionImpl
public class LogsGroupRunConfigurationFragment implements RunConfigurationFragment<RunConfiguration> {
    @Override
    public String getId() {
        return StandardRunConfigurationFragmentGroup.LOGS;
    }

    @Override
    public LocalizeValue getName() {
        return ExecutionLocalize.runConfigurationFragmentLogsSettings();
    }

    @Override
    public LocalizeValue getGroup() {
        return ExecutionLocalize.runConfigurationFragmentLogs();
    }

    @Override
    public int getOrder() {
        return StandardRunConfigurationFragmentOrder.LOGS;
    }

    @Override
    public RunConfigurationFragmentType getType() {
        return RunConfigurationFragmentType.GROUP;
    }

    @Override
    public boolean isApplicable(RunConfiguration configuration) {
        return configuration instanceof RunConfigurationBase && !(configuration instanceof CompoundRunConfiguration);
    }
}
