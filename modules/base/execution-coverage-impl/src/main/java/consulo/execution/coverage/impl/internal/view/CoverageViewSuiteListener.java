package consulo.execution.coverage.impl.internal.view;

import consulo.execution.coverage.*;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * @author anna
 * @since 2012-01-05
 */
public class CoverageViewSuiteListener implements CoverageSuiteListener {
    private final CoverageDataManager myDataManager;
    private final Project myProject;

    public CoverageViewSuiteListener(CoverageDataManager dataManager, Project project) {
        myDataManager = dataManager;
        myProject = project;
    }

    @Override
    public void beforeSuiteChosen() {
        CoverageSuitesBundle suitesBundle = myDataManager.getCurrentSuitesBundle();
        if (suitesBundle != null) {
            CoverageViewManager.getInstance(myProject).closeView(CoverageViewManager.getDisplayName(suitesBundle));
        }
    }

    @Override
    @RequiredUIAccess
    public void afterSuiteChosen() {
        CoverageSuitesBundle suitesBundle = myDataManager.getCurrentSuitesBundle();
        if (suitesBundle == null) {
            return;
        }
        CoverageViewManager viewManager = CoverageViewManager.getInstance(myProject);
        if (suitesBundle.getCoverageEngine().createCoverageViewExtension(myProject, suitesBundle, viewManager.getState()) != null) {
            viewManager.createToolWindow(CoverageViewManager.getDisplayName(suitesBundle), shouldActivate(suitesBundle));
        }
    }

    private static boolean shouldActivate(CoverageSuitesBundle suitesBundle) {
        for (CoverageSuite suite : suitesBundle.getSuites()) {
            if (!(suite.getCoverageDataFileProvider() instanceof DefaultCoverageFileProvider)) {
                return false;
            }
        }
        return true;
    }
}
