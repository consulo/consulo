// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.project.impl.internal;

import consulo.application.internal.ProgressIndicatorBase;
import consulo.application.internal.ProgressIndicatorEx;
import consulo.project.DumbModeTask;
import org.jspecify.annotations.Nullable;

public final class DumbServiceMergingTaskQueue extends MergingTaskQueue<DumbModeTask> {
    @Override
    public @Nullable QueuedDumbModeTask extractNextTask() {
        return (QueuedDumbModeTask) super.extractNextTask();
    }

    @Override
    protected QueuedDumbModeTask wrapTask(DumbModeTask task, ProgressIndicatorBase indicator) {
        return new QueuedDumbModeTask(task, indicator);
    }

    public static final class QueuedDumbModeTask extends MergingTaskQueue.QueuedTask<DumbModeTask> {
        QueuedDumbModeTask(DumbModeTask task, ProgressIndicatorEx progress) {
            super(task, progress);
        }

        @Override
        String getInfoString() {
            return "(dumb mode task) " + super.getInfoString();
        }
    }
}
