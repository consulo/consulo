// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

import consulo.application.progress.ProgressIndicator;
import consulo.application.util.Semaphore;
import consulo.language.index.impl.internal.localize.IndexingLocalize;
import consulo.project.DumbModeTask;
import consulo.project.impl.internal.DumbServiceImpl;
import org.jspecify.annotations.Nullable;

/**
 * Keeps the project in dumb mode while the file based indexes are reloaded.
 */
final class WaitForSemaphoreInDumbModeTask extends DumbModeTask {
    private final Semaphore mySemaphore;
    private final String myReason;

    WaitForSemaphoreInDumbModeTask(Semaphore semaphore, String reason) {
        mySemaphore = semaphore;
        myReason = reason;
    }

    @Override
    public void performInDumbMode(ProgressIndicator indicator, Exception trace) {
        if (DumbServiceImpl.isSynchronousTaskExecution()) {
            // the task runs inside queueTask, waiting here would never let the indexes be turned on
            return;
        }
        indicator.setText(IndexingLocalize.indexesReloading());
        mySemaphore.waitFor();
    }

    @Override
    public @Nullable DumbModeTask tryMergeWith(DumbModeTask taskFromQueue) {
        if (taskFromQueue instanceof WaitForSemaphoreInDumbModeTask task && task.mySemaphore == mySemaphore) {
            return this;
        }
        return null;
    }

    @Override
    public String toString() {
        return myReason + " (re-load indexes)";
    }
}
