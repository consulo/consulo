// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.project.impl.internal;

import consulo.application.internal.ProgressIndicatorBase;
import consulo.application.internal.ProgressIndicatorEx;
import consulo.project.DumbModeTask;
import org.jspecify.annotations.Nullable;

public class DumbServiceMergingTaskQueue extends MergingTaskQueue<DumbModeTask> {
    @Override
    public @Nullable QueuedDumbModeTask extractNextTask() {
        return (QueuedDumbModeTask) super.extractNextTask();
    }

    @Override
    protected QueuedDumbModeTask wrapTask(DumbModeTask task, ProgressIndicatorBase indicator) {
        return new QueuedDumbModeTask(task, indicator);
    }

    public static class QueuedDumbModeTask extends MergingTaskQueue.QueuedTask<DumbModeTask> {
        QueuedDumbModeTask(DumbModeTask task, ProgressIndicatorEx progress) {
            super(task, progress);
        }
    }
}
