package consulo.execution.coverage.action;

import consulo.annotation.component.ActionImpl;
import consulo.execution.coverage.CoverageDataManager;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.execution.coverage.localize.ExecutionCoverageLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

/**
 * @author anna
 * @since 2012-02-14
 */
@ActionImpl(id = "HideCoverage")
public class HideCoverageInfoAction extends AnAction {
    public HideCoverageInfoAction() {
        super(
            ExecutionCoverageLocalize.actionHidecoverageText(),
            ExecutionCoverageLocalize.actionHidecoverageDescription(),
            PlatformIconGroup.actionsCancel()
        );
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        CoverageDataManager.getInstance(e.getRequiredData(Project.KEY)).chooseSuitesBundle(null);
    }

    @Override
    @RequiredUIAccess
    public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        presentation.setEnabled(false);
        presentation.setVisible(e.isFromActionToolbar());
        Project project = e.getData(Project.KEY);
        if (project != null) {
            CoverageSuitesBundle suitesBundle = CoverageDataManager.getInstance(project).getCurrentSuitesBundle();
            presentation.setEnabled(suitesBundle != null);
            presentation.setVisible(suitesBundle != null);
        }
    }
}
