// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.project.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.component.util.ModificationTracker;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.concurrent.coroutine.ObservableValue;

import java.util.List;
import java.util.concurrent.Future;

@ServiceAPI(ComponentScope.PROJECT)
public interface UnindexedFilesScannerExecutor {
    static UnindexedFilesScannerExecutor getInstance(Project project) {
        return project.getInstance(UnindexedFilesScannerExecutor.class);
    }

    ObservableValue<Boolean> isRunning();

    boolean hasQueuedTasks();

    ObservableValue<Integer> startedOrStoppedEvent();

    ModificationTracker getModificationTracker();

    void suspendScanningAndIndexingThenRun(LocalizeValue activityName, Runnable runnable);

    void suspendQueue();

    void resumeQueue();

    void cancelAllTasksAndWait();

    ObservableValue<List<LocalizeValue>> getPauseReason();

    Future<?> submitTask(FilesScanningTask task);
}
