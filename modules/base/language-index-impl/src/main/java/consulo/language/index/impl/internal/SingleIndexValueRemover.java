// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

import consulo.index.io.ID;
import consulo.language.psi.stub.FileContent;
import consulo.util.concurrent.ConcurrencyUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

public final class SingleIndexValueRemover {
    public final ID<?, ?> indexId;

    private final FileBasedIndexImpl indexImpl;

    private final int inputId;
    private final @Nullable String fileInfo;
    private final FileIndexingResult.ApplicationMode applicationMode;

    /**
     * Time of {@code index.update(inputId, null)}, in nanoseconds
     */
    public long evaluatingValueRemoverTime;

    SingleIndexValueRemover(
        FileBasedIndexImpl indexImpl,
        ID<?, ?> indexId,
        @Nullable VirtualFile file,
        @Nullable FileContent fileContent,
        int inputId,
        FileIndexingResult.ApplicationMode applicationMode
    ) {
        this.indexImpl = indexImpl;
        this.indexId = indexId;
        this.inputId = inputId;
        this.fileInfo = FileBasedIndexImpl.getFileInfoLogString(inputId, file, fileContent);
        this.applicationMode = applicationMode;
    }

    /**
     * Contrary to the {@link SingleIndexValueApplier}, the remover does both 'prepare update' and 'apply update to the index'
     * steps here. This is because for removes {@code InvertedIndex.update(int, null)} is almost trivial, with ~0 cost.
     *
     * @return false in case index update is not necessary or the update has failed
     */
    public boolean remove() {
        if (!RebuildStatus.isOk(indexId) && !indexImpl.myIsUnitTestMode) {
            return false; // the index is scheduled for rebuild, no need to update
        }
        indexImpl.increaseLocalModCount();

        UpdatableIndex<?, ?, FileContent, ?> index = indexImpl.getIndex(indexId);

        try {
            Supplier<Boolean> storageUpdate;
            long startTime = System.nanoTime();
            try {
                storageUpdate = index.update(inputId, null);
            }
            finally {
                this.evaluatingValueRemoverTime = System.nanoTime() - startTime;
            }

            if (indexImpl.runUpdateForPersistentData(storageUpdate)) {
                if (FileBasedIndexImpl.LOG.isTraceEnabled()) {
                    FileBasedIndexImpl.LOG.trace("index " + indexId + " deletion finished for " + fileInfo);
                }
                ConcurrencyUtil.withLock(
                    indexImpl.myReadLock,
                    () -> {
                        index.setUnindexedStateForFile(inputId);
                    }
                );
            }
            return true;
        }
        catch (RuntimeException exception) {
            indexImpl.requestIndexRebuildOnException(exception, indexId);
            return false;
        }
    }

    @Override
    public String toString() {
        return "SingleIndexValueRemover{" +
            "indexId=" + indexId +
            ", inputId=" + inputId +
            ", fileInfo='" + fileInfo + '\'' +
            ", applicationMode =" + applicationMode +
            '}';
    }
}
