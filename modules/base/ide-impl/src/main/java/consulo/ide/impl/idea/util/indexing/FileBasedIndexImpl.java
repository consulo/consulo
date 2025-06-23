// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.util.indexing;

import com.google.common.annotations.VisibleForTesting;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.AccessRule;
import consulo.application.Application;
import consulo.application.HeavyProcessLatch;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.event.ApplicationListener;
import consulo.application.impl.internal.concurent.BoundedTaskExecutor;
import consulo.application.impl.internal.start.StartupUtil;
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
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionImplUtil;
import consulo.ide.impl.idea.openapi.editor.impl.EditorHighlighterCache;
import consulo.ide.impl.idea.openapi.fileTypes.impl.FileTypeManagerImpl;
import consulo.ide.impl.idea.openapi.project.DumbServiceImpl;
import consulo.ide.impl.idea.openapi.roots.impl.PushedFilePropertiesUpdaterImpl;
import consulo.ide.impl.idea.util.ConcurrencyUtil;
import consulo.ide.impl.idea.util.ThrowableConvertor;
import consulo.ide.impl.idea.util.indexing.hash.FileContentHashIndex;
import consulo.ide.impl.idea.util.indexing.hash.FileContentHashIndexExtension;
import consulo.ide.impl.idea.util.indexing.provided.ProvidedIndexExtension;
import consulo.ide.impl.idea.util.indexing.provided.ProvidedIndexExtensionLocator;
import consulo.ide.impl.localize.IndexingLocalize;
import consulo.ide.impl.psi.impl.cache.impl.id.PlatformIdTableBuilding;
import consulo.ide.impl.psi.stubs.SerializationManagerEx;
import consulo.index.io.*;
import consulo.index.io.data.DataOutputStream;
import consulo.index.io.data.IOUtil;
import consulo.language.ast.ASTNode;
import consulo.language.file.FileTypeManager;
import consulo.language.impl.internal.psi.PsiDocumentTransactionListener;
import consulo.language.impl.internal.psi.PsiManagerImpl;
import consulo.language.impl.internal.psi.PsiTreeChangeEventImpl;
import consulo.language.impl.internal.psi.stub.FileContentImpl;
import consulo.language.impl.internal.psi.stub.SubstitutedFileType;
import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.impl.util.NoAccessDuringPsiEvents;
import consulo.language.internal.psi.stub.IdIndex;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.EverythingGlobalScope;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.*;
import consulo.language.psi.stub.gist.GistManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.*;
import consulo.project.content.scope.ProjectAwareSearchScope;
import consulo.project.ui.notification.NotificationDisplayType;
import consulo.project.ui.notification.NotificationGroup;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.collection.*;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import consulo.util.collection.primitive.ints.IntSet;
import consulo.util.dataholder.Key;
import consulo.util.io.FileUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.ShutDownTracker;
import consulo.util.lang.StringUtil;
import consulo.util.lang.SystemProperties;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.*;
import consulo.virtualFileSystem.event.AsyncFileListener;
import consulo.virtualFileSystem.fileType.FileNameMatcher;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeEvent;
import consulo.virtualFileSystem.fileType.FileTypeListener;
import consulo.virtualFileSystem.internal.FlushingDaemon;
import consulo.virtualFileSystem.internal.InternalNewVirtualFile;
import consulo.virtualFileSystem.internal.PersistentFS;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;

