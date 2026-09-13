// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.AccessRule;
import consulo.application.ReadAction;
import consulo.application.Application;
import consulo.application.HeavyProcessLatch;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.event.ApplicationListener;
import consulo.application.impl.internal.start.StartupUtil;
import consulo.application.internal.NoAccessDuringPsiEventsService;
import consulo.application.internal.ProgressIndicatorUtils;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.NotNullLazyValue;
import consulo.application.util.function.Processors;
import consulo.application.util.function.ThrowableComputable;
import consulo.application.util.registry.Registry;
import consulo.component.ProcessCanceledException;
import consulo.component.messagebus.MessageBusConnection;
import consulo.container.boot.ContainerPathManager;
import consulo.content.CollectingContentIterator;
import consulo.content.ContentIterator;
import consulo.content.scope.SearchScope;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.event.FileDocumentManagerListener;
import consulo.index.io.*;
import consulo.index.io.data.DataOutputStream;
import consulo.index.io.data.IOUtil;
import consulo.language.ast.ASTNode;
import consulo.language.file.FileTypeManager;
import consulo.language.impl.internal.psi.PsiDocumentTransactionListener;
import consulo.language.impl.internal.psi.PsiManagerImpl;
import consulo.language.impl.internal.psi.PsiTreeChangeEventImpl;
import consulo.language.impl.internal.psi.stub.FileContentImpl;
import consulo.language.impl.internal.psi.stub.IndexedFileImpl;
import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.index.impl.internal.dependencies.FileIndexingStamp;
import consulo.language.index.impl.internal.events.DirtyFiles;
import consulo.language.index.impl.internal.events.FileIndexingRequest;
import consulo.language.index.impl.internal.events.FilesToUpdateCollector;
import consulo.language.index.impl.internal.events.ProjectDirtyFiles;
import consulo.language.index.impl.internal.dependencies.IndexingRequestToken;
import consulo.language.index.impl.internal.dependencies.IsFileChangedResult;
import consulo.language.index.impl.internal.dependencies.ProjectIndexingDependenciesService;
import consulo.language.index.impl.internal.localize.IndexingLocalize;
import consulo.language.index.impl.internal.moduleAware.ModuleAwareIndexMetaRecorder;
import consulo.language.index.impl.internal.moduleAware.ModuleAwareIndexMetaStorage;
import consulo.language.index.impl.internal.moduleAware.ModuleAwareIndexOptionValueStorage;
import consulo.language.index.impl.internal.projectFilter.IncrementalProjectIndexableFilesFilterHolder;
import consulo.language.index.impl.internal.projectFilter.ProjectIndexableFilesFilterHolder;
import consulo.language.internal.FileTypeManagerEx;
import consulo.language.internal.LanguageInternal;
import consulo.language.internal.SerializationManagerEx;
import consulo.language.internal.SubstitutedFileType;
import consulo.language.internal.psi.stub.IdIndex;
import consulo.language.psi.search.FileTypeIndex;
import consulo.language.index.impl.internal.stub.StubUpdatingIndex;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.EverythingGlobalScope;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.*;
import consulo.language.index.impl.internal.roots.IndexableFilesContributor;
import consulo.language.index.impl.internal.roots.IndexableFilesDeduplicateFilter;
import consulo.language.index.impl.internal.roots.IndexableFilesIterator;
import consulo.language.psi.stub.gist.GistManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.*;
import consulo.project.content.scope.ProjectAwareSearchScope;
import consulo.project.internal.DumbInternalUtil;
import consulo.project.internal.DumbServiceInternal;
import consulo.project.internal.SingleProjectHolder;
import consulo.project.ui.notification.NotificationDisplayType;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationService;
import consulo.util.collection.*;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import consulo.util.concurrent.ConcurrencyUtil;
import consulo.util.dataholder.Key;
import consulo.util.io.FileUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.ShutDownTracker;
import consulo.util.lang.StringUtil;
import consulo.util.lang.function.ThrowableFunction;
import consulo.util.lang.function.ThrowableSupplier;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.*;
import consulo.virtualFileSystem.event.AsyncFileListener;
import consulo.virtualFileSystem.fileType.FileNameMatcher;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeEvent;
import consulo.virtualFileSystem.fileType.FileTypeListener;
import consulo.virtualFileSystem.internal.FlushingDaemon;
import consulo.virtualFileSystem.internal.PersistentFS;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;
import org.jspecify.annotations.Nullable;

import java.io.*;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.util.Stack;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @author Eugene Zhuravlev
 */
@Singleton
@ServiceImpl
public final class FileBasedIndexImpl extends FileBasedIndex {
    static final NotificationGroup NOTIFICATIONS =
        new NotificationGroup("ideCaches", IndexingLocalize.notificationGroupIdeCaches(), NotificationDisplayType.BALLOON, false);

    private static final ThreadLocal<VirtualFile> ourIndexedFile = new ThreadLocal<>();
    private static final ThreadLocal<VirtualFile> ourFileToBeIndexed = new ThreadLocal<>();
    static final Logger LOG = Logger.getInstance(FileBasedIndexImpl.class);
    private static final ThreadLocal<Stack<DumbModeAccessType>> ourDumbModeAccessTypeStack = ThreadLocal.withInitial(Stack::new);

    private final Application myApplication;
    
    private final NotificationService myNotificationService;
    private final List<ID<?, ?>> myIndicesForDirectories = new SmartList<>();

    private final Map<ID<?, ?>, DocumentUpdateTask> myUnsavedDataUpdateTasks = new ConcurrentHashMap<>();

    private final Set<ID<?, ?>> myNotRequiringContentIndices = new HashSet<>();
    final Set<ID<?, ?>> myRequiringContentIndices = new HashSet<>();
    private final Set<ID<?, ?>> myPsiDependentIndices = new HashSet<>();
    private final Set<FileType> myNoLimitCheckTypes = new HashSet<>();

    private volatile boolean myExtensionsRelatedDataWasLoaded;

    private final PerIndexDocumentVersionMap myLastIndexedDocStamps = new PerIndexDocumentVersionMap();

    private final NotNullLazyValue<ChangedFilesCollector> myChangedFilesCollector =
        NotNullLazyValue.createValue(() -> {
            Application application = Application.get();
            return application.getExtensionPoint(AsyncFileListener.class).findExtensionOrFail(ChangedFilesCollector.class);
        });

    List<IndexableFileSet> myIndexableSets = Lists.newLockFreeCopyOnWriteList();
    private final Map<IndexableFileSet, Project> myIndexableSetToProjectMap = new HashMap<>();

    private final ProjectIndexableFilesFilterHolder myIndexableFilesFilterHolder = new IncrementalProjectIndexableFilesFilterHolder();

    private final MessageBusConnection myConnection;
    private final FileDocumentManager myFileDocumentManager;

    private final Set<ID<?, ?>> myUpToDateIndicesForUnsavedOrTransactedDocuments = ContainerUtil.newConcurrentSet();
    private volatile SmartFMap<Document, PsiFile> myTransactionMap = SmartFMap.emptyMap();

    final boolean myIsUnitTestMode;
    private @Nullable ScheduledFuture<?> myFlushingFuture;
    private final AtomicInteger myLocalModCount = new AtomicInteger();
    private final IndexAccessValidator myAccessValidator = new IndexAccessValidator();

    private volatile @Nullable RegisteredIndexes myRegisteredIndexes;
    private volatile IndexConfiguration myState;
    private volatile @Nullable RequiredIndexesEvaluator myRequiredIndexesEvaluator;

    private final FilesToUpdateCollector myFilesToUpdateCollector = new FilesToUpdateCollector();

    /**
     * Dirty file ids that are not tracked by {@link ChangedFilesCollector} nor by {@link FilesToUpdateCollector}:
     * ids of files removed while their project was closed, and ids restored from the previous session queues.
     */
    private final DirtyFiles myDirtyFiles = new DirtyFiles();
    private final Map<Project, SimpleReference<Long>> myLastSeenIndexesInOrphanQueue = new ConcurrentHashMap<>();
    private final IntSet myStaleIds = IntSets.synchronize(new IntOpenHashSet());

    /**
     * VFS creation time captured on indexes loading. It is used to identify the VFS epoch of the dirty files queue: at the moment
     * when we write the queue, VFS might have already been disposed via shutdown hook (in the case on emergency shutdown)
     */
    private volatile long myVfsCreationStamp;

    private IndexConfiguration getState() {
        RegisteredIndexes registeredIndexes = myRegisteredIndexes;
        if (registeredIndexes == null || !registeredIndexes.isInitialized()) {
            //throw new IndexNotReadyException();
            LOG.error("Unexpected initialization problem");
        }

        IndexConfiguration state = myState; // memory barrier
        if (state == null) {
            myState = state = requireRegisteredIndexes().getConfigurationState();
        }
        return state;
    }

    private RegisteredIndexes requireRegisteredIndexes() {
        RegisteredIndexes registeredIndexes = myRegisteredIndexes;
        if (registeredIndexes == null) {
            throw new IllegalStateException("Indexes are not loaded yet");
        }
        return registeredIndexes;
    }

    public @Nullable RegisteredIndexes getRegisteredIndexes() {
        return myRegisteredIndexes;
    }

    public FilesToUpdateCollector getFilesToUpdateCollector() {
        return myFilesToUpdateCollector;
    }

    DirtyFiles getDirtyFiles() {
        return myDirtyFiles;
    }

    long getVfsCreationStamp() {
        return myVfsCreationStamp;
    }

    void setVfsCreationStamp(long vfsCreationStamp) {
        myVfsCreationStamp = vfsCreationStamp;
    }

    Application getApplication() {
        return myApplication;
    }

    NotificationService getNotificationService() {
        return myNotificationService;
    }

    void clearUpToDateIndexesForUnsavedOrTransactedDocs() {
        myUpToDateIndicesForUnsavedOrTransactedDocuments.clear();
    }

    void extensionsDataWasLoaded() {
        myExtensionsRelatedDataWasLoaded = true;
    }

    void setInitializedState(IndexConfiguration state) {
        myState = state; // memory barrier
        myRequiredIndexesEvaluator = new RequiredIndexesEvaluator(state, myIndicesForDirectories);
    }

    void registerIndexExtension(FileBasedIndexExtension<?, ?> extension) {
        ID<?, ?> name = extension.getName();

        myUnsavedDataUpdateTasks.put(name, new DocumentUpdateTask(name));

        if (!extension.dependsOnFileContent()) {
            if (extension.indexDirectories()) {
                myIndicesForDirectories.add(name);
            }
            myNotRequiringContentIndices.add(name);
        }
        else {
            myRequiringContentIndices.add(name);
        }

        if (isPsiDependentIndex(extension)) {
            myPsiDependentIndices.add(name);
        }
        myNoLimitCheckTypes.addAll(extension.getFileTypesWithSizeLimitNotApplicable());
    }

    void addStaleIds(IntSet staleIds) {
        synchronized (myStaleIds) {
            myStaleIds.addAll(staleIds);
        }
    }

    void ensureStaleIdsDeleted() {
        synchronized (myStaleIds) {
            if (myStaleIds.isEmpty()) {
                return;
            }
            try {
                StaleIndexesChecker.clearStaleIndexes(myStaleIds);
            }
            catch (Exception e) {
                LOG.error(e);
            }
            finally {
                myStaleIds.clear();
            }
        }
    }

    void ensureDirtyFileIndexesDeleted(Collection<Integer> dirtyFiles) {
        if (dirtyFiles.isEmpty()) {
            return;
        }
        ProgressManager.getInstance().executeNonCancelableSection(() -> {
            Collection<ID<?, ?>> indexIDs = requireRegisteredIndexes().getConfigurationState().getIndexIDs();
            for (int fileId : dirtyFiles) {
                removeFileDataFromIndices(indexIDs, fileId, null);
            }
        });
    }

    void registerProject(Project project, Collection<Integer> projectDirtyFileIdsFromLastSession) {
        getChangedFilesCollector().getDirtyFiles().addProject(project);
        myFilesToUpdateCollector.registerProject(project);
        myLastSeenIndexesInOrphanQueue.put(project, SimpleReference.create());
        // don't lose these ids if the project is closed before indexes are removed
        myDirtyFiles.addProject(project).addFiles(projectDirtyFileIdsFromLastSession);
        myIndexableFilesFilterHolder.onProjectOpened(project, myVfsCreationStamp);
    }

