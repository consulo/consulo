// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

import consulo.application.Application;
import consulo.application.event.ApplicationListener;
import consulo.application.impl.internal.start.StartupUtil;
import consulo.application.util.registry.Registry;
import consulo.container.boot.ContainerPathManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.index.io.ID;
import consulo.index.io.StorageException;
import consulo.index.io.data.DataOutputStream;
import consulo.index.io.data.IOUtil;
import consulo.language.index.impl.internal.dependencies.AppIndexingDependenciesService;
import consulo.language.index.impl.internal.localize.IndexingLocalize;
import consulo.language.psi.stub.AdditionalIndexableFileSet;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.util.io.FileUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.ManagingFS;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import org.jspecify.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

final class FileBasedIndexDataInitialization
    extends IndexInfrastructure.DataInitialization<FileBasedIndexDataInitialization.FileBasedIndexDataInitializationResult> {

    private static final Logger LOG = Logger.getInstance(FileBasedIndexDataInitialization.class);

    private boolean myCurrentVersionCorrupted;

    private final FileBasedIndexImpl myFileBasedIndex;
    private final RegisteredIndexes myRegisteredIndexes;
    private final IntSet myStaleIds = IntSets.synchronize(new IntOpenHashSet());
    private volatile OrphanDirtyFilesQueue myOrphanDirtyFilesQueue;
    private volatile @Nullable OrphanDirtyFilesQueueDiscardReason myOrphanDirtyFilesQueueRecreationReason;
    private final IndexVersionRegistrationSink myRegistrationResultSink = new IndexVersionRegistrationSink();
    private final IndexConfiguration myState = new IndexConfiguration();

    FileBasedIndexDataInitialization(FileBasedIndexImpl index, RegisteredIndexes registeredIndexes) {
        myFileBasedIndex = index;
        myRegisteredIndexes = registeredIndexes;
    }

    private void initAssociatedDataForExtensions(OrphanDirtyFilesQueue orphanDirtyFilesQueue) {
        ID.reload();

        Iterator<FileBasedIndexExtension> extensions = IndexInfrastructure.hasIndices()
            ? FileBasedIndexExtension.EXTENSION_POINT_NAME.getExtensionList().iterator() : Collections.emptyIterator();

        IntSet allDirtyFiles = new IntOpenHashSet(orphanDirtyFilesQueue.getFileIds());
        if (StaleIndexesChecker.shouldCheckStaleIndexesOnStartup()) {
            readAllProjectDirtyFilesQueues(allDirtyFiles);
        }

        // todo: init contentless indices first ?
        while (extensions.hasNext()) {
            FileBasedIndexExtension<?, ?> extension = extensions.next();
            if (extension == null) {
                break;
            }
            RebuildStatus.registerIndex(extension.getName());
            myFileBasedIndex.registerIndexExtension(extension);

            addNestedInitializationTask(() -> {
                try {
                    myStaleIds.addAll(
                        FileBasedIndexImpl.registerIndexer(extension, myState, myRegistrationResultSink, allDirtyFiles)
                    );
                }
                catch (IOException io) {
                    throw io;
                }
                catch (Throwable t) {
                    StartupUtil.handleComponentError(t, extension.getClass(), null);
                }
            });
        }

        myFileBasedIndex.extensionsDataWasLoaded();
    }

    public static void readAllProjectDirtyFilesQueues(IntSet dirtyFiles) {
        File[] projectQueueFiles = PersistentDirtyFilesQueue.getQueuesDir().toFile().listFiles();
        if (projectQueueFiles != null) {
            for (File file : projectQueueFiles) {
                dirtyFiles.addAll(
                    PersistentDirtyFilesQueue
                        .readProjectDirtyFilesQueue(file.toPath(), ManagingFS.getInstance().getCreationTimestamp())
                        .getFileIds()
                );
            }
        }
    }

    @Override
    protected void prepare() {
        // PersistentFS lifecycle should contain FileBasedIndex lifecycle, so,
        // 1) we call for it's instance before index creation to make sure it's initialized
        // 2) we dispose FileBasedIndex before PersistentFS disposing
        ManagingFS fs = ManagingFS.getInstance();

        // capture VFS creation time. It will be used to identify VFS epoch for dirty files queue.
        // at the moment when we write the queue, VFS might have already been disposed via shutdown hook (in the case on emergency shutdown)
        myFileBasedIndex.setVfsCreationStamp(fs.getCreationTimestamp());

        Disposable disposable = myFileBasedIndex::performShutdown;
        myFileBasedIndex.getApplication().addApplicationListener(
            new ApplicationListener() {
                @Override
                public void writeActionStarted(Object action) {
                    myFileBasedIndex.clearUpToDateIndexesForUnsavedOrTransactedDocs();
                }
            },
            disposable
        );
        Disposer.register((Disposable) fs, disposable);

        myCurrentVersionCorrupted = CorruptionMarker.requireInvalidation();
        if (myCurrentVersionCorrupted) {
            CorruptionMarker.dropIndexes();
            AppIndexingDependenciesService.getInstance().invalidateAllStamps("Indexes corrupted");
        }

        Pair<OrphanDirtyFilesQueue, OrphanDirtyFilesQueueDiscardReason> queueAndReason =
            PersistentDirtyFilesQueue.readOrphanDirtyFilesQueue(PersistentDirtyFilesQueue.getQueueFile(), fs.getCreationTimestamp());
        myOrphanDirtyFilesQueue = queueAndReason.first;
        myOrphanDirtyFilesQueueRecreationReason = queueAndReason.second;

        initAssociatedDataForExtensions(myOrphanDirtyFilesQueue);

        PersistentIndicesConfiguration.loadConfiguration();
    }

    @Override
    protected void onThrowable(Throwable t) {
        LOG.error(t);
    }

    @Override
    protected FileBasedIndexDataInitializationResult finish() {
        try {
            myState.finalizeFileTypeMappingForIndices();

            showChangedIndexesNotification();

            myRegistrationResultSink.logChangedAndFullyBuiltIndices(
                FileBasedIndexImpl.LOG,
                "Indices to be rebuilt after version change:",
                myCurrentVersionCorrupted ? "Indices to be rebuilt after corruption:" : "Indices to be built:"
            );

            myState.freeze();
            FileBasedIndexDataInitializationResult result = new FileBasedIndexDataInitializationResult(
                myState,
                myCurrentVersionCorrupted,
                myOrphanDirtyFilesQueue,
                myOrphanDirtyFilesQueueRecreationReason
            );
            myRegisteredIndexes.setInitializationResult(result); // memory barrier
            myFileBasedIndex.setInitializedState(myState);

            // check if rebuild was requested for any index during registration
            for (ID<?, ?> indexId : myState.getIndexIDs()) {
                try {
                    RebuildStatus.clearIndexIfNecessary(indexId, () -> myFileBasedIndex.clearIndex(indexId));
                }
                catch (StorageException e) {
                    myFileBasedIndex.requestRebuild(indexId);
                    FileBasedIndexImpl.LOG.error(e);
                }
            }

            myFileBasedIndex.registerIndexableSet(new AdditionalIndexableFileSet(), null);
            return result;
        }
        finally {
            myFileBasedIndex.setUpShutDownTask();
            myFileBasedIndex.addStaleIds(myStaleIds);
            myFileBasedIndex.setUpFlusher();
            myRegisteredIndexes.ensureLoadedIndexesUpToDate();
            myRegisteredIndexes.markInitialized(); // this will ensure that all changes to component's state will be visible to other threads
            saveRegisteredIndicesAndDropUnregisteredOnes(myState.getIndexIDs());
        }
    }

    private void showChangedIndexesNotification() {
        Application application = myFileBasedIndex.getApplication();
        if (application.isHeadlessEnvironment() || !Registry.is("ide.showIndexRebuildMessage")) {
            return;
        }

        String rebuildNotification;
        if (myCurrentVersionCorrupted) {
            rebuildNotification = IndexingLocalize.indexCorruptedNotificationText().get();
        }
        else if (myRegistrationResultSink.hasChangedIndexes()) {
            rebuildNotification = IndexingLocalize.indexFormatChangedNotificationText(myRegistrationResultSink.changedIndices()).get();
        }
        else {
            return;
        }

        myFileBasedIndex.getNotificationService().newInfo(FileBasedIndexImpl.NOTIFICATIONS)
            .title(IndexingLocalize.indexRebuildNotificationTitle())
            .content(LocalizeValue.localizeTODO(rebuildNotification))
            .notify(null);
    }

    private static void saveRegisteredIndicesAndDropUnregisteredOnes(Collection<? extends ID<?, ?>> ids) {
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

    static final class FileBasedIndexDataInitializationResult {
        final IndexConfiguration myState;
        final boolean myWasCorrupted;
        final OrphanDirtyFilesQueue myOrphanDirtyFilesQueue;
        final @Nullable OrphanDirtyFilesQueueDiscardReason myOrphanDirtyFilesQueueDiscardReason;

        FileBasedIndexDataInitializationResult(
            IndexConfiguration state,
            boolean currentVersionCorrupted,
            OrphanDirtyFilesQueue queue,
            @Nullable OrphanDirtyFilesQueueDiscardReason orphanDirtyFilesQueueDiscardReason
        ) {
            myState = state;
            myWasCorrupted = currentVersionCorrupted;
            myOrphanDirtyFilesQueue = queue;
            myOrphanDirtyFilesQueueDiscardReason = orphanDirtyFilesQueueDiscardReason;
        }
    }
}
