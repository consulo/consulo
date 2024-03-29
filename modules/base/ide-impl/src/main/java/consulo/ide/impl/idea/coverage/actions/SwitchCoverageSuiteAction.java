package consulo.ide.impl.idea.coverage.actions;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.project.Project;

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
