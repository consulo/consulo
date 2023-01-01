package consulo.execution.test.action;

import consulo.application.AllIcons;
import consulo.execution.ExecutionDataKeys;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.test.autotest.AbstractAutoTestManager;
import consulo.execution.test.autotest.AutoTestManager;
import consulo.execution.ui.RunContentDescriptor;
import consulo.language.editor.CommonDataKeys;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;

/**
 * @author yole
 */
public class ToggleAutoTestAction extends ToggleAction {
  /**
   * @param environment To be removed in IDEA 16
   * @deprecated use default constructor instead
   */
  @Deprecated
  public ToggleAutoTestAction(ExecutionEnvironment environment) {
    this();
  }

  public ToggleAutoTestAction() {
    super("Toggle auto-test", "Toggle automatic rerun of tests on code changes", AllIcons.Actions.SwapPanels);
  }

  @Override
  public boolean isSelected(AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    RunContentDescriptor descriptor = e.getData(ExecutionDataKeys.RUN_CONTENT_DESCRIPTOR);
    return project != null && descriptor != null && getAutoTestManager(project).isAutoTestEnabled(descriptor);
  }

  @Override
  public void setSelected(AnActionEvent e, boolean state) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    RunContentDescriptor descriptor = e.getData(ExecutionDataKeys.RUN_CONTENT_DESCRIPTOR);
    ExecutionEnvironment environment = e.getData(ExecutionDataKeys.EXECUTION_ENVIRONMENT);
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
