// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

import consulo.application.Application;
import consulo.application.util.Semaphore;
import consulo.language.index.impl.internal.events.ProjectDirtyFiles;
import consulo.language.index.impl.internal.stub.StubIndexImpl;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.StubIndex;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.impl.internal.DumbServiceImpl;
import consulo.project.internal.UnindexedFilesScannerExecutor;
import consulo.virtualFileSystem.ManagingFS;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Shuts the file based and the stub indexes down and loads them again from the current index extensions, keeping
 * the open projects in dumb mode meanwhile. Both calls run off the UI thread, without a read or a write action.
 */
public final class FileBasedIndexTumbler {
    private static final Logger LOG = Logger.getInstance(FileBasedIndexTumbler.class);

    private final String myReason;
    private final FileBasedIndexImpl myFileBasedIndex;
    private final Semaphore myDumbModeSemaphore = new Semaphore();

    private int myNestedLevelCount;

    public FileBasedIndexTumbler(String reason) {
        myReason = reason;
        myFileBasedIndex = (FileBasedIndexImpl) FileBasedIndex.getInstance();
    }

    public synchronized void turnOff() {
        LOG.info("Turning off file-based index. Reason: `" + myReason + "`. Nested level count: before=" + myNestedLevelCount +
            ", after=" + (myNestedLevelCount + 1));
        LOG.assertTrue(!Application.get().isWriteAccessAllowed());
        try {
            if (myNestedLevelCount == 0) {
                boolean wasUp = myDumbModeSemaphore.isUp();
                myDumbModeSemaphore.down();
                if (wasUp) {
                    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
                        if (project.isDisposed()) {
                            continue;
                        }

                        UnindexedFilesScannerExecutor scannerExecutor = UnindexedFilesScannerExecutor.getInstance(project);
                        scannerExecutor.suspendQueue();
                        scannerExecutor.cancelAllTasksAndWait();

                        PerProjectIndexingQueue.getInstance(project).clear();

                        DumbService.getInstance(project).cancelAllTasksAndWait();
                        new WaitForSemaphoreInDumbModeTask(myDumbModeSemaphore, myReason).queue(project);
                    }
                }

                myFileBasedIndex.waitUntilIndicesAreInitialized();
                // shutdown clears the in-memory dirty files, the rescanning on turning on reads them back from the project queues
                myFileBasedIndex.persistOpenProjectsDirtyFiles();
                myFileBasedIndex.performShutdown(true, myReason);
                myFileBasedIndex.dropRegisteredIndexes();
                RebuildStatus.reset();
                IndexingStamp.dropTimestampMemoryCaches();
            }
        }
        finally {
            myNestedLevelCount++;
        }
    }

    public synchronized void turnOn() {
        LOG.info("Turning on file-based index. Reason: `" + myReason + "`. Nested level count: before=" + myNestedLevelCount +
            ", after=" + (myNestedLevelCount - 1));
        myNestedLevelCount--;
        LOG.assertTrue(
            myNestedLevelCount >= 0,
            "nestedLevelCount is less than 0: " + myNestedLevelCount + ". This probably means that " +
                "DynamicPluginListener.pluginsChanged event was fired without corresponding 'before' event"
        );
        if (myNestedLevelCount != 0) {
            return;
        }

        try {
            myFileBasedIndex.loadIndexes();
            ((StubIndexImpl) StubIndex.getInstance()).initializeStubIndexes();
            if (DumbServiceImpl.isSynchronousTaskExecution()) {
                myFileBasedIndex.waitUntilIndicesAreInitialized();
            }
            for (Project project : ProjectManager.getInstance().getOpenProjects()) {
                if (!project.isDisposed()) {
                    UnindexedFilesScannerExecutor.getInstance(project).resumeQueue();
                }
            }
        }
        finally {
            myDumbModeSemaphore.up();
        }

        scheduleRescanning();
    }

    private void scheduleRescanning() {
        RegisteredIndexes registeredIndexes = myFileBasedIndex.getRegisteredIndexes();
        if (registeredIndexes == null) {
            LOG.error("Indexes are not loaded after turning on. Reason: `" + myReason + "`");
            return;
        }

        IndexingFlag.cleanupProcessedFlag(myReason);

        OrphanDirtyFilesQueue orphanQueue = registeredIndexes.getOrphanDirtyFilesQueue();
        long vfsCreationTimestamp = ManagingFS.getInstance().getCreationTimestamp();
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (project.isDisposed()) {
                continue;
            }

            ProjectDirtyFilesQueue projectDirtyFilesQueue = PersistentDirtyFilesQueue.readProjectDirtyFilesQueue(
                PersistentDirtyFilesQueue.getQueueFile(project),
                vfsCreationTimestamp
            );
            ProjectDirtyFiles projectDirtyFiles = myFileBasedIndex.getDirtyFiles().getProjectDirtyFiles(project);
            if (projectDirtyFiles != null) {
                projectDirtyFiles.addFiles(projectDirtyFilesQueue.getFileIds());
            }
            myFileBasedIndex.setLastSeenIndexInOrphanQueue(project, projectDirtyFilesQueue.getLastSeenIndexInOrphanQueue());

            CompletableFuture<?> indexesCleanupJob = UnindexedFilesScannerStartup.scanAndIndexProjectAfterOpen(
                project,
                orphanQueue,
                registeredIndexes.getOrphanDirtyFilesQueueDiscardReason(),
                List.of(),
                projectDirtyFilesQueue,
                false,
                true,
                "On FileBasedIndexTumbler.turnOn (reason=" + myReason + ")",
                ScanningType.FULL_ON_INDEX_RESTART,
                ScanningType.PARTIAL_ON_INDEX_RESTART
            );
            if (indexesCleanupJob != null) {
                UnindexedFilesScannerStartup.forgetProjectDirtyFilesOnCompletion(
                    indexesCleanupJob,
                    myFileBasedIndex,
                    project,
                    projectDirtyFilesQueue,
                    orphanQueue.getUntrimmedSize()
                );
            }
        }
        LOG.info("Index rescanning has been started. Reason: `" + myReason + "`");
    }
}
