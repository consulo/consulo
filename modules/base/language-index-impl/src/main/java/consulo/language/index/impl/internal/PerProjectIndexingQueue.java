// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.language.index.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.progress.ProgressManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.concurrent.coroutine.Mutex;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public final class PerProjectIndexingQueue {
    public static final class QueuedFiles {
        // Files that will be re-indexed
        private final Set<VirtualFile> myRequestsSoFar = ConcurrentHashMap.newKeySet();

        private final ObservableValue<Integer> myEstimatedFilesCount = ObservableValue.of(0);

        // Ids of scannings from which [filesSoFar] came
        private final Set<Long> myScanningIdsSoFar = createSetForScanningIds();

        int currentFilesCount() {
            return myRequestsSoFar.size();
        }

        ObservableValue<Integer> getEstimatedFilesCount() {
            return myEstimatedFilesCount;
        }

        public int getSize() {
            return myRequestsSoFar.size();
        }

        public boolean isEmpty() {
            return myRequestsSoFar.isEmpty();
        }

        public Set<VirtualFile> getRequests() {
            return Collections.unmodifiableSet(myRequestsSoFar);
        }

        public Set<Long> getScanningIds() {
            return Collections.unmodifiableSet(myScanningIdsSoFar);
        }

        void addFile(VirtualFile file, long scanningId) {
            myScanningIdsSoFar.add(scanningId);
            if (myRequestsSoFar.add(file)) {
                myEstimatedFilesCount.update(count -> count + 1);
            }
        }

        @Deprecated
        void addRequests(Collection<VirtualFile> files, Collection<Long> scanningId) {
            myScanningIdsSoFar.addAll(scanningId);
            int added = 0;
            for (VirtualFile file : files) {
                if (myRequestsSoFar.add(file)) {
                    added++;
                }
            }
            if (added > 0) {
                int delta = added;
                myEstimatedFilesCount.update(count -> count + delta);
            }
        }

        @Deprecated
        public static QueuedFiles fromFilesCollection(Collection<VirtualFile> files, Collection<Long> scanningIds) {
            QueuedFiles res = new QueuedFiles();
            res.addRequests(files, scanningIds);
            return res;
        }
    }

    private static final Logger LOG = Logger.getInstance(PerProjectIndexingQueue.class);

    private static final String INDEXING_MUTEX_OWNER = "indexing";

    public static PerProjectIndexingQueue getInstance(Project project) {
        return project.getInstance(PerProjectIndexingQueue.class);
    }

    private final Project myProject;

    private final ObservableValue<QueuedFiles> myQueuedFiles = ObservableValue.of(new QueuedFiles());
    private final ReadWriteLock myQueuedFilesLock = new ReentrantReadWriteLock();

    private final ObservableValue<Integer> myEstimatedFilesCount = ObservableValue.of(0);
    private @Nullable Runnable myEstimatedFilesCountSubscription;

    private volatile boolean myAllowFlushing = true;

    private final Mutex myScanningIndexingMutex = new Mutex();

    @Inject
    public PerProjectIndexingQueue(Project project) {
        myProject = project;
        subscribeToEstimatedFilesCount(myQueuedFiles.get());
        myQueuedFiles.addListener(this::subscribeToEstimatedFilesCount);
    }

    private synchronized void subscribeToEstimatedFilesCount(QueuedFiles queuedFiles) {
        Runnable previousSubscription = myEstimatedFilesCountSubscription;
        if (previousSubscription != null) {
            previousSubscription.run();
        }
        ObservableValue<Integer> count = queuedFiles.getEstimatedFilesCount();
        myEstimatedFilesCountSubscription = count.addListener(myEstimatedFilesCount::set);
        myEstimatedFilesCount.set(count.get());
    }

    public boolean flushNow(String reason) {
        if (!myAllowFlushing) {
            LOG.info("Flushing is not allowed at the moment");
            return false;
        }

        int queuedFilesCount;
        myQueuedFilesLock.readLock().lock();
        try {
            // note: read lock only ensures that reference to queuedFiles does change during the operation,
            // so we can safely invoke queuedFiles.value. List of queued files may change (currentFilesCount may change)
            // In order to prevent currentFilesCount changing, we need a write lock. At the moment this is not a problem, because
            // currentFilesCount may only grow, and it is expected that this number may change immediately after we release the lock.
            queuedFilesCount = myQueuedFiles.get().currentFilesCount();
        }
        finally {
            myQueuedFilesLock.readLock().unlock();
        }

        if (queuedFilesCount > 0) {
            // note that DumbModeWhileScanningTrigger will not finish dumb mode until scanning is finished
            new UnindexedFilesIndexer(myProject, reason).queue(myProject);
            return true;
        }
        else {
            LOG.info("Finished for [" + myProject.getName() + "]. No files to index with loading content.");
            return false;
        }
    }

    public void clear() {
        getAndResetQueuedFiles();
    }

    @TestOnly
    public <T> T disableFlushingDuring(Supplier<T> block) {
        myAllowFlushing = false;
        try {
            return block.get();
        }
        finally {
            myAllowFlushing = true;
        }
    }

    @TestOnly
    public QueuedFiles getQueuedFiles() {
        myQueuedFilesLock.readLock().lock();
        try {
            return myQueuedFiles.get();
        }
        finally {
            myQueuedFilesLock.readLock().unlock();
        }
    }

    QueuedFiles getAndResetQueuedFiles() {
        myQueuedFilesLock.writeLock().lock();
        try {
            return myQueuedFiles.getAndUpdate(files -> new QueuedFiles());
        }
        finally {
            myQueuedFilesLock.writeLock().unlock();
        }
    }

    /**
     * Will throw {@link consulo.component.ProcessCanceledException} if the queue is suspended via cancelAllTasksAndWait
     */
    public void addFile(VirtualFile vFile, long scanningId) {
        // readLock here is to make sure that queuedFiles does not change during the operation
        myQueuedFilesLock.readLock().lock();
        try {
            // .value for each file, because we want to put files into a new queue after getAndResetQueuedFiles invocation
            myQueuedFiles.get().addFile(vFile, scanningId);
        }
        finally {
            myQueuedFilesLock.readLock().unlock();
        }
    }

    public ObservableValue<Integer> estimatedFilesCount() {
        return myEstimatedFilesCount;
    }

    Mutex getScanningIndexingMutex() {
        return myScanningIndexingMutex;
    }

    void wrapIndexing(Runnable indexingRoutine) {
        myScanningIndexingMutex.lockCancellable(INDEXING_MUTEX_OWNER, ProgressManager::checkCanceled);
        try {
            indexingRoutine.run();
        }
        finally {
            myScanningIndexingMutex.unlock(INDEXING_MUTEX_OWNER);
        }
    }

    private static Set<Long> createSetForScanningIds() {
        return ConcurrentHashMap.newKeySet(4);
    }

    @TestOnly
    public static final class TestCompanion {
        private final PerProjectIndexingQueue myQueue;

        public TestCompanion(PerProjectIndexingQueue queue) {
            myQueue = queue;
        }

        public QueuedFiles getAndResetQueuedFiles() {
            return myQueue.getAndResetQueuedFiles();
        }
    }
}
