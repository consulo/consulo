package consulo.execution.coverage.action;

import consulo.application.AllIcons;
import consulo.execution.coverage.CoverageDataManager;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

/**
 * User: anna
 * Date: 2/14/12
 */
public class HideCoverageInfoAction extends AnAction {
  public HideCoverageInfoAction() {
    super("&Hide Coverage Data", "Hide coverage data", AllIcons.Actions.Cancel);
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(final AnActionEvent e) {
    CoverageDataManager.getInstance(e.getData(Project.KEY)).chooseSuitesBundle(null);
  }

  @Override
  @RequiredUIAccess
  public void update(AnActionEvent e) {
    final Presentation presentation = e.getPresentation();
    presentation.setEnabled(false);
    presentation.setVisible(ActionPlaces.isToolbarPlace(e.getPlace()));
    final Project project = e.getData(Project.KEY);
    if (project != null) {
      final CoverageSuitesBundle suitesBundle = CoverageDataManager.getInstance(project).getCurrentSuitesBundle();
      presentation.setEnabled(suitesBundle != null);
      presentation.setVisible(suitesBundle != null);
    }
  }
}