    public void onProjectClosing(Project project) {
        removeProjectFileSets(project);

        SimpleReference<Long> lastSeenIndex = myLastSeenIndexesInOrphanQueue.remove(project);
        persistDirtyFiles(project, lastSeenIndex == null || lastSeenIndex.isNull() ? 0 : lastSeenIndex.get());

        myFilesToUpdateCollector.unregisterProject(project);
        getChangedFilesCollector().getDirtyFiles().removeProject(project);
        myDirtyFiles.removeProject(project);

        myIndexableFilesFilterHolder.onProjectClosing(project, myVfsCreationStamp);
    }

    void setLastSeenIndexInOrphanQueue(Project project, long index) {
        SimpleReference<Long> ref = myLastSeenIndexesInOrphanQueue.get(project);
        if (ref != null) {
            ref.set(index);
        }
    }

    private void persistDirtyFiles(Project project, long lastSeenIndex) {
        IntSet dirtyFileIds = getAllDirtyFiles(project);
        new ProjectDirtyFilesQueue(dirtyFileIds, lastSeenIndex).store(project, myVfsCreationStamp);
    }

    public IntSet getAllDirtyFiles(@Nullable Project project) {
        IntSet dirtyFileIds = new IntOpenHashSet();
        collectDirtyFiles(getChangedFilesCollector().getDirtyFiles(), project, dirtyFileIds);
        collectDirtyFiles(myFilesToUpdateCollector.getDirtyFiles(), project, dirtyFileIds);
        collectDirtyFiles(myDirtyFiles, project, dirtyFileIds);
        return dirtyFileIds;
    }

    private static void collectDirtyFiles(
        DirtyFiles dirtyFiles,
        @Nullable Project project,
        IntSet dirtyFileIds
    ) {
        ProjectDirtyFiles dirtyFilesSet = dirtyFiles.getProjectDirtyFiles(project);
        if (dirtyFilesSet != null) {
            dirtyFilesSet.addAllTo(dirtyFileIds);
        }
    }

    @Inject
    public FileBasedIndexImpl(Application application, NotificationService notificationService) {
        myApplication = application;
        myNotificationService = notificationService;
        myFileDocumentManager = FileDocumentManager.getInstance();
        myIsUnitTestMode = application.isUnitTestMode();

        MessageBusConnection connection = application.getMessageBus().connect();
        connection.subscribe(PsiDocumentTransactionListener.class, new PsiDocumentTransactionListener() {
            @Override
            public void transactionStarted(Document doc, PsiFile file) {
                myTransactionMap = myTransactionMap.plus(doc, file);
                myUpToDateIndicesForUnsavedOrTransactedDocuments.clear();
            }

            @Override
            public void transactionCompleted(Document doc, PsiFile file) {
                myTransactionMap = myTransactionMap.minus(doc);
            }
        });

        connection.subscribe(FileTypeListener.class, new FileTypeListener() {
            private @Nullable Map<FileType, Set<String>> myTypeToExtensionMap;

            @Override
            public void beforeFileTypesChanged(FileTypeEvent event) {
                cleanupProcessedFlag("File types changed");
                myTypeToExtensionMap = new HashMap<>();
                FileTypeManager fileTypeManager = FileTypeManager.getInstance();
                for (FileType type : fileTypeManager.getRegisteredFileTypes()) {
                    myTypeToExtensionMap.put(type, getExtensions(type, fileTypeManager));
                }
            }

            @Override
            public void fileTypesChanged(FileTypeEvent event) {
                resetHints();
                Map<FileType, Set<String>> oldTypeToExtensionsMap = myTypeToExtensionMap;
                myTypeToExtensionMap = null;

                // file type added
                if (event.getAddedFileType() != null) {
                    rebuildAllIndices("The following file type was added: " + event.getAddedFileType());
                    return;
                }

                if (oldTypeToExtensionsMap == null) {
                    return;
                }

                Map<FileType, Set<String>> newTypeToExtensionsMap = new HashMap<>();
                FileTypeManager fileTypeManager = FileTypeManager.getInstance();
                for (FileType type : fileTypeManager.getRegisteredFileTypes()) {
                    newTypeToExtensionsMap.put(type, getExtensions(type, fileTypeManager));
                }
                // file type changes and removals
                if (!newTypeToExtensionsMap.keySet().containsAll(oldTypeToExtensionsMap.keySet())) {
                    Set<FileType> removedFileTypes = new HashSet<>(oldTypeToExtensionsMap.keySet());
                    removedFileTypes.removeAll(newTypeToExtensionsMap.keySet());
                    rebuildAllIndices("The following file types were removed/are no longer associated: " + removedFileTypes);
                    return;
                }
                for (Map.Entry<FileType, Set<String>> entry : oldTypeToExtensionsMap.entrySet()) {
                    FileType fileType = entry.getKey();
                    Set<String> strings = entry.getValue();
                    if (!newTypeToExtensionsMap.get(fileType).containsAll(strings)) {
                        Set<String> removedExtensions = new HashSet<>(strings);
                        removedExtensions.removeAll(newTypeToExtensionsMap.get(fileType));
                        rebuildAllIndices(
                            fileType.getDisplayName() + " is no longer associated with extension(s) " + String.join(",", removedExtensions)
                        );
                        return;
                    }
                }
            }

            
            private Set<String> getExtensions(FileType type, FileTypeManager fileTypeManager) {
                Set<String> set = new HashSet<>();
                for (FileNameMatcher matcher : fileTypeManager.getAssociations(type)) {
                    set.add(matcher.getPresentableString());
                }
                return set;
            }

            private void rebuildAllIndices(String reason) {
                doClearIndices();
                scheduleIndexRebuild("File type change" + ", " + reason);
            }
        });

        connection.subscribe(FileDocumentManagerListener.class, new FileDocumentManagerListener() {
            @Override
            public void fileContentReloaded(VirtualFile file, Document document) {
                cleanupMemoryStorage(true);
            }

            @Override
            public void unsavedDocumentsDropped() {
                cleanupMemoryStorage(false);
            }
        });

        myConnection = connection;

        initComponent();
    }

    void doClearIndices() {
        waitUntilIndicesAreInitialized();
        IndexingStamp.flushCaches();
        for (ID<?, ?> indexId : getState().getIndexIDs()) {
            try {
                clearIndex(indexId);
            }
            catch (StorageException e) {
                LOG.info(e);
            }
        }
    }

