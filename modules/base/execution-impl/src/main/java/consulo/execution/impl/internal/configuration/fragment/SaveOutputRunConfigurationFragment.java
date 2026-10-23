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
import consulo.execution.localize.ExecutionLocalize;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
public class SaveOutputRunConfigurationFragment implements RunConfigurationFragment<RunConfiguration> {
    @Override
    public String getId() {
        return "logs.save.output";
    }

    @Override
    public LocalizeValue getName() {
        return ExecutionLocalize.runConfigurationFragmentLogsSaveOutput();
    }

    @Override
    public @Nullable String getParentId() {
        return StandardRunConfigurationFragmentGroup.LOGS;
    }

    @Override
    public int getOrder() {
        return StandardRunConfigurationFragmentOrder.LOGS + 10;
    }

    @Override
    public boolean isApplicable(RunConfiguration configuration) {
        return configuration instanceof RunConfigurationBase;
    }

    @Override
    public boolean isVisibleByDefault(RunConfiguration target) {
        return ((RunConfigurationBase) target).isSaveOutputToFile();
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<RunConfiguration> createEditor(Project project, Disposable parent) {
        FileChooserTextBoxBuilder builder = FileChooserTextBoxBuilder.create(project);
        builder.dialogTitle(ExecutionLocalize.logsSaveConsoleOutputChooserTitle());
        builder.dialogDescription(ExecutionLocalize.logsSaveConsoleOutputChooserDescription());
        builder.fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor());
        builder.uiDisposable(parent);
        FileChooserTextBoxBuilder.Controller outputFile = builder.build();

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addLabeled(ExecutionLocalize.logsSaveConsoleOutputToFile(), outputFile.getComponent());
            }

            @RequiredUIAccess
            @Override
            public void reset(RunConfiguration target) {
                String path = ((RunConfigurationBase) target).getOutputFilePath();
                outputFile.setValue(path == null ? "" : FileUtil.toSystemDependentName(path), false);
            }

            @RequiredUIAccess
            @Override
            public void apply(RunConfiguration target) {
                RunConfigurationBase configuration = (RunConfigurationBase) target;
                String path = outputFile.getValue();
                configuration.setFileOutputPath(StringUtil.isEmpty(path) ? null : FileUtil.toSystemIndependentName(path));
                configuration.setSaveOutputToFile(!StringUtil.isEmpty(path));
            }
        };
    }

    @Override
    public void applyHidden(RunConfiguration target) {
        ((RunConfigurationBase) target).setSaveOutputToFile(false);
    }
}
