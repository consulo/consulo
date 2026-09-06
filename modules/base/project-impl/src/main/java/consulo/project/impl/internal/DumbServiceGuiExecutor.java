// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.project.impl.internal;

import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorListener;
import consulo.application.progress.TaskInfo;
import consulo.localize.LocalizeValue;
import consulo.project.DumbModeTask;
import consulo.project.Project;
import consulo.project.impl.internal.MergingTaskQueue.SubmissionReceipt;
import consulo.project.localize.ProjectLocalize;
import org.jspecify.annotations.Nullable;

public final class DumbServiceGuiExecutor extends MergingQueueGuiExecutor<DumbModeTask> {
    public DumbServiceGuiExecutor(
        Project project,
        DumbServiceMergingTaskQueue queue,
        ExecutorStateListener listener,
        ApplicationConcurrency concurrency
    ) {
        super(project, queue, listener, ProjectLocalize.progressIndexing(), ProjectLocalize.progressIndexingPaused(), concurrency);
    }

    MergingQueueGuiSuspender guiSuspender() {
        return super.getGuiSuspender();
    }

    @Override
    public @Nullable SubmissionReceipt processTasksWithProgress(@Nullable ProgressIndicator visibleIndicator) {
        DumbServiceAppIconProgress dumbServiceAppIconProgress = new DumbServiceAppIconProgress(getProject());
        if (visibleIndicator != null && !Application.get().isHeadlessEnvironment()) {
            visibleIndicator.addListener(new ProgressIndicatorListener() {
                @Override
                public void onFractionChange(double fraction) {
                    dumbServiceAppIconProgress.setFraction(fraction);
                }
            });
        }
        try {
            return super.processTasksWithProgress(visibleIndicator);
        }
        finally {
            // none of TaskInfo methods are used inside, this is just to satisfy the API
            dumbServiceAppIconProgress.finish(DummyTaskInfo.INSTANCE);
        }
    }

    private static final class DummyTaskInfo implements TaskInfo {
        private static final DummyTaskInfo INSTANCE = new DummyTaskInfo();

        @Override
        public String getTitle() {
            return "";
        }

        @Override
        public LocalizeValue getCancelTextValue() {
            return LocalizeValue.empty();
        }

        @Override
        public LocalizeValue getCancelTooltipTextValue() {
            return LocalizeValue.empty();
        }

        @Override
        public boolean isCancellable() {
            return false;
        }
    }
}
