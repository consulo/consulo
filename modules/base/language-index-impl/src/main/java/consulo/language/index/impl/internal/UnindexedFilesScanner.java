// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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

import consulo.application.Application;
import consulo.application.internal.ProgressIndicatorUtils;
import consulo.application.progress.ProgressManager;
import consulo.application.util.registry.Registry;
import consulo.component.ProcessCanceledException;
import consulo.language.index.impl.internal.FilesFilterScanningHandler.IdleFilesFilterScanningHandler;
import consulo.language.index.impl.internal.FilesFilterScanningHandler.UpdatingFilesFilterScanningHandler;
import consulo.language.index.impl.internal.IndexingProgressReporter.CheckPauseOnlyProgressIndicator;
import consulo.language.index.impl.internal.IndexingProgressReporter.IndexingSubTaskProgressReporter;
import consulo.language.index.impl.internal.UnindexedFilesScannerStartup.FirstScanningState;
import consulo.language.index.impl.internal.dependencies.FileIndexingStamp;
import consulo.language.index.impl.internal.dependencies.IncompleteTaskToken;
import consulo.language.index.impl.internal.dependencies.ProjectIndexingDependenciesService;
import consulo.language.index.impl.internal.dependencies.ScanningRequestToken;
import consulo.language.index.impl.internal.gist.GistManagerImpl;
import consulo.language.index.impl.internal.localize.IndexingLocalize;
import consulo.language.index.impl.internal.projectFilter.ProjectIndexableFilesFilterHolder;
import consulo.language.index.impl.internal.roots.IndexableFilesDeduplicateFilter;
import consulo.language.index.impl.internal.roots.IndexableFilesIterator;
import consulo.language.index.impl.internal.roots.kind.IndexableSetOrigin;
import consulo.language.index.impl.internal.roots.kind.SdkOrigin;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.IndexedFile;
import consulo.language.psi.stub.gist.GistManager;
import consulo.logging.Logger;
import consulo.module.content.PushedFilePropertiesUpdater;
import consulo.project.Project;
import consulo.project.internal.FilesScanningTask;
import consulo.project.internal.UnindexedFilesScannerExecutor;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.UserDataHolderEx;
import consulo.util.lang.ControlFlowException;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import org.jetbrains.annotations.TestOnly;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiPredicate;

/**
 * A task in {@link UnindexedFilesScannerExecutor}: (re-)scan files to index.
 * Typical usage is {@code UnindexedFilesScanner(...).queue()} -- submits the {@code UnindexedFilesScanner} as a task into a shared
 * {@link UnindexedFilesScannerExecutor}, don't wait, return a {@link Future} to control async execution.
 * <p>
 * By default, files to scan are defined in {@link FileBasedIndexImpl#getOrderedIndexableFilesProviders} (=project's indexable files), but
 * could be overriden with {@link ScanningParameters}, see {@link ScanningIterators#getPredefinedIndexableFilesIterators()}.
 * <p>
 * {@link consulo.module.content.FilePropertyPusher}s are applied to the scanned files, before enqueueing them into
 * {@link PerProjectIndexingQueue}
 * <p>
 * Scanning is done in {@link UnindexedFilesUpdater#getNumberOfScanningThreads()} parallel workers.
 * <p>
 * BEWARE: Scanner implements {@link Closeable}, but usually it doesn't need try-with-resources, because {@link #close} actually called
 * async, while processed by {@link UnindexedFilesScannerExecutor}. The only case there {@link #close} should be called explicitly is when
 * the scanner object is created, but does NOT {@link #queue}-ed.
 * MAYBE RC: drop Closeable, just leave {@link #close} method? -- avoids 'Closeable without try-w-resources' inspection warnings
 */
public final class UnindexedFilesScanner implements FilesScanningTask, Closeable {
    static final Logger LOG = Logger.getInstance(UnindexedFilesScanner.class);

    private static final AtomicLong ourScanningSessionIds = new AtomicLong();

    public enum TestMode {
        PUSHING,
        PUSHING_AND_SCANNING
    }


    public static volatile @Nullable TestMode ourTestMode = null;