    /**
     * Method is used only to _estimate_ size/number of files to be (re)indexed -- to decide to enter dumbMode or not
     *
     * @return false if stopped early (i.e. processor returns false at some point), true if scanned until the end
     */
    boolean processChangedFiles(Project project, Predicate<? super VirtualFile> processor) {
        // can be performance critical, better to use cycle instead of streams
        // avoid missing files when events are processed concurrently
        List<FileIndexingRequest> requests = new ArrayList<>();
        getChangedFilesCollector().getEventMerger().getChangedFiles()
            .forEach(file -> requests.add(FileIndexingRequest.updateRequest(file)));
        Iterator<FileIndexingRequest> pending = myFilesToUpdateCollector.getFilesToUpdateAsIterator();
        while (pending.hasNext()) {
            requests.add(pending.next());
        }

        Set<FileIndexingRequest> checkedFiles = new HashSet<>();
        Predicate<FileIndexingRequest> filterPredicate = filesToBeIndexedForProjectCondition(project);

        for (FileIndexingRequest indexingRequest : requests) {
            if (filterPredicate.test(indexingRequest) && checkedFiles.add(indexingRequest)) {
                if (!processor.test(indexingRequest.getFile())) {
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean isProjectOrWorkspaceFile(VirtualFile file, @Nullable FileType fileType) {
        return ProjectCoreUtil.isProjectOrWorkspaceFile(file, fileType);
    }

    static boolean belongsToScope(VirtualFile file, VirtualFile restrictedTo, SearchScope filter) {
        return file instanceof VirtualFileWithId && file.isValid()
            && (restrictedTo == null || Comparing.equal(file, restrictedTo))
            && (filter == null || restrictedTo != null || filter.accept(file));
    }

    @Override
    public void requestReindex(VirtualFile file) {
        requestReindex(file, true);
    }

    public void requestReindex(VirtualFile file, boolean forceRebuild) {
        GistManager.getInstance().invalidateData();
        // todo: this is the same vfs event handling sequence that is produces after events of FileContentUtilCore.reparseFiles
        // but it is more costly than current code, see IDEA-192192
        //myChangedFilesCollector.invalidateIndicesRecursively(file, false);
        //myChangedFilesCollector.buildIndicesForFileRecursively(file, false);
        ChangedFilesCollector changedFilesCollector = getChangedFilesCollector();
        if (forceRebuild) {
            file.putUserData(IndexingDataKeys.REBUILD_REQUESTED, Boolean.TRUE);
            IndexingFlag.cleanProcessedFlagRecursively(file);
        }
        changedFilesCollector.scheduleForIndexingRecursively(file, true);
        RegisteredIndexes registeredIndexes = myRegisteredIndexes;
        if (registeredIndexes != null && registeredIndexes.isInitialized()) {
            changedFilesCollector.ensureUpToDateAsync();
        }
    }

    private void initComponent() {
        LOG.info("Loading indexes");

        myRegisteredIndexes = new RegisteredIndexes(this);

        if (!IndexInfrastructure.ourDoAsyncIndicesInitialization) {
            waitUntilIndicesAreInitialized();
        }
    }

    void waitUntilIndicesAreInitialized() {
        requireRegisteredIndexes().waitUntilIndicesAreInitialized();
    }

    /**
     * @return true if registered index requires full rebuild for some reason, e.g. is just created or corrupted
     */
    static <K, V> IntSet registerIndexer(
        FileBasedIndexExtension<K, V> extension,
        IndexConfiguration state,
        IndexVersionRegistrationSink registrationStatusSink,
        IntSet dirtyFiles
    ) throws IOException {
        ID<K, V> name = extension.getName();
        int version = extension.getVersion();

        File versionFile = IndexInfrastructure.getVersionFile(name);

        IndexVersion.IndexVersionDiff diff = IndexVersion.versionDiffers(name, version);
        registrationStatusSink.setIndexVersionDiff(name, diff);
        if (diff != IndexVersion.IndexVersionDiff.UP_TO_DATE) {
            boolean versionFileExisted = versionFile.exists();

            File rootDir = IndexInfrastructure.getIndexRootDir(name);
            if (versionFileExisted) {
                FileUtil.deleteWithRenaming(rootDir);
            }
            IndexVersion.rewriteVersion(name, version);
        }

        return initIndexStorage(extension, version, state, registrationStatusSink, dirtyFiles);
    }

    private static <K, V> IntSet initIndexStorage(
        FileBasedIndexExtension<K, V> extension,
        int version,
        IndexConfiguration state,
        IndexVersionRegistrationSink registrationStatusSink,
        IntSet dirtyFiles
    ) throws IOException {
        VfsAwareMapIndexStorage<K, V> storage = null;
        ID<K, V> name = extension.getName();
        UpdatableIndex<K, V, FileContent, ?> registeredIndex = null;

        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                storage = new VfsAwareMapIndexStorage<>(
                    IndexInfrastructure.getStorageFile(name),
                    extension.getKeyDescriptor(),
                    extension.getValueExternalizer(),
                    extension.getCacheSize(),
                    extension.keyIsUniqueForIndexedFile(),
                    extension.traceKeyHashToVirtualFileMapping()
                );

                InputFilter inputFilter = extension.getInputFilter();

                Set<FileType> addedTypes;
                if (inputFilter instanceof FileTypeSpecificInputFilter fileTypeSpecificInputFilter) {
                    addedTypes = new HashSet<>();
                    fileTypeSpecificInputFilter.registerFileTypesUsedForIndexing(type -> {
                        if (type != null) {
                            addedTypes.add(type);
                        }
                    });
                }
                else {
                    addedTypes = null;
                }

                UpdatableIndex<K, V, FileContent, ?> index = createIndex(extension, new MemoryIndexStorage<>(storage, name));
                registeredIndex = index;

                state.registerIndex(
                    name,
                    index,
                    inputFilter,
                    version + GlobalIndexFilter.getFiltersVersion(name),
                    addedTypes
                );
                break;
            }
            catch (Exception e) {
                LOG.info(e);
                try {
                    if (storage != null) {
                        storage.close();
                    }
                    storage = null;
                }
                catch (Exception ignored) {
                }

                FileUtil.deleteWithRenaming(IndexInfrastructure.getIndexRootDir(name));

                registrationStatusSink.setIndexVersionDiff(name, new IndexVersion.IndexVersionDiff.CorruptedRebuild(version));
                IndexVersion.rewriteVersion(name, version);
            }
        }

        try {
            if (StaleIndexesChecker.shouldCheckStaleIndexesOnStartup()
                && StubUpdatingIndex.INDEX_ID.equals(extension.getName())
                && registeredIndex != null) {
                return StaleIndexesChecker.checkIndexForStaleRecords(registeredIndex, dirtyFiles, true);
            }
        }
        catch (Exception e) {
            LOG.error("Exception while checking for stale records", e);
        }
        return new IntOpenHashSet();
    }

    
    @SuppressWarnings("unchecked")
    private static <K, V> UpdatableIndex<K, V, FileContent, ?> createIndex(
        FileBasedIndexExtension<K, V> extension,
        MemoryIndexStorage<K, V> storage
    ) throws StorageException, IOException {
        return extension instanceof CustomImplementationFileBasedIndexExtension
            ? ((CustomImplementationFileBasedIndexExtension<K, V>) extension).createIndexImplementation(extension, storage)
            : new VfsAwareMapReduceIndex<K, V, FileContent, Void>(extension, storage);
    }

    void setUpShutDownTask() {
        ShutDownTracker.getInstance().registerShutdownTask(this::performShutdown);
    }

    void performShutdown() {
        RegisteredIndexes registeredIndexes = myRegisteredIndexes;
        if (registeredIndexes == null || !registeredIndexes.performShutdown()) {
            return; // already shut down
        }

        ProgressManager.getInstance().executeNonCancelableSection(registeredIndexes::waitUntilAllIndicesAreInitialized);
        try {
            if (myFlushingFuture != null) {
                myFlushingFuture.cancel(false);
                myFlushingFuture = null;
            }
        }
        finally {
            LOG.info("START INDEX SHUTDOWN");
            try {
                PersistentIndicesConfiguration.saveConfiguration();

                IntSet orphanDirtyFilesFromThisSession = getAllDirtyFiles(null);
                int maxSize = Registry.intValue("maximum.size.of.orphan.dirty.files.queue", 1000000);
                registeredIndexes.getOrphanDirtyFilesQueue()
                    .plus(orphanDirtyFilesFromThisSession)
                    .takeLast(maxSize)
                    .store(myVfsCreationStamp);
                // remove events from event merger, so they don't show up after FileBasedIndex is restarted
                getChangedFilesCollector().clear();
                myFilesToUpdateCollector.clear();
                myDirtyFiles.clear();
                myVfsCreationStamp = 0;

                IndexingStamp.close();
                IndexingFlag.unlockAllFiles();
                IndexingFlag.close();

                IndexConfiguration state = getState();
                for (ID<?, ?> indexId : state.getIndexIDs()) {
                    try {
                        UpdatableIndex<?, ?, FileContent, ?> index = state.getIndex(indexId);
                        assert index != null;
                        if (!RebuildStatus.isOk(indexId)) {
                            index.clear(); // if the index was scheduled for rebuild, only clean it
                        }
                        index.dispose();
                    }
                    catch (Throwable throwable) {
                        LOG.info("Problem disposing " + indexId, throwable);
                    }
                }

                myConnection.disconnect();
            }
            catch (Throwable e) {
                LOG.error("Problems during index shutdown", e);
            }
            finally {
                IndexVersion.clearCachedIndexVersions();
            }
            LOG.info("END INDEX SHUTDOWN");
        }
    }


    void removeDataFromIndicesForFile(int fileId, VirtualFile file) {
        List<ID<?, ?>> states = IndexingStamp.getNontrivialFileIndexedStates(fileId);

        IndexingFlag.cleanProcessingFlag(fileId);
        if (!states.isEmpty()) {
            ProgressManager.getInstance().executeNonCancelableSection(() -> removeFileDataFromIndices(states, fileId, file));
        }
        if (!file.isValid()) {
            myIndexableFilesFilterHolder.removeFile(fileId);
        }
    }

    void removeFileDataFromIndices(Collection<? extends ID<?, ?>> affectedIndices, int inputId, @Nullable VirtualFile file) {
        // document diff can depend on previous value that will be removed
        removeTransientFileDataFromIndices(affectedIndices, inputId, file);

        Throwable unexpectedError = null;
        for (ID<?, ?> indexId : affectedIndices) {
            try {
                removeSingleIndexValue(indexId, inputId);
            }
            catch (ProcessCanceledException pce) {
                LOG.error(pce);
            }
            catch (Throwable e) {
                LOG.info(e);
                if (unexpectedError == null) {
                    unexpectedError = e;
                }
            }
        }
        IndexingStamp.flushCache(inputId);

        if (unexpectedError != null) {
            LOG.error(unexpectedError);
        }
    }

    private void removeTransientFileDataFromIndices(
        Collection<? extends ID<?, ?>> indices,
        int inputId,
        @Nullable VirtualFile file
    ) {
        for (ID<?, ?> indexId : indices) {
            UpdatableIndex<?, ?, FileContent, ?> index = getIndex(indexId);
            assert index != null;
            index.removeTransientDataForFile(inputId);
        }

        Document document = (file == null || !file.isValid()) ? null : myFileDocumentManager.getCachedDocument(file);
        if (document != null) {
            myLastIndexedDocStamps.clearForDocument(document);
            document.putUserData(ourFileContentKey, null);
        }

        if (!myUpToDateIndicesForUnsavedOrTransactedDocuments.isEmpty()) {
            myUpToDateIndicesForUnsavedOrTransactedDocuments.clear();
        }
    }

    private void flushAllIndices(long modCount) {
        if (HeavyProcessLatch.INSTANCE.isRunning()) {
            return;
        }
        if (IndexingStamp.isDirty()) {
            IndexingStamp.flushCaches();
        }
        ModuleAwareIndexMetaStorage metaStorage = Application.get().getInstanceIfCreated(ModuleAwareIndexMetaStorage.class);
        if (metaStorage != null) {
            metaStorage.flush();
        }
        ModuleAwareIndexOptionValueStorage valueStorage = Application.get().getInstanceIfCreated(ModuleAwareIndexOptionValueStorage.class);
        if (valueStorage != null) {
            valueStorage.flush();
        }
        IndexConfiguration state = getState();
        for (ID<?, ?> indexId : new ArrayList<>(state.getIndexIDs())) {
            if (HeavyProcessLatch.INSTANCE.isRunning() || modCount != myLocalModCount.get()) {
                return; // do not interfere with 'main' jobs
            }
            try {
                UpdatableIndex<?, ?, FileContent, ?> index = state.getIndex(indexId);
                if (index != null) {
                    index.flush();
                }
            }
            catch (Throwable e) {
                requestRebuild(indexId, e);
            }
        }
    }

    @Override
    @RequiredReadAction
    public <K> Collection<K> getAllKeys(ID<K, ?> indexId, Project project) {
        Set<K> allKeys = new HashSet<>();
        processAllKeys(indexId, Processors.cancelableCollectProcessor(allKeys), project);
        return allKeys;
    }

    @Override
    @RequiredReadAction
    public <K> boolean processAllKeys(ID<K, ?> indexId, Predicate<? super K> processor, @Nullable Project project) {
        return processAllKeys(
            indexId,
            processor,
            project == null ? new EverythingGlobalScope() : GlobalSearchScope.allScope(project),
            null
        );
    }

    @Override
    @RequiredReadAction
    public <K> boolean processAllKeys(ID<K, ?> indexId, Predicate<? super K> processor, SearchScope scope, @Nullable IdFilter idFilter) {
        try {
            waitUntilIndicesAreInitialized();
            UpdatableIndex<K, ?, FileContent, ?> index = getIndex(indexId);
            if (index == null) {
                return true;
            }
            ensureUpToDate(indexId, ((ProjectAwareSearchScope) scope).getProject(), scope);
            return index.processAllKeys(processor, scope, idFilter);
        }
        catch (StorageException e) {
            scheduleRebuild(indexId, e);
        }
        catch (RuntimeException e) {
            Throwable cause = e.getCause();
            if (cause instanceof StorageException || cause instanceof IOException) {
                scheduleRebuild(indexId, cause);
            }
            else {
                throw e;
            }
        }

        return false;
    }

    
    @Override
    @RequiredReadAction
    public <K, V> Map<K, V> getFileData(ID<K, V> id, VirtualFile virtualFile, Project project) {
        int fileId = getFileId(virtualFile);
        Map<K, V> map = processExceptions(
            id,
            virtualFile,
            GlobalSearchScope.fileScope(project, virtualFile),
            index -> index.getIndexedFileData(fileId)
        );
        return Maps.notNullize(map);
    }

    private static final ThreadLocal<Integer> myUpToDateCheckState = new ThreadLocal<>();

    public static <T, E extends Throwable> T disableUpToDateCheckIn(ThrowableComputable<T, E> runnable) throws E {
        disableUpToDateCheckForCurrentThread();
        try {
            return runnable.compute();
        }
        finally {
            enableUpToDateCheckForCurrentThread();
        }
    }

    private static void disableUpToDateCheckForCurrentThread() {
        Integer currentValue = myUpToDateCheckState.get();
        myUpToDateCheckState.set(currentValue == null ? 1 : currentValue + 1);
    }

    private static void enableUpToDateCheckForCurrentThread() {
        Integer currentValue = myUpToDateCheckState.get();
        if (currentValue != null) {
            int newValue = currentValue - 1;
            if (newValue != 0) {
                myUpToDateCheckState.set(newValue);
            }
            else {
                myUpToDateCheckState.remove();
            }
        }
    }

    static boolean isUpToDateCheckEnabled() {
        Integer value = myUpToDateCheckState.get();
        return value == null || value == 0;
    }

    private final ThreadLocal<Boolean> myReentrancyGuard = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * DO NOT CALL DIRECTLY IN CLIENT CODE
     * The method is internal to indexing engine end is called internally. The method is public due to implementation details
     */
    @Override
    @RequiredReadAction
    public <K> void ensureUpToDate(ID<K, ?> indexId, @Nullable Project project, @Nullable SearchScope filter) {
        waitUntilIndicesAreInitialized();
        ensureUpToDate(indexId, project, filter, null);
    }

    @RequiredReadAction
    <K> void ensureUpToDate(
        ID<K, ?> indexId,
        @Nullable Project project,
        @Nullable SearchScope filter,
        @Nullable VirtualFile restrictedFile
    ) {
        ProgressManager.checkCanceled();
        getChangedFilesCollector().ensureUpToDate();
        myApplication.assertReadAccessAllowed();

        NoAccessDuringPsiEventsService.getInstance().checkCallContext();

        if (!needsFileContentLoading(indexId)) {
            return; //indexed eagerly in foreground while building unindexed file list
        }
        if (filter == GlobalSearchScope.EMPTY_SCOPE) {
            return;
        }

        boolean dumbModeAccessRestricted = ourDumbModeAccessTypeStack.get().isEmpty();
        if (dumbModeAccessRestricted && DumbInternalUtil.isDumbMode(project)) {
            handleDumbMode(project);
        }

        if (myReentrancyGuard.get()) {
            //assert false : "ensureUpToDate() is not reentrant!";
            return;
        }
        myReentrancyGuard.set(Boolean.TRUE);

        try {
            if (isUpToDateCheckEnabled()) {
                try {
                    if (!RebuildStatus.isOk(indexId)) {
                        throw new ProcessCanceledException();
                    }
                    forceUpdate(project, filter, restrictedFile);
                    indexUnsavedDocuments(indexId, project, filter, restrictedFile);
                }
                catch (RuntimeException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof StorageException || cause instanceof IOException) {
                        scheduleRebuild(indexId, e);
                    }
                    else {
                        throw e;
                    }
                }
            }
        }
        finally {
            myReentrancyGuard.set(Boolean.FALSE);
        }
    }

    private static void handleDumbMode(@Nullable Project project) {
        ProgressManager.checkCanceled();
        Throwable cause;
        if (project == null) {
            cause = null;
        } else {
            DumbServiceInternal dumbService = (DumbServiceInternal) DumbService.getInstance(project);
            cause = dumbService.getDumbModeStartTrace();
        }

        throw IndexNotReadyException.create(cause);
    }

    @Override
    @RequiredReadAction
    public <K, V> List<V> getValues(ID<K, V> indexId, K dataKey, SearchScope filter) {
        VirtualFile restrictToFile = null;

        if (filter instanceof Iterable) {
            // optimisation: in case of one-file-scope we can do better.
            // check if the scope knows how to extract some files off itself
            //noinspection unchecked
            Iterator<VirtualFile> virtualFileIterator = ((Iterable<VirtualFile>) filter).iterator();
            if (virtualFileIterator.hasNext()) {
                VirtualFile restrictToFileCandidate = virtualFileIterator.next();
                if (!virtualFileIterator.hasNext()) {
                    restrictToFile = restrictToFileCandidate;
                }
            }
        }

        List<V> values = new SmartList<>();
        ValueProcessor<V> processor = (file, value) -> {
            values.add(value);
            return true;
        };
        if (restrictToFile != null) {
            processValuesInOneFile(indexId, dataKey, restrictToFile, processor, filter);
        }
        else {
            processValuesInScope(indexId, dataKey, true, filter, null, processor);
        }
        return values;
    }

    @Override
    @RequiredReadAction
    public <K, V> Collection<VirtualFile> getContainingFiles(ID<K, V> indexId, K dataKey, SearchScope filter) {
        Set<VirtualFile> files = new HashSet<>();
        processValuesInScope(
            indexId,
            dataKey,
            false,
            filter,
            null,
            (file, value) -> {
                files.add(file);
                return true;
            }
        );
        return files;
    }

    @Override
    @RequiredReadAction
    public <K, V> boolean processValues(
        ID<K, V> indexId,
        K dataKey,
        @Nullable VirtualFile inFile,
        ValueProcessor<? super V> processor,
        SearchScope filter
    ) {
        return processValues(indexId, dataKey, inFile, processor, filter, null);
    }

    @Override
    @RequiredReadAction
    public <K, V> boolean processValues(
        ID<K, V> indexId,
        K dataKey,
        @Nullable VirtualFile inFile,
        ValueProcessor<? super V> processor,
        SearchScope filter,
        @Nullable IdFilter idFilter
    ) {
        return inFile != null
            ? processValuesInOneFile(indexId, dataKey, inFile, processor, filter)
            : processValuesInScope(indexId, dataKey, false, filter, idFilter, processor);
    }

    @Override
    @RequiredReadAction
    public <K, V> long getIndexModificationStamp(ID<K, V> indexId, Project project) {
        UpdatableIndex<K, V, FileContent, ?> index = getState().getIndex(indexId);
        ensureUpToDate(indexId, project, GlobalSearchScope.allScope(project));
        return index.getModificationStamp();
    }

    @FunctionalInterface
    public interface IdValueProcessor<V> {
        /**
         * @param fileId the id of the file that the value came from
         * @param value  a value to process
         * @return false if no further processing is needed, true otherwise
         */
        boolean process(int fileId, V value);
    }

    /**
     * Process values for a given index key together with their containing file ids. Note that project is supplied
     * only to ensure that all the indices in that project are up to date; there's no guarantee that the processed file ids belong
     * to this project.
     */
    @RequiredReadAction
    public <K, V> boolean processAllValues(ID<K, V> indexId, K key, Project project, IdValueProcessor<? super V> processor) {
        return processValueIterator(
            indexId,
            key,
            null,
            GlobalSearchScope.allScope(project),
            valueIt -> {
                while (valueIt.hasNext()) {
                    V value = valueIt.next();
                    for (ValueContainer.IntIterator inputIdsIterator = valueIt.getInputIdsIterator(); inputIdsIterator.hasNext(); ) {
                        if (!processor.process(inputIdsIterator.next(), value)) {
                            return false;
                        }
                        ProgressManager.checkCanceled();
                    }
                }
                return true;
            }
        );
    }

    @RequiredReadAction
    private <K, V, R> @Nullable R processExceptions(
        ID<K, V> indexId,
        @Nullable VirtualFile restrictToFile,
        SearchScope filter,
        ThrowableFunction<? super UpdatableIndex<K, V, FileContent, ?>, ? extends R, ? extends StorageException> computable
    ) {
        try {
            waitUntilIndicesAreInitialized();
            UpdatableIndex<K, V, FileContent, ?> index = getIndex(indexId);
            if (index == null) {
                return null;
            }
            Project project = ((ProjectAwareSearchScope) filter).getProject();
            //assert project != null : "GlobalSearchScope#getProject() should be not-null for all index queries";
            ensureUpToDate(indexId, project, filter, restrictToFile);

            return myAccessValidator.validate(
                indexId,
                () -> ConcurrencyUtil.withLock(index.getReadLock(), () -> computable.apply(index))
            );
        }
        catch (StorageException e) {
            scheduleRebuild(indexId, e);
        }
        catch (RuntimeException e) {
            Throwable cause = getCauseToRebuildIndex(e);
            if (cause != null) {
                scheduleRebuild(indexId, cause);
            }
            else {
                throw e;
            }
        }
        return null;
    }

    @RequiredReadAction
    private <K, V> boolean processValuesInOneFile(
        ID<K, V> indexId,
        K dataKey,
        VirtualFile restrictToFile,
        ValueProcessor<? super V> processor,
        SearchScope scope
    ) {
        if (!(restrictToFile instanceof VirtualFileWithId)) {
            return true;
        }

        int restrictedFileId = getFileId(restrictToFile);
        return processValueIterator(
            indexId,
            dataKey,
            restrictToFile,
            scope,
            valueIt -> {
                while (valueIt.hasNext()) {
                    V value = valueIt.next();
                    if (valueIt.getValueAssociationPredicate().contains(restrictedFileId) && !processor.process(restrictToFile, value)) {
                        return false;
                    }
                    ProgressManager.checkCanceled();
                }
                return true;
            }
        );
    }

    @RequiredReadAction
    private <K, V> boolean processValuesInScope(
        ID<K, V> indexId,
        K dataKey,
        boolean ensureValueProcessedOnce,
        SearchScope scope,
        @Nullable IdFilter idFilter,
        ValueProcessor<? super V> processor
    ) {
        PersistentFS fs = (PersistentFS) ManagingFS.getInstance();
        IdFilter filter = idFilter != null ? idFilter : createProjectIndexableFiles(((ProjectAwareSearchScope) scope).getProject());

        return processValueIterator(
            indexId,
            dataKey,
            null,
            scope,
            valueIt -> {
                while (valueIt.hasNext()) {
                    V value = valueIt.next();
                    for (ValueContainer.IntIterator inputIdsIterator = valueIt.getInputIdsIterator(); inputIdsIterator.hasNext(); ) {
                        int id = inputIdsIterator.next();
                        if (filter != null && !filter.containsFileId(id)) {
                            continue;
                        }
                        VirtualFile file = IndexInfrastructure.findFileByIdIfCached(fs, id);
                        if (file != null && scope.accept(file)) {
                            if (!processor.process(file, value)) {
                                return false;
                            }
                            if (ensureValueProcessedOnce) {
                                ProgressManager.checkCanceled();
                                break; // continue with the next value
                            }
                        }

                        ProgressManager.checkCanceled();
                    }
                }
                return true;
            }
        );
    }

    @RequiredReadAction
    private <K, V> boolean processValueIterator(
        ID<K, V> indexId,
        K dataKey,
        @Nullable VirtualFile restrictToFile,
        SearchScope scope,
        Predicate<? super InvertedIndexValueIterator<V>> valueProcessor
    ) {
        Boolean result = processExceptions(
            indexId,
            restrictToFile,
            scope,
            index -> valueProcessor.test((InvertedIndexValueIterator<V>) index.getData(dataKey).getValueIterator())
        );
        return result == null || result;
    }

    @Override
    @RequiredReadAction
    public <K, V> boolean processFilesContainingAllKeys(
        ID<K, V> indexId,
        Collection<? extends K> dataKeys,
        SearchScope filter,
        @Nullable Predicate<? super V> valueChecker,
        Predicate<? super VirtualFile> processor
    ) {
        IdFilter filesSet = createProjectIndexableFiles(((ProjectAwareSearchScope) filter).getProject());
        consulo.util.collection.primitive.ints.IntSet set =
            collectFileIdsContainingAllKeys(indexId, dataKeys, filter, valueChecker, filesSet);
        return set != null && processVirtualFiles(set, filter, processor);
    }

    @TestOnly
    public void cleanupForNextTest() {
        getChangedFilesCollector().ensureUpToDate();

        myTransactionMap = SmartFMap.emptyMap();
        IndexConfiguration state = getState();
        for (ID<?, ?> indexId : state.getIndexIDs()) {
            UpdatableIndex<?, ?, FileContent, ?> index = state.getIndex(indexId);
            assert index != null;
            index.cleanupForNextTest();
        }
    }

    //@ApiStatus.Internal
    public ChangedFilesCollector getChangedFilesCollector() {
        return myChangedFilesCollector.getValue();
    }

    @Override
    public @Nullable IdFilter createProjectIndexableFiles(@Nullable Project project) {
        if (project == null || project.isDefault()) {
            return null;
        }
        return myIndexableFilesFilterHolder.getProjectIndexableFiles(project);
    }

    public ProjectIndexableFilesFilterHolder getIndexableFilesFilterHolder() {
        return myIndexableFilesFilterHolder;
    }

    public @Nullable Project findProjectForFileId(int fileId) {
        return myIndexableFilesFilterHolder.findProjectForFile(fileId);
    }

    private List<Project> ensureFileBelongsToIndexableFilter(int fileId, VirtualFile file) {
        return myIndexableFilesFilterHolder.ensureFileIdPresent(fileId, () -> getContainingProjects(file));
    }

    List<ID<?, ?>> getIndicesForDirectories() {
        return myIndicesForDirectories;
    }

    Set<ID<?, ?>> getNotRequiringContentIndices() {
        return myNotRequiringContentIndices;
    }

    public Set<Project> getContainingProjects(VirtualFile file) {
        Project project = SingleProjectHolder.theOnlyOpenProject();
        if (project != null) {
            return belongsToIndexableFiles(file) ? Collections.singleton(project) : Collections.emptySet();
        }
        else {
            Set<Project> projects = null;
            for (IndexableFileSet set : myIndexableSets) {
                Project setProject = myIndexableSetToProjectMap.get(set);
                if (setProject != null && (projects == null || !projects.contains(setProject)) && set.isInSet(file)) {
                    if (projects == null) {
                        projects = new HashSet<>();
                    }
                    projects.add(setProject);
                }
            }
            return projects == null ? Collections.emptySet() : projects;
        }
    }

    /**
     * @return true if the file belongs to the specific project's indexable files set
     */
    public boolean belongsToProjectIndexableFiles(VirtualFile file, Project project) {
        return ContainerUtil.exists(myIndexableSets, set -> project.equals(myIndexableSetToProjectMap.get(set)) && set.isInSet(file));
    }

    /**
     * @return true if the file belongs to _any_ registered project's indexable files set
     */
    public boolean belongsToIndexableFiles(VirtualFile file) {
        return ContainerUtil.find(myIndexableSets, set -> set.isInSet(file)) != null;
    }

    @RequiredReadAction
    private <K, V> consulo.util.collection.primitive.ints.@Nullable IntSet collectFileIdsContainingAllKeys(
        ID<K, V> indexId,
        Collection<? extends K> dataKeys,
        SearchScope filter,
        @Nullable Predicate<? super V> valueChecker,
        @Nullable IdFilter projectFilesFilter
    ) {
        ThrowableFunction<UpdatableIndex<K, V, FileContent, ?>, consulo.util.collection.primitive.ints.IntSet, StorageException>
            converter =
            index -> InvertedIndexUtil.collectInputIdsContainingAllKeys(
                index,
                dataKeys,
                __ -> {
                    ProgressManager.checkCanceled();
                    return true;
                },
                valueChecker,
                projectFilesFilter == null ? null : projectFilesFilter::containsFileId
            );

        return processExceptions(indexId, null, filter, converter);
    }

    private static boolean processVirtualFiles(
        consulo.util.collection.primitive.ints.IntSet ids,
        SearchScope filter,
        Predicate<? super VirtualFile> processor
    ) {
        PersistentFS fs = (PersistentFS) ManagingFS.getInstance();
        PrimitiveIterator.OfInt iterator = ids.iterator();
        while (iterator.hasNext()) {
            int id = iterator.nextInt();

            ProgressManager.checkCanceled();
            VirtualFile file = IndexInfrastructure.findFileByIdIfCached(fs, id);

            if (file != null && filter.accept(file)) {
                if (!processor.test(file)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static @Nullable Throwable getCauseToRebuildIndex(RuntimeException e) {
        if (Application.get().isUnitTestMode()) {
            // avoid rebuilding index in tests since we do it synchronously in requestRebuild and we can have readAction at hand
            return null;
        }
        if (e instanceof ProcessCanceledException) {
            return null;
        }
        if (e instanceof IndexOutOfBoundsException) {
            return e; // something wrong with direct byte buffer
        }
        Throwable cause = e.getCause();
        if (cause instanceof StorageException || cause instanceof IOException || cause instanceof IllegalArgumentException) {
            return cause;
        }
        return null;
    }

    @Override
    @RequiredReadAction
    public <K, V> boolean getFilesWithKey(
        ID<K, V> indexId,
        Set<? extends K> dataKeys,
        Predicate<? super VirtualFile> processor,
        SearchScope filter
    ) {
        return processFilesContainingAllKeys(indexId, dataKeys, filter, null, processor);
    }

    @Override
    public <K> void scheduleRebuild(ID<K, ?> indexId, Throwable e) {
        requestRebuild(indexId, e);
    }

    private static void scheduleIndexRebuild(String reason) {
        LOG.info("scheduleIndexRebuild, reason: " + reason);
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            new UnindexedFilesScanner(project, "Index rebuild requested: " + reason).queue();
        }
    }

    void clearIndicesIfNecessary() {
        waitUntilIndicesAreInitialized();
        for (ID<?, ?> indexId : getState().getIndexIDs()) {
            try {
                RebuildStatus.clearIndexIfNecessary(indexId, getIndex(indexId)::clear);
            }
            catch (StorageException e) {
                requestRebuild(indexId);
                LOG.error(e);
            }
        }
    }

    void clearIndex(ID<?, ?> indexId) throws StorageException {
        advanceIndexVersion(indexId);

        UpdatableIndex<?, ?, FileContent, ?> index = myState.getIndex(indexId);
        assert index != null : "Index with key " + indexId + " not found or not registered properly";
        index.clear();
    }

    private void advanceIndexVersion(ID<?, ?> indexId) {
        try {
            IndexVersion.rewriteVersion(indexId, myState.getIndexVersion(indexId));
        }
        catch (IOException e) {
            LOG.error(e);
        }
    }

    
    private Set<Document> getUnsavedDocuments() {
        Document[] documents = myFileDocumentManager.getUnsavedDocuments();
        if (documents.length == 0) {
            return Collections.emptySet();
        }
        if (documents.length == 1) {
            return Collections.singleton(documents[0]);
        }
        return new HashSet<>(Arrays.asList(documents));
    }

    
    private Set<Document> getTransactedDocuments() {
        return myTransactionMap.keySet();
    }

    private void indexUnsavedDocuments(
        ID<?, ?> indexId,
        @Nullable Project project,
        SearchScope filter,
        VirtualFile restrictedFile
    ) {
        if (myUpToDateIndicesForUnsavedOrTransactedDocuments.contains(indexId)) {
            return; // no need to index unsaved docs        // todo: check scope ?
        }

        Collection<Document> documents = getUnsavedDocuments();
        boolean psiBasedIndex = myPsiDependentIndices.contains(indexId);
        if (psiBasedIndex) {
            Set<Document> transactedDocuments = getTransactedDocuments();
            if (documents.isEmpty()) {
                documents = transactedDocuments;
            }
            else if (!transactedDocuments.isEmpty()) {
                documents = new HashSet<>(documents);
                documents.addAll(transactedDocuments);
            }
            Document[] uncommittedDocuments =
                project != null ? PsiDocumentManager.getInstance(project).getUncommittedDocuments() : Document.EMPTY_ARRAY;
            if (uncommittedDocuments.length > 0) {
                List<Document> uncommittedDocumentsCollection = Arrays.asList(uncommittedDocuments);
                if (documents.isEmpty()) {
                    documents = uncommittedDocumentsCollection;
                }
                else {
                    if (!(documents instanceof HashSet)) {
                        documents = new HashSet<>(documents);
                    }

                    documents.addAll(uncommittedDocumentsCollection);
                }
            }
        }

        if (!documents.isEmpty()) {
            Collection<Document> documentsToProcessForProject = ContainerUtil.filter(
                documents,
                document -> belongsToScope(myFileDocumentManager.getFile(document), restrictedFile, filter)
            );

            if (!documentsToProcessForProject.isEmpty()) {
                DocumentUpdateTask task = myUnsavedDataUpdateTasks.get(indexId);
                assert task != null : "Task for unsaved data indexing was not initialized for index " + indexId;

                if (runUpdate(true, () -> task.processAll(documentsToProcessForProject, project))
                    && documentsToProcessForProject.size() == documents.size() && !hasActiveTransactions()) {
                    ProgressManager.checkCanceled();
                    myUpToDateIndicesForUnsavedOrTransactedDocuments.add(indexId);
                }
            }
        }
    }

    private boolean hasActiveTransactions() {
        return !myTransactionMap.isEmpty();
    }

    private interface DocumentContent {
        
        CharSequence getText();

        long getModificationStamp();
    }

    private static class AuthenticContent implements DocumentContent {
        private final Document myDocument;

        private AuthenticContent(Document document) {
            myDocument = document;
        }

        
        @Override
        public CharSequence getText() {
            return myDocument.getImmutableCharSequence();
        }

        @Override
        public long getModificationStamp() {
            return myDocument.getModificationStamp();
        }
    }

    private static class PsiContent implements DocumentContent {
        private final Document myDocument;
        private final PsiFile myFile;

        private PsiContent(Document document, PsiFile file) {
            myDocument = document;
            myFile = file;
        }

        
        @Override
        public CharSequence getText() {
            if (myFile.getViewProvider().getModificationStamp() != myDocument.getModificationStamp()) {
                ASTNode node = myFile.getNode();
                assert node != null;
                return node.getChars();
            }
            return myDocument.getImmutableCharSequence();
        }

        @Override
        public long getModificationStamp() {
            return myFile.getViewProvider().getModificationStamp();
        }
    }

    private static final Key<WeakReference<FileContentImpl>> ourFileContentKey = Key.create("unsaved.document.index.content");

    // returns false if doc was not indexed because it is already up to date
    // return true if document was indexed
    // caller is responsible to ensure no concurrent same document processing
    private void indexUnsavedDocument(
        Document document,
        ID<?, ?> requestedIndexId,
        Project project,
        VirtualFile vFile
    ) {
        PsiFile dominantContentFile = project == null ? null : findLatestKnownPsiForUncommittedDocument(document, project);

        DocumentContent content;
        if (dominantContentFile != null && dominantContentFile.getViewProvider()
            .getModificationStamp() != document.getModificationStamp()) {
            content = new PsiContent(document, dominantContentFile);
        }
        else {
            content = new AuthenticContent(document);
        }

        boolean psiBasedIndex = myPsiDependentIndices.contains(requestedIndexId);

        long currentDocStamp =
            psiBasedIndex ? PsiDocumentManager.getInstance(project).getLastCommittedStamp(document) : content.getModificationStamp();

        long previousDocStamp = myLastIndexedDocStamps.get(document, requestedIndexId);
        if (previousDocStamp == currentDocStamp) {
            return;
        }

        CharSequence contentText = content.getText();
        getFileTypeManager().freezeFileTypeTemporarilyIn(vFile, () -> {
            IndexedFileImpl indexedFile = new IndexedFileImpl(vFile, vFile.getFileType());
            indexedFile.setProject(project);
            if (getRequiredIndexes(indexedFile).contains(requestedIndexId)) {
                int inputId = Math.abs(getFileId(vFile));

                if (!isTooLarge(vFile, contentText.length())) {
                    // Reasonably attempt to use same file content when calculating indices
                    // as we can evaluate them several at once and store in file content
                    WeakReference<FileContentImpl> previousContentRef = document.getUserData(ourFileContentKey);
                    FileContentImpl previousContent = consulo.util.lang.ref.SoftReference.dereference(previousContentRef);
                    FileContentImpl newFc;
                    if (previousContent != null && previousContent.getStamp() == currentDocStamp) {
                        newFc = previousContent;
                    }
                    else {
                        newFc = new FileContentImpl(vFile, contentText, currentDocStamp);
                        document.putUserData(ourFileContentKey, new WeakReference<>(newFc));
                    }

                    initFileContent(newFc, project, dominantContentFile);
                    newFc.ensureThreadSafeLighterAST();

                    if (content instanceof AuthenticContent) {
                        LanguageInternal.getInstance().rememberEditorHighlight(newFc, document);
                    }

                    markFileIndexed(vFile);
                    try {
                        updateIndexInNonCancellableSection(requestedIndexId, inputId, newFc);
                    }
                    finally {
                        unmarkBeingIndexed();
                        cleanFileContent(newFc, dominantContentFile);
                    }
                }
                else { // effectively wipe the data from the indices
                    getIndex(requestedIndexId).update(inputId, null).get();
                }
            }

            long previousState = myLastIndexedDocStamps.set(document, requestedIndexId, currentDocStamp);
            assert previousState == previousDocStamp;
        });
    }

    private final StorageGuard myStorageLock = new StorageGuard();
    private volatile boolean myPreviousDataBufferingState;
    private final Object myBufferingStateUpdateLock = new Object();

    public void runCleanupAction(Runnable cleanupAction) {
        Supplier<Boolean> updateComputable = () -> {
            cleanupAction.run();
            return true;
        };
        runUpdate(false, updateComputable);
        runUpdate(true, updateComputable);
    }

    private boolean runUpdate(boolean transientInMemoryIndices, Supplier<Boolean> update) {
        StorageGuard.StorageModeExitHandler storageModeExitHandler = myStorageLock.enter(transientInMemoryIndices);

        if (myPreviousDataBufferingState != transientInMemoryIndices) {
            synchronized (myBufferingStateUpdateLock) {
                if (myPreviousDataBufferingState != transientInMemoryIndices) {
                    IndexConfiguration state = getState();
                    for (ID<?, ?> indexId : state.getIndexIDs()) {
                        UpdatableIndex<?, ?, FileContent, ?> index = state.getIndex(indexId);
                        assert index != null;
                        index.setBufferingEnabled(transientInMemoryIndices);
                    }
                    myPreviousDataBufferingState = transientInMemoryIndices;
                }
            }
        }

        try {
            return update.get();
        }
        finally {
            storageModeExitHandler.leave();
        }
    }

    void cleanupMemoryStorage(boolean skipPsiBasedIndices) {
        myLastIndexedDocStamps.clear();
        IndexConfiguration state = myState;
        if (state == null) {
            // avoid waiting for end of indices initialization (IDEA-173382)
            // in memory content will appear on indexing (in read action) and here is event dispatch (write context)
            return;
        }
        for (ID<?, ?> indexId : state.getIndexIDs()) {
            if (skipPsiBasedIndices && myPsiDependentIndices.contains(indexId)) {
                continue;
            }
            UpdatableIndex<?, ?, FileContent, ?> index = state.getIndex(indexId);
            assert index != null;
            index.cleanupMemoryStorage();
        }
    }

    @Override
    public void requestRebuild(ID<?, ?> indexId, Throwable throwable) {
        if (!myExtensionsRelatedDataWasLoaded) {
            IndexInfrastructure.submitGenesisTask(() -> {
                waitUntilIndicesAreInitialized(); // should be always true here since the genesis pool is sequential
                doRequestRebuild(indexId, throwable);
                return null;
            });
        }
        else {
            doRequestRebuild(indexId, throwable);
        }
    }

    private void doRequestRebuild(ID<?, ?> indexId, Throwable throwable) {
        cleanupProcessedFlag("Rebuild requested for index " + indexId);
        if (!myExtensionsRelatedDataWasLoaded) {
            reportUnexpectedAsyncInitState();
        }

        if (RebuildStatus.requestRebuild(indexId)) {
            String message = "Rebuild requested for index " + indexId;
            Application app = Application.get();
            if (app.isUnitTestMode() && app.isReadAccessAllowed() && !app.isDispatchThread()) {
                // shouldn't happen in tests in general; so fail early with the exception that caused index to be rebuilt.
                // otherwise reindexing will fail anyway later, but with a much more cryptic assertion
                LOG.error(message, throwable);
            }
            else {
                LOG.info(message, throwable);
            }

            cleanupProcessedFlag(message);

            RegisteredIndexes registeredIndexes = myRegisteredIndexes;
            if (registeredIndexes == null || !registeredIndexes.isInitialized()) {
                return;
            }
            advanceIndexVersion(indexId);

            scheduleIndexRebuild("checkRebuild");
        }
    }

    private static void reportUnexpectedAsyncInitState() {
        LOG.error("Unexpected async indices initialization problem");
    }

    public <K, V> UpdatableIndex<K, V, FileContent, ?> getIndex(ID<K, V> indexId) {
        return getState().getIndex(indexId);
    }

    private InputFilter getInputFilter(ID<?, ?> indexId) {
        RegisteredIndexes registeredIndexes = myRegisteredIndexes;
        if (registeredIndexes == null || !registeredIndexes.isInitialized()) {
            // 1. early vfs event that needs invalidation
            // 2. pushers that do synchronous indexing for contentless indices
            waitUntilIndicesAreInitialized();
        }

        return getState().getInputFilter(indexId);
    }

    private boolean acceptsInput(ID<?, ?> indexId, @Nullable Project project, VirtualFile file) {
        return file instanceof VirtualFileWithId
            && getInputFilter(indexId).acceptInput(project, file)
            && !GlobalIndexFilter.isExcludedFromIndexViaFilters(file, indexId);
    }

    
    Collection<FileIndexingRequest> getFilesToUpdate(Project project) {
        return ContainerUtil.filter(getAllFilesToUpdate(), filesToBeIndexedForProjectCondition(project)::test);
    }

    public Collection<FileIndexingRequest> getAllFilesToUpdate() {
        getChangedFilesCollector().ensureUpToDate();
        return myFilesToUpdateCollector.getFilesToUpdate();
    }

    private Predicate<FileIndexingRequest> filesToBeIndexedForProjectCondition(Project project) {
        return indexingRequest -> {
            if (indexingRequest.isDeleteRequest() || !indexingRequest.getFile().isValid()) {
                return true;
            }

            for (IndexableFileSet set : myIndexableSets) {
                Project proj = myIndexableSetToProjectMap.get(set);
                if (proj != null && !proj.equals(project)) {
                    continue; // skip this set as associated with a different project
                }
                if (AccessRule.read(() -> set.isInSet(indexingRequest.getFile()))) {
                    return true;
                }
            }
            return false;
        };
    }

    public boolean isFileUpToDate(VirtualFile file) {
        return file instanceof VirtualFileWithId && !myFilesToUpdateCollector.isScheduledForUpdate(file);
    }

    // caller is responsible to ensure no concurrent same document processing
    void processRefreshedFile(
        @Nullable Project project,
        IndexFileContent fileContent,
        boolean isDeleteRequest,
        FileIndexingStamp indexingStamp
    ) {
        // ProcessCanceledException will cause re-adding the file to processing list
        VirtualFile file = fileContent.getVirtualFile();
        if (myFilesToUpdateCollector.isScheduledForUpdate(file)) {
            indexFileContent(project, fileContent, isDeleteRequest, indexingStamp);
        }
    }

    public void indexFileContent(
        @Nullable Project project,
        IndexFileContent content,
        boolean isDeleteRequest,
        FileIndexingStamp indexingStamp
    ) {
        VirtualFile file = content.getVirtualFile();
        int fileId = Math.abs(getIdMaskingNonIdBasedFile(file));

        boolean setIndexedStatus = true;
        long fileStatusLockObject = IndexingFlag.getOrCreateHash(file);
        try {
            // if file was scheduled for update due to vfs events then it is present in the files-to-update collector
            // in this case we consider that current indexing (out of roots backed CacheUpdater) will cover its content
            if (file.isValid() && content.getTimeStamp() != file.getTimeStamp()) {
                content = new IndexFileContent(file);
            }
            if (isDeleteRequest || !file.isValid() || isTooLarge(file)) {
                removeDataFromIndicesForFile(fileId, file);
            }
            else {
                setIndexedStatus = doIndexFileContent(project, content, indexingStamp);
            }

            myFilesToUpdateCollector.removeFileIdFromFilesScheduledForUpdate(fileId);
            if (setIndexedStatus) {
                IndexingFlag.setIndexedIfFileWithSameLock(file, fileStatusLockObject, indexingStamp);
            }
        }
        finally {
            IndexingStamp.flushCache(fileId);
            IndexingFlag.unlockFile(file);
        }
    }

    private boolean doIndexFileContent(@Nullable Project project, IndexFileContent content, FileIndexingStamp indexingStamp) {
        VirtualFile file = content.getVirtualFile();
        SimpleReference<Boolean> setIndexedStatus = SimpleReference.create(Boolean.TRUE);
        int inputId = Math.abs(getFileId(file));
        Project guessedProject = project == null ? ProjectLocator.getInstance().guessProjectForFile(file) : project;
        IndexedFileImpl indexedFile = new IndexedFileImpl(file, file.getFileType());
        indexedFile.setProject(guessedProject);

        FileIndexingResult.ApplicationMode applicationMode = getIndexApplicationMode();
        List<SingleIndexValueApplier<?>> appliers = new ArrayList<>();
        List<SingleIndexValueRemover> removers = new ArrayList<>();

        getFileTypeManager().freezeFileTypeTemporarilyIn(file, () -> {
            PsiFile psiFile = null;
            FileContentImpl fc = null;

            Set<ID<?, ?>> currentIndexedStates = getAppliedIndexes(inputId);
            List<ID<?, ?>> requiredIndexes = getRequiredIndexes(indexedFile);
            for (ID<?, ?> indexId : requiredIndexes) {
                currentIndexedStates.remove(indexId);
                ProgressManager.checkCanceled();

                if (fc == null) {
                    byte[] currentBytes;
                    try {
                        currentBytes = content.getBytes();
                    }
                    catch (IOException e) {
                        currentBytes = ArrayUtil.EMPTY_BYTE_ARRAY;
                    }
                    fc = new FileContentImpl(file, currentBytes);

                    psiFile = content.getUserData(IndexingDataKeys.PSI_FILE);
                    initFileContent(fc, guessedProject, psiFile);
                }

                boolean shouldUpdate;
                boolean requiredIndexNotUpToDate = getIndexingState(indexedFile, indexId, indexingStamp).updateRequired();
                if (requiredIndexNotUpToDate) {
                    shouldUpdate = RebuildStatus.isOk(indexId);
                    if (!shouldUpdate) {
                        setIndexedStatus.set(Boolean.FALSE);
                    }
                }
                else {
                    shouldUpdate = false;
                }

                if (shouldUpdate) {
                    try {
                        ProgressManager.checkCanceled();
                        SingleIndexValueApplier<?> singleIndexValueApplier =
                            createSingleIndexValueApplier(indexId, file, inputId, fc);
                        if (singleIndexValueApplier == null) {
                            setIndexedStatus.set(Boolean.FALSE);
                        }
                        else {
                            appliers.add(singleIndexValueApplier);
                        }
                    }
                    catch (ProcessCanceledException e) {
                        cleanFileContent(fc, psiFile);
                        throw e;
                    }
                }
            }

            if (psiFile != null) {
                psiFile.putUserData(PsiFileImpl.BUILDING_STUB, null);
            }

            for (ID<?, ?> indexId : currentIndexedStates) {
                ProgressManager.checkCanceled();
                SingleIndexValueRemover remover = createSingleIndexRemover(indexId, file, fc, inputId, applicationMode);
                if (remover == null) {
                    setIndexedStatus.set(Boolean.FALSE);
                }
                else {
                    removers.add(remover);
                }
            }
        });
        file.putUserData(IndexingDataKeys.REBUILD_REQUESTED, null);

        for (SingleIndexValueRemover remover : removers) {
            if (!remover.remove()) {
                setIndexedStatus.set(Boolean.FALSE);
            }
        }
        for (SingleIndexValueApplier<?> applier : appliers) {
            if (!applier.apply()) {
                setIndexedStatus.set(Boolean.FALSE);
            }
            else {
                ModuleAwareIndexMetaRecorder.recordIfApplicable(applier.indexId, file, guessedProject);
            }
        }
        return setIndexedStatus.get();
    }

    @Override
    public boolean isIndexingCandidate(VirtualFile file, ID<?, ?> indexId) {
        return !isTooLarge(file) && getAffectedIndexCandidates(file).contains(indexId);
    }

    
    List<ID<?, ?>> getAffectedIndexCandidates(VirtualFile file) {
        if (file.isDirectory()) {
            return isProjectOrWorkspaceFile(file, null) ? Collections.emptyList() : myIndicesForDirectories;
        }
        FileType fileType = file.getFileType();
        if (isProjectOrWorkspaceFile(file, fileType)) {
            return Collections.emptyList();
        }

        return getState().getFileTypesForIndex(fileType);
    }

    private static void cleanFileContent(FileContentImpl fc, PsiFile psiFile) {
        if (psiFile != null) {
            psiFile.putUserData(PsiFileImpl.BUILDING_STUB, null);
        }
        fc.putUserData(IndexingDataKeys.PSI_FILE, null);
    }

    private static void initFileContent(FileContentImpl fc, Project project, PsiFile psiFile) {
        if (psiFile != null) {
            psiFile.putUserData(PsiFileImpl.BUILDING_STUB, true);
            fc.putUserData(IndexingDataKeys.PSI_FILE, psiFile);
        }

        fc.setProject(project);
    }

    Set<ID<?, ?>> getAppliedIndexes(int inputId) {
        return new HashSet<>(IndexingStamp.getNontrivialFileIndexedStates(inputId));
    }

    List<ID<?, ?>> getRequiredIndexes(IndexedFile indexedFile) {
        RequiredIndexesEvaluator evaluator = myRequiredIndexesEvaluator;
        if (evaluator == null) {
            // 1. early vfs event that needs invalidation
            // 2. pushers that do synchronous indexing for contentless indices
            waitUntilIndicesAreInitialized();
            evaluator = myRequiredIndexesEvaluator;
            if (evaluator == null) {
                reportUnexpectedAsyncInitState();
                return Collections.emptyList();
            }
        }
        return evaluator.getRequiredIndexes(indexedFile);
    }

    void resetHints() {
        IndexConfiguration state = myState;
        if (state != null) {
            myRequiredIndexesEvaluator = new RequiredIndexesEvaluator(state, myIndicesForDirectories);
        }
    }

    FileIndexingStateWithExplanation getIndexingState(IndexedFile file, ID<?, ?> indexId, FileIndexingStamp indexingStamp) {
        return getIndexingState(file, getIndex(indexId), indexingStamp);
    }

    FileIndexingStateWithExplanation getIndexingState(
        IndexedFile file,
        UpdatableIndex<?, ?, ?, ?> index,
        FileIndexingStamp indexingStamp
    ) {
        VirtualFile virtualFile = file.getFile();
        if (isMock(virtualFile)) {
            return FileIndexingStateWithExplanation.notIndexed();
        }
        if (IndexingFlag.isFileChanged(file.getFile(), indexingStamp) == IsFileChangedResult.YES) {
            return FileIndexingStateWithExplanation.outdated("File has changed according to IndexingFlag");
        }
        return index.getIndexingStateForFile(((NewVirtualFile) virtualFile).getId(), file);
    }

    FileIndexingStamp getLatestFileIndexingStamp(@Nullable Project project, VirtualFile file) {
        Project projectForFile = project != null ? project : ContainerUtil.getFirstItem(getContainingProjects(file));
        if (projectForFile == null) {
            return ProjectIndexingDependenciesService.NULL_STAMP;
        }
        IndexingRequestToken indexingRequest =
            ProjectIndexingDependenciesService.getInstance(projectForFile).getLatestIndexingRequestToken();
        return indexingRequest.getFileIndexingStamp(file);
    }

    public void dropNontrivialIndexedStates(int inputId) {
        for (ID<?, ?> id : IndexingStamp.getNontrivialFileIndexedStates(inputId)) {
            dropNontrivialIndexedStates(inputId, id);
        }
    }

    public void dropNontrivialIndexedStates(int inputId, ID<?, ?> indexId) {
        getIndex(indexId).invalidateIndexedStateForFile(inputId);
    }

    static String getFileInfoLogString(int inputId, @Nullable VirtualFile file, @Nullable FileContent currentFC) {
        if (file == null && currentFC == null) {
            return String.valueOf(inputId);
        }
        String fileName = currentFC != null ? currentFC.getFileName() : file.getName();
        return fileName + "(id=" + inputId + ")";
    }

    void requestIndexRebuildOnException(RuntimeException exception, ID<?, ?> indexId) {
        Throwable causeToRebuildIndex = getCauseToRebuildIndex(exception);
        if (causeToRebuildIndex != null) {
            requestRebuild(indexId, causeToRebuildIndex);
        }
        else {
            throw exception;
        }
    }

    boolean updateSingleIndex(ID<?, ?> indexId, VirtualFile file, int inputId, FileContent currentFC) {
        SingleIndexValueApplier<?> applier = createSingleIndexValueApplier(indexId, file, inputId, currentFC);
        if (applier != null) {
            return applier.apply();
        }
        return true;
    }

    /**
     * @return null in case index update is not needed
     */
    @Nullable
    <FileIndexMetaData> SingleIndexValueApplier<FileIndexMetaData> createSingleIndexValueApplier(
        ID<?, ?> indexId,
        VirtualFile file,
        int inputId,
        FileContent currentFC
    ) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("index " + indexId + " update requested for " + getFileInfoLogString(inputId, file, currentFC));
        }
        if (!myExtensionsRelatedDataWasLoaded) {
            reportUnexpectedAsyncInitState();
        }
        if (!RebuildStatus.isOk(indexId) && !myIsUnitTestMode) {
            return null; // the index is scheduled for rebuild, no need to update
        }
        increaseLocalModCount();

        @SuppressWarnings("unchecked")
        UpdatableIndex<?, ?, FileContent, FileIndexMetaData> index =
            (UpdatableIndex<?, ?, FileContent, FileIndexMetaData>) getIndex(indexId);
        assert index != null;

        ensureFileBelongsToIndexableFilter(inputId, file);

        markFileIndexed(file);
        try {
            FileIndexMetaData fileIndexMetaData = index.getFileIndexMetaData(new IndexedFileImpl(file, currentFC.getFileType()));

            // important: no hard referencing currentFC to avoid OOME, the methods introduced for this purpose!
            long indexEvaluationStartedNs = System.nanoTime();
            Supplier<Boolean> storageUpdate = index.update(inputId, currentFC);

            return new SingleIndexValueApplier<>(
                this,
                indexId,
                inputId,
                fileIndexMetaData,
                storageUpdate,
                file,
                currentFC,
                System.nanoTime() - indexEvaluationStartedNs
            );
        }
        catch (RuntimeException exception) {
            requestIndexRebuildOnException(exception, indexId);
            return null;
        }
        finally {
            unmarkBeingIndexed();
        }
    }

    void increaseLocalModCount() {
        myLocalModCount.incrementAndGet();
    }

    private void removeSingleIndexValue(ID<?, ?> indexId, int inputId) {
        SingleIndexValueRemover remover = createSingleIndexRemover(indexId, null, null, inputId, getIndexApplicationMode());
        if (remover != null) {
            remover.remove();
        }
    }

    /**
     * @return null in case index value removal is not necessary
     */
    @Nullable
    SingleIndexValueRemover createSingleIndexRemover(
        ID<?, ?> indexId,
        @Nullable VirtualFile file,
        @Nullable FileContent fileContent,
        int inputId,
        FileIndexingResult.ApplicationMode applicationMode
    ) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("index " + indexId + " deletion requested for " + getFileInfoLogString(inputId, file, fileContent));
        }
        if (!myExtensionsRelatedDataWasLoaded) {
            reportUnexpectedAsyncInitState();
        }
        if (!RebuildStatus.isOk(indexId) && !myIsUnitTestMode) {
            return null; // the index is scheduled for rebuild, no need to update
        }
        return new SingleIndexValueRemover(this, indexId, file, fileContent, inputId, applicationMode);
    }

