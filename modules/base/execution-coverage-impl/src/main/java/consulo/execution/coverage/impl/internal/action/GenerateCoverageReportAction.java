package consulo.execution.coverage.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.execution.coverage.CoverageDataManager;
import consulo.execution.coverage.CoverageEngine;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.execution.coverage.internal.ExecutionCoverageInternal;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import jakarta.annotation.Nonnull;

/**
 * @author anna
 * @since 2007-11-20
 */
@ActionImpl(id = "GenerateCoverageReport")
public class GenerateCoverageReportAction extends AnAction implements DumbAware {
    public GenerateCoverageReportAction() {
        super(LocalizeValue.localizeTODO("_Generate Coverage Report"), LocalizeValue.empty(), PlatformIconGroup.actionsExport());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        CoverageDataManager coverageDataManager = CoverageDataManager.getInstance(project);
        CoverageSuitesBundle currentSuite = coverageDataManager.getCurrentSuitesBundle();

        CoverageEngine coverageEngine = currentSuite.getCoverageEngine();

        UIAccess uiAccess = UIAccess.current();

        ExecutionCoverageInternal.getInstance().showExportDialog(project, currentSuite.getPresentableName())
            .whenCompleteAsync(
                (o, throwable) -> {
                    // success complete
                    if (throwable == null) {
                        coverageEngine.generateReport(project, e.getDataContext(), currentSuite);
                    }
                },
                uiAccess
            );
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        presentation.setEnabledAndVisible(false);
        Project project = e.getData(Project.KEY);
        if (project != null) {
            CoverageSuitesBundle currentSuite = CoverageDataManager.getInstance(project).getCurrentSuitesBundle();
            if (currentSuite != null) {
                CoverageEngine coverageEngine = currentSuite.getCoverageEngine();
                if (coverageEngine.isReportGenerationAvailable(project, e.getDataContext(), currentSuite)) {
                    presentation.setEnabledAndVisible(true);
                }
            }
        }
    }
}