    private final Project myProject;
    private final boolean myOnProjectOpen;
    private final @Nullable Future<?> myStartCondition;
    private final @Nullable Boolean myShouldHideProgressInSmartMode;
    private final @Nullable BiPredicate<? super IndexedFile, ? super FileIndexingStamp> myForceReindexingTrigger;
    private final boolean myForceCheckingForOutdatedIndexesUsingFileModCount;
    private final CompletableFuture<ScanningParameters> myScanningParameters;

    private final FileBasedIndexImpl myFileBasedIndex = (FileBasedIndexImpl) FileBasedIndex.getInstance();
    private final FilesFilterScanningHandler myFilterHandler;
    private final long myScanningSessionId = ourScanningSessionIds.incrementAndGet();
    private final IncompleteTaskToken myTaskToken;

    public UnindexedFilesScanner(
        Project project,
        boolean onProjectOpen,
        boolean isIndexingFilesFilterUpToDate,
        @Nullable Future<?> startCondition,
        @Nullable Boolean shouldHideProgressInSmartMode,
        @Nullable BiPredicate<? super IndexedFile, ? super FileIndexingStamp> forceReindexingTrigger,
        boolean forceCheckingForOutdatedIndexesUsingFileModCount,
        CompletableFuture<ScanningParameters> scanningParameters
    ) {
        myProject = project;
        myOnProjectOpen = onProjectOpen;
        myStartCondition = startCondition;
        myShouldHideProgressInSmartMode = shouldHideProgressInSmartMode;
        myForceReindexingTrigger = forceReindexingTrigger;
        myForceCheckingForOutdatedIndexesUsingFileModCount = forceCheckingForOutdatedIndexesUsingFileModCount;
        myScanningParameters = scanningParameters;

        ProjectIndexableFilesFilterHolder filterHolder = myFileBasedIndex.getIndexableFilesFilterHolder();
        myFilterHandler = isIndexingFilesFilterUpToDate
            ? new IdleFilesFilterScanningHandler()
            : new UpdatingFilesFilterScanningHandler(filterHolder);
        myTaskToken = ProjectIndexingDependenciesService.getInstance(project).newIncompleteTaskToken();
    }

    public UnindexedFilesScanner(Project project, CompletableFuture<ScanningParameters> scanningParameters) {
        this(project, false, false, null, null, null, false, scanningParameters);
    }

    public UnindexedFilesScanner(Project project, String indexingReason, @Nullable Boolean shouldHideProgressInSmartMode) {
        this(project, false, false, null, shouldHideProgressInSmartMode, null, false,
            CompletableFuture.completedFuture(new ScanningIterators(indexingReason)));
    }

    public UnindexedFilesScanner(Project project, String indexingReason) {
        this(project, CompletableFuture.completedFuture(new ScanningIterators(indexingReason)));
    }

    public UnindexedFilesScanner(
        Project project,
        @Nullable List<IndexableFilesIterator> predefinedIndexableFilesIterators,
        String indexingReason
    ) {
        this(project, CompletableFuture.completedFuture(new ScanningIterators(indexingReason, predefinedIndexableFilesIterators)));
    }

    @TestOnly
    public UnindexedFilesScanner(Project project) {
        this(project, CompletableFuture.completedFuture(new ScanningIterators("<unknown>")));
    }

    private String prepareLogMessage(String message) {
        return "[" + myProject.getLocationHash() + "] " + message;
    }

    private void logInfo(String message) {
        LOG.info(prepareLogMessage(message));
    }

    private boolean defaultHideProgressInSmartModeStrategy() {
        return Registry.is("scanning.hide.progress.in.smart.mode", true) &&
            myProject.getUserData(UnindexedFilesScannerStartup.FIRST_SCANNING_REQUESTED) == FirstScanningState.REQUESTED;
    }

    public boolean shouldHideProgressInSmartMode() {
        return myShouldHideProgressInSmartMode != null ? myShouldHideProgressInSmartMode : defaultHideProgressInSmartModeStrategy();
    }

    long getScanningSessionId() {
        return myScanningSessionId;
    }