    private static FileIndexingResult.ApplicationMode getIndexApplicationMode() {
        return FileIndexingResult.ApplicationMode.SameThreadOutsideReadLock;
    }

    static FileIndexingResult.ApplicationMode getContentIndependentIndexesApplicationMode() {
        return FileIndexingResult.ApplicationMode.SameThreadOutsideReadLock;
    }

    private void updateIndexInNonCancellableSection(ID<?, ?> requestedIndexId, int inputId, FileContentImpl newFc) {
        Supplier<Boolean> update = getIndex(requestedIndexId).update(inputId, newFc);
        ProgressManager.getInstance().executeNonCancelableSection(update::get);
    }

    boolean runUpdateForPersistentData(Supplier<Boolean> storageUpdate) {
        return runUpdate(false, () -> {
            SimpleReference<Boolean> result = SimpleReference.create(Boolean.FALSE);
            ProgressManager.getInstance().executeNonCancelableSection(() -> result.set(storageUpdate.get()));
            return result.get();
        });
    }

    private static void markFileIndexed(@Nullable VirtualFile file) {
        if (ourIndexedFile.get() != null) {
            throw new AssertionError("Reentrant indexing");
        }
        ourIndexedFile.set(file);
    }

    private static void unmarkBeingIndexed() {
        ourIndexedFile.remove();
    }

