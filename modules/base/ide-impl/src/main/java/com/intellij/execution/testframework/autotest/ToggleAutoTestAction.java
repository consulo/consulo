package com.intellij.execution.testframework.autotest;

import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ui.RunContentDescriptor;
import consulo.application.AllIcons;
import consulo.ui.ex.action.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import consulo.ui.ex.action.ToggleAction;
import consulo.project.Project;

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
    RunContentDescriptor descriptor = e.getData(LangDataKeys.RUN_CONTENT_DESCRIPTOR);
    return project != null && descriptor != null && getAutoTestManager(project).isAutoTestEnabled(descriptor);
  }

  @Override
  public void setSelected(AnActionEvent e, boolean state) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    RunContentDescriptor descriptor = e.getData(LangDataKeys.RUN_CONTENT_DESCRIPTOR);
    ExecutionEnvironment environment = e.getData(LangDataKeys.EXECUTION_ENVIRONMENT);
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