    /**
     * We may not have information about whether it's a full update or not, in which case we return null
     */
    @Override
    public @Nullable Boolean isFullIndexUpdate() {
        ScanningParameters parameters = getCompletedSafe();
        if (parameters == null) {
            return null;
        }
        return parameters instanceof ScanningIterators scanningIterators && scanningIterators.isFullIndexUpdate();
    }

    ScanningParameters getScanningParameters() {
        return myScanningParameters.join();
    }

    public UnindexedFilesScanner tryMergeWith(FilesScanningTask oldTask) {
        UnindexedFilesScanner old = (UnindexedFilesScanner) oldTask;

        LOG.assertTrue(myProject.equals(old.myProject));

        CompletableFuture<ScanningParameters> mergedParameters =
            myScanningParameters.thenCombine(old.myScanningParameters, UnindexedFilesScanner::mergeScanningParameters);

        LOG.assertTrue(!(myStartCondition != null && old.myStartCondition != null), "Merge of two start conditions is not implemented");
        Boolean mergedHideProgress;
        if (myShouldHideProgressInSmartMode == null) {
            mergedHideProgress = old.myShouldHideProgressInSmartMode;
        }
        else if (old.myShouldHideProgressInSmartMode != null) {
            mergedHideProgress = myShouldHideProgressInSmartMode && old.myShouldHideProgressInSmartMode;
        }
        else {
            mergedHideProgress = myShouldHideProgressInSmartMode;
        }

        BiPredicate<? super IndexedFile, ? super FileIndexingStamp> triggerA = myForceReindexingTrigger;
        BiPredicate<? super IndexedFile, ? super FileIndexingStamp> triggerB = old.myForceReindexingTrigger;

        BiPredicate<IndexedFile, FileIndexingStamp> mergedPredicate;
        if (triggerA == null && triggerB == null) {
            mergedPredicate = null;
        }
        else {
            mergedPredicate = (f, stamp) ->
                (triggerA != null && triggerA.test(f, stamp)) || (triggerB != null && triggerB.test(f, stamp));
        }

        return new UnindexedFilesScanner(
            myProject,
            false,
            false,
            myStartCondition != null ? myStartCondition : old.myStartCondition,
            mergedHideProgress,
            mergedPredicate,
            myForceCheckingForOutdatedIndexesUsingFileModCount || old.myForceCheckingForOutdatedIndexesUsingFileModCount,
            mergedParameters
        );
    }

    private static ScanningParameters mergeScanningParameters(ScanningParameters parameters, ScanningParameters oldParameters) {
        if (parameters instanceof CancelledScanning) {
            return oldParameters;
        }
        if (oldParameters instanceof CancelledScanning) {
            return parameters;
        }

        ScanningIterators iterators = (ScanningIterators) parameters;
        ScanningIterators oldIterators = (ScanningIterators) oldParameters;

        String reason;
        if (oldIterators.isFullIndexUpdate()) {
            reason = oldIterators.getIndexingReason();
        }
        else if (iterators.isFullIndexUpdate()) {
            reason = iterators.getIndexingReason();
        }
        else {
            reason = "Merged " + removePrefix(iterators.getIndexingReason(), "Merged ") +
                " with " + removePrefix(oldIterators.getIndexingReason(), "Merged ");
        }

        return new ScanningIterators(
            reason,
            mergeIterators(iterators.getPredefinedIndexableFilesIterators(), oldIterators.getPredefinedIndexableFilesIterators()),
            ScanningType.merge(iterators.getScanningType(), oldIterators.getScanningType())
        );
    }

    private static String removePrefix(String value, String prefix) {
        return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }

    private void scan(
        CheckPauseOnlyProgressIndicator indicator,
        IndexingProgressReporter progressReporter,
        ScanningIterators scanningIterators
    ) {
        List<IndexableFilesIterator> orderedProviders = getIndexableFilesIterators(scanningIterators);

        ProgressManager.checkCanceled();
        ProjectIndexingDependenciesService projectIndexingDependenciesService = ProjectIndexingDependenciesService.getInstance(myProject);
        ScanningRequestToken scanningRequest = myOnProjectOpen
            ? projectIndexingDependenciesService.newScanningTokenOnProjectOpen(myForceCheckingForOutdatedIndexesUsingFileModCount)
            : projectIndexingDependenciesService.newScanningToken();

        ScanningSession session = new ScanningSession(
            myProject,
            myForceReindexingTrigger,
            myFilterHandler,
            indicator,
            progressReporter,
            myScanningSessionId,
            scanningRequest
        );
        try {
            session.collectIndexableFilesConcurrently(orderedProviders);
        }
        finally {
            projectIndexingDependenciesService.completeToken(scanningRequest, scanningIterators.isFullIndexUpdate());
        }
        ProgressManager.checkCanceled();

        logInfo(getLogScanningCompletedStageMessage(session));
    }

    private List<IndexableFilesIterator> getIndexableFilesIterators(ScanningIterators scanningIterators) {
        ProgressManager.checkCanceled();
        try {
            List<IndexableFilesIterator> predefinedIndexableFilesIterators = scanningIterators.getPredefinedIndexableFilesIterators();
            if (predefinedIndexableFilesIterators == null) {
                return collectProviders(myProject, myFileBasedIndex);
            }
            else {
                return predefinedIndexableFilesIterators;
            }
        }
        finally {
            ProgressManager.checkCanceled();
        }
    }

    void applyDelayedPushOperations() {
        ProgressManager.checkCanceled();
        PushedFilePropertiesUpdater pusher = PushedFilePropertiesUpdater.getInstance(myProject);
        if (pusher instanceof PushedFilePropertiesUpdaterImpl pusherImpl) {
            pusherImpl.performDelayedPushTasks();
        }
        ProgressManager.checkCanceled();
    }