    @Override
    public VirtualFile getFileBeingCurrentlyIndexed() {
        VirtualFile file = ourIndexedFile.get();
        return file != null ? file : ourFileToBeIndexed.get();
    }

    @Override
    public @Nullable DumbModeAccessType getCurrentDumbModeAccessType() {
        Stack<DumbModeAccessType> dumbModeAccessTypeStack = ourDumbModeAccessTypeStack.get();
        return dumbModeAccessTypeStack.isEmpty() ? null : dumbModeAccessTypeStack.peek();
    }

    private class FileIndexingRequestUpdateTask extends UpdateTask<FileIndexingRequest> {
        @Override
        void doProcess(FileIndexingRequest item, Project project) {
            processRefreshedFile(
                project,
                new IndexFileContent(item.getFile()),
                item.isDeleteRequest(),
                getLatestFileIndexingStamp(project, item.getFile())
            );
        }
    }

    private final FileIndexingRequestUpdateTask myForceUpdateTask = new FileIndexingRequestUpdateTask();
    private volatile long myLastOtherProjectInclusionStamp;

    private void forceUpdate(@Nullable Project project, @Nullable SearchScope filter, @Nullable VirtualFile restrictedTo) {
        Collection<FileIndexingRequest> allFilesToUpdate = getAllFilesToUpdate();

        if (!allFilesToUpdate.isEmpty()) {
            boolean includeFilesFromOtherProjects =
                restrictedTo == null && System.currentTimeMillis() - myLastOtherProjectInclusionStamp > 100;
            List<FileIndexingRequest> virtualFilesToBeUpdatedForProject = ContainerUtil.filter(
                allFilesToUpdate,
                new ProjectFilesCondition(createProjectIndexableFiles(project), filter, restrictedTo, includeFilesFromOtherProjects)
            );

            if (!virtualFilesToBeUpdatedForProject.isEmpty()) {
                myForceUpdateTask.processAll(virtualFilesToBeUpdatedForProject, project);
            }
            if (includeFilesFromOtherProjects) {
                myLastOtherProjectInclusionStamp = System.currentTimeMillis();
            }
        }
    }

