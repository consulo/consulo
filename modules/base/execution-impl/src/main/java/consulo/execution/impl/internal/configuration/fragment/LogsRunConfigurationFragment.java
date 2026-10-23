package consulo.execution.impl.internal.configuration.fragment;

import consulo.annotation.component.ExtensionImpl;
import consulo.disposer.Disposable;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.execution.configuration.fragment.StandardRunConfigurationFragmentGroup;
import consulo.execution.configuration.fragment.StandardRunConfigurationFragmentOrder;
import consulo.execution.configuration.log.ui.LogFilesEditor;
import consulo.execution.localize.ExecutionLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
public class LogsRunConfigurationFragment implements RunConfigurationFragment<RunConfiguration> {
    @Override
    public String getId() {
        return "logs";
    }

    @Override
    public LocalizeValue getName() {
        return ExecutionLocalize.runConfigurationFragmentLogsFiles();
    }

    @Override
    public @Nullable String getParentId() {
        return StandardRunConfigurationFragmentGroup.LOGS;
    }

    @Override
    public int getOrder() {
        return StandardRunConfigurationFragmentOrder.LOGS;
    }

    @Override
    public boolean isApplicable(RunConfiguration configuration) {
        return configuration instanceof RunConfigurationBase;
    }

    @Override
    public boolean isVisibleByDefault(RunConfiguration target) {
        RunConfigurationBase configuration = (RunConfigurationBase) target;
        return !configuration.getLogFiles().isEmpty() || !configuration.getPredefinedLogFiles().isEmpty();
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<RunConfiguration> createEditor(Project project, Disposable parent) {
        LogFilesEditor editor = new LogFilesEditor(() -> {
        });

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addBottom(editor.createComponent());
            }

            @RequiredUIAccess
            @Override
            public void reset(RunConfiguration target) {
                editor.reset((RunConfigurationBase) target);
            }

            @RequiredUIAccess
            @Override
            public void apply(RunConfiguration target) {
                editor.apply((RunConfigurationBase) target);
            }
        };
    }

    @Override
    public void applyHidden(RunConfiguration target) {
        RunConfigurationBase configuration = (RunConfigurationBase) target;
        configuration.removeAllLogFiles();
        configuration.removeAllPredefinedLogFiles();
    }
}
