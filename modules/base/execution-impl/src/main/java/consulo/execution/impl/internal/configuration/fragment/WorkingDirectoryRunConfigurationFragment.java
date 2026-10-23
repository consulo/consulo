package consulo.execution.impl.internal.configuration.fragment;

import consulo.annotation.component.ExtensionImpl;
import consulo.disposer.Disposable;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.WorkingDirectoryAware;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.execution.configuration.fragment.StandardRunConfigurationFragmentOrder;
import consulo.execution.configuration.fragment.WorkingDirectoryFragmentState;
import consulo.execution.localize.ExecutionLocalize;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.pathMacro.MacroSelector;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.util.io.PathUtil;

@ExtensionImpl
public class WorkingDirectoryRunConfigurationFragment implements RunConfigurationFragment<RunConfiguration> {
    @Override
    public String getId() {
        return "workingDirectory";
    }

    @Override
    public LocalizeValue getName() {
        return ExecutionLocalize.runConfigurationFragmentWorkingDirectory();
    }

    @Override
    public int getOrder() {
        return StandardRunConfigurationFragmentOrder.WORKING_DIRECTORY;
    }

    @Override
    public boolean isRemovable() {
        return false;
    }

    @Override
    public boolean isApplicable(RunConfiguration configuration) {
        return configuration instanceof WorkingDirectoryAware;
    }

    @Override
    public Class<WorkingDirectoryFragmentState> getStateClass() {
        return WorkingDirectoryFragmentState.class;
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<RunConfiguration> createEditor(Project project, Disposable parent) {
        FileChooserTextBoxBuilder builder = FileChooserTextBoxBuilder.create(project);
        builder.fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFolderDescriptor());
        builder.dialogTitle(ExecutionLocalize.selectWorkingDirectoryMessage());
        builder.dialogDescription(LocalizeValue.empty());

        FileChooserTextBoxBuilder.Controller[] controller = new FileChooserTextBoxBuilder.Controller[1];
        builder.firstActions(DumbAwareAction.create(
            LocalizeValue.localizeTODO("Insert Macro"),
            LocalizeValue.empty(),
            PlatformIconGroup.generalInlinevariables(),
            e -> MacroSelector.getInstance().select(project, null, macro -> controller[0].setValue(macro.getDecoratedName()))
        ));
        FileChooserTextBoxBuilder.Controller box = builder.build();
        controller[0] = box;

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addLabeled(ExecutionLocalize.runConfigurationWorkingDirectoryLabel(), box.getComponent());
            }

            @RequiredUIAccess
            @Override
            public void reset(RunConfiguration target) {
                String value = ((WorkingDirectoryAware) target).getWorkingDirectory();
                box.setValue(value == null ? "" : PathUtil.toSystemDependentName(value));
            }

            @RequiredUIAccess
            @Override
            public void apply(RunConfiguration target) {
                ((WorkingDirectoryAware) target).setWorkingDirectory(box.getValue());
            }
        };
    }
}
