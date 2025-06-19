package consulo.ide.impl.idea.coverage.actions;

import consulo.dataContext.DataContext;
import consulo.execution.coverage.CoverageDataManager;
import consulo.execution.coverage.CoverageEngine;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.execution.coverage.localize.ExecutionCoverageLocalize;
import consulo.ide.impl.idea.codeInspection.export.ExportToHTMLDialog;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

/**
 * @author anna
 * @since 2007-11-20
 */
public class GenerateCoverageReportAction extends AnAction {
    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        Project project = dataContext.getData(Project.KEY);
        assert project != null;
        CoverageDataManager coverageDataManager = CoverageDataManager.getInstance(project);
        CoverageSuitesBundle currentSuite = coverageDataManager.getCurrentSuitesBundle();

        CoverageEngine coverageEngine = currentSuite.getCoverageEngine();
        ExportToHTMLDialog dialog = new ExportToHTMLDialog(project, true);
        dialog.setTitle(ExecutionCoverageLocalize.generateCoverageReportFor(currentSuite.getPresentableName()));
        dialog.reset();
        dialog.show();
        if (!dialog.isOK()) {
            return;
        }
        dialog.apply();

        coverageEngine.generateReport(project, dataContext, currentSuite);
    }

    @Override
    @RequiredUIAccess
    public void update(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        Presentation presentation = e.getPresentation();
        presentation.setEnabled(false);
        presentation.setVisible(false);
        Project project = dataContext.getData(Project.KEY);
        if (project != null) {
            CoverageSuitesBundle currentSuite = CoverageDataManager.getInstance(project).getCurrentSuitesBundle();
            if (currentSuite != null) {
                CoverageEngine coverageEngine = currentSuite.getCoverageEngine();
                if (coverageEngine.isReportGenerationAvailable(project, dataContext, currentSuite)) {
                    presentation.setEnabled(true);
                    presentation.setVisible(true);
                }
            }
        }
    }
}
