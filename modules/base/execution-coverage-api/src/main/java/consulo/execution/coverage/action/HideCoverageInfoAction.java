package consulo.execution.coverage.action;

import consulo.annotation.component.ActionImpl;
import consulo.execution.coverage.CoverageDataManager;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.execution.coverage.localize.ExecutionCoverageLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.LegacyAnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

/**
 * @author anna
 * @since 2012-02-14
 */
@ActionImpl(id = "HideCoverage")
public class HideCoverageInfoAction extends LegacyAnAction {
    public HideCoverageInfoAction() {
        super(
            ExecutionCoverageLocalize.actionHideCoverageText(),
            ExecutionCoverageLocalize.actionHideCoverageDescription(),
            PlatformIconGroup.actionsCancel()
        );
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        CoverageDataManager.getInstance(e.getRequiredData(Project.KEY)).chooseSuitesBundle(null);
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        CoverageSuitesBundle suitesBundle = project != null ? CoverageDataManager.getInstance(project).getCurrentSuitesBundle() : null;
        Presentation presentation = e.getPresentation();
        presentation.setEnabled(suitesBundle != null);
        presentation.setVisible(e.isFromActionToolbar() && suitesBundle != null);
    }
}
