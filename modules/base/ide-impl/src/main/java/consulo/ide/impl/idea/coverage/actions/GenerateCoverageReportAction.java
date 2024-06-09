/*
 * User: anna
 * Date: 20-Nov-2007
 */
package consulo.ide.impl.idea.coverage.actions;

import consulo.ide.impl.idea.codeInspection.export.ExportToHTMLDialog;
import consulo.dataContext.DataContext;
import consulo.execution.coverage.CoverageDataManager;
import consulo.execution.coverage.CoverageEngine;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

public class GenerateCoverageReportAction extends AnAction {

  @Override
  public void actionPerformed(final AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    final Project project = dataContext.getData(Project.KEY);
    assert project != null;
    final CoverageDataManager coverageDataManager = CoverageDataManager.getInstance(project);
    final CoverageSuitesBundle currentSuite = coverageDataManager.getCurrentSuitesBundle();


    final CoverageEngine coverageEngine = currentSuite.getCoverageEngine();
    final ExportToHTMLDialog dialog = new ExportToHTMLDialog(project, true);
    dialog.setTitle("Generate Coverage Report for: \'" + currentSuite.getPresentableName() + "\'");
    dialog.reset();
    dialog.show();
    if (!dialog.isOK()) return;
    dialog.apply();

    coverageEngine.generateReport(project, dataContext, currentSuite);
  }

  public void update(final AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    final Presentation presentation = e.getPresentation();
    presentation.setEnabled(false);
    presentation.setVisible(false);
    final Project project = dataContext.getData(Project.KEY);
    if (project != null) {
      final CoverageSuitesBundle currentSuite = CoverageDataManager.getInstance(project).getCurrentSuitesBundle();
      if (currentSuite != null) {
        final CoverageEngine coverageEngine = currentSuite.getCoverageEngine();
        if (coverageEngine.isReportGenerationAvailable(project, dataContext, currentSuite)) {
          presentation.setEnabled(true);
          presentation.setVisible(true);
        }
      }
    }
  }

}
