package consulo.execution.coverage;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.execution.coverage.view.CoverageView;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Roman.Chernyatchik
 */
public abstract class BaseCoverageAnnotator implements CoverageAnnotator {
    private final Project myProject;

    @Nullable
    protected abstract Runnable createRenewRequest(
        @Nonnull final CoverageSuitesBundle suite,
        @Nonnull final CoverageDataManager dataManager
    );

    public BaseCoverageAnnotator(final Project project) {
        myProject = project;
    }

    public void onSuiteChosen(CoverageSuitesBundle newSuite) {
    }

    public void renewCoverageData(@Nonnull final CoverageSuitesBundle suite, @Nonnull final CoverageDataManager dataManager) {
        final Runnable request = createRenewRequest(suite, dataManager);
        if (request != null) {
            if (myProject.isDisposed()) {
                return;
            }
            ProgressManager.getInstance().run(new Task.Backgroundable(myProject, "Loading coverage data...", false) {
                @Override
                public void run(@Nonnull ProgressIndicator indicator) {
                    request.run();
                }

                @RequiredUIAccess
                @Override
                public void onSuccess() {
                    final CoverageView coverageView = CoverageViewManager.getInstance((Project) myProject).getToolwindow(suite);
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
