// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.project;

import consulo.application.progress.ProgressIndicator;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

/**
 * A task that should be executed in IDE dumb mode, via {@link DumbService#queueTask(DumbModeTask)}.
 */
public abstract class DumbModeTask implements MergeableQueueTask<DumbModeTask> {
    private final @Nullable Object myEquivalenceObject;

    /**
     * Consider implementing {@link DumbModeTask#tryMergeWith(DumbModeTask)} to allow alike tasks to merge while waiting in queue
     */
    public DumbModeTask() {
        myEquivalenceObject = null;
    }

    /**
     * Tasks with same class and {@code equivalenceObject} would be merged while waiting in queue
     * unless {@link DumbModeTask#tryMergeWith(DumbModeTask)} is overwritten.
     *
     * @deprecated Consider using {@link DumbModeTask()} and overwriting {@link DumbModeTask#tryMergeWith(DumbModeTask)} instead.
     */
    @Deprecated
    public DumbModeTask(Object equivalenceObject) {
        myEquivalenceObject = Pair.create(getClass(), equivalenceObject);
    }

    public abstract void performInDumbMode(ProgressIndicator indicator, Exception trace);

    @Override
    public final void perform(ProgressIndicator indicator, Exception trace) {
        performInDumbMode(indicator, trace);
    }

    @Override
    public void dispose() {
    }

    /**
     * Allows merging tasks waiting in queue for execution.
     *
     * @return {@code null} - if current task has nothing to do with {@code taskFromQueue}; <p>
     *         {@code this} - if you want to remove {@code taskFromQueue} from the queue and add current one;  <p>
     *         some other task - then it would be added to the queue, and {@code taskFromQueue} would be removed.
     */
    @Override
    public @Nullable DumbModeTask tryMergeWith(DumbModeTask taskFromQueue) {
        if (myEquivalenceObject != null && myEquivalenceObject.equals(taskFromQueue.myEquivalenceObject)) {
            return this;
        }
        return null;
    }

    /**
     * Queues dumb mode task to be performed in dumb mode. See {@link DumbService#queueTask(DumbModeTask)}.
     */
    public final void queue(Project project) {
        DumbService.getInstance(project).queueTask(this);
    }
}
