// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

import consulo.application.internal.ProgressIndicatorUtils;
import consulo.component.ProcessCanceledException;
import consulo.language.index.impl.internal.FileBasedIndexDataInitialization.FileBasedIndexDataInitializationResult;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RegisteredIndexes {
    private final FileBasedIndexImpl myFileBasedIndex;
    private final Future<FileBasedIndexDataInitializationResult> myStateFuture;

    private volatile boolean myInitialized;

    private volatile @Nullable FileBasedIndexDataInitializationResult myInitResult;
    private volatile @Nullable Future<?> myAllIndicesInitializedFuture;

    private final AtomicBoolean myShutdownPerformed = new AtomicBoolean(false);

    RegisteredIndexes(FileBasedIndexImpl fileBasedIndex) {
        myFileBasedIndex = fileBasedIndex;
        myStateFuture = IndexInfrastructure.submitGenesisTask(new FileBasedIndexDataInitialization(fileBasedIndex, this));
    }

    boolean performShutdown() {
        return myShutdownPerformed.compareAndSet(false, true);
    }

    boolean isShutdownPerformed() {
        return myShutdownPerformed.get();
    }

    void setInitializationResult(FileBasedIndexDataInitializationResult result) {
        myInitResult = result;
    }

    @Nullable
    IndexConfiguration getState() {
        FileBasedIndexDataInitializationResult result = myInitResult;
        return result == null ? null : result.myState;
    }

    IndexConfiguration getConfigurationState() {
        return getInitializationResult().myState;
    }

    boolean getWasCorrupted() {
        return getInitializationResult().myWasCorrupted;
    }

    OrphanDirtyFilesQueue getOrphanDirtyFilesQueue() {
        return getInitializationResult().myOrphanDirtyFilesQueue;
    }

    @Nullable
    OrphanDirtyFilesQueueDiscardReason getOrphanDirtyFilesQueueDiscardReason() {
        return getInitializationResult().myOrphanDirtyFilesQueueDiscardReason;
    }

    private FileBasedIndexDataInitializationResult getInitializationResult() {
        FileBasedIndexDataInitializationResult result = myInitResult; // memory barrier
        if (result == null) {
            try {
                myInitResult = result = ProgressIndicatorUtils.awaitWithCheckCanceled(myStateFuture);
            }
            catch (ProcessCanceledException ex) {
                throw ex;
            }
            catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
        return result;
    }

    void waitUntilAllIndicesAreInitialized() {
        waitUntilIndicesAreInitialized();
        Future<?> allIndicesInitializedFuture = myAllIndicesInitializedFuture;
        if (allIndicesInitializedFuture != null) {
            ProgressIndicatorUtils.awaitWithCheckCanceled(allIndicesInitializedFuture);
        }
    }

    void waitUntilIndicesAreInitialized() {
        ProgressIndicatorUtils.awaitWithCheckCanceled(myStateFuture);
    }

    void markInitialized() {
        myInitialized = true;
    }

    void ensureLoadedIndexesUpToDate() {
        myAllIndicesInitializedFuture = IndexInfrastructure.submitGenesisTask(() -> {
            if (!myShutdownPerformed.get()) {
                myFileBasedIndex.ensureStaleIdsDeleted();
                myFileBasedIndex.getChangedFilesCollector().ensureUpToDateAsync();
            }
            return null;
        });
    }

    boolean areIndexesReady() {
        Future<?> allIndicesInitializedFuture = myAllIndicesInitializedFuture;
        return myStateFuture.isDone() && allIndicesInitializedFuture != null && allIndicesInitializedFuture.isDone();
    }

    public boolean isInitialized() {
        return myInitialized;
    }
}
