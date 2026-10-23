package consulo.sandboxPlugin.ide.run;

import consulo.disposer.Disposable;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.StringUtil;

class SandExecutableFragment implements RunConfigurationFragment<SandExecutableConfiguration> {
    @Override
    public String getId() {
        return "sandExecutable";
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Executable");
    }

    @Override
    public boolean isRemovable() {
        return false;
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<SandExecutableConfiguration> createEditor(Project project, Disposable parent) {
        TextBox executableBox = TextBox.create();

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addLabeled(LocalizeValue.localizeTODO("Executable:"), executableBox);
            }

            @RequiredUIAccess
            @Override
            public void reset(SandExecutableConfiguration target) {
                executableBox.setValue(target.getExecutable());
            }

            @RequiredUIAccess
            @Override
            public void apply(SandExecutableConfiguration target) {
                target.setExecutable(StringUtil.notNullize(executableBox.getValue()));
            }
        };
    }
}