    final Lock myReadLock;
    final Lock myWriteLock;

    {
        ReadWriteLock lock = new ReentrantReadWriteLock();
        myReadLock = lock.readLock();
        myWriteLock = lock.writeLock();
    }

    boolean needsFileContentLoading(ID<?, ?> indexId) {
        return !myNotRequiringContentIndices.contains(indexId);
    }

    @Nullable IndexableFileSet getIndexableSetForFile(VirtualFile file) {
        for (IndexableFileSet set : myIndexableSets) {
            if (set.isInSet(file)) {
                return set;
            }
        }
        return null;
    }

    void doTransientStateChangeForFile(int fileId, VirtualFile file, List<Project> dirtyQueueProjects) {
        waitUntilIndicesAreInitialized();
        if (!clearUpToDateStateForPsiIndicesOfUnsavedDocuments(file, IndexingStamp.getNontrivialFileIndexedStates(fileId))) {
            // change in persistent file
            clearUpToDateStateForPsiIndicesOfVirtualFile(file, dirtyQueueProjects);
        }
    }

    public void doInvalidateIndicesForFile(
        int fileId,
        VirtualFile file,
        Set<Project> containingProjects,
        List<Project> dirtyQueueProjects
    ) {
        waitUntilIndicesAreInitialized();
        myIndexableFilesFilterHolder.removeFile(fileId);
        if (containingProjects.isEmpty() && file.isValid()) {
            myDirtyFiles.addFile(Collections.emptyList(), fileId); // can be indexed by project which is currently closed
        }
        IndexingFlag.cleanProcessedFlagRecursively(file);

        List<ID<?, ?>> nontrivialFileIndexedStates = IndexingStamp.getNontrivialFileIndexedStates(fileId);

        // transient index value can depend on disk value because former is diff to latter
        // it doesn't matter content froze or not: indices might depend on file name too
        removeTransientFileDataFromIndices(nontrivialFileIndexedStates, fileId, file);

        // The file was removed
        for (ID<?, ?> indexId : nontrivialFileIndexedStates) {
            if (!myRequiringContentIndices.contains(indexId)) {
                removeSingleIndexValue(indexId, fileId);
            }
        }
        if (nontrivialFileIndexedStates.isEmpty() || (file.isValid() && file.isDirectory())) {
            //nontrivialFileIndexedStates={} means the file was never indexed -- so, no need to remove it from indexes.
            // but we still need to cancel any queued updates for it (apart from 'remove' updates):
            myFilesToUpdateCollector.removeScheduledFileFromUpdate(file);
        }
        else {
            // its data should be (lazily) wiped for every index
            myFilesToUpdateCollector.scheduleForUpdate(FileIndexingRequest.deleteRequest(file), containingProjects, dirtyQueueProjects);
        }
    }

