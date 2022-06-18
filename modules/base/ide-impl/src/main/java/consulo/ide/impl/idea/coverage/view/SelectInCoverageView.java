package consulo.ide.impl.idea.coverage.view;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.ide.SelectInContext;
import consulo.ide.impl.idea.ide.SelectInTarget;
import consulo.ide.impl.idea.ide.StandardTargetWeights;
import consulo.execution.coverage.CoverageDataManager;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.execution.coverage.CoverageViewManager;
import consulo.execution.coverage.view.CoverageView;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;

/**
 * User: anna
 * Date: 1/3/12
 */
@ExtensionImpl
public class SelectInCoverageView implements  SelectInTarget {
  private final Project myProject;

  @Inject
  public SelectInCoverageView(Project project) {
    myProject = project;
  }

  public String toString() {
    return CoverageViewManager.TOOLWINDOW_ID;
  }

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

  public void selectIn(final SelectInContext context, final boolean requestFocus) {
    final CoverageSuitesBundle suitesBundle = CoverageDataManager.getInstance(myProject).getCurrentSuitesBundle();
    if (suitesBundle != null) {
      final CoverageViewManager coverageViewManager = CoverageViewManager.getInstance(myProject);
      final CoverageView coverageView = coverageViewManager.getToolwindow(suitesBundle);
      coverageView.select(context.getVirtualFile());
      coverageViewManager.activateToolwindow(coverageView, requestFocus);
    }
  }

  public String getToolWindowId() {
    return CoverageViewManager.TOOLWINDOW_ID;
  }

  public String getMinorViewId() {
    return null;
  }

  public float getWeight() {
    return StandardTargetWeights.STRUCTURE_WEIGHT + 0.5f;
  }
}
