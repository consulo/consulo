package consulo.ide.impl.idea.coverage.view;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.execution.coverage.CoverageDataManager;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.execution.coverage.CoverageViewManager;
import consulo.execution.coverage.localize.ExecutionCoverageLocalize;
import consulo.execution.coverage.view.CoverageView;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.view.SelectInContext;
import consulo.project.ui.view.SelectInTarget;
import consulo.project.ui.view.StandardTargetWeights;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

/**
 * @author anna
 * @since 2012-01-03
 */
@ExtensionImpl
public class SelectInCoverageView implements SelectInTarget {
    private final Project myProject;

    @Inject
    public SelectInCoverageView(Project project) {
        myProject = project;
    }

    @Nonnull
    @Override
    public LocalizeValue getActionText() {
        return ExecutionCoverageLocalize.coverageViewTitle();
    }

    @Override
    @RequiredReadAction
    public boolean canSelect(SelectInContext context) {
        CoverageSuitesBundle suitesBundle = CoverageDataManager.getInstance(myProject).getCurrentSuitesBundle();
        if (suitesBundle != null) {
            CoverageView coverageView = CoverageViewManager.getInstance(myProject).getToolwindow(suitesBundle);
            if (coverageView != null) {
                VirtualFile file = context.getVirtualFile();
                return !file.isDirectory() && coverageView.canSelect(file);
            }
        }
        return false;
    }

    @Override
    @RequiredUIAccess
    public void selectIn(SelectInContext context, boolean requestFocus) {
        CoverageSuitesBundle suitesBundle = CoverageDataManager.getInstance(myProject).getCurrentSuitesBundle();
        if (suitesBundle != null) {
            CoverageViewManager coverageViewManager = CoverageViewManager.getInstance(myProject);
            CoverageView coverageView = coverageViewManager.getToolwindow(suitesBundle);
            coverageView.select(context.getVirtualFile());
            coverageViewManager.activateToolwindow(coverageView, requestFocus);
        }
    }

    @Override
    public String getToolWindowId() {
        return CoverageViewManager.TOOLWINDOW_ID;
    }

    @Override
    public String getMinorViewId() {
        return null;
    }

    @Override
    public float getWeight() {
        return StandardTargetWeights.STRUCTURE_WEIGHT + 0.5f;
    }
}