    public void scheduleFileForIncrementalIndexing(
        int fileId,
        VirtualFile file,
        boolean onlyContentChanged,
        List<Project> dirtyQueueProjects
    ) {
        Set<Project> containingProjects = getContainingProjects(file);
        if (containingProjects.isEmpty() || !canBeIndexed(file)) {
            // large file might be scheduled for update in before event when its size was not large
            doInvalidateIndicesForFile(fileId, file, containingProjects, dirtyQueueProjects);
            return;
        }
        myIndexableFilesFilterHolder.ensureFileIdPresent(fileId, () -> containingProjects);
        Project projectForFile = ContainerUtil.getFirstItem(containingProjects);

        FileIndexingStamp indexingStamp = getLatestFileIndexingStamp(projectForFile, file);

        List<ID<?, ?>> nontrivialFileIndexedStates = IndexingStamp.getNontrivialFileIndexedStates(fileId);

        // transient index value can depend on disk value because the former is diff to latter
        // it doesn't matter content froze or not: indices might depend on file name too
        removeTransientFileDataFromIndices(nontrivialFileIndexedStates, fileId, file);

        IndexedFileImpl indexedFile = new IndexedFileImpl(file, file.getFileType());
        indexedFile.setProject(projectForFile);

        // Apply index contentless indexes in-place
        // For 'normal indices' schedule the file for update and reset stamps for all affected indices (there
        // can be a client that used indices between before and after events, in such case indices are up-to-date due to force update
        // with old content)
        ourFileToBeIndexed.set(file);
        try {
            getFileTypeManager().freezeFileTypeTemporarilyIn(file, () -> {
                FileContent fileContent = new FileContentImpl(file);

                Set<ID<?, ?>> indexesToInvalidate = new HashSet<>(nontrivialFileIndexedStates);
                boolean hasContentlessIndex = false;
                for (ID<?, ?> indexId : getRequiredIndexes(indexedFile)) {
                    if (tryIndexWithoutContent(indexId, projectForFile, file, fileId, fileContent, onlyContentChanged)) {
                        hasContentlessIndex = true;
                        indexesToInvalidate.remove(indexId); // IndexingStamp has been updated by applier just now
                    }
                    else {
                        indexesToInvalidate.add(indexId);
                    }
                }

                if (!indexesToInvalidate.isEmpty()) {
                    for (ID<?, ?> indexId : indexesToInvalidate) {
                        getIndex(indexId).invalidateIndexedStateForFile(fileId);
                    }

                    myFilesToUpdateCollector.scheduleForUpdate(
                        FileIndexingRequest.updateRequest(file),
                        containingProjects,
                        union(dirtyQueueProjects, containingProjects)
                    );
                }
                else {
                    IndexingFlag.setFileIndexed(file, indexingStamp);
                }
                if (!indexesToInvalidate.isEmpty() || hasContentlessIndex) {
                    IndexingStamp.flushCache(fileId);
                }
            });
        }
        finally {
            ourFileToBeIndexed.remove();
        }
    }