    @TestOnly
    public @Nullable String getIndexingReasonBlocking() {
        if (canReadScanningParameters()) {
            ScanningParameters parameters = getScanningParametersBlocking();
            if (parameters instanceof ScanningIterators scanningIterators) {
                return scanningIterators.getIndexingReason();
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }

    @TestOnly
    public @Nullable List<IndexableFilesIterator> getPredefinedIndexableFileIteratorsBlocking() {
        if (canReadScanningParameters()) {
            ScanningParameters parameters = getScanningParametersBlocking();
            if (parameters instanceof ScanningIterators scanningIterators) {
                return scanningIterators.getPredefinedIndexableFilesIterators();
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }

    @TestOnly
    public @Nullable ScanningType getScanningTypeBlocking() {
        if (canReadScanningParameters()) {
            ScanningParameters parameters = getScanningParametersBlocking();
            if (parameters instanceof ScanningIterators scanningIterators) {
                return scanningIterators.getScanningType();
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }

    @TestOnly
    private boolean canReadScanningParameters() {
        // If we use runBlockingMaybeCancellable right away, it can lead to a deadlock
        return myScanningParameters.isDone() || !myProject.getApplication().isWriteAccessAllowed();
    }

    @TestOnly
    private ScanningParameters getScanningParametersBlocking() {
        return myScanningParameters.join();
    }

    private @Nullable List<IndexableFilesIterator> tryGetPredefinedIndexableFileIterators() {
        ScanningParameters scanningIterators = getCompletedSafe();
        if (!(scanningIterators instanceof ScanningIterators iterators)) {
            return null;
        }
        return iterators.getPredefinedIndexableFilesIterators();
    }

    private @Nullable ScanningParameters getCompletedSafe() {
        if (!myScanningParameters.isDone() || myScanningParameters.isCompletedExceptionally() || myScanningParameters.isCancelled()) {
            return null;
        }
        return myScanningParameters.getNow(null);
    }

    private void scanAndUpdateUnindexedFiles(
        CheckPauseOnlyProgressIndicator indicator,
        IndexingProgressReporter progressReporter,
        ScanningIterators scanningIterators
    ) {
        try {
            if (!IndexInfrastructure.hasIndices()) {
                return;
            }
            scanUnindexedFiles(indicator, progressReporter, scanningIterators);
        }
        finally {
            ((UserDataHolderEx) myProject).replace(
                UnindexedFilesScannerStartup.FIRST_SCANNING_REQUESTED,
                FirstScanningState.REQUESTED,
                FirstScanningState.PERFORMED
            );
        }
    }

    private void scanUnindexedFiles(
        CheckPauseOnlyProgressIndicator indicator,
        IndexingProgressReporter progressReporter,
        ScanningIterators scanningIterators
    ) {
        logInfo("Started scanning for indexing of [" + myProject.getName() + "]. Reason: " + scanningIterators.getIndexingReason());

        progressReporter.setText(IndexingLocalize.progressIndexingScanning());

        if (scanningIterators.isFullIndexUpdate()) {
            myFileBasedIndex.clearIndicesIfNecessary();
        }

        scan(indicator, progressReporter, scanningIterators);

        // the full VFS refresh makes sense only after it's loaded, i.e., after scanning files to index is finished
        InitialVfsRefreshService service = InitialVfsRefreshService.getInstance(myProject);
        Application application = myProject.getApplication();
        if (application.isCommandLine()) {
            service.runInitialVfsRefresh();
        }
        else {
            service.scheduleInitialVfsRefresh();
        }
    }

    static final class ScanningSession {
        private final Project myProject;
        private final @Nullable BiPredicate<? super IndexedFile, ? super FileIndexingStamp> myForceReindexingTrigger;
        private final FilesFilterScanningHandler myFilterHandler;
        private final CheckPauseOnlyProgressIndicator myIndicator;
        private final IndexingProgressReporter myProgressReporter;
        private final long myScanningSessionId;
        private final ScanningRequestToken myScanningRequest;

        private final AtomicLong myNumberOfScannedFiles = new AtomicLong();
        private final AtomicLong myNumberOfFilesForIndexing = new AtomicLong();

        ScanningSession(
            Project project,
            @Nullable BiPredicate<? super IndexedFile, ? super FileIndexingStamp> forceReindexingTrigger,
            FilesFilterScanningHandler filterHandler,
            CheckPauseOnlyProgressIndicator indicator,
            IndexingProgressReporter progressReporter,
            long scanningSessionId,
            ScanningRequestToken scanningRequest
        ) {
            myProject = project;
            myForceReindexingTrigger = forceReindexingTrigger;
            myFilterHandler = filterHandler;
            myIndicator = indicator;
            myProgressReporter = progressReporter;
            myScanningSessionId = scanningSessionId;
            myScanningRequest = scanningRequest;
        }

        long getNumberOfScannedFiles() {
            return myNumberOfScannedFiles.get();
        }

        long getNumberOfFilesForIndexing() {
            return myNumberOfFilesForIndexing.get();
        }

        void collectIndexableFilesConcurrently(List<IndexableFilesIterator> providers) {
            if (providers.isEmpty()) {
                return;
            }

            IndexableFilesDeduplicateFilter indexableFilesDeduplicateFilter = IndexableFilesDeduplicateFilter.create();

            LOG.info("Scanning of [" + myProject.getName() + "] uses " + UnindexedFilesUpdater.getNumberOfScanningThreads()
                + " scanning threads");
            myProgressReporter.setText(IndexingLocalize.progressIndexingScanning());
            myProgressReporter.setSubTasksCount(providers.size());

            List<Runnable> tasks = ContainerUtil.map(providers, provider -> () -> {
                try {
                    scanSingleProvider(provider, indexableFilesDeduplicateFilter);
                }
                catch (Throwable t) {
                    if (t instanceof ProcessCanceledException) {
                        throw t;
                    }
                    if (t instanceof ControlFlowException) {
                        LOG.warn("Unexpected exception during scanning: " + t.getMessage());
                    }
                    else {
                        LOG.error("Unexpected exception during scanning (ignored)", t);
                    }
                }
            });

            PushedFilePropertiesUpdaterImpl.invokeConcurrentlyIfPossible(tasks);
        }

        private void scanSingleProvider(IndexableFilesIterator provider, IndexableFilesDeduplicateFilter indexableFilesDeduplicateFilter) {
            IndexableFilesDeduplicateFilter thisProviderDeduplicateFilter =
                IndexableFilesDeduplicateFilter.createDelegatingTo(indexableFilesDeduplicateFilter);

            try (IndexingSubTaskProgressReporter subTaskReporter = myProgressReporter.getSubTaskReporter()) {
                subTaskReporter.setText(provider.getRootsScanningProgressText());
                ArrayDeque<VirtualFile> files = getFilesToScan(provider, thisProviderDeduplicateFilter);
                scanFiles(provider, files);
            }
            // A plugin's code (e.g. an index input filter) may throw an Error -- NoClassDefFoundError.
            // Such an Error must not escape and cancel the scanning of _all_ the other providers.
            catch (Throwable e) {
                myScanningRequest.markUnsuccessful();

                // Some code doesn't care if we are inside a non-cancellable section, or in a coroutine.
                // E.g., ComponentManagerImpl.doGetService does `throw new PCE("out-of-thin")`.
                // Handle all these PCE and CE as a valid cancellation.
                if (e instanceof ProcessCanceledException) {
                    throw e;
                }
                ProgressManager.checkCanceled();

                // CollectingIterator should skip failing files by itself. But if provider.iterateFiles cannot iterate files and throws
                // exception, we want to ignore the whole origin and let other origins complete normally.
                LOG.error("Error while scanning files of " + provider.getDebugName()
                    + ". To reindex files under this origin IDE has to be restarted", e);
            }
        }

        private void scanFiles(IndexableFilesIterator provider, ArrayDeque<VirtualFile> files) {
            PerProjectIndexingQueue indexingQueue = PerProjectIndexingQueue.getInstance(myProject);
            outerWhile:
            while (!files.isEmpty()) {
                myIndicator.suspendIfPaused();

                UnindexedFilesFinder finder = ourTestMode == TestMode.PUSHING
                    ? null
                    : new UnindexedFilesFinder(myProject, myForceReindexingTrigger, myScanningRequest);
                PushingUtil pushingUtil = new PushingUtil(myProject, provider);
                if (!pushingUtil.mayBeUsed()) {
                    LOG.warn("Iterator based on " + provider + " can't be used.");
                    return;
                }

                while (!files.isEmpty()) {
                    if (myIndicator.isPaused()) {
                        continue outerWhile;
                    }
                    ProgressManager.checkCanceled();
                    VirtualFile file = files.removeFirst();
                    try {
                        if (file.isValid()) {
                            if (file instanceof VirtualFileWithId fileWithId) {
                                myFilterHandler.addFileId(myProject, fileWithId.getId());
                            }
                            pushingUtil.applyPushers(file);
                            UnindexedFileStatus status = finder != null ? finder.getFileStatus(file) : null;
                            if (status != null) {
                                myNumberOfScannedFiles.incrementAndGet();
                                if (status.shouldIndex() && ourTestMode == null) {
                                    myNumberOfFilesForIndexing.incrementAndGet();
                                    indexingQueue.addFile(file, myScanningSessionId);
                                }
                            }
                        }
                    }
                    catch (Throwable e) {
                        if (e instanceof ControlFlowException) {
                            //Cancellation is not a failure: the read action is restarted (ReadAction.CannotReadException is a PCE), so the
                            // file must stay in the queue to be scanned again.
                            files.addFirst(file);
                            throw e;
                        }

                        // A plugin's code invoked from getFileStatus()/applyPushers() may throw an Error -- e.g. a broken
                        // index input filter throwing NoClassDefFoundError for every file. Skip the file, keep scanning the rest,
                        // and make sure the incomplete result is not mistaken for a complete one
                        myScanningRequest.markUnsuccessful();
                        LOG.error("Error while scanning " + file.getPresentableUrl() + "\n" +
                            "To reindex this file IDE has to be restarted", e);
                    }
                }
            }
        }

        private ArrayDeque<VirtualFile> getFilesToScan(
            IndexableFilesIterator provider,
            IndexableFilesDeduplicateFilter thisProviderDeduplicateFilter
        ) {
            ArrayDeque<VirtualFile> files = new ArrayDeque<>(1024);
            provider.iterateFiles(myProject, fileOrDir -> {
                ProgressManager.checkCanceled();
                files.add(fileOrDir);
                return true;
            }, thisProviderDeduplicateFilter);
            return files;
        }
    }

    void perform(
        CheckPauseOnlyProgressIndicator indicator,
        IndexingProgressReporter progressReporter,
        ScanningIterators scanningParameters
    ) {
        try {
            myFilterHandler.scanningStarted(myProject, scanningParameters.isFullIndexUpdate());
            try {
                waitForPreconditions();
                ((GistManagerImpl) GistManager.getInstance()).runWithMergingDependentCacheInvalidations(
                    () -> scanAndUpdateUnindexedFiles(indicator, progressReporter, scanningParameters)
                );
            }
            catch (Throwable e) {
                logInfo("Scanning is interrupted (scanning id=" + myScanningSessionId + "). " + e.getMessage());
                throw e;
            }
        }
        finally {
            myFilterHandler.scanningCompleted(myProject);
        }
    }

    private void waitForPreconditions() {
        myFileBasedIndex.waitUntilIndicesAreInitialized(); // wait until stale ids are deleted
        if (myStartCondition != null) { // wait until indexes for dirty files are cleared
            ProgressIndicatorUtils.awaitWithCheckCanceled(myStartCondition);
        }
        // Not sure that ensureUpToDate is really needed, but it wouldn't hurt to clear up queue not from EDT
        // It was added in this commit: 'Process vfs events asynchronously (IDEA-109525), first cut Maxim.Mossienko 13.11.16, 14:15'
        myFileBasedIndex.getChangedFilesCollector().ensureUpToDate();
    }

    @Override
    public String toString() {
        List<IndexableFilesIterator> filesIterators = tryGetPredefinedIndexableFileIterators();
        String partialInfo = filesIterators != null ? ", " + filesIterators.size() + " iterators" : "";
        return "UnindexedFilesScanner[" + myProject.getName() + partialInfo + "]";
    }

    public Future<?> queue() {
        return UnindexedFilesScannerExecutor.getInstance(myProject).submitTask(this);
    }

    @Override
    public void close() {
        if (!myProject.isDisposed()) {
            ProjectIndexingDependenciesService.getInstance(myProject).completeToken(myTaskToken);
        }
    }

    private String getLogScanningCompletedStageMessage(ScanningSession session) {
        return "Scanning completed for [" + myProject.getName() + "]. " +
            "Number of scanned files: " + session.getNumberOfScannedFiles() +
            "; number of files for indexing: " + session.getNumberOfFilesForIndexing();
    }

    private static @Nullable List<IndexableFilesIterator> mergeIterators(
        @Nullable List<IndexableFilesIterator> iterators,
        @Nullable List<IndexableFilesIterator> otherIterators
    ) {
        if (iterators == null || otherIterators == null) {
            return null;
        }
        Map<IndexableSetOrigin, IndexableFilesIterator> uniqueIterators = new LinkedHashMap<>();
        for (IndexableFilesIterator iterator : iterators) {
            uniqueIterators.putIfAbsent(iterator.getOrigin(), iterator);
        }
        for (IndexableFilesIterator iterator : otherIterators) {
            uniqueIterators.putIfAbsent(iterator.getOrigin(), iterator);
        }
        return new ArrayList<>(uniqueIterators.values());
    }

    private static List<IndexableFilesIterator> collectProviders(Project project, FileBasedIndexImpl index) {
        List<IndexableFilesIterator> originalOrderedProviders = index.getOrderedIndexableFilesProviders(project);

        List<IndexableFilesIterator> orderedProviders = new ArrayList<>();
        for (IndexableFilesIterator provider : originalOrderedProviders) {
            if (!(provider.getOrigin() instanceof SdkOrigin)) {
                orderedProviders.add(provider);
            }
        }

        for (IndexableFilesIterator provider : originalOrderedProviders) {
            if (provider.getOrigin() instanceof SdkOrigin) {
                orderedProviders.add(provider);
            }
        }

        return orderedProviders;
    }
}
