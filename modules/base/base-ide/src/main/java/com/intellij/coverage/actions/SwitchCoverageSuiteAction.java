package com.intellij.coverage.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;

/**
 * @author ven
 */
public class SwitchCoverageSuiteAction extends AnAction {

  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getDataContext().getData(CommonDataKeys.PROJECT);
    new CoverageSuiteChooserDialog(project).show();
  }

  public void update(AnActionEvent e) {
    super.update(e);
    Project project = e.getDataContext().getData(CommonDataKeys.PROJECT);
    e.getPresentation().setEnabled(project != null);
  }
}
