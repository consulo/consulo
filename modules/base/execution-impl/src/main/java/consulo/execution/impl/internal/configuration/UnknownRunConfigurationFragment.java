package consulo.execution.impl.internal.configuration;

import consulo.disposer.Disposable;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.execution.localize.ExecutionLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.style.ComponentColors;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * The page of a configuration whose type is not loaded: a notice, nothing to edit.
 */
final class UnknownRunConfigurationFragment implements RunConfigurationFragment<RunConfiguration> {
    @Override
    public String getId() {
        return "unknown";
    }

    @Override
    public LocalizeValue getName() {
        return ExecutionLocalize.runConfigurationEditorNotSupported();
    }

    @Override
    public boolean isRemovable() {
        return false;
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<RunConfiguration> createEditor(Project project, Disposable parent) {
        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                Label label = Label.create(ExecutionLocalize.runConfigurationEditorNotSupported());
                label.setForegroundColor(ComponentColors.DISABLED_TEXT);
                layout.addBottom(label);
            }

            @RequiredUIAccess
            @Override
            public void reset(RunConfiguration target) {
            }

            @RequiredUIAccess
            @Override
            public void apply(RunConfiguration target) {
            }
        };
    }
}
