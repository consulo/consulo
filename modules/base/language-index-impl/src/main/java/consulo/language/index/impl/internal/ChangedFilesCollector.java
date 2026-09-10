// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.internal.ProgressIndicatorUtils;
import consulo.application.progress.ProgressManager;
import consulo.application.util.concurrent.SequentialTaskExecutor;
import consulo.application.util.registry.Registry;
import consulo.component.ProcessCanceledException;
import consulo.content.ContentIterator;
import consulo.document.FileDocumentManager;
import consulo.language.index.impl.internal.events.DirtyFiles;
import consulo.language.psi.PsiManager;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.localHistory.LocalHistory;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.ConcurrencyUtil;
import consulo.util.lang.SystemProperties;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import consulo.virtualFileSystem.event.AsyncFileListener;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import jakarta.inject.Inject;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Collects file changes supplied from VFS via {@link AsyncFileListener} into the event merger.
 * Collected changes could be accessed via {@link #getEventMerger()}
 */
@ExtensionImpl
final class ChangedFilesCollector extends IndexedFilesListener {
    private static final Logger LOG = Logger.getInstance(ChangedFilesCollector.class);

    private static final int MIN_CHANGES_TO_PROCESS_ASYNC = 20;

    private static final boolean WARM_UP_FILE_TYPE_CACHE_OUTSIDE_NCS =
        SystemProperties.getBooleanProperty("indexes.warm-file-type-cache-outside-ncs", true);

    /**
     * This {@link DirtyFiles} container tracks the (VFS event -&gt; FileIndexingRequest) phase of the pipeline: {@code fileId} is tracked
     * here from the moment VFS reports it via listener, until the moment {@code fileId} is delivered into {@link FileBasedIndexImpl}
     * -- i.e., converted to a request and put into the files-to-update collector.
     */
    private final DirtyFiles myDirtyFiles = new DirtyFiles();

    private final AtomicInteger myProcessedEventIndex = new AtomicInteger();

    private final Phaser myWorkersFinishedSync = new Phaser() {
        @Override
        protected boolean onAdvance(int phase, int registeredParties) {
            return false;
        }
    };

    /** Used in {@link #ensureUpToDateAsync()} to process changes asynchronously */
    private final Executor myVfsEventsExecutor =
        SequentialTaskExecutor.createSequentialApplicationPoolExecutor("FileBasedIndex Vfs Event Processor");
    private final AtomicInteger myScheduledVfsEventsWorkers = new AtomicInteger();
    private final FileBasedIndexImpl myManager = (FileBasedIndexImpl) FileBasedIndex.getInstance();

    @Inject
    ChangedFilesCollector() {
    }

    DirtyFiles getDirtyFiles() {
        return myDirtyFiles;
    }

    @Override
    protected void iterateIndexableFiles(VirtualFile file, ContentIterator iterator) {
        if (myManager.belongsToIndexableFiles(file)) {
            VirtualFileUtil.visitChildrenRecursively(file, new VirtualFileVisitor<Void>() {
                @Override
                public boolean visitFile(VirtualFile fileOrDir) {
                    if (!myManager.belongsToIndexableFiles(fileOrDir)) {
                        return false;
                    }
                    iterator.processFile(fileOrDir);
                    return true;
                }
            });
        }
    }

    void clear() {
        myDirtyFiles.clear();
        Application application = ApplicationManager.getApplication();
        if (application == null) {
            // If the application is already disposed (ApplicationManager.getApplication() == null)
            // it means that this method is invoked via ShutDownTracker and the process will be shut down
            // so we don't need to clear collectors.
            return;
        }
        ProgressManager.getInstance().executeNonCancelableSection(
            () -> application.runReadAction(() -> processFilesInReadAction(changeInfo -> true))
        );
    }

    @Override
    protected void recordFileEvent(VirtualFile fileOrDir, boolean onlyContentDependent) {
        addToDirtyFiles(fileOrDir);
        super.recordFileEvent(fileOrDir, onlyContentDependent);
    }

    @Override
    protected void recordFileRemovedEvent(VirtualFile file) {
        addToDirtyFiles(file);
        super.recordFileRemovedEvent(file);
    }

    private void addToDirtyFiles(VirtualFile fileOrDir) {
        if (!(fileOrDir instanceof VirtualFileWithId virtualFileWithId)) {
            return;
        }

        int fileId = virtualFileWithId.getId();
        List<Project> projects = myManager.getIndexableFilesFilterHolder().findProjectsForFile(fileId);
        myDirtyFiles.addFile(projects, fileId);

        if (Boolean.getBoolean(DEBUG_PROPERTY)) {
            LOG.warn("DIRTY-DEBUG addToDirtyFiles id=" + fileId
                + " file=" + fileOrDir.getPath()
                + " projectsFromFilter=" + projects
                + " projectsInQueue=" + myDirtyFiles.getProjects()
                + " collector=" + System.identityHashCode(this)
                + " thread=" + Thread.currentThread().getName());
        }
    }

    @Override
    public AsyncFileListener.ChangeApplier prepareChange(List<? extends VFileEvent> events) {
        boolean shouldCleanup = ContainerUtil.exists(events, ChangedFilesCollector::memoryStorageCleaningNeeded);
        AsyncFileListener.ChangeApplier superApplier = super.prepareChange(events);

        return new AsyncFileListener.ChangeApplier() {
            @Override
            public void beforeVfsChange() {
                if (shouldCleanup) {
                    myManager.cleanupMemoryStorage(false);
                }
                superApplier.beforeVfsChange();
            }

            @Override
            public void afterVfsChange() {
                superApplier.afterVfsChange();
                RegisteredIndexes registeredIndexes = myManager.getRegisteredIndexes();
                if (registeredIndexes != null && registeredIndexes.isInitialized()) {
                    ensureUpToDateAsync();
                }
            }
        };
    }

    private static boolean memoryStorageCleaningNeeded(VFileEvent event) {
        Object requestor = event.getRequestor();
        return requestor instanceof FileDocumentManager || requestor instanceof PsiManager || requestor == LocalHistory.VFS_EVENT_REQUESTOR;
    }

    void ensureUpToDate() {
        if (!FileBasedIndexImpl.isUpToDateCheckEnabled()) {
            return;
        }
        //assert ApplicationManager.getApplication().isReadAccessAllowed() || ShutDownTracker.isShutdownHookRunning();
        myManager.waitUntilIndicesAreInitialized();

        if (ApplicationManager.getApplication().isReadAccessAllowed()) {
            processFilesToUpdateInReadAction();
        }
        else {
            processFilesInReadActionWithYieldingToWriteAction();
        }
    }

    void ensureUpToDateAsync() {
        if (getEventMerger().getApproximateChangesCount() < MIN_CHANGES_TO_PROCESS_ASYNC
            || !myScheduledVfsEventsWorkers.compareAndSet(0, 1)) {
            return;
        }

        myVfsEventsExecutor.execute(() -> {
            try {
                processFilesInReadActionWithYieldingToWriteAction();

                if (Registry.is("try.starting.dumb.mode.where.many.files.changed")) {
                    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
                        try {
                            FileBasedIndexProjectHandler.scheduleReindexingInDumbMode(project);
                        }
                        catch (ProcessCanceledException ignored) {
                        }
                        catch (Exception e) {
                            LOG.error(e);
                        }
                    }
                }
            }
            finally {
                myScheduledVfsEventsWorkers.decrementAndGet();
            }
        });
    }

    void processFilesToUpdateInReadAction() {
        processFilesInReadAction(new VfsEventsMerger.VfsEventProcessor() {
            @Override
            public void prepare(VfsEventsMerger.ChangeInfo info) {
                if (WARM_UP_FILE_TYPE_CACHE_OUTSIDE_NCS && !info.isFileRemoved()) {
                    // IJPL-238333: Warm the file type cache OUTSIDE the lock/non-cancellable section below.
                    // freezeFileTypeTemporarilyIn() inside scheduleFileForIncrementalIndexing() triggers FileType
                    // detection by content => does IO on first access => better avoid it inside locks/NCS => let's
                    // do it earlier, so the fileType is cached, and just read from the cache inside lock/NCS below
                    info.getFile().getFileType();
                }
            }

            @Override
            public boolean process(VfsEventsMerger.ChangeInfo info) {
                int fileId = info.getFileId();
                try {
                    VirtualFile file = info.getFile();
                    List<Project> dirtyQueueProjects = myDirtyFiles.getProjects(fileId);
                    if (info.isTransientStateChanged()) {
                        myManager.doTransientStateChangeForFile(fileId, file, dirtyQueueProjects);
                    }
                    if (info.isContentChanged()) {
                        myManager.scheduleFileForIncrementalIndexing(fileId, file, true, dirtyQueueProjects);
                    }
                    if (info.isFileRemoved()) {
                        myManager.doInvalidateIndicesForFile(fileId, file, Collections.emptySet(), dirtyQueueProjects);
                    }
                    if (info.isFileAdded()) {
                        myManager.scheduleFileForIncrementalIndexing(fileId, file, false, dirtyQueueProjects);
                    }
                }
                catch (Throwable t) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Exception while processing " + info, t);
                    }
                    throw t;
                }
                finally {
                    if (Boolean.getBoolean(DEBUG_PROPERTY)) {
                        LOG.warn("DIRTY-DEBUG consumed id=" + fileId
                            + " collector=" + System.identityHashCode(ChangedFilesCollector.this)
                            + " thread=" + Thread.currentThread().getName());
                    }
                    myDirtyFiles.removeFile(fileId);
                }
                return true;
            }
        });
    }

    private void processFilesInReadAction(VfsEventsMerger.VfsEventProcessor processor) {
        assert ApplicationManager.getApplication().isReadAccessAllowed(); // no vfs events -> event processing code can finish

        int publishedEventIndex = getEventMerger().getPublishedEventIndex();
        int processedEventIndex = myProcessedEventIndex.get();
        if (processedEventIndex == publishedEventIndex) {
            return;
        }

        myWorkersFinishedSync.register();
        int phase = myWorkersFinishedSync.getPhase();
        try {
            myManager.waitUntilIndicesAreInitialized();
            getEventMerger().processChanges(new VfsEventsMerger.VfsEventProcessor() {
                @Override
                public void prepare(VfsEventsMerger.ChangeInfo changeInfo) {
                    processor.prepare(changeInfo);
                }

                @Override
                public boolean process(VfsEventsMerger.ChangeInfo changeInfo) {
                    ConcurrencyUtil.withLock(myManager.myWriteLock, () -> {
                        try {
                            ProgressManager.getInstance().executeNonCancelableSection(() -> processor.process(changeInfo));
                        }
                        finally {
                            IndexingStamp.flushCache(changeInfo.getFileId());
                        }
                    });
                    return true;
                }

                @Override
                public void endBatch() {
                    ConcurrencyUtil.withLock(myManager.myWriteLock, processor::endBatch);
                }
            });
        }
        finally {
            myWorkersFinishedSync.arriveAndDeregister();
        }

        try {
            awaitWithCheckCancelled(myWorkersFinishedSync, phase);
        }
        catch (InterruptedException e) {
            LOG.warn(e);
            throw new ProcessCanceledException(e);
        }

        if (getEventMerger().getPublishedEventIndex() == publishedEventIndex) {
            myProcessedEventIndex.compareAndSet(processedEventIndex, publishedEventIndex);
        }
    }

    private void processFilesInReadActionWithYieldingToWriteAction() {
        while (getEventMerger().hasChanges()) {
            if (!ProgressIndicatorUtils.runInReadActionWithWriteActionPriority(this::processFilesToUpdateInReadAction)) {
                ProgressIndicatorUtils.yieldToPendingWriteActions();
            }
        }
    }

    private static void awaitWithCheckCancelled(Phaser phaser, int phase) throws InterruptedException {
        while (true) {
            ProgressManager.checkCanceled();
            try {
                phaser.awaitAdvanceInterruptibly(phase, 100, TimeUnit.MILLISECONDS);
                break;
            }
            catch (TimeoutException ignored) {
            }
        }
    }
}