import java.io.*;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.util.Stack;
import java.util.*;
import java.util.concurrent.*;
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
    private static final String CORRUPTION_MARKER_NAME = "corruption.marker";
    private static final ThreadLocal<Stack<DumbModeAccessType>> ourDumbModeAccessTypeStack = ThreadLocal.withInitial(Stack::new);

    private final Application myApplication;
    private final List<ID<?, ?>> myIndicesForDirectories = new SmartList<>();

    private final Map<ID<?, ?>, DocumentUpdateTask> myUnsavedDataUpdateTasks = new ConcurrentHashMap<>();

    private final Set<ID<?, ?>> myNotRequiringContentIndices = new HashSet<>();
    private final Set<ID<?, ?>> myRequiringContentIndices = new HashSet<>();
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

    private final MessageBusConnection myConnection;
    private final FileDocumentManager myFileDocumentManager;

    private final Set<ID<?, ?>> myUpToDateIndicesForUnsavedOrTransactedDocuments = ContainerUtil.newConcurrentSet();
    private volatile SmartFMap<Document, PsiFile> myTransactionMap = SmartFMap.emptyMap();

    private final boolean myIsUnitTestMode;
    @Nullable
    private ScheduledFuture<?> myFlushingFuture;
    private final AtomicInteger myLocalModCount = new AtomicInteger();
    AtomicInteger myFilesModCount = new AtomicInteger();
    AtomicInteger myUpdatingFiles = new AtomicInteger();
    private final Set<Project> myProjectsBeingUpdated = ContainerUtil.newConcurrentSet();
    private final IndexAccessValidator myAccessValidator = new IndexAccessValidator();

    volatile boolean myInitialized;

    private Future<IndexConfiguration> myStateFuture;
    private volatile IndexConfiguration myState;
    private volatile Future<?> myAllIndicesInitializedFuture;

    private IndexConfiguration getState() {
        if (!myInitialized) {
            //throw new IndexNotReadyException();
            LOG.error("Unexpected initialization problem");
        }

        IndexConfiguration state = myState; // memory barrier
        if (state == null) {
            try {
                myState = state = myStateFuture.get();
            }
            catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
        return state;
    }

    @Inject
    public FileBasedIndexImpl(Application application) {
        myApplication = application;
        myFileDocumentManager = FileDocumentManager.getInstance();
        myIsUnitTestMode = application.isUnitTestMode();

        MessageBusConnection connection = application.getMessageBus().connect();
        connection.subscribe(PsiDocumentTransactionListener.class, new PsiDocumentTransactionListener() {
            @Override
            public void transactionStarted(@Nonnull Document doc, @Nonnull PsiFile file) {
                myTransactionMap = myTransactionMap.plus(doc, file);
                myUpToDateIndicesForUnsavedOrTransactedDocuments.clear();
            }

            @Override
            public void transactionCompleted(@Nonnull Document doc, @Nonnull PsiFile file) {
                myTransactionMap = myTransactionMap.minus(doc);
            }
        });

        connection.subscribe(FileTypeListener.class, new FileTypeListener() {
            @Nullable
            private Map<FileType, Set<String>> myTypeToExtensionMap;

            @Override
            public void beforeFileTypesChanged(@Nonnull FileTypeEvent event) {
                cleanupProcessedFlag();
                myTypeToExtensionMap = new HashMap<>();
                FileTypeManager fileTypeManager = FileTypeManager.getInstance();
                for (FileType type : fileTypeManager.getRegisteredFileTypes()) {
                    myTypeToExtensionMap.put(type, getExtensions(type, fileTypeManager));
                }
            }

            @Override
            public void fileTypesChanged(@Nonnull FileTypeEvent event) {
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
                            fileType.getName() + " is no longer associated with extension(s) " + String.join(",", removedExtensions)
                        );
                        return;
                    }
                }
            }

            @Nonnull
            private Set<String> getExtensions(@Nonnull FileType type, @Nonnull FileTypeManager fileTypeManager) {
                Set<String> set = new HashSet<>();
                for (FileNameMatcher matcher : fileTypeManager.getAssociations(type)) {
                    set.add(matcher.getPresentableString());
                }
                return set;
            }

            private void rebuildAllIndices(@Nonnull String reason) {
                doClearIndices();
                scheduleIndexRebuild("File type change" + ", " + reason);
            }
        });

        connection.subscribe(FileDocumentManagerListener.class, new FileDocumentManagerListener() {
            @Override
            public void fileContentReloaded(@Nonnull VirtualFile file, @Nonnull Document document) {
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

    @VisibleForTesting
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

    boolean processChangedFiles(@Nonnull Project project, @Nonnull Predicate<? super VirtualFile> processor) {
        // avoid missing files when events are processed concurrently
        Stream<VirtualFile> stream = Stream.concat(
            getChangedFilesCollector().getEventMerger().getChangedFiles(),
            getChangedFilesCollector().myFilesToUpdate.values().stream()
        );
        return stream
            .filter(filesToBeIndexedForProjectCondition(project))
            .distinct()
            .mapToInt(f -> processor.test(f) ? 1 : 0)
            .allMatch(success -> success == 1);
    }

    public static boolean isProjectOrWorkspaceFile(@Nonnull VirtualFile file, @Nullable FileType fileType) {
        return ProjectCoreUtil.isProjectOrWorkspaceFile(file, fileType);
    }

    static boolean belongsToScope(VirtualFile file, VirtualFile restrictedTo, SearchScope filter) {
        return file instanceof VirtualFileWithId && file.isValid()
            && (restrictedTo == null || Comparing.equal(file, restrictedTo))
            && (filter == null || restrictedTo != null || filter.accept(file));
    }

    @Override
    public void requestReindex(@Nonnull VirtualFile file) {
        GistManager.getInstance().invalidateData();
        // todo: this is the same vfs event handling sequence that is produces after events of FileContentUtilCore.reparseFiles
        // but it is more costly than current code, see IDEA-192192
        //myChangedFilesCollector.invalidateIndicesRecursively(file, false);
        //myChangedFilesCollector.buildIndicesForFileRecursively(file, false);
        ChangedFilesCollector changedFilesCollector = getChangedFilesCollector();
        changedFilesCollector.invalidateIndicesRecursively(file, true, changedFilesCollector.getEventMerger());
        if (myInitialized) {
            changedFilesCollector.ensureUpToDateAsync();
        }
    }

    private void initComponent() {
        myStateFuture = IndexInfrastructure.submitGenesisTask(new FileIndexDataInitialization());

        if (!IndexInfrastructure.ourDoAsyncIndicesInitialization) {
            waitUntilIndicesAreInitialized();
        }
    }

    void waitUntilIndicesAreInitialized() {
        try {
            myStateFuture.get();
        }
        catch (Throwable t) {
            LOG.error(t);
        }
    }

    /**
     * @return true if registered index requires full rebuild for some reason, e.g. is just created or corrupted
     */
    private static <K, V> void registerIndexer(
        Project project,
        @Nonnull FileBasedIndexExtension<K, V> extension,
        @Nonnull IndexConfiguration state,
        @Nonnull IndicesRegistrationResult registrationStatusSink
    ) throws IOException {
        ID<K, V> name = extension.getName();
        int version = extension.getVersion();

        File versionFile = IndexInfrastructure.getVersionFile(name);

        if (IndexingStamp.versionDiffers(name, version)) {
            boolean versionFileExisted = versionFile.exists();

            if (versionFileExisted) {
                registrationStatusSink.registerIndexAsChanged(name);
            }
            else {
                registrationStatusSink.registerIndexAsInitiallyBuilt(name);
            }

            if (extension.hasSnapshotMapping() && versionFileExisted) {
                FileUtil.deleteWithRenaming(IndexInfrastructure.getPersistentIndexRootDir(name));
            }
            File rootDir = IndexInfrastructure.getIndexRootDir(name);
            if (versionFileExisted) {
                FileUtil.deleteWithRenaming(rootDir);
            }
            IndexingStamp.rewriteVersion(name, version);
        }
        else {
            registrationStatusSink.registerIndexAsUptoDate(name);
        }

        initIndexStorage(extension, version, state);
    }

    private static <K, V> void initIndexStorage(
        @Nonnull FileBasedIndexExtension<K, V> extension,
        int version,
        @Nonnull IndexConfiguration state
    ) throws IOException {
        VfsAwareMapIndexStorage<K, V> storage = null;
        ID<K, V> name = extension.getName();
        boolean contentHashesEnumeratorOk = false;

        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                if (extension.hasSnapshotMapping()) {
                    ContentHashesSupport.initContentHashesEnumerator();
                    contentHashesEnumeratorOk = true;
                }

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
                if (inputFilter instanceof FileBasedIndex.FileTypeSpecificInputFilter fileTypeSpecificInputFilter) {
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

                UpdatableIndex<K, V, FileContent> index = createIndex(extension, new MemoryIndexStorage<>(storage, name));

                ProvidedIndexExtension<K, V> providedExtension = ProvidedIndexExtensionLocator.findProvidedIndexExtensionFor(extension);
                if (providedExtension != null) {
                    index = ProvidedIndexExtension.wrapWithProvidedIndex(providedExtension, extension, index);
                }

                state.registerIndex(
                    name,
                    index,
                    (p, file) -> file instanceof VirtualFileWithId
                        && inputFilter.acceptInput(p, file) && !GlobalIndexFilter.isExcludedFromIndexViaFilters(file, name),
                    version + GlobalIndexFilter.getFiltersVersion(name),
                    addedTypes
                );
                break;
            }
            catch (Exception e) {
                LOG.info(e);
                boolean instantiatedStorage = storage != null;
                try {
                    if (storage != null) {
                        storage.close();
                    }
                    storage = null;
                }
                catch (Exception ignored) {
                }

                FileUtil.deleteWithRenaming(IndexInfrastructure.getIndexRootDir(name));

                if (extension.hasSnapshotMapping() && (!contentHashesEnumeratorOk || instantiatedStorage)) {
                    // todo there is possibility of corruption of storage and content hashes
                    FileUtil.deleteWithRenaming(IndexInfrastructure.getPersistentIndexRootDir(name));
                }
                IndexingStamp.rewriteVersion(name, version);
            }
        }
    }

    private static void saveRegisteredIndicesAndDropUnregisteredOnes(@Nonnull Collection<? extends ID<?, ?>> ids) {
        if (Application.get().isDisposed() || !IndexInfrastructure.hasIndices()) {
            return;
        }
        File registeredIndicesFile = new File(ContainerPathManager.get().getIndexRoot(), "registered");
        Set<String> indicesToDrop = new HashSet<>();
        boolean exceptionThrown = false;
        if (registeredIndicesFile.exists()) {
            try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(registeredIndicesFile)))) {
                int size = in.readInt();
                for (int idx = 0; idx < size; idx++) {
                    indicesToDrop.add(IOUtil.readString(in));
                }
            }
            catch (Throwable e) { // workaround for IDEA-194253
                LOG.info(e);
                exceptionThrown = true;
                ids.stream().map(ID::getName).forEach(indicesToDrop::add);
            }
        }
        if (!exceptionThrown) {
            for (ID<?, ?> key : ids) {
                indicesToDrop.remove(key.getName());
            }
        }
        if (!indicesToDrop.isEmpty()) {
            LOG.info("Dropping indices:" + StringUtil.join(indicesToDrop, ","));
            for (String s : indicesToDrop) {
                FileUtil.deleteWithRenaming(IndexInfrastructure.getIndexRootDir(ID.create(s)));
            }
        }

        FileUtil.createIfDoesntExist(registeredIndicesFile);
        try (DataOutputStream os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(registeredIndicesFile)))) {
            os.writeInt(ids.size());
            for (ID<?, ?> id : ids) {
                IOUtil.writeString(id.getName(), os);
            }
        }
        catch (IOException e) {
            LOG.info(e);
        }
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    private static <K, V> UpdatableIndex<K, V, FileContent> createIndex(
        @Nonnull FileBasedIndexExtension<K, V> extension,
        @Nonnull MemoryIndexStorage<K, V> storage
    ) throws StorageException, IOException {
        return extension instanceof CustomImplementationFileBasedIndexExtension
            ? ((CustomImplementationFileBasedIndexExtension<K, V>) extension).createIndexImplementation(extension, storage)
            : new VfsAwareMapReduceIndex<>(extension, storage);
    }

    private final AtomicBoolean myShutdownPerformed = new AtomicBoolean(false);

    private void performShutdown() {
        if (!myShutdownPerformed.compareAndSet(false, true)) {
            return; // already shut down
        }

        waitUntilAllIndicesAreInitialized();
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

                for (VirtualFile file : getChangedFilesCollector().getAllFilesToUpdate()) {
                    if (!file.isValid()) {
                        removeDataFromIndicesForFile(Math.abs(getIdMaskingNonIdBasedFile(file)), file);
                    }
                }
                IndexingStamp.flushCaches();

                IndexConfiguration state = getState();
                for (ID<?, ?> indexId : state.getIndexIDs()) {
                    try {
                        UpdatableIndex<?, ?, FileContent> index = state.getIndex(indexId);
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

                ContentHashesSupport.flushContentHashes();
                SharedIndicesData.flushData();
                myConnection.disconnect();
            }
            catch (Throwable e) {
                LOG.error("Problems during index shutdown", e);
            }
            LOG.info("END INDEX SHUTDOWN");
        }
    }

    private void waitUntilAllIndicesAreInitialized() {
        try {
            waitUntilIndicesAreInitialized();
            myAllIndicesInitializedFuture.get();
        }
        catch (Throwable ignore) {
        }
    }

    private void removeDataFromIndicesForFile(int fileId, VirtualFile file) {
        VirtualFile originalFile =
            file instanceof DeletedVirtualFileStub deletedVirtualFileStub ? deletedVirtualFileStub.getOriginalFile() : file;
        List<ID<?, ?>> states = IndexingStamp.getNontrivialFileIndexedStates(fileId);

        if (!states.isEmpty()) {
            ProgressManager.getInstance().executeNonCancelableSection(() -> removeFileDataFromIndices(states, fileId, originalFile));
        }
    }

    private void removeFileDataFromIndices(@Nonnull Collection<? extends ID<?, ?>> affectedIndices, int inputId, VirtualFile file) {
        // document diff can depend on previous value that will be removed
        removeTransientFileDataFromIndices(affectedIndices, inputId, file);

        Throwable unexpectedError = null;
        for (ID<?, ?> indexId : affectedIndices) {
            try {
                updateSingleIndex(indexId, null, inputId, null);
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

    private void removeTransientFileDataFromIndices(Collection<? extends ID<?, ?>> indices, int inputId, VirtualFile file) {
        for (ID<?, ?> indexId : indices) {
            UpdatableIndex<?, ?, FileContent> index = myState.getIndex(indexId);
            assert index != null;
            index.removeTransientDataForFile(inputId);
        }

        Document document = myFileDocumentManager.getCachedDocument(file);
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
        IndexingStamp.flushCaches();
        IndexConfiguration state = getState();
        for (ID<?, ?> indexId : new ArrayList<>(state.getIndexIDs())) {
            if (HeavyProcessLatch.INSTANCE.isRunning() || modCount != myLocalModCount.get()) {
                return; // do not interfere with 'main' jobs
            }
            try {
                UpdatableIndex<?, ?, FileContent> index = state.getIndex(indexId);
                if (index != null) {
                    index.flush();
                }
            }
            catch (Throwable e) {
                requestRebuild(indexId, e);
            }
        }

        ContentHashesSupport.flushContentHashes();
        SharedIndicesData.flushData();
    }

    @Override
    @Nonnull
    @RequiredReadAction
    public <K> Collection<K> getAllKeys(@Nonnull ID<K, ?> indexId, @Nonnull Project project) {
        Set<K> allKeys = new HashSet<>();
        processAllKeys(indexId, Processors.cancelableCollectProcessor(allKeys), project);
        return allKeys;
    }

    @Override
    @RequiredReadAction
    public <K> boolean processAllKeys(@Nonnull ID<K, ?> indexId, @Nonnull Predicate<? super K> processor, @Nullable Project project) {
        return processAllKeys(
            indexId,
            processor,
            project == null ? new EverythingGlobalScope() : GlobalSearchScope.allScope(project),
            null
        );
    }

    @Override
    @RequiredReadAction
    public <K> boolean processAllKeys(
        @Nonnull ID<K, ?> indexId,
        @Nonnull Predicate<? super K> processor,
        @Nonnull SearchScope scope,
        @Nullable IdFilter idFilter
    ) {
        try {
            waitUntilIndicesAreInitialized();
            UpdatableIndex<K, ?, FileContent> index = getIndex(indexId);
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

    @Nonnull
    @Override
    @RequiredReadAction
    public <K, V> Map<K, V> getFileData(@Nonnull ID<K, V> id, @Nonnull VirtualFile virtualFile, @Nonnull Project project) {
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

    public static <T, E extends Throwable> T disableUpToDateCheckIn(@Nonnull ThrowableComputable<T, E> runnable) throws E {
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
    public <K> void ensureUpToDate(@Nonnull ID<K, ?> indexId, @Nullable Project project, @Nullable SearchScope filter) {
        waitUntilIndicesAreInitialized();
        ensureUpToDate(indexId, project, filter, null);
    }

    @RequiredReadAction
    <K> void ensureUpToDate(
        @Nonnull ID<K, ?> indexId,
        @Nullable Project project,
        @Nullable SearchScope filter,
        @Nullable VirtualFile restrictedFile
    ) {
        ProgressManager.checkCanceled();
        getChangedFilesCollector().ensureUpToDate();
        myApplication.assertReadAccessAllowed();

        NoAccessDuringPsiEvents.checkCallContext();

        if (!needsFileContentLoading(indexId)) {
            return; //indexed eagerly in foreground while building unindexed file list
        }
        if (filter == GlobalSearchScope.EMPTY_SCOPE) {
            return;
        }

        boolean dumbModeAccessRestricted = ourDumbModeAccessTypeStack.get().isEmpty();
        if (dumbModeAccessRestricted && ActionImplUtil.isDumbMode(project)) {
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
        ProgressManager.checkCanceled(); // DumbModeAction.CANCEL
        DumbServiceImpl dumbService = (DumbServiceImpl) DumbService.getInstance(project);
        throw IndexNotReadyException.create(project == null ? null : dumbService.getDumbModeStartTrace());
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public <K, V> List<V> getValues(@Nonnull ID<K, V> indexId, @Nonnull K dataKey, @Nonnull SearchScope filter) {
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

    @Nonnull
    @Override
    @RequiredReadAction
    public <K, V> Collection<VirtualFile> getContainingFiles(
        @Nonnull ID<K, V> indexId,
        @Nonnull K dataKey,
        @Nonnull SearchScope filter
    ) {
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
        @Nonnull ID<K, V> indexId,
        @Nonnull K dataKey,
        @Nullable VirtualFile inFile,
        @Nonnull ValueProcessor<? super V> processor,
        @Nonnull SearchScope filter
    ) {
        return processValues(indexId, dataKey, inFile, processor, filter, null);
    }

    @Override
    @RequiredReadAction
    public <K, V> boolean processValues(
        @Nonnull ID<K, V> indexId,
        @Nonnull K dataKey,
        @Nullable VirtualFile inFile,
        @Nonnull ValueProcessor<? super V> processor,
        @Nonnull SearchScope filter,
        @Nullable IdFilter idFilter
    ) {
        return inFile != null
            ? processValuesInOneFile(indexId, dataKey, inFile, processor, filter)
            : processValuesInScope(indexId, dataKey, false, filter, idFilter, processor);
    }

    @Override
    @RequiredReadAction
    public <K, V> long getIndexModificationStamp(@Nonnull ID<K, V> indexId, @Nonnull Project project) {
        UpdatableIndex<K, V, FileContent> index = getState().getIndex(indexId);
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
    public <K, V> boolean processAllValues(
        @Nonnull ID<K, V> indexId,
        @Nonnull K key,
        @Nonnull Project project,
        @Nonnull IdValueProcessor<? super V> processor
    ) {
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

    @Nullable
    @RequiredReadAction
    private <K, V, R> R processExceptions(
        @Nonnull ID<K, V> indexId,
        @Nullable VirtualFile restrictToFile,
        @Nonnull SearchScope filter,
        @Nonnull ThrowableConvertor<? super UpdatableIndex<K, V, FileContent>, ? extends R, ? extends StorageException> computable
    ) {
        try {
            waitUntilIndicesAreInitialized();
            UpdatableIndex<K, V, FileContent> index = getIndex(indexId);
            if (index == null) {
                return null;
            }
            Project project = ((ProjectAwareSearchScope) filter).getProject();
            //assert project != null : "GlobalSearchScope#getProject() should be not-null for all index queries";
            ensureUpToDate(indexId, project, filter, restrictToFile);

            return myAccessValidator.validate(
                indexId,
                () -> ConcurrencyUtil.withLock(index.getReadLock(), () -> computable.convert(index))
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
        @Nonnull ID<K, V> indexId,
        @Nonnull K dataKey,
        @Nonnull VirtualFile restrictToFile,
        @Nonnull ValueProcessor<? super V> processor,
        @Nonnull SearchScope scope
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
        @Nonnull ID<K, V> indexId,
        @Nonnull K dataKey,
        boolean ensureValueProcessedOnce,
        @Nonnull SearchScope scope,
        @Nullable IdFilter idFilter,
        @Nonnull ValueProcessor<? super V> processor
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
        @Nonnull ID<K, V> indexId,
        @Nonnull K dataKey,
        @Nullable VirtualFile restrictToFile,
        @Nonnull SearchScope scope,
        @Nonnull Predicate<? super InvertedIndexValueIterator<V>> valueProcessor
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
        @Nonnull ID<K, V> indexId,
        @Nonnull Collection<? extends K> dataKeys,
        @Nonnull SearchScope filter,
        @Nullable Predicate<? super V> valueChecker,
        @Nonnull Predicate<? super VirtualFile> processor
    ) {
        ProjectIndexableFilesFilter filesSet = createProjectIndexableFiles(((ProjectAwareSearchScope) filter).getProject());
        IntSet set = collectFileIdsContainingAllKeys(indexId, dataKeys, filter, valueChecker, filesSet);
        return set != null && processVirtualFiles(set, filter, processor);
    }

    private static final Key<SoftReference<ProjectIndexableFilesFilter>> ourProjectFilesSetKey = Key.create("projectFiles");

    @TestOnly
    public void cleanupForNextTest() {
        getChangedFilesCollector().ensureUpToDate();

        myTransactionMap = SmartFMap.emptyMap();
        IndexConfiguration state = getState();
        for (ID<?, ?> indexId : state.getIndexIDs()) {
            UpdatableIndex<?, ?, FileContent> index = state.getIndex(indexId);
            assert index != null;
            index.cleanupForNextTest();
        }
    }

    //@ApiStatus.Internal
    public ChangedFilesCollector getChangedFilesCollector() {
        return myChangedFilesCollector.getValue();
    }

    public static final class ProjectIndexableFilesFilter extends IdFilter {
        private static final int SHIFT = 6;
        private static final int MASK = (1 << SHIFT) - 1;
        private final long[] myBitMask;
        private final int myModificationCount;
        private final int myMinId;
        private final int myMaxId;

        private ProjectIndexableFilesFilter(@Nonnull IntList set, int modificationCount) {
            myModificationCount = modificationCount;
            int[] minMax = new int[2];
            if (!set.isEmpty()) {
                minMax[0] = minMax[1] = set.get(0);
            }
            set.forEach(value -> {
                minMax[0] = Math.min(minMax[0], value);
                minMax[1] = Math.max(minMax[1], value);
            });
            myMaxId = minMax[1];
            myMinId = minMax[0];
            myBitMask = new long[((myMaxId - myMinId) >> SHIFT) + 1];
            set.forEach(value -> {
                value -= myMinId;
                myBitMask[value >> SHIFT] |= (1L << (value & MASK));
            });
        }

        @Override
        public boolean containsFileId(int id) {
            if (id < myMinId) {
                return false;
            }
            if (id > myMaxId) {
                return false;
            }
            id -= myMinId;
            return (myBitMask[id >> SHIFT] & (1L << (id & MASK))) != 0;
        }
    }

    void filesUpdateStarted(Project project) {
        getChangedFilesCollector().ensureUpToDate();
        myProjectsBeingUpdated.add(project);
        myFilesModCount.incrementAndGet();
    }

    void filesUpdateFinished(@Nonnull Project project) {
        myProjectsBeingUpdated.remove(project);
        myFilesModCount.incrementAndGet();
    }

    private final Lock myCalcIndexableFilesLock = new ReentrantLock();

    @Override
    @Nullable
    public ProjectIndexableFilesFilter createProjectIndexableFiles(@Nullable Project project) {
        if (project == null || project.isDefault() || myUpdatingFiles.get() > 0) {
            return null;
        }
        if (myProjectsBeingUpdated.contains(project)) {
            return null;
        }

        SoftReference<ProjectIndexableFilesFilter> reference = project.getUserData(ourProjectFilesSetKey);
        ProjectIndexableFilesFilter data = consulo.ide.impl.idea.reference.SoftReference.dereference(reference);
        int currentFileModCount = myFilesModCount.get();
        if (data != null && data.myModificationCount == currentFileModCount) {
            return data;
        }

        if (myCalcIndexableFilesLock.tryLock()) { // make best effort for calculating filter
            try {
                reference = project.getUserData(ourProjectFilesSetKey);
                data = consulo.ide.impl.idea.reference.SoftReference.dereference(reference);
                if (data != null && data.myModificationCount == currentFileModCount) {
                    return data;
                }

                long start = System.currentTimeMillis();

                IntList filesSet = IntLists.newArrayList();
                iterateIndexableFiles(fileOrDir -> {
                    ProgressManager.checkCanceled();
                    if (fileOrDir instanceof VirtualFileWithId virtualFileWithId) {
                        filesSet.add(virtualFileWithId.getId());
                    }
                    return true;
                }, project, SilentProgressIndicator.create());
                ProjectIndexableFilesFilter filter = new ProjectIndexableFilesFilter(filesSet, currentFileModCount);
                project.putUserData(ourProjectFilesSetKey, new SoftReference<>(filter));

                long finish = System.currentTimeMillis();
                LOG.debug(filesSet.size() + " files iterated in " + (finish - start) + " ms");

                return filter;
            }
            finally {
                myCalcIndexableFilesLock.unlock();
            }
        }
        return null; // ok, no filtering
    }

    @Nullable
    @RequiredReadAction
    private <K, V> IntSet collectFileIdsContainingAllKeys(
        @Nonnull ID<K, V> indexId,
        @Nonnull Collection<? extends K> dataKeys,
        @Nonnull SearchScope filter,
        @Nullable Predicate<? super V> valueChecker,
        @Nullable ProjectIndexableFilesFilter projectFilesFilter
    ) {
        ThrowableConvertor<UpdatableIndex<K, V, FileContent>, IntSet, StorageException> convertor =
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

        return processExceptions(indexId, null, filter, convertor);
    }

    private static boolean processVirtualFiles(
        @Nonnull IntSet ids,
        @Nonnull SearchScope filter,
        @Nonnull Predicate<? super VirtualFile> processor
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

    @Nullable
    public static Throwable getCauseToRebuildIndex(@Nonnull RuntimeException e) {
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
        @Nonnull ID<K, V> indexId,
        @Nonnull Set<? extends K> dataKeys,
        @Nonnull Predicate<? super VirtualFile> processor,
        @Nonnull SearchScope filter
    ) {
        return processFilesContainingAllKeys(indexId, dataKeys, filter, null, processor);
    }

    @Override
    public <K> void scheduleRebuild(@Nonnull ID<K, ?> indexId, @Nonnull Throwable e) {
        requestRebuild(indexId, e);
    }

    private static void scheduleIndexRebuild(String reason) {
        LOG.info("scheduleIndexRebuild, reason: " + reason);
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            DumbService.getInstance(project).queueTask(new UnindexedFilesUpdater(project));
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

    private void clearIndex(@Nonnull ID<?, ?> indexId) throws StorageException {
        advanceIndexVersion(indexId);

        UpdatableIndex<?, ?, FileContent> index = myState.getIndex(indexId);
        assert index != null : "Index with key " + indexId + " not found or not registered properly";
        index.clear();
    }

    private void advanceIndexVersion(ID<?, ?> indexId) {
        try {
            IndexingStamp.rewriteVersion(indexId, myState.getIndexVersion(indexId));
        }
        catch (IOException e) {
            LOG.error(e);
        }
    }

    @Nonnull
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

    @Nonnull
    private Set<Document> getTransactedDocuments() {
        return myTransactionMap.keySet();
    }

    private void indexUnsavedDocuments(
        @Nonnull ID<?, ?> indexId,
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
        @Nonnull
        CharSequence getText();

        long getModificationStamp();
    }

    private static class AuthenticContent implements DocumentContent {
        private final Document myDocument;

        private AuthenticContent(Document document) {
            myDocument = document;
        }

        @Nonnull
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

        @Nonnull
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
        @Nonnull Document document,
        @Nonnull ID<?, ?> requestedIndexId,
        Project project,
        @Nonnull VirtualFile vFile
    ) {
        PsiFile dominantContentFile = project == null ? null : findLatestKnownPsiForUncomittedDocument(document, project);

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
            if (getAffectedIndexCandidates(vFile).contains(requestedIndexId) && getInputFilter(requestedIndexId).acceptInput(
                project,
                vFile
            )) {
                int inputId = Math.abs(getFileId(vFile));

                if (!isTooLarge(vFile, contentText.length())) {
                    // Reasonably attempt to use same file content when calculating indices
                    // as we can evaluate them several at once and store in file content
                    WeakReference<FileContentImpl> previousContentRef = document.getUserData(ourFileContentKey);
                    FileContentImpl previousContent = consulo.ide.impl.idea.reference.SoftReference.dereference(previousContentRef);
                    FileContentImpl newFc;
                    if (previousContent != null && previousContent.getStamp() == currentDocStamp) {
                        newFc = previousContent;
                    }
                    else {
                        newFc = new FileContentImpl(vFile, contentText, currentDocStamp);
                        if (IdIndex.ourSnapshotMappingsEnabled) {
                            newFc.putUserData(UpdatableSnapshotInputMappingIndex.FORCE_IGNORE_MAPPING_INDEX_UPDATE, Boolean.TRUE);
                        }
                        document.putUserData(ourFileContentKey, new WeakReference<>(newFc));
                    }

                    initFileContent(newFc, project, dominantContentFile);
                    newFc.ensureThreadSafeLighterAST();

                    if (content instanceof AuthenticContent) {
                        newFc.putUserData(
                            PlatformIdTableBuilding.EDITOR_HIGHLIGHTER,
                            EditorHighlighterCache.getEditorHighlighterForCachesBuilding(document)
                        );
                    }

                    markFileIndexed(vFile);
                    try {
                        getIndex(requestedIndexId).update(inputId, newFc).compute();
                    }
                    finally {
                        unmarkBeingIndexed();
                        cleanFileContent(newFc, dominantContentFile);
                    }
                }
                else { // effectively wipe the data from the indices
                    getIndex(requestedIndexId).update(inputId, null).compute();
                }
            }

            long previousState = myLastIndexedDocStamps.set(document, requestedIndexId, currentDocStamp);
            assert previousState == previousDocStamp;
        });
    }

    private final StorageGuard myStorageLock = new StorageGuard();
    private volatile boolean myPreviousDataBufferingState;
    private final Object myBufferingStateUpdateLock = new Object();

    //@ApiStatus.Experimental
    public void runCleanupAction(@Nonnull Runnable cleanupAction) {
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
                        UpdatableIndex<?, ?, FileContent> index = state.getIndex(indexId);
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
            UpdatableIndex<?, ?, FileContent> index = state.getIndex(indexId);
            assert index != null;
            index.cleanupMemoryStorage();
        }
    }

    @Override
    public void requestRebuild(@Nonnull ID<?, ?> indexId, Throwable throwable) {
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

    private void doRequestRebuild(@Nonnull ID<?, ?> indexId, Throwable throwable) {
        cleanupProcessedFlag();
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

            cleanupProcessedFlag();

            if (!myInitialized) {
                return;
            }
            advanceIndexVersion(indexId);

            scheduleIndexRebuild("checkRebuild");
        }
    }

    private static void reportUnexpectedAsyncInitState() {
        LOG.error("Unexpected async indices initialization problem");
    }

    public <K, V> UpdatableIndex<K, V, FileContent> getIndex(ID<K, V> indexId) {
        return getState().getIndex(indexId);
    }

    private InputFilter getInputFilter(@Nonnull ID<?, ?> indexId) {
        if (!myInitialized) {
            // 1. early vfs event that needs invalidation
            // 2. pushers that do synchronous indexing for contentless indices
            waitUntilIndicesAreInitialized();
        }

        return getState().getInputFilter(indexId);
    }

    @Nonnull
    Collection<VirtualFile> getFilesToUpdate(Project project) {
        return ContainerUtil.filter(getChangedFilesCollector().getAllFilesToUpdate(), filesToBeIndexedForProjectCondition(project)::test);
    }

    @Nonnull
    private Predicate<VirtualFile> filesToBeIndexedForProjectCondition(Project project) {
        return virtualFile -> {
            if (!virtualFile.isValid()) {
                return true;
            }

            for (IndexableFileSet set : myIndexableSets) {
                Project proj = myIndexableSetToProjectMap.get(set);
                if (proj != null && !proj.equals(project)) {
                    continue; // skip this set as associated with a different project
                }
                if (AccessRule.read(() -> set.isInSet(virtualFile))) {
                    return true;
                }
            }
            return false;
        };
    }

    public boolean isFileUpToDate(VirtualFile file) {
        return !getChangedFilesCollector().isScheduledForUpdate(file);
    }

    // caller is responsible to ensure no concurrent same document processing
    void processRefreshedFile(@Nullable Project project, @Nonnull consulo.ide.impl.idea.ide.caches.FileContent fileContent) {
        // ProcessCanceledException will cause re-adding the file to processing list
        VirtualFile file = fileContent.getVirtualFile();
        if (getChangedFilesCollector().isScheduledForUpdate(file)) {
            indexFileContent(project, fileContent);
        }
    }

    public void indexFileContent(@Nullable Project project, @Nonnull consulo.ide.impl.idea.ide.caches.FileContent content) {
        VirtualFile file = content.getVirtualFile();
        int fileId = Math.abs(getIdMaskingNonIdBasedFile(file));

        boolean setIndexedStatus = true;
        try {
            // if file was scheduled for update due to vfs events then it is present in myFilesToUpdate
            // in this case we consider that current indexing (out of roots backed CacheUpdater) will cover its content
            if (file.isValid() && content.getTimeStamp() != file.getTimeStamp()) {
                content = new consulo.ide.impl.idea.ide.caches.FileContent(file);
            }
            if (!file.isValid() || isTooLarge(file)) {
                removeDataFromIndicesForFile(fileId, file);
                if (file instanceof DeletedVirtualFileStub deletedVirtualFileStub && deletedVirtualFileStub.isResurrected()) {
                    doIndexFileContent(project, new consulo.ide.impl.idea.ide.caches.FileContent(deletedVirtualFileStub.getOriginalFile()));
                }
            }
            else {
                setIndexedStatus = doIndexFileContent(project, content);
            }
        }
        finally {
            IndexingStamp.flushCache(fileId);
        }

        getChangedFilesCollector().removeFileIdFromFilesScheduledForUpdate(fileId);
        if (file instanceof InternalNewVirtualFile virtualFileSystemEntry && setIndexedStatus) {
            virtualFileSystemEntry.setFileIndexed(true);
        }
    }

    private boolean doIndexFileContent(@Nullable Project project, @Nonnull consulo.ide.impl.idea.ide.caches.FileContent content) {
        VirtualFile file = content.getVirtualFile();
        SimpleReference<Boolean> setIndexedStatus = SimpleReference.create(Boolean.TRUE);
        getFileTypeManager().freezeFileTypeTemporarilyIn(file, () -> {
            FileType fileType = file.getFileType();
            Project finalProject = project == null ? ProjectLocator.getInstance().guessProjectForFile(file) : project;
            PsiFile psiFile = null;
            FileContentImpl fc = null;
            int inputId = Math.abs(getFileId(file));
            Set<ID<?, ?>> currentIndexedStates = new HashSet<>(IndexingStamp.getNontrivialFileIndexedStates(inputId));

            List<ID<?, ?>> affectedIndexCandidates = getAffectedIndexCandidates(file);
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0, size = affectedIndexCandidates.size(); i < size; ++i) {
                ID<?, ?> indexId = affectedIndexCandidates.get(i);
                if (shouldIndexFile(project, file, indexId)) {
                    if (fc == null) {
                        byte[] currentBytes;
                        try {
                            currentBytes = content.getBytes();
                        }
                        catch (IOException e) {
                            currentBytes = ArrayUtil.EMPTY_BYTE_ARRAY;
                        }
                        fc = new FileContentImpl(file, currentBytes);

                        if (IdIndex.ourSnapshotMappingsEnabled) {
                            FileType substituteFileType = SubstitutedFileType.substituteFileType(file, fileType, finalProject);
                            byte[] hash = calculateHash(currentBytes, fc.getCharset(), fileType, substituteFileType);
                            fc.setHash(hash);
                        }

                        psiFile = content.getUserData(IndexingDataKeys.PSI_FILE);
                        initFileContent(fc, finalProject, psiFile);
                    }

                    try {
                        ProgressManager.checkCanceled();
                        if (!updateSingleIndex(indexId, file, inputId, fc)) {
                            setIndexedStatus.set(Boolean.FALSE);
                        }
                        currentIndexedStates.remove(indexId);
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
                if (!getIndex(indexId).isIndexedStateForFile(inputId, file)) {
                    ProgressManager.checkCanceled();
                    if (!updateSingleIndex(indexId, file, inputId, null)) {
                        setIndexedStatus.set(Boolean.FALSE);
                    }
                }
            }
        });
        return setIndexedStatus.get();
    }

    @Nonnull
    public static byte[] calculateHash(
        @Nonnull byte[] currentBytes,
        @Nonnull Charset charset,
        @Nonnull FileType fileType,
        @Nonnull FileType substituteFileType
    ) {
        return fileType.isBinary()
            ? ContentHashesSupport.calcContentHash(currentBytes, substituteFileType)
            : ContentHashesSupport.calcContentHashWithFileType(currentBytes, charset, substituteFileType);
    }

    public boolean isIndexingCandidate(@Nonnull VirtualFile file, @Nonnull ID<?, ?> indexId) {
        return !isTooLarge(file) && getAffectedIndexCandidates(file).contains(indexId);
    }

    @Nonnull
    private List<ID<?, ?>> getAffectedIndexCandidates(@Nonnull VirtualFile file) {
        if (file.isDirectory()) {
            return isProjectOrWorkspaceFile(file, null) ? Collections.emptyList() : myIndicesForDirectories;
        }
        FileType fileType = file.getFileType();
        if (isProjectOrWorkspaceFile(file, fileType)) {
            return Collections.emptyList();
        }

        return getState().getFileTypesForIndex(fileType);
    }

    private static void cleanFileContent(@Nonnull FileContentImpl fc, PsiFile psiFile) {
        if (psiFile != null) {
            psiFile.putUserData(PsiFileImpl.BUILDING_STUB, null);
        }
        fc.putUserData(IndexingDataKeys.PSI_FILE, null);
    }

    private static void initFileContent(@Nonnull FileContentImpl fc, Project project, PsiFile psiFile) {
        if (psiFile != null) {
            psiFile.putUserData(PsiFileImpl.BUILDING_STUB, true);
            fc.putUserData(IndexingDataKeys.PSI_FILE, psiFile);
        }

        fc.setProject(project);
    }

    private boolean updateSingleIndex(@Nonnull ID<?, ?> indexId, @Nullable VirtualFile file, int inputId, @Nullable FileContent currentFC) {
        if (!myExtensionsRelatedDataWasLoaded) {
            reportUnexpectedAsyncInitState();
        }
        if (!RebuildStatus.isOk(indexId) && !myIsUnitTestMode) {
            return false; // the index is scheduled for rebuild, no need to update
        }
        myLocalModCount.incrementAndGet();

        UpdatableIndex<?, ?, FileContent> index = getIndex(indexId);
        assert index != null;

        markFileIndexed(file);
        boolean updateCalculated = false;
        try {
            // important: no hard referencing currentFC to avoid OOME, the methods introduced for this purpose!
            // important: update is called out of try since possible indexer extension is HANDLED as single file fail / restart indexing policy
            Supplier<Boolean> update = index.update(inputId, currentFC);
            updateCalculated = true;

            runIndexUpdate(indexId, update, file, inputId);
        }
        catch (RuntimeException exception) {
            Throwable causeToRebuildIndex = getCauseToRebuildIndex(exception);
            if (causeToRebuildIndex != null && (updateCalculated || causeToRebuildIndex instanceof IOException)) {
                requestRebuild(indexId, exception);
                return false;
            }
            throw exception;
        }
        finally {
            unmarkBeingIndexed();
        }
        return true;
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
    @Nullable
    public DumbModeAccessType getCurrentDumbModeAccessType() {
        Stack<DumbModeAccessType> dumbModeAccessTypeStack = ourDumbModeAccessTypeStack.get();
        return dumbModeAccessTypeStack.isEmpty() ? null : dumbModeAccessTypeStack.peek();
    }

    private class VirtualFileUpdateTask extends UpdateTask<VirtualFile> {
        @Override
        void doProcess(VirtualFile item, Project project) {
            processRefreshedFile(project, new consulo.ide.impl.idea.ide.caches.FileContent(item));
        }
    }

    private final VirtualFileUpdateTask myForceUpdateTask = new VirtualFileUpdateTask();
    private volatile long myLastOtherProjectInclusionStamp;

    private void forceUpdate(@Nullable Project project, @Nullable SearchScope filter, @Nullable VirtualFile restrictedTo) {
        Collection<VirtualFile> allFilesToUpdate = getChangedFilesCollector().getAllFilesToUpdate();

        if (!allFilesToUpdate.isEmpty()) {
            boolean includeFilesFromOtherProjects =
                restrictedTo == null && System.currentTimeMillis() - myLastOtherProjectInclusionStamp > 100;
            List<VirtualFile> virtualFilesToBeUpdatedForProject = ContainerUtil.filter(
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

    private final Lock myReadLock;
    final Lock myWriteLock;

    {
        ReadWriteLock lock = new ReentrantReadWriteLock();
        myReadLock = lock.readLock();
        myWriteLock = lock.writeLock();
    }

    private void runIndexUpdate(@Nonnull ID<?, ?> indexId, @Nonnull Supplier<Boolean> update, @Nullable VirtualFile file, int inputId) {
        if (runUpdate(false, update)) {
            ConcurrencyUtil.withLock(
                myReadLock,
                () -> {
                    UpdatableIndex<?, ?, FileContent> index = getIndex(indexId);
                    if (file != null) {
                        index.setIndexedStateForFile(inputId, file);
                    }
                    else {
                        index.resetIndexedStateForFile(inputId);
                    }
                }
            );
        }
    }

    private boolean needsFileContentLoading(@Nonnull ID<?, ?> indexId) {
        return !myNotRequiringContentIndices.contains(indexId);
    }

    @Nullable
    IndexableFileSet getIndexableSetForFile(VirtualFile file) {
        for (IndexableFileSet set : myIndexableSets) {
            if (set.isInSet(file)) {
                return set;
            }
        }
        return null;
    }

    void doTransientStateChangeForFile(int fileId, @Nonnull VirtualFile file) {
        waitUntilIndicesAreInitialized();
        if (!clearUpToDateStateForPsiIndicesOfUnsavedDocuments(file, IndexingStamp.getNontrivialFileIndexedStates(fileId))) {
            // change in persistent file
            clearUpToDateStateForPsiIndicesOfVirtualFile(file);
        }
    }

    void doInvalidateIndicesForFile(int fileId, @Nonnull VirtualFile file, boolean contentChanged) {
        waitUntilIndicesAreInitialized();
        cleanProcessedFlag(file);

        List<ID<?, ?>> nontrivialFileIndexedStates = IndexingStamp.getNontrivialFileIndexedStates(fileId);
        Collection<ID<?, ?>> fileIndexedStatesToUpdate = ContainerUtil.intersection(nontrivialFileIndexedStates, myRequiringContentIndices);

        // transient index value can depend on disk value because former is diff to latter
        // it doesn't matter content hanged or not: indices might depend on file name too
        removeTransientFileDataFromIndices(nontrivialFileIndexedStates, fileId, file);

        if (contentChanged) {
            // only mark the file as outdated, reindex will be done lazily
            if (!fileIndexedStatesToUpdate.isEmpty()) {
                //noinspection ForLoopReplaceableByForEach
                for (int i = 0, size = nontrivialFileIndexedStates.size(); i < size; ++i) {
                    ID<?, ?> indexId = nontrivialFileIndexedStates.get(i);
                    if (needsFileContentLoading(indexId)) {
                        getIndex(indexId).resetIndexedStateForFile(fileId);
                    }
                }

                // the file is for sure not a dir and it was previously indexed by at least one index
                if (file.isValid()) {
                    if (!isTooLarge(file)) {
                        getChangedFilesCollector().scheduleForUpdate(file);
                    }
                    else {
                        getChangedFilesCollector().scheduleForUpdate(new DeletedVirtualFileStub((VirtualFileWithId) file));
                    }
                }
                else {
                    LOG.info("Unexpected state in update:" + file);
                }
            }
        }
        else { // file was removed
            for (ID<?, ?> indexId : nontrivialFileIndexedStates) {
                if (myNotRequiringContentIndices.contains(indexId)) {
                    updateSingleIndex(indexId, null, fileId, null);
                }
            }
            if (!fileIndexedStatesToUpdate.isEmpty()) {
                // its data should be (lazily) wiped for every index
                getChangedFilesCollector().scheduleForUpdate(new DeletedVirtualFileStub((VirtualFileWithId) file));
            }
            else {
                getChangedFilesCollector().removeScheduledFileFromUpdate(file); // no need to update it anymore
            }
        }
    }

    void scheduleFileForIndexing(Project project, int fileId, @Nonnull VirtualFile file, boolean contentChange) {
        // handle 'content-less' indices separately
        boolean fileIsDirectory = file.isDirectory();

        if (!contentChange) {
            FileContent fileContent = null;
            for (ID<?, ?> indexId : fileIsDirectory ? myIndicesForDirectories : myNotRequiringContentIndices) {
                if (getInputFilter(indexId).acceptInput(project, file)) {
                    if (fileContent == null) {
                        fileContent = new FileContentImpl(file);
                    }
                    updateSingleIndex(indexId, file, fileId, fileContent);
                }
            }
        }
        // For 'normal indices' schedule the file for update and reset stamps for all affected indices (there
        // can be client that used indices between before and after events, in such case indices are up to date due to force update
        // with old content)
        if (!fileIsDirectory) {
            if (!file.isValid() || isTooLarge(file)) {
                // large file might be scheduled for update in before event when its size was not large
                getChangedFilesCollector().removeScheduledFileFromUpdate(file);
            }
            else {
                ourFileToBeIndexed.set(file);
                try {
                    getFileTypeManager().freezeFileTypeTemporarilyIn(file, () -> {
                        List<ID<?, ?>> candidates = getAffectedIndexCandidates(file);

                        boolean scheduleForUpdate = false;

                        //noinspection ForLoopReplaceableByForEach
                        for (int i = 0, size = candidates.size(); i < size; ++i) {
                            ID<?, ?> indexId = candidates.get(i);
                            if (needsFileContentLoading(indexId) && getInputFilter(indexId).acceptInput(project, file)) {
                                getIndex(indexId).resetIndexedStateForFile(fileId);
                                scheduleForUpdate = true;
                            }
                        }

                        if (scheduleForUpdate) {
                            IndexingStamp.flushCache(fileId);
                            getChangedFilesCollector().scheduleForUpdate(file);
                        }
                        else if (file instanceof InternalNewVirtualFile virtualFileSystemEntry) {
                            virtualFileSystemEntry.setFileIndexed(true);
                        }
                    });
                }
                finally {
                    ourFileToBeIndexed.remove();
                }
            }
        }
    }

    private static FileTypeManagerImpl getFileTypeManager() {
        return (FileTypeManagerImpl) FileTypeManager.getInstance();
    }

    private boolean clearUpToDateStateForPsiIndicesOfUnsavedDocuments(
        @Nonnull VirtualFile file,
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

    static int getIdMaskingNonIdBasedFile(@Nonnull VirtualFile file) {
        return file instanceof VirtualFileWithId virtualFileWithId ? virtualFileWithId.getId() : IndexingStamp.INVALID_FILE_ID;
    }

    private class UnindexedFilesFinder implements CollectingContentIterator {
        private final List<VirtualFile> myFiles = new ArrayList<>();
        private final boolean myDoTraceForFilesToBeIndexed = LOG.isTraceEnabled();

        @Nonnull
        @Override
        public List<VirtualFile> getFiles() {
            List<VirtualFile> files;
            synchronized (myFiles) {
                files = myFiles;
            }

            // When processing roots concurrently myFiles looses the local order of local vs archive files
            // If we process the roots in 2 threads we can just separate local vs archive
            // IMPORTANT: also remove duplicated file that can appear due to roots intersection
            BitSet usedFileIds = new BitSet(files.size());
            List<VirtualFile> localFileSystemFiles = new ArrayList<>(files.size() / 2);
            List<VirtualFile> archiveFiles = new ArrayList<>(files.size() / 2);

            for (VirtualFile file : files) {
                int fileId = ((VirtualFileWithId) file).getId();
                if (usedFileIds.get(fileId)) {
                    continue;
                }
                usedFileIds.set(fileId);

                if (file.getFileSystem() instanceof LocalFileSystem) {
                    localFileSystemFiles.add(file);
                }
                else {
                    archiveFiles.add(file);
                }
            }

            localFileSystemFiles.addAll(archiveFiles);
            return localFileSystemFiles;
        }

        @Override
        public boolean processFile(@Nonnull VirtualFile file) {
            return AccessRule.read(() -> {
                if (!file.isValid()) {
                    return true;
                }
                if (file instanceof InternalNewVirtualFile virtualFileSystemEntry && virtualFileSystemEntry.isFileIndexed()) {
                    return true;
                }

                if (!(file instanceof VirtualFileWithId)) {
                    return true;
                }
                getFileTypeManager().freezeFileTypeTemporarilyIn(file, () -> {
                    boolean isUptoDate = true;
                    boolean isDirectory = file.isDirectory();
                    if (!isDirectory && !isTooLarge(file)) {
                        List<ID<?, ?>> affectedIndexCandidates = getAffectedIndexCandidates(file);
                        //noinspection ForLoopReplaceableByForEach
                        for (int i = 0, size = affectedIndexCandidates.size(); i < size; ++i) {
                            ID<?, ?> indexId = affectedIndexCandidates.get(i);
                            try {
                                if (needsFileContentLoading(indexId) && shouldIndexFile(null, file, indexId)) {
                                    if (myDoTraceForFilesToBeIndexed) {
                                        LOG.trace("Scheduling indexing of " + file + " by request of index " + indexId);
                                    }
                                    synchronized (myFiles) {
                                        myFiles.add(file);
                                    }
                                    isUptoDate = false;
                                    break;
                                }
                            }
                            catch (RuntimeException e) {
                                Throwable cause = e.getCause();
                                if (cause instanceof IOException || cause instanceof StorageException) {
                                    LOG.info(e);
                                    requestRebuild(indexId);
                                }
                                else {
                                    throw e;
                                }
                            }
                        }
                    }
                    FileContent fileContent = null;
                    int inputId = Math.abs(getIdMaskingNonIdBasedFile(file));
                    for (ID<?, ?> indexId : isDirectory ? myIndicesForDirectories : myNotRequiringContentIndices) {
                        if (shouldIndexFile(null, file, indexId)) {
                            if (fileContent == null) {
                                fileContent = new FileContentImpl(file);
                            }
                            updateSingleIndex(indexId, file, inputId, fileContent);
                        }
                    }
                    IndexingStamp.flushCache(inputId);

                    if (isUptoDate && file instanceof InternalNewVirtualFile virtualFileSystemEntry) {
                        virtualFileSystemEntry.setFileIndexed(true);
                    }
                });

                ProgressManager.checkCanceled();
                return true;
            });
        }
    }

    private boolean shouldIndexFile(Project project, @Nonnull VirtualFile file, @Nonnull ID<?, ?> indexId) {
        return getInputFilter(indexId).acceptInput(project, file)
            && (isMock(file) || !getIndex(indexId).isIndexedStateForFile(((NewVirtualFile) file).getId(), file));
    }

    static boolean isMock(VirtualFile file) {
        return !(file instanceof NewVirtualFile);
    }

    private boolean isTooLarge(@Nonnull VirtualFile file) {
        return RawFileLoaderHelper.isTooLargeForIntelligence(file)
            && (!myNoLimitCheckTypes.contains(file.getFileType()) || RawFileLoaderHelper.isTooLargeForContentLoading(file));
    }

    private boolean isTooLarge(@Nonnull VirtualFile file, long contentSize) {
        return RawFileLoaderHelper.isTooLargeForIntelligence(file, contentSize)
            && (!myNoLimitCheckTypes.contains(file.getFileType()) || RawFileLoaderHelper.isTooLargeForContentLoading(file, contentSize));
    }

    @Nonnull
    CollectingContentIterator createContentIterator() {
        return new UnindexedFilesFinder();
    }

    @Override
    public void registerIndexableSet(@Nonnull IndexableFileSet set, @Nullable Project project) {
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

    private void clearUpToDateStateForPsiIndicesOfVirtualFile(VirtualFile virtualFile) {
        if (virtualFile instanceof VirtualFileWithId virtualFileWithId) {
            int fileId = virtualFileWithId.getId();
            boolean wasIndexed = false;
            List<ID<?, ?>> candidates = getAffectedIndexCandidates(virtualFile);
            for (ID<?, ?> candidate : candidates) {
                if (myPsiDependentIndices.contains(candidate)) {
                    if (getInputFilter(candidate).acceptInput(null, virtualFile)) {
                        getIndex(candidate).resetIndexedStateForFile(fileId);
                        wasIndexed = true;
                    }
                }
            }
            if (wasIndexed) {
                getChangedFilesCollector().scheduleForUpdate(virtualFile);
                IndexingStamp.flushCache(fileId);
            }
        }
    }

    @Override
    public void removeIndexableSet(@Nonnull IndexableFileSet set) {
        if (!myIndexableSetToProjectMap.containsKey(set)) {
            return;
        }
        myIndexableSets.remove(set);
        myIndexableSetToProjectMap.remove(set);

        ChangedFilesCollector changedFilesCollector = getChangedFilesCollector();
        for (VirtualFile file : changedFilesCollector.getAllFilesToUpdate()) {
            int fileId = Math.abs(getIdMaskingNonIdBasedFile(file));
            if (!file.isValid()) {
                removeDataFromIndicesForFile(fileId, file);
                changedFilesCollector.removeFileIdFromFilesScheduledForUpdate(fileId);
            }
            else if (getIndexableSetForFile(file) == null) { // todo remove data from indices for removed
                changedFilesCollector.removeFileIdFromFilesScheduledForUpdate(fileId);
            }
        }

        IndexingStamp.flushCaches();
    }

    @Override
    public VirtualFile findFileById(Project project, int id) {
        return IndexInfrastructure.findFileById((PersistentFS) ManagingFS.getInstance(), id);
    }

    @Nullable
    private static PsiFile findLatestKnownPsiForUncomittedDocument(@Nonnull Document doc, @Nonnull Project project) {
        return PsiDocumentManager.getInstance(project).getCachedPsiFile(doc);
    }

    private static void cleanupProcessedFlag() {
        VirtualFile[] roots = ManagingFS.getInstance().getRoots();
        for (VirtualFile root : roots) {
            cleanProcessedFlag(root);
        }
    }

    static void cleanProcessedFlag(@Nonnull VirtualFile file) {
        if (!(file instanceof InternalNewVirtualFile)) {
            return;
        }

        InternalNewVirtualFile nvf = (InternalNewVirtualFile) file;
        if (file.isDirectory()) {
            nvf.setFileIndexed(false);
            for (VirtualFile child : nvf.getCachedChildren()) {
                cleanProcessedFlag(child);
            }
        }
        else {
            nvf.setFileIndexed(false);
        }
    }

    @Override
    public void iterateIndexableFilesConcurrently(
        @Nonnull ContentIterator processor,
        @Nonnull Project project,
        @Nonnull ProgressIndicator indicator
    ) {
        PushedFilePropertiesUpdaterImpl.invokeConcurrentlyIfPossible(
            new Exception(),
            myApplication,
            collectScanRootRunnables(processor, project, indicator)
        );
    }

    public void iterateIndexableFilesConcurrently(
        @Nonnull ContentIterator processor,
        @Nonnull Project project,
        @Nonnull ProgressIndicator indicator,
        @Nonnull Exception exception
    ) {
        PushedFilePropertiesUpdaterImpl.invokeConcurrentlyIfPossible(
            exception,
            myApplication,
            collectScanRootRunnables(processor, project, indicator)
        );
    }

    @Override
    public void iterateIndexableFiles(
        @Nonnull ContentIterator processor,
        @Nonnull Project project,
        ProgressIndicator indicator
    ) {
        for (Runnable r : collectScanRootRunnables(processor, project, indicator)) r.run();
    }

    @Nonnull
    private static List<Runnable> collectScanRootRunnables(
        @Nonnull ContentIterator processor,
        @Nonnull Project project,
        ProgressIndicator indicator
    ) {
        FileBasedIndexScanRunnableCollector collector = FileBasedIndexScanRunnableCollector.getInstance(project);
        return collector.collectScanRootRunnables(processor, indicator);
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

    private class FileIndexDataInitialization extends IndexInfrastructure.DataInitialization<IndexConfiguration> {
        private final IndexConfiguration state = new IndexConfiguration();
        private final IndicesRegistrationResult registrationResultSink = new IndicesRegistrationResult();
        private boolean currentVersionCorrupted;

        private void initAssociatedDataForExtensions() {
            ID.reload();

            //Activity activity = StartUpMeasurer.startActivity("file index extensions iteration");
            Iterator<FileBasedIndexExtension> extensions = IndexInfrastructure.hasIndices()
                ? FileBasedIndexExtension.EXTENSION_POINT_NAME.getExtensionList().iterator() : Collections.emptyIterator();

            // todo: init contentless indices first ?
            while (extensions.hasNext()) {
                FileBasedIndexExtension<?, ?> extension = extensions.next();
                if (extension == null) {
                    break;
                }
                ID<?, ?> name = extension.getName();
                RebuildStatus.registerIndex(name);

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

                addNestedInitializationTask(() -> {
                    try {
                        registerIndexer(null, extension, state, registrationResultSink);
                    }
                    catch (IOException io) {
                        throw io;
                    }
                    catch (Throwable t) {
                        StartupUtil.handleComponentError(t, extension.getClass(), null);
                    }
                });
            }

            myExtensionsRelatedDataWasLoaded = true;
            //activity.end();
        }

        @Override
        protected void prepare() {
            // PersistentFS lifecycle should contain FileBasedIndex lifecycle, so,
            // 1) we call for it's instance before index creation to make sure it's initialized
            // 2) we dispose FileBasedIndex before PersistentFS disposing
            ManagingFS fs = ManagingFS.getInstance();
            Disposable disposable = FileBasedIndexImpl.this::performShutdown;
            myApplication.addApplicationListener(
                new ApplicationListener() {
                    @Override
                    public void writeActionStarted(@Nonnull Object action) {
                        myUpToDateIndicesForUnsavedOrTransactedDocuments.clear();
                    }
                },
                disposable
            );
            Disposer.register((Disposable) fs, disposable);

            initAssociatedDataForExtensions();

            File indexRoot = ContainerPathManager.get().getIndexRoot();

            PersistentIndicesConfiguration.loadConfiguration();

            File corruptionMarker = new File(indexRoot, CORRUPTION_MARKER_NAME);
            currentVersionCorrupted = IndexInfrastructure.hasIndices() && corruptionMarker.exists();
            if (currentVersionCorrupted) {
                FileUtil.deleteWithRenaming(indexRoot);
                indexRoot.mkdirs();
                // serialization manager is initialized before and use removed index root so we need to reinitialize it
                SerializationManagerEx.getInstanceEx().reinitializeNameStorage();
                ID.reinitializeDiskStorage();
                PersistentIndicesConfiguration.saveConfiguration();
                FileUtil.delete(corruptionMarker);
            }
        }

        @Override
        protected void onThrowable(@Nonnull Throwable t) {
            LOG.error(t);
        }

        @Override
        protected IndexConfiguration finish() {
            try {
                state.finalizeFileTypeMappingForIndices();

                String changedIndicesText = registrationResultSink.changedIndices();
                String rebuildNotification = null;

                if (currentVersionCorrupted) {
                    rebuildNotification = IndexingLocalize.indexCorruptedNotificationText().get();
                }
                else if (!changedIndicesText.isEmpty()) {
                    rebuildNotification = IndexingLocalize.indexFormatChangedNotificationText(changedIndicesText).get();
                }

                registrationResultSink.logChangedAndFullyBuiltIndices(
                    LOG,
                    "Indices to be rebuilt after version change:",
                    currentVersionCorrupted ? "Indices to be rebuilt after corruption:" : "Indices to be built:"
                );
                if (rebuildNotification != null && !myApplication.isHeadlessEnvironment() && Registry.is("ide.showIndexRebuildMessage")) {
                    NOTIFICATIONS.buildInfo()
                        .title(IndexingLocalize.indexRebuildNotificationTitle())
                        .content(LocalizeValue.localizeTODO(rebuildNotification))
                        .notify(null);
                }

                state.freeze();
                myState = state; // memory barrier
                // check if rebuild was requested for any index during registration
                for (ID<?, ?> indexId : state.getIndexIDs()) {
                    try {
                        RebuildStatus.clearIndexIfNecessary(indexId, () -> clearIndex(indexId));
                    }
                    catch (StorageException e) {
                        requestRebuild(indexId);
                        LOG.error(e);
                    }
                }

                registerIndexableSet(new AdditionalIndexableFileSet(), null);
                return state;
            }
            finally {
                ShutDownTracker.getInstance().registerShutdownTask(FileBasedIndexImpl.this::performShutdown);

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
                myAllIndicesInitializedFuture = IndexInfrastructure.submitGenesisTask(() -> {
                    if (!myShutdownPerformed.get()) {
                        getChangedFilesCollector().ensureUpToDateAsync();
                    }
                    return null;
                });
                myInitialized = true;  // this will ensure that all changes to component's state will be visible to other threads
                saveRegisteredIndicesAndDropUnregisteredOnes(state.getIndexIDs());
            }
        }
    }

    @Override
    public void invalidateCaches() {
        File indexRoot = ContainerPathManager.get().getIndexRoot();
        LOG.info("Requesting explicit indices invalidation", new Throwable());
        try {
            File corruptionMarker = new File(indexRoot, CORRUPTION_MARKER_NAME);
            //noinspection IOResourceOpenedButNotSafelyClosed
            new FileOutputStream(corruptionMarker).close();
        }
        catch (Throwable ignore) {
        }
    }

    @TestOnly
    @RequiredUIAccess
    public void waitForVfsEventsExecuted(long timeout, @Nonnull TimeUnit unit) throws Exception {
        UIAccess.assertIsUIThread();
        BoundedTaskExecutor executor = (BoundedTaskExecutor) getChangedFilesCollector().myVfsEventsExecutor;
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (System.nanoTime() < deadline) {
            try {
                executor.waitAllTasksExecuted(100, TimeUnit.MILLISECONDS);
                return;
            }
            catch (TimeoutException e) {
                UIUtil.dispatchAllInvocationEvents();
            }
        }
    }

    public synchronized FileContentHashIndex getFileContentHashIndex(@Nonnull File enumeratorPath) {
        UpdatableIndex<Integer, Void, FileContent> index = getState().getIndex(FileContentHashIndexExtension.HASH_INDEX_ID);
        if (index == null) {
            IndicesRegistrationResult registrationResult = new IndicesRegistrationResult();
            try {
                registerIndexer(null, FileContentHashIndexExtension.create(enumeratorPath, myApplication), myState, registrationResult);
                registrationResult.logChangedAndFullyBuiltIndices(
                    LOG,
                    "Version was changed for:",
                    "Index is to be rebuilt:"
                );
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            return (FileContentHashIndex) index;
        }
        return (FileContentHashIndex) getState().getIndex(FileContentHashIndexExtension.HASH_INDEX_ID);
    }

    private static final boolean INDICES_ARE_PSI_DEPENDENT_BY_DEFAULT =
        SystemProperties.getBooleanProperty("idea.indices.psi.dependent.default", true);

    static boolean isPsiDependentIndex(@Nonnull IndexExtension<?, ?, ?> extension) {
        if (INDICES_ARE_PSI_DEPENDENT_BY_DEFAULT) {
            return extension instanceof FileBasedIndexExtension && ((FileBasedIndexExtension<?, ?>) extension).dependsOnFileContent()
                && !(extension instanceof DocumentChangeDependentIndex);
        }
        else {
            return extension instanceof PsiDependentIndex;
        }
    }

    @Override
    public <T, E extends Throwable> T ignoreDumbMode(
        @Nonnull DumbModeAccessType dumbModeAccessType,
        @Nonnull ThrowableComputable<T, E> computable
    ) throws E {
        assert myApplication.isReadAccessAllowed();
        if (FileBasedIndex.isIndexAccessDuringDumbModeEnabled()) {
            Stack<DumbModeAccessType> dumbModeAccessTypeStack = ourDumbModeAccessTypeStack.get();
            dumbModeAccessTypeStack.push(dumbModeAccessType);
            try {
                return computable.compute();
            }
            finally {
                DumbModeAccessType type = dumbModeAccessTypeStack.pop();
                assert dumbModeAccessType == type;
            }
        }
        else {
            return computable.compute();
        }
    }
}