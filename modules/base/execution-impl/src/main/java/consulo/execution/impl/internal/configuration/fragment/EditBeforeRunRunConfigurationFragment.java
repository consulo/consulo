package consulo.execution.impl.internal.configuration.fragment;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.fragment.RunnerAndConfigurationSettingsFragment;
import consulo.execution.configuration.fragment.StandardRunConfigurationFragmentOrder;
import consulo.execution.configuration.fragment.TagRunConfigurationFragment;
import consulo.execution.localize.ExecutionLocalize;
import consulo.localize.LocalizeValue;

@ExtensionImpl
public class EditBeforeRunRunConfigurationFragment
    implements RunnerAndConfigurationSettingsFragment, TagRunConfigurationFragment<RunnerAndConfigurationSettings> {
    @Override
    public String getId() {
        return "editBeforeRun";
    }

    @Override
    public LocalizeValue getName() {
        return ExecutionLocalize.runConfigurationFragmentEditBeforeRun();
    }

    @Override
    public LocalizeValue getGroup() {
        return ExecutionLocalize.runConfigurationFragmentGroupBeforeLaunch();
    }

    @Override
    public int getOrder() {
        return StandardRunConfigurationFragmentOrder.LAST;
    }

    @Override
    public boolean isSet(RunnerAndConfigurationSettings target) {
        return target.isEditBeforeRun();
    }

    @Override
    public void set(RunnerAndConfigurationSettings target, boolean value) {
        target.setEditBeforeRun(value);
    }
}