    private static Set<Project> union(Collection<? extends Project> first, Collection<? extends Project> second) {
        Set<Project> result = new LinkedHashSet<>(first);
        result.addAll(second);
        return result;
    }

    private boolean canBeIndexed(VirtualFile file) {
        return file.isValid() && (file.isDirectory() || !isTooLarge(file));
    }

    private boolean tryIndexWithoutContent(
        ID<?, ?> indexId,
        @Nullable Project project,
        VirtualFile file,
        int fileId,
        FileContent fileContent,
        boolean onlyContentChanged
    ) {
        if (needsFileContentLoading(indexId)) {
            return false;
        }
        else if (!onlyContentChanged || indexId == FileTypeIndex.NAME) {
            // Mostly to preserve old behavior and to please the test com.intellij.util.indexing.RequestedToRebuildIndexTest. Rationale:
            //   1. Don't update content-independent indexes if only content has changed - indexes didn't change.
            //   2. FileTypeIndex actually depends on content, but pretends to be content-independent. Update it as well.
            // The test fails because scheduleFileForIndexing is invoked twice from ChangedFilesCollector.processFilesToUpdateInReadAction for
            //   files in state CONTENT_CHANGED+ADDED (once with onlyContentChanged=true, and then with onlyContentChanged=false)
            return acceptsInput(indexId, project, file) && updateSingleIndex(indexId, file, fileId, fileContent);
        }
        else {
            return true; // no update needed
        }
    }

    static FileTypeManagerEx getFileTypeManager() {
        return (FileTypeManagerEx) FileTypeManager.getInstance();
    }

    private boolean clearUpToDateStateForPsiIndicesOfUnsavedDocuments(
        VirtualFile file,
        Collection<? extends ID<?, ?>> affectedIndices
    ) {
        if (!myUpToDateIndicesForUnsavedOrTransactedDocuments.isEmpty()) {
            myUpToDateIndicesForUnsavedOrTransactedDocuments.clear();
        }

        Document document = myFileDocumentManager.getCachedDocument(file);

        if (document != null && myFileDocumentManager.isDocumentUnsaved(document)) {   // will be reindexed in indexUnsavedDocuments
            myLastIndexedDocStamps.clearForDocument(document); // Q: non psi indices
            document.putUserData(ourFileContentKey, null);

            return true;
        }

        removeTransientFileDataFromIndices(ContainerUtil.intersection(affectedIndices, myPsiDependentIndices), getFileId(file), file);
        return false;
    }

    static int getIdMaskingNonIdBasedFile(VirtualFile file) {
        return file instanceof VirtualFileWithId virtualFileWithId ? virtualFileWithId.getId() : IndexingStamp.INVALID_FILE_ID;
    }

    boolean shouldIndexFile(@Nullable Project project, VirtualFile file, ID<?, ?> indexId) {
        if (!acceptsInput(indexId, project, file)) {
            return false;
        }
        if (isMock(file)) {
            return true;
        }
        IndexedFileImpl indexedFile = new IndexedFileImpl(file, file.getFileType());
        indexedFile.setProject(project);
        return getIndex(indexId).getIndexingStateForFile(((NewVirtualFile) file).getId(), indexedFile).updateRequired();
    }

    static boolean isMock(VirtualFile file) {
        return !(file instanceof NewVirtualFile);
    }

    boolean isTooLarge(VirtualFile file) {
        return RawFileLoaderHelper.isTooLargeForIntelligence(file)
            && (!myNoLimitCheckTypes.contains(file.getFileType()) || RawFileLoaderHelper.isTooLargeForContentLoading(file));
    }

    private boolean isTooLarge(VirtualFile file, long contentSize) {
        return RawFileLoaderHelper.isTooLargeForIntelligence(file, contentSize)
            && (!myNoLimitCheckTypes.contains(file.getFileType()) || RawFileLoaderHelper.isTooLargeForContentLoading(file, contentSize));
    }


    @Override
    public void registerIndexableSet(IndexableFileSet set, @Nullable Project project) {
        myIndexableSets.add(set);
        myIndexableSetToProjectMap.put(set, project);
        if (project != null) {
            ((PsiManagerImpl) PsiManager.getInstance(project)).addTreeChangePreprocessor(event -> {
                PsiTreeChangeEventImpl treeChangeEvent = (PsiTreeChangeEventImpl) event;
                if (treeChangeEvent.isGenericChange() && treeChangeEvent.getCode() == PsiTreeChangeEventImpl.PsiEventType.CHILDREN_CHANGED) {
                    PsiFile file = event.getFile();

                    if (file != null) {
                        VirtualFile virtualFile = file.getVirtualFile();
                        if (virtualFile instanceof VirtualFileWithId) {
                            getChangedFilesCollector().getEventMerger().recordTransientStateChangeEvent(virtualFile);
                        }
                    }
                }
            });
        }
    }

    private void clearUpToDateStateForPsiIndicesOfVirtualFile(VirtualFile virtualFile, List<Project> dirtyQueueProjects) {
        if (virtualFile instanceof VirtualFileWithId virtualFileWithId) {
            int fileId = virtualFileWithId.getId();
            boolean wasIndexed = false;
            List<ID<?, ?>> candidates = getAffectedIndexCandidates(virtualFile);
            for (ID<?, ?> candidate : candidates) {
                if (myPsiDependentIndices.contains(candidate)) {
                    if (acceptsInput(candidate, null, virtualFile)) {
                        getIndex(candidate).invalidateIndexedStateForFile(fileId);
                        wasIndexed = true;
                    }
                }
            }
            if (wasIndexed) {
                Set<Project> containingProjects = getContainingProjects(virtualFile);
                myFilesToUpdateCollector.scheduleForUpdate(
                    FileIndexingRequest.updateRequest(virtualFile),
                    containingProjects,
                    union(dirtyQueueProjects, containingProjects)
                );
                IndexingStamp.flushCache(fileId);
            }
        }
    }

    @Override
    public void removeIndexableSet(IndexableFileSet set) {
        if (!myIndexableSetToProjectMap.containsKey(set)) {
            return;
        }
        myIndexableSets.remove(set);
        myIndexableSetToProjectMap.remove(set);

        for (FileIndexingRequest request : getAllFilesToUpdate()) {
            VirtualFile file = request.getFile();
            int fileId = request.getFileId();
            if (!file.isValid()) {
                removeDataFromIndicesForFile(fileId, file);
                myFilesToUpdateCollector.removeFileIdFromFilesScheduledForUpdate(fileId);
            }
            else if (getIndexableSetForFile(file) == null) { // todo remove data from indices for removed
                myFilesToUpdateCollector.removeFileIdFromFilesScheduledForUpdate(fileId);
            }
        }

        IndexingStamp.flushCaches();
    }

    @Override
    public VirtualFile findFileById(Project project, int id) {
        return IndexInfrastructure.findFileById((PersistentFS) ManagingFS.getInstance(), id);
    }

    private static @Nullable PsiFile findLatestKnownPsiForUncommittedDocument(Document doc, Project project) {
        return PsiDocumentManager.getInstance(project).getCachedPsiFile(doc);
    }

    private static void cleanupProcessedFlag(String debugReason) {
        IndexingFlag.cleanupProcessedFlag(debugReason);
    }

    @Override
    public void iterateIndexableFiles(ContentIterator processor, Project project, @Nullable ProgressIndicator indicator) {
        List<IndexableFilesIterator> providers = getOrderedIndexableFilesProviders(project);
        IndexableFilesDeduplicateFilter indexableFilesDeduplicateFilter = IndexableFilesDeduplicateFilter.create();
        for (IndexableFilesIterator provider : providers) {
            if (indicator != null) {
                indicator.checkCanceled();
            }
            if (!provider.iterateFiles(project, processor, indexableFilesDeduplicateFilter)) {
                break;
            }
        }
    }

    /**
     * Returns providers of files to be indexed. Indexing is performed in the order corresponding to the resulting list.
     */
    public List<IndexableFilesIterator> getOrderedIndexableFilesProviders(Project project) {
        return ReadAction.compute(() -> {
            if (project.isDisposed()) {
                return List.of();
            }

            List<IndexableFilesIterator> providers = new ArrayList<>();
            myApplication.getExtensionPoint(IndexableFilesContributor.class)
                .forEach(contributor -> providers.addAll(contributor.getIndexableFiles(project)));
            return providers;
        });
    }

    public void registerProjectFileSets(Project project) {
        myApplication.getExtensionPoint(IndexableFilesContributor.class).forEach(extension -> {
            Predicate<VirtualFile> contributorsPredicate = extension.getOwnFilePredicate(project);
            registerIndexableSet(new IndexableFileSet() {
                @Override
                public boolean isInSet(VirtualFile file) {
                    return contributorsPredicate.test(file);
                }

                @Override
                public String toString() {
                    return "IndexableFileSet[" + extension + "]";
                }
            }, project);
        });
    }

    public void removeProjectFileSets(Project project) {
        List<IndexableFileSet> sets = new ArrayList<>();
        for (IndexableFileSet set : myIndexableSets) {
            if (project.equals(myIndexableSetToProjectMap.get(set))) {
                sets.add(set);
            }
        }
        for (IndexableFileSet set : sets) {
            myIndexableSets.remove(set);
            myIndexableSetToProjectMap.remove(set);
        }
    }

    private final class DocumentUpdateTask extends UpdateTask<Document> {
        private final ID<?, ?> myIndexId;

        DocumentUpdateTask(ID<?, ?> indexId) {
            myIndexId = indexId;
        }

        @Override
        void doProcess(Document document, Project project) {
            indexUnsavedDocument(document, myIndexId, project, myFileDocumentManager.getFile(document));
        }
    }

    void setUpFlusher() {
        myFlushingFuture = FlushingDaemon.everyFiveSeconds(new Runnable() {
            private final SerializationManagerEx mySerializationManager = SerializationManagerEx.getInstanceEx();
            private int lastModCount;

            @Override
            public void run() {
                mySerializationManager.flushNameStorage();

                int currentModCount = myLocalModCount.get();
                if (lastModCount == currentModCount) {
                    flushAllIndices(lastModCount);
                }
                lastModCount = currentModCount;
            }
        });
    }

    @Override
    public void invalidateCaches() {
        CorruptionMarker.requestInvalidation();
    }

    static boolean isPsiDependentIndex(IndexExtension<?, ?, ?> extension) {
        return extension instanceof FileBasedIndexExtension && ((FileBasedIndexExtension<?, ?>) extension).dependsOnFileContent()
            && !(extension instanceof DocumentChangeDependentIndex);
    }

    @Override
    public <T, E extends Throwable> T ignoreDumbMode(DumbModeAccessType dumbModeAccessType, ThrowableSupplier<T, E> computable) throws E {
        assert myApplication.isReadAccessAllowed();
        if (FileBasedIndex.isIndexAccessDuringDumbModeEnabled()) {
            Stack<DumbModeAccessType> dumbModeAccessTypeStack = ourDumbModeAccessTypeStack.get();
            dumbModeAccessTypeStack.push(dumbModeAccessType);
            try {
                return computable.get();
            }
            finally {
                DumbModeAccessType type = dumbModeAccessTypeStack.pop();
                assert dumbModeAccessType == type;
            }
        }
        else {
            return computable.get();
        }
    }
}