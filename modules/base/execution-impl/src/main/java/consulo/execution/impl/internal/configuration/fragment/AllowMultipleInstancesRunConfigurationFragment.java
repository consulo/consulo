package consulo.execution.impl.internal.configuration.fragment;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.fragment.RunnerAndConfigurationSettingsFragment;
import consulo.execution.configuration.fragment.StandardRunConfigurationFragmentOrder;
import consulo.execution.configuration.fragment.TagRunConfigurationFragment;
import consulo.execution.localize.ExecutionLocalize;
import consulo.localize.LocalizeValue;

@ExtensionImpl
public class AllowMultipleInstancesRunConfigurationFragment
    implements RunnerAndConfigurationSettingsFragment, TagRunConfigurationFragment<RunnerAndConfigurationSettings> {
    @Override
    public String getId() {
        return "runParallel";
    }

    @Override
    public LocalizeValue getName() {
        return ExecutionLocalize.runConfigurationFragmentAllowMultipleInstances();
    }

    @Override
    public LocalizeValue getGroup() {
        return ExecutionLocalize.runConfigurationFragmentGroupRun();
    }

    @Override
    public int getOrder() {
        return StandardRunConfigurationFragmentOrder.REDIRECT_INPUT + 50;
    }

    @Override
    public boolean isApplicable(RunConfiguration configuration) {
        ConfigurationFactory factory = configuration.getFactory();
        return factory != null && factory.canConfigurationBeSingleton();
    }

    @Override
    public boolean isSet(RunnerAndConfigurationSettings target) {
        return !target.isSingleton();
    }

    @Override
    public void set(RunnerAndConfigurationSettings target, boolean value) {
        target.setSingleton(!value);
    }
}
