package consulo.ide.impl.idea.coverage.view;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.coverage.CoverageDataManager;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.execution.coverage.CoverageViewManager;
import consulo.execution.coverage.view.CoverageView;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.view.SelectInContext;
import consulo.project.ui.view.SelectInTarget;
import consulo.project.ui.view.StandardTargetWeights;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

/**
 * User: anna
 * Date: 1/3/12
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
        return LocalizeValue.localizeTODO("Coverage");
    }

    @Override
    public boolean canSelect(final SelectInContext context) {
        final CoverageSuitesBundle suitesBundle = CoverageDataManager.getInstance(myProject).getCurrentSuitesBundle();
        if (suitesBundle != null) {
            final CoverageView coverageView = CoverageViewManager.getInstance(myProject).getToolwindow(suitesBundle);
            if (coverageView != null) {
                final VirtualFile file = context.getVirtualFile();
                return !file.isDirectory() && coverageView.canSelect(file);
            }
        }
        return false;
    }

    @Override
    public void selectIn(final SelectInContext context, final boolean requestFocus) {
        final CoverageSuitesBundle suitesBundle = CoverageDataManager.getInstance(myProject).getCurrentSuitesBundle();
        if (suitesBundle != null) {
            final CoverageViewManager coverageViewManager = CoverageViewManager.getInstance(myProject);
            final CoverageView coverageView = coverageViewManager.getToolwindow(suitesBundle);
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
