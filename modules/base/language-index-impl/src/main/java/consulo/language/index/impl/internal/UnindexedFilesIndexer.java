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

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.language.index.impl.internal.PerProjectIndexingQueue.QueuedFiles;
import consulo.language.index.impl.internal.dependencies.IncompleteIndexingToken;
import consulo.language.index.impl.internal.events.FileIndexingRequest;
import consulo.language.index.impl.internal.dependencies.IndexingRequestToken;
import consulo.language.index.impl.internal.dependencies.ProjectIndexingDependenciesService;
import consulo.language.index.impl.internal.gist.GistManagerImpl;
import consulo.language.index.impl.internal.localize.IndexingLocalize;
import consulo.language.index.impl.internal.roots.IndexableFilesIterator;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.gist.GistManager;
import consulo.logging.Logger;
import consulo.project.DumbModeTask;
import consulo.project.Project;
import consulo.util.lang.ControlFlowException;
import consulo.util.lang.StringUtil;
import consulo.util.lang.SystemProperties;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * UnindexedFilesIndexer is to index files: explicitly provided (see {@linkplain PerProjectIndexingQueue}), and implicitly marked as dirty,
 * e.g., by VFS (as reported by FileBasedIndexImpl#getFilesToUpdate).
 */
public final class UnindexedFilesIndexer extends DumbModeTask {
    private record IndexingReason(String reason, int counter) {
        @Override
        public String toString() {
            if (counter > 1) {
                return reason + " (x" + counter + ")";
            }
            else {
                return reason;
            }
        }
    }

    private static final Logger LOG = Logger.getInstance(UnindexedFilesIndexer.class);
    private final Project myProject;
    private final FileBasedIndexImpl myIndex;
    private final List<IndexingReason> myIndexingReasons;
    private final @Nullable PerProjectIndexingQueue myCustomFilesSource;
    private final IncompleteIndexingToken myTaskToken;


    public UnindexedFilesIndexer(Project project, String indexingReason) {
        this(project, null, indexingReason);
    }

    /**
     * If customFilesSource is NOT null,
     * then files from customFilesSource will be indexed in the first order, then files reported by FileBasedIndexImpl#getFilesToUpdate
     * (files from PerProjectIndexingQueue are not indexed in this case)
     * <br>
     * If customFilesSource is null,
     * then files from PerProjectIndexingQueue will be indexed in the first order, then files reported by
     * FileBasedIndexImpl#getFilesToUpdate
     */

    public UnindexedFilesIndexer(Project project, @Nullable PerProjectIndexingQueue customFilesSource, String indexingReason) {
        this(project, customFilesSource, Collections.singletonList(new IndexingReason(indexingReason, 1)));
    }

    private UnindexedFilesIndexer(
        Project project,
        @Nullable PerProjectIndexingQueue customFilesSource,
        List<IndexingReason> indexingReasons
    ) {
        myProject = project;
        myIndex = (FileBasedIndexImpl) FileBasedIndex.getInstance();
        myCustomFilesSource = customFilesSource;
        myIndexingReasons = indexingReasons;
        myTaskToken = ProjectIndexingDependenciesService.getInstance(project).newIncompleteIndexingToken();
    }

    void indexFiles(ProgressIndicator indicator) {
        if (SystemProperties.getBooleanProperty("idea.indexes.pretendNoFiles", false)) {
            LOG.info("Finished for " + myProject.getName() + ". System property 'idea.indexes.pretendNoFiles' is enabled.");
            return;
        }

        indicator.setIndeterminate(false);
        indicator.setFraction(0);
        indicator.setText(IndexingLocalize.progressIndexingUpdating());

        doIndexFiles(indicator);
    }

    private void doIndexFiles(ProgressIndicator indicator) {
        IndexingRequestToken indexingRequest = ProjectIndexingDependenciesService.getInstance(myProject).getLatestIndexingRequestToken();

        QueuedFiles queuedFiles = getExplicitlyRequestedFiles();

        Collection<VirtualFile> explicitlyRequestedFiles = queuedFiles.getRequests();
        if (!explicitlyRequestedFiles.isEmpty()) {
            doIndexFiles(indicator, "<indexing queue>", explicitlyRequestedFiles, indexingRequest);
        }

        // Order is important: getRefreshedFiles may return some subset of getExplicitlyRequestedFilesSets files (e.g., new files)
        // We first index explicitly requested files, this will also mark indexed files as "up-to-date", then we index remaining dirty files
        IndexableFilesIterator refreshedFilesProvider = new FileBasedIndexProjectHandler.IndexableFilesIteratorForRefreshedFiles(myProject);
        Collection<FileIndexingRequest> refreshedFiles = getRefreshedFiles();
        if (!refreshedFiles.isEmpty()) {
            indicator.setText(refreshedFilesProvider.getIndexingProgressText());
            doIndexRequests(indicator, refreshedFilesProvider.getDebugName(), refreshedFiles, indexingRequest);
        }
    }

    private Collection<FileIndexingRequest> getRefreshedFiles() {
        return new FileBasedIndexProjectHandler.ProjectChangedFilesScanner(myProject).scan();
    }

    private void doIndexRequests(
        ProgressIndicator indicator,
        String fileSetName,
        Collection<FileIndexingRequest> requests,
        IndexingRequestToken indexingRequest
    ) {
        List<VirtualFile> filesToLoad = new ArrayList<>(requests.size());
        for (FileIndexingRequest request : requests) {
            if (request.isDeleteRequest()) {
                VirtualFile file = request.getFile();
                myIndex.indexFileContent(myProject, new IndexFileContent(file), true, indexingRequest.getFileIndexingStamp(file));
            }
            else {
                filesToLoad.add(request.getFile());
            }
        }
        if (!filesToLoad.isEmpty()) {
            doIndexFiles(indicator, fileSetName, filesToLoad, indexingRequest);
        }
    }

    private QueuedFiles getExplicitlyRequestedFiles() {
        PerProjectIndexingQueue sourceQueue;
        if (myCustomFilesSource != null) {
            sourceQueue = myCustomFilesSource;
        }
        else {
            sourceQueue = PerProjectIndexingQueue.getInstance(myProject);
        }
        return sourceQueue.getAndResetQueuedFiles();
    }

    private void doIndexFiles(
        ProgressIndicator indicator,
        String fileSetName,
        Collection<VirtualFile> files,
        IndexingRequestToken indexingRequest
    ) {
        int totalFiles = files.size();
        LOG.info("Unindexed files update started: " + totalFiles + " files to index (" + fileSetName + ")");

        long startedAt = System.nanoTime();
        CacheUpdateRunner.processFiles(
            indicator,
            files,
            myProject,
            content -> myIndex.indexFileContent(
                myProject,
                content,
                false,
                indexingRequest.getFileIndexingStamp(content.getVirtualFile())
            )
        );
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        ProgressManager.checkCanceled();

        LOG.info("Unindexed files update finished: " + totalFiles + " files in " + elapsedMillis + " ms on "
            + CacheUpdateRunner.indexingThreadCount() + " threads ("
            + (elapsedMillis == 0 ? totalFiles : totalFiles * 1000L / elapsedMillis) + " files/s)");
    }

    @Override
    public void performInDumbMode(ProgressIndicator indicator, Exception trace) {
        // indexingQueue.wrapIndexing will try to acquire scanningIndexingMutex, and will wait until scanning is completed.
        // Set progress text for this "waiting"
        indicator.setIndeterminate(true);
        indicator.setText(IndexingLocalize.progressIndexingWaitingForScanningToComplete());

        PerProjectIndexingQueue indexingQueue = PerProjectIndexingQueue.getInstance(myProject);
        indexingQueue.wrapIndexing(() -> doPerformInDumbMode(indicator));
    }

    private void doPerformInDumbMode(ProgressIndicator indicator) {
        // make sure that indexes are loaded, because we can get here without scanning (e.g., from VFS refresh)
        myIndex.waitUntilIndicesAreInitialized();
        if (!IndexInfrastructure.hasIndices()) {
            return;
        }

        try {
            ((GistManagerImpl) GistManager.getInstance()).runWithMergingDependentCacheInvalidations(() -> indexFiles(indicator));
        }
        catch (Throwable e) {
            myTaskToken.markUnsuccessful();
            if (e instanceof ControlFlowException) {
                LOG.info("Cancelled indexing of " + myProject.getName());
            }
            throw e;
        }
    }

    @Override
    public void dispose() {
        if (!myProject.isDisposed()) {
            ProjectIndexingDependenciesService.getInstance(myProject).completeToken(myTaskToken);
        }
    }

    @Override
    public @Nullable UnindexedFilesIndexer tryMergeWith(DumbModeTask taskFromQueue) {
        if (!(taskFromQueue instanceof UnindexedFilesIndexer otherIndexingTask)) {
            return null;
        }
        if (otherIndexingTask.myCustomFilesSource != myCustomFilesSource) {
            LOG.error("Indexing tasks with different sources should not appear in the same task queue. " +
                "This is likely a bug in a test (custom sources is a test-only feature)");
            return null;
        }

        List<IndexingReason> mergedReason = mergeReasons(otherIndexingTask);
        return new UnindexedFilesIndexer(myProject, myCustomFilesSource, mergedReason);
    }

    private List<IndexingReason> mergeReasons(UnindexedFilesIndexer otherIndexingTask) {
        TreeMap<String, IndexingReason> mergedReasons = new TreeMap<>(); // to preserve the order
        for (IndexingReason reason : myIndexingReasons) {
            mergedReasons.put(reason.reason(), reason);
        }
        for (IndexingReason reason : otherIndexingTask.myIndexingReasons) {
            IndexingReason existingReason = mergedReasons.get(reason.reason());
            if (existingReason != null) {
                mergedReasons.put(existingReason.reason(),
                    new IndexingReason(existingReason.reason(), existingReason.counter() + reason.counter()));
            }
            else {
                mergedReasons.put(reason.reason(), reason);
            }
        }

        return new ArrayList<>(mergedReasons.values());
    }

    public String getIndexingReason() {
        if (myIndexingReasons.size() == 1) {
            return myIndexingReasons.get(0).toString();
        }
        else if (myIndexingReasons.size() > 1) {
            return "Merged " + StringUtil.join(myIndexingReasons, " with ");
        }
        else {
            LOG.error("indexingReasons should never be empty");
            return "<unknown>";
        }
    }

    @Override
    public String toString() {
        return "UnindexedFilesIndexer[" + myProject.getName() + ", reasons: " + myIndexingReasons + "]";
    }
}
