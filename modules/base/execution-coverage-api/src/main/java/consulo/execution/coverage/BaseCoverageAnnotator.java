package consulo.execution.coverage;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.execution.coverage.localize.ExecutionCoverageLocalize;
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
    protected abstract Runnable createRenewRequest(@Nonnull CoverageSuitesBundle suite, @Nonnull CoverageDataManager dataManager);

    public BaseCoverageAnnotator(Project project) {
        myProject = project;
    }

    @Override
    public void onSuiteChosen(CoverageSuitesBundle newSuite) {
    }

    @Override
    public void renewCoverageData(@Nonnull final CoverageSuitesBundle suite, @Nonnull CoverageDataManager dataManager) {
        final Runnable request = createRenewRequest(suite, dataManager);
        if (request != null) {
            if (myProject.isDisposed()) {
                return;
            }
            ProgressManager.getInstance().run(new Task.Backgroundable(myProject, ExecutionCoverageLocalize.coverageViewLoadingData(), false) {
                @Override
                public void run(@Nonnull ProgressIndicator indicator) {
                    request.run();
                }

                @Override
                @RequiredUIAccess
                public void onSuccess() {
                    CoverageView coverageView = CoverageViewManager.getInstance((Project) myProject).getToolwindow(suite);
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
