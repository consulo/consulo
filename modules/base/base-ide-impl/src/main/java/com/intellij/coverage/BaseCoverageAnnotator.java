package com.intellij.coverage;

import com.intellij.coverage.view.CoverageView;
import com.intellij.coverage.view.CoverageViewManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * @author Roman.Chernyatchik
 */
public abstract class BaseCoverageAnnotator implements CoverageAnnotator {

  private final Project myProject;

  @Nullable
  protected abstract Runnable createRenewRequest(@Nonnull final CoverageSuitesBundle suite, @Nonnull final CoverageDataManager dataManager);

  public BaseCoverageAnnotator(final Project project) {
    myProject = project;
  }

  public void onSuiteChosen(CoverageSuitesBundle newSuite) {
  }

  public void renewCoverageData(@Nonnull final CoverageSuitesBundle suite, @Nonnull final CoverageDataManager dataManager) {
    final Runnable request = createRenewRequest(suite, dataManager);
    if (request != null) {
      if (myProject.isDisposed()) return;
      ProgressManager.getInstance().run(new Task.Backgroundable(myProject, "Loading coverage data...", false) {
        @Override
        public void run(@Nonnull ProgressIndicator indicator) {
          request.run();
        }

        @RequiredUIAccess
        @Override
        public void onSuccess() {
          final CoverageView coverageView = CoverageViewManager.getInstance(myProject).getToolwindow(suite);
          if (coverageView != null) {
            coverageView.updateParentTitle();
          }
        }
      });
    }
  }

  public Project getProject() {
    return myProject;
  }

  public static class FileCoverageInfo {
    public int totalLineCount;
    public int coveredLineCount;
  }

  public static class DirCoverageInfo extends FileCoverageInfo {
    public int totalFilesCount;
    public int coveredFilesCount;
  }
}
