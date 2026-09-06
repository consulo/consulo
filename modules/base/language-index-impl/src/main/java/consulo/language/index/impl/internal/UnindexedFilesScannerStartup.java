// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

import consulo.application.ReadAction;
import consulo.application.progress.ProgressManager;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.util.registry.Registry;
import consulo.language.index.impl.internal.dependencies.AppIndexingDependenciesService;
import consulo.language.index.impl.internal.dependencies.AppIndexingDependenciesToken;
import consulo.language.index.impl.internal.dependencies.ProjectIndexingDependenciesService;
import consulo.language.index.impl.internal.events.ProjectDirtyFiles;
import consulo.language.index.impl.internal.projectFilter.ProjectIndexableFilesFilterHolder;
import consulo.language.index.impl.internal.roots.IndexableFilesIterator;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderUtil;
import consulo.virtualFileSystem.ManagingFS;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public final class UnindexedFilesScannerStartup {
    private static final Logger LOG = UnindexedFilesScanner.LOG;

    /** Key in [Project]: `null` before first scanning => [FirstScanningState.REQUESTED] => [FirstScanningState.PERFORMED] */
    static final Key<FirstScanningState> FIRST_SCANNING_REQUESTED = Key.create("FIRST_SCANNING_REQUESTED");

    enum FirstScanningState {
        REQUESTED,
        PERFORMED
    }

    static final ReentrantLock INITIAL_SCANNING_LOCK = new ReentrantLock();

    private static final Key<Boolean> PERSISTENT_INDEXABLE_FILES_FILTER_INVALIDATED =
        Key.create("PERSISTENT_INDEXABLE_FILES_FILTER_INVALIDATED");

    private UnindexedFilesScannerStartup() {
    }

    private static int getDumbModeThreshold() {
        return Registry.intValue("scanning.dumb.mode.threshold", 20);
    }

    static @Nullable CompletableFuture<?> scanAndIndexProjectAfterOpen(
        Project project,
        OrphanDirtyFilesQueue orphanQueue,
        @Nullable OrphanDirtyFilesQueueDiscardReason orphanQueueDiscardReason,
        Collection<Integer> additionalOrphanDirtyFiles,
        ProjectDirtyFilesQueue projectDirtyFilesQueue,
        boolean allowSkippingFullScanning,
        boolean requireReadingIndexableFilesIndexFromDisk,
        String indexingReason,
        ScanningType fullScanningType,
        ScanningType partialScanningType
    ) {
        FileBasedIndexImpl fileBasedIndex = (FileBasedIndexImpl) FileBasedIndex.getInstance();
        boolean isFilterInvalidated;
        INITIAL_SCANNING_LOCK.lock();
        try {
            UserDataHolderUtil.computeIfAbsent(project, FIRST_SCANNING_REQUESTED, () -> FirstScanningState.REQUESTED);
            isFilterInvalidated = project.getUserData(PERSISTENT_INDEXABLE_FILES_FILTER_INVALIDATED) == Boolean.TRUE;
        }
        finally {
            INITIAL_SCANNING_LOCK.unlock();
        }

        ProjectIndexableFilesFilterHolder filterHolder = fileBasedIndex.getIndexableFilesFilterHolder();
        AppIndexingDependenciesToken appCurrent = AppIndexingDependenciesService.getInstance().getCurrent();
        FilterCheckState filterCheckState = new FilterCheckState(project, filterHolder, isFilterInvalidated, appCurrent);
        List<ReusingPersistentFilterCondition> filterUpToDateUnsatisfiedConditions = ReadAction.compute(() -> project.isDisposed()
            ? null
            : findFilterUpToDateUnsatisfiedConditions(filterCheckState, requireReadingIndexableFilesIndexFromDisk));
        if (filterUpToDateUnsatisfiedConditions == null) {
            return null;
        }

        GetNotSeenDirtyFileIdsResult notSeenIds = getNotSeenIds(orphanQueue, project, projectDirtyFilesQueue, orphanQueueDiscardReason);
        SkippingScanningCheckState scanningCheckState =
            new SkippingScanningCheckState(allowSkippingFullScanning, filterUpToDateUnsatisfiedConditions, notSeenIds);
        List<SkippingFullScanningCondition> skippingScanningUnsatisfiedConditions = new ArrayList<>();
        for (SkippingFullScanningCondition condition : SkippingFullScanningCondition.values()) {
            if (!condition.canSkipFullScanning(scanningCheckState)) {
                skippingScanningUnsatisfiedConditions.add(condition);
            }
        }

        if (skippingScanningUnsatisfiedConditions.isEmpty()) {
            LOG.info("Full scanning on startup will be skipped for project [" + project.getName() + "]");
            List<Integer> allNotSeenIds =
                plus(((AllNotSeenDirtyFileIds) notSeenIds).getResult(), additionalOrphanDirtyFiles);
            return scheduleDirtyFilesScanning(project, allNotSeenIds, projectDirtyFilesQueue, indexingReason, partialScanningType);
        }
        else {
            LOG.info("Full scanning on startup will NOT be skipped for project [" + project.getName() + "] " +
                "because of following unsatisfied conditions:\n" +
                skippingScanningUnsatisfiedConditions.stream()
                    .map(condition -> condition.name() + ": " + condition.explain(scanningCheckState))
                    .collect(Collectors.joining("\n")));
            return scheduleFullScanning(project, notSeenIds, additionalOrphanDirtyFiles, projectDirtyFilesQueue,
                filterUpToDateUnsatisfiedConditions.isEmpty(), indexingReason, fullScanningType);
        }
    }

    public static boolean isFirstProjectScanningRequested(Project project) {
        return project.getUserData(FIRST_SCANNING_REQUESTED) != null;
    }

    public static boolean isFirstProjectScanningPerformed(Project project) {
        return project.getUserData(FIRST_SCANNING_REQUESTED) == FirstScanningState.PERFORMED;
    }

    static boolean invalidateProjectFilterIfFirstScanningNotRequested(Project project) {
        INITIAL_SCANNING_LOCK.lock();
        try {
            if (isFirstProjectScanningRequested(project)) {
                return false;
            }
            else {
                LOG.info("First scanning is not yet requested (project=" + project.getName() + "), " +
                    "current scanning request will be ignored and full scanning on startup will be performed instead.");
                setProjectFilterIsInvalidated(project, true);
                return true;
            }
        }
        finally {
            INITIAL_SCANNING_LOCK.unlock();
        }
    }

    static void setProjectFilterIsInvalidated(Project project, boolean invalid) {
        project.putUserData(PERSISTENT_INDEXABLE_FILES_FILTER_INVALIDATED, invalid ? Boolean.TRUE : null);
    }

    static void forgetProjectDirtyFilesOnCompletion(
        CompletableFuture<?> job,
        FileBasedIndexImpl fileBasedIndex,
        Project project,
        ProjectDirtyFilesQueue projectDirtyFilesQueue,
        long orphanQueueUntrimmedSize
    ) {
        job.whenComplete((result, e) -> {
            if (e != null) {
                return;
            }
            ProjectDirtyFiles projectDirtyFiles = fileBasedIndex.getDirtyFiles().getProjectDirtyFiles(project);
            if (projectDirtyFiles != null) {
                projectDirtyFiles.removeFiles(projectDirtyFilesQueue.getFileIds());
            }
            fileBasedIndex.setLastSeenIndexInOrphanQueue(project, orphanQueueUntrimmedSize);
        });
    }

    private static CompletableFuture<?> scheduleFullScanning(
        Project project,
        GetNotSeenDirtyFileIdsResult notSeenIds,
        Collection<Integer> additionalOrphanDirtyFiles,
        ProjectDirtyFilesQueue projectDirtyFilesQueue,
        boolean isFilterUpToDate,
        String indexingReason,
        ScanningType fullScanningType
    ) {
        CompletableFuture<?> someDirtyFilesScheduledForIndexing;
        if (notSeenIds instanceof AllNotSeenDirtyFileIds allNotSeenDirtyFileIds) {
            List<Integer> ids = plus(allNotSeenDirtyFileIds.getResult(), additionalOrphanDirtyFiles);
            someDirtyFilesScheduledForIndexing = CompletableFuture.supplyAsync(
                () -> clearIndexesForDirtyFiles(project, ids, projectDirtyFilesQueue, false),
                AppExecutorUtil.getAppExecutorService()
            );
        }
        else {
            someDirtyFilesScheduledForIndexing = CompletableFuture.completedFuture(null);
        }
        CompletableFuture<ScanningParameters> parameters =
            CompletableFuture.completedFuture(new ScanningIterators(indexingReason, null, fullScanningType));
        ReadAction.run(() -> {
            if (!project.isDisposed()) {
                new UnindexedFilesScanner(
                    project,
                    true,
                    isFilterUpToDate,
                    someDirtyFilesScheduledForIndexing,
                    null,
                    null,
                    !(notSeenIds instanceof AllNotSeenDirtyFileIds),
                    parameters
                ).queue();
            }
        });
        return someDirtyFilesScheduledForIndexing;
    }

    private static boolean isShutdownPerformedForFileBasedIndex(FileBasedIndexImpl fileBasedIndex) {
        RegisteredIndexes registeredIndexes = fileBasedIndex.getRegisteredIndexes();
        return registeredIndexes == null || registeredIndexes.isShutdownPerformed();
    }

    private static CompletableFuture<?> scheduleDirtyFilesScanning(
        Project project,
        List<Integer> allNotSeenIds,
        ProjectDirtyFilesQueue projectDirtyFilesQueue,
        String indexingReason,
        ScanningType partialScanningType
    ) {
        CompletableFuture<ResultOfClearIndexesForDirtyFiles> projectDirtyFiles = CompletableFuture.supplyAsync(
            () -> clearIndexesForDirtyFiles(project, allNotSeenIds, projectDirtyFilesQueue, true),
            AppExecutorUtil.getAppExecutorService()
        );
        CompletableFuture<List<VirtualFile>> projectDirtyFilesFromProjectQueue =
            projectDirtyFiles.thenApply(result -> result == null ? List.<VirtualFile>of() : result.getProjectDirtyFilesFromProjectQueue());
        CompletableFuture<List<VirtualFile>> projectDirtyFilesFromOrphanQueue =
            projectDirtyFiles.thenApply(result -> result == null ? List.<VirtualFile>of() : result.getProjectDirtyFilesFromOrphanQueue());
        List<IndexableFilesIterator> iterators = List.of(
            new DirtyFilesIndexableFilesIterator(projectDirtyFilesFromProjectQueue, false),
            new DirtyFilesIndexableFilesIterator(projectDirtyFilesFromOrphanQueue, true)
        );

        CompletableFuture<ScanningParameters> scanningIterators =
            CompletableFuture.completedFuture(new ScanningIterators(indexingReason, iterators, partialScanningType));
        ReadAction.run(() -> {
            if (!project.isDisposed()) {
                new UnindexedFilesScanner(project, true, true, projectDirtyFiles, null, null, false, scanningIterators).queue();
            }
        });
        return projectDirtyFiles;
    }

    private static @Nullable ResultOfClearIndexesForDirtyFiles clearIndexesForDirtyFiles(
        Project project,
        Collection<Integer> notSeenIds,
        ProjectDirtyFilesQueue projectDirtyFilesQueue,
        boolean findAllVirtualFiles
    ) {
        FileBasedIndexImpl fileBasedIndex = (FileBasedIndexImpl) FileBasedIndex.getInstance();
        if (isShutdownPerformedForFileBasedIndex(fileBasedIndex)) {
            return null;
        }

        List<VirtualFile> projectDirtyFilesFromOrphanQueue = findProjectFiles(project, notSeenIds, -1);
        List<Integer> allProjectDirtyFileIds = new ArrayList<>(projectDirtyFilesQueue.getFileIds());
        for (VirtualFile file : projectDirtyFilesFromOrphanQueue) {
            if (file instanceof VirtualFileWithId fileWithId) {
                allProjectDirtyFileIds.add(fileWithId.getId());
            }
        }
        fileBasedIndex.ensureDirtyFileIndexesDeleted(allProjectDirtyFileIds);

        int vfToFindLimit = findAllVirtualFiles
            ? -1
            : Math.max(0, getDumbModeThreshold() - projectDirtyFilesFromOrphanQueue.size() - 1);

        List<VirtualFile> projectDirtyFilesFromProjectQueue =
            findProjectFiles(project, projectDirtyFilesQueue.getFileIds(), vfToFindLimit);
        List<VirtualFile> projectDirtyFiles = new ArrayList<>(projectDirtyFilesFromProjectQueue);
        projectDirtyFiles.addAll(projectDirtyFilesFromOrphanQueue);
        scheduleForIndexing(projectDirtyFiles, project, fileBasedIndex, getDumbModeThreshold() - 1);
        return new ResultOfClearIndexesForDirtyFiles(projectDirtyFilesFromProjectQueue, projectDirtyFilesFromOrphanQueue);
    }

    private static GetNotSeenDirtyFileIdsResult getNotSeenIds(
        OrphanDirtyFilesQueue orphanQueue,
        Project project,
        ProjectDirtyFilesQueue projectQueue,
        @Nullable OrphanDirtyFilesQueueDiscardReason orphanQueueDiscardReason
    ) {
        if (projectQueue.getLastSeenIndexInOrphanQueue() > orphanQueue.getUntrimmedSize()) {
            LOG.error("It should not happen that project has seen file id in orphan queue at index larger than number of files that " +
                "orphan queue ever had. " +
                "projectQueue.lastSeenIdsInOrphanQueue=" + projectQueue.getLastSeenIndexInOrphanQueue() +
                ", orphanQueue.untrimmedSize=" + orphanQueue.getUntrimmedSize() + ", " +
                "orphanQueue.fileIds.size=" + orphanQueue.getFileIds().size() + ", project=" + project + ", " +
                "orphanQueueDiscardReason=" + orphanQueueDiscardReason);
            return ProjectDirtyFilesQueuePointsToIncorrectPosition.INSTANCE;
        }

        long untrimmedIndexOfFirstElementInOrphanQueue = orphanQueue.getUntrimmedSize() - orphanQueue.getFileIds().size();
        int trimmedIndexOfFirstUnseenElement =
            (int) (projectQueue.getLastSeenIndexInOrphanQueue() - untrimmedIndexOfFirstElementInOrphanQueue);
        if (trimmedIndexOfFirstUnseenElement < 0) {
            return new DirtyFileIdsWereMissed(orphanQueue, projectQueue);
        }
        return new AllNotSeenDirtyFileIds(
            orphanQueue.getFileIds().subList(trimmedIndexOfFirstUnseenElement, orphanQueue.getFileIds().size()));
    }

    private sealed interface GetNotSeenDirtyFileIdsResult
        permits ProjectDirtyFilesQueuePointsToIncorrectPosition, DirtyFileIdsWereMissed, AllNotSeenDirtyFileIds {
        String explain();
    }

    private static final class ProjectDirtyFilesQueuePointsToIncorrectPosition implements GetNotSeenDirtyFileIdsResult {
        private static final ProjectDirtyFilesQueuePointsToIncorrectPosition INSTANCE =
            new ProjectDirtyFilesQueuePointsToIncorrectPosition();

        @Override
        public String explain() {
            return "Project dirty files queue points to an index in orphan queue at index larger than number of files that orphan " +
                "queue ever had";
        }
    }

    private static final class DirtyFileIdsWereMissed implements GetNotSeenDirtyFileIdsResult {
        private final OrphanDirtyFilesQueue myOrphanDirtyFilesQueue;
        private final ProjectDirtyFilesQueue myProjectQueue;

        DirtyFileIdsWereMissed(OrphanDirtyFilesQueue orphanDirtyFilesQueue, ProjectDirtyFilesQueue projectQueue) {
            myOrphanDirtyFilesQueue = orphanDirtyFilesQueue;
            myProjectQueue = projectQueue;
        }

        @Override
        public String explain() {
            return "There are file ids that project missed: orphanQueue.untrimmedSize=" + myOrphanDirtyFilesQueue.getUntrimmedSize() +
                ", orphanQueue.fileIds.size=" + myOrphanDirtyFilesQueue.getFileIds().size() +
                ", projectQueue.lastSeenIndexInOrphanQueue=" + myProjectQueue.getLastSeenIndexInOrphanQueue();
        }
    }

    private static final class AllNotSeenDirtyFileIds implements GetNotSeenDirtyFileIdsResult {
        private final Collection<Integer> myResult;

        AllNotSeenDirtyFileIds(Collection<Integer> result) {
            myResult = result;
        }

        Collection<Integer> getResult() {
            return myResult;
        }

        @Override
        public String explain() {
            return "All not seen ids are known: " + myResult;
        }
    }

    private static List<VirtualFile> findProjectFiles(Project project, Collection<Integer> dirtyFilesIds, int limit) {
        ManagingFS fs = ManagingFS.getInstance();
        FileBasedIndexImpl fileBasedIndex = (FileBasedIndexImpl) FileBasedIndex.getInstance();
        boolean exceptionLogged = false;
        List<VirtualFile> projectFiles = new ArrayList<>();
        Collection<Integer> idsToProcess = limit <= 0 ? dirtyFilesIds : take(dirtyFilesIds, limit);
        for (int fileId : idsToProcess) {
            try {
                VirtualFile file = fs.findFileById(fileId);
                // Blocking read action because the lambda is fast and the number of files can be large.
                // And the previous solution was de-facto blocking because somebody (me) forgot checkCancelled.
                Boolean inProject = file == null
                    ? Boolean.FALSE
                    : ReadAction.compute(() -> fileBasedIndex.belongsToProjectIndexableFiles(file, project));
                if (inProject) {
                    projectFiles.add(file);
                }
            }
            catch (AssertionError e) {
                if (!exceptionLogged) {
                    LOG.debug("VfsRootAccessNotAllowedError occurred. " +
                        "Probably previous test with different rules for project roots saved these files to dirty files queue. " +
                        "Example of error:", e);
                    exceptionLogged = true;
                }
            }
        }
        return projectFiles;
    }

    private static void scheduleForIndexing(
        List<VirtualFile> someProjectDirtyFilesFiles,
        Project project,
        FileBasedIndexImpl fileBasedIndex,
        int limit
    ) {
        ReadAction.run(() -> {
            List<VirtualFile> files = limit > 0 ? take(someProjectDirtyFilesFiles, limit) : someProjectDirtyFilesFiles;
            for (VirtualFile file : files) {
                if (file instanceof VirtualFileWithId fileWithId) {
                    ProgressManager.getInstance().executeNonCancelableSection(
                        () -> fileBasedIndex.scheduleFileForIncrementalIndexing(fileWithId.getId(), file, false, List.of(project)));
                }
            }
        });
    }

    private static List<ReusingPersistentFilterCondition> findFilterUpToDateUnsatisfiedConditions(
        FilterCheckState state,
        boolean requireReadingIndexableFilesIndexFromDisk
    ) {
        List<ReusingPersistentFilterCondition> conditions = new ArrayList<>();
        for (ReusingPersistentFilterCondition condition : ReusingPersistentFilterCondition.values()) {
            if (condition.isUpToDate(state)) {
                continue;
            }
            if (!requireReadingIndexableFilesIndexFromDisk && condition == ReusingPersistentFilterCondition.IS_FILTER_LOADED_FROM_DISK) {
                continue;
            }
            conditions.add(condition);
        }
        return conditions;
    }

    private static final class FilterCheckState {
        private final Project myProject;
        private final ProjectIndexableFilesFilterHolder myFilterHolder;
        private final boolean myIsFilterInvalidated;
        private final AppIndexingDependenciesToken myAppCurrent;

        FilterCheckState(
            Project project,
            ProjectIndexableFilesFilterHolder filterHolder,
            boolean isFilterInvalidated,
            AppIndexingDependenciesToken appCurrent
        ) {
            myProject = project;
            myFilterHolder = filterHolder;
            myIsFilterInvalidated = isFilterInvalidated;
            myAppCurrent = appCurrent;
        }
    }

    private enum ReusingPersistentFilterCondition {
        IS_PERSISTENT_FILTER_ENABLED {
            @Override
            boolean isUpToDate(FilterCheckState state) {
                return ProjectIndexableFilesFilterHolder.usePersistentFilesFilter();
            }
        },
        IS_FILTER_LOADED_FROM_DISK {
            @Override
            boolean isUpToDate(FilterCheckState state) {
                return state.myFilterHolder.wasDataLoadedFromDisk(state.myProject);
            }
        },
        IS_SCANNING_AND_INDEXING_COMPLETED {
            @Override
            boolean isUpToDate(FilterCheckState state) {
                return ProjectIndexingDependenciesService.getInstance(state.myProject).isScanningAndIndexingCompleted();
            }
        },
        FILTER_IS_NOT_INVALIDATED {
            @Override
            boolean isUpToDate(FilterCheckState state) {
                return !state.myIsFilterInvalidated;
            }
        },
        INDEXING_REQUEST_ID_DID_NOT_CHANGE_AFTER_LAST_SCANNING {
            @Override
            boolean isUpToDate(FilterCheckState state) {
                ProjectIndexingDependenciesService projectService = ProjectIndexingDependenciesService.getInstance(state.myProject);
                return state.myAppCurrent.toInt() == projectService.getAppIndexingRequestIdOfLastScanning();
            }
        };

        abstract boolean isUpToDate(FilterCheckState state);
    }

    private static final class SkippingScanningCheckState {
        private final boolean myAllowSkippingFullScanning;
        private final List<ReusingPersistentFilterCondition> myFilterUpToDateUnsatisfiedConditions;
        private final GetNotSeenDirtyFileIdsResult myNotSeenIds;

        SkippingScanningCheckState(
            boolean allowSkippingFullScanning,
            List<ReusingPersistentFilterCondition> filterUpToDateUnsatisfiedConditions,
            GetNotSeenDirtyFileIdsResult notSeenIds
        ) {
            myAllowSkippingFullScanning = allowSkippingFullScanning;
            myFilterUpToDateUnsatisfiedConditions = filterUpToDateUnsatisfiedConditions;
            myNotSeenIds = notSeenIds;
        }
    }

    private enum SkippingFullScanningCondition {
        ALLOWED {
            @Override
            boolean canSkipFullScanning(SkippingScanningCheckState state) {
                return state.myAllowSkippingFullScanning;
            }

            @Override
            String explain(SkippingScanningCheckState state) {
                return "Full scanning was requested";
            }
        },
        FILTER_IS_NOT_UP_TO_DATE {
            @Override
            boolean canSkipFullScanning(SkippingScanningCheckState state) {
                return state.myFilterUpToDateUnsatisfiedConditions.isEmpty();
            }

            @Override
            String explain(SkippingScanningCheckState state) {
                return "Persistent indexable files filter is NOT up-to-date because of following unsatisfied conditions: " +
                    state.myFilterUpToDateUnsatisfiedConditions;
            }
        },
        REGISTRY_FILE_IS_ON {
            @Override
            boolean canSkipFullScanning(SkippingScanningCheckState state) {
                return Registry.is("full.scanning.on.startup.can.be.skipped");
            }

            @Override
            String explain(SkippingScanningCheckState state) {
                return "Registry flag 'full.scanning.on.startup.can.be.skipped' is turned off";
            }
        },
        DIRTY_FILE_IDS_WERE_MISSED {
            @Override
            boolean canSkipFullScanning(SkippingScanningCheckState state) {
                return state.myNotSeenIds instanceof AllNotSeenDirtyFileIds;
            }

            @Override
            String explain(SkippingScanningCheckState state) {
                return state.myNotSeenIds.explain();
            }
        };

        abstract boolean canSkipFullScanning(SkippingScanningCheckState state);

        abstract String explain(SkippingScanningCheckState state);
    }

    private static final class ResultOfClearIndexesForDirtyFiles {
        private final List<VirtualFile> myProjectDirtyFilesFromProjectQueue;
        private final List<VirtualFile> myProjectDirtyFilesFromOrphanQueue;

        ResultOfClearIndexesForDirtyFiles(
            List<VirtualFile> projectDirtyFilesFromProjectQueue,
            List<VirtualFile> projectDirtyFilesFromOrphanQueue
        ) {
            myProjectDirtyFilesFromProjectQueue = projectDirtyFilesFromProjectQueue;
            myProjectDirtyFilesFromOrphanQueue = projectDirtyFilesFromOrphanQueue;
        }

        List<VirtualFile> getProjectDirtyFilesFromProjectQueue() {
            return myProjectDirtyFilesFromProjectQueue;
        }

        List<VirtualFile> getProjectDirtyFilesFromOrphanQueue() {
            return myProjectDirtyFilesFromOrphanQueue;
        }
    }

    private static List<Integer> plus(Collection<Integer> collection, Collection<Integer> other) {
        List<Integer> result = new ArrayList<>(collection);
        result.addAll(other);
        return result;
    }

    private static <T> List<T> take(Collection<T> collection, int limit) {
        List<T> result = new ArrayList<>(Math.min(limit, collection.size()));
        for (T element : collection) {
            if (result.size() >= limit) {
                break;
            }
            result.add(element);
        }
        return result;
    }
}
