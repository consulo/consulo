package consulo.execution.test.action;

import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.test.autotest.AbstractAutoTestManager;
import consulo.execution.test.autotest.AutoTestManager;
import consulo.execution.ui.RunContentDescriptor;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;

/**
 * @author yole
 */
public class ToggleAutoTestAction extends ToggleAction {
    public ToggleAutoTestAction() {
        super(
            ExecutionLocalize.actionToggleAutoTestText(),
            ExecutionLocalize.actionToggleAutoTestDescription(),
            PlatformIconGroup.actionsSwappanels()
        );
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        RunContentDescriptor descriptor = e.getData(RunContentDescriptor.KEY);
        return project != null && descriptor != null && getAutoTestManager(project).isAutoTestEnabled(descriptor);
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        Project project = e.getData(Project.KEY);
        RunContentDescriptor descriptor = e.getData(RunContentDescriptor.KEY);
        ExecutionEnvironment environment = e.getData(ExecutionEnvironment.KEY);
        if (project != null && descriptor != null && environment != null) {
            getAutoTestManager(project).setAutoTestEnabled(descriptor, environment, state);
        }
    }

    public boolean isDelayApplicable() {
        return true;
    }

    public AbstractAutoTestManager getAutoTestManager(Project project) {
        return AutoTestManager.getInstance(project);
    }
}
