// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

import consulo.index.io.ID;
import consulo.language.psi.stub.FileContent;
import consulo.util.concurrent.ConcurrencyUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

public final class SingleIndexValueApplier<FileIndexMetaData> {
    public final ID<?, ?> indexId;

    private final FileBasedIndexImpl indexImpl;

    private final int inputId;

    private final @Nullable FileIndexMetaData fileIndexMetaData;
    private final Supplier<Boolean> storageUpdate;
    private final String fileInfo;
    private final boolean isMock;

    /**
     * Time of {@code index.update(inputId, currentFC)}, in nanoseconds
     */
    public final long evaluatingIndexValueApplierTime;

    SingleIndexValueApplier(
        FileBasedIndexImpl index,
        ID<?, ?> indexId,
        int inputId,
        @Nullable FileIndexMetaData fileIndexMetaData,
        Supplier<Boolean> update,
        VirtualFile file,
        FileContent currentFC,
        long evaluatingIndexValueApplierTime
    ) {
        indexImpl = index;

        this.indexId = indexId;
        this.inputId = inputId;

        this.fileIndexMetaData = fileIndexMetaData;
        this.evaluatingIndexValueApplierTime = evaluatingIndexValueApplierTime;
        storageUpdate = update;
        fileInfo = FileBasedIndexImpl.getFileInfoLogString(inputId, file, currentFC);
        isMock = FileBasedIndexImpl.isMock(currentFC.getFile());
    }

    public boolean wasIndexProvidedByExtension() {
        return false;
    }

    public boolean apply() {
        try {
            return doApply();
        }
        catch (RuntimeException exception) {
            indexImpl.requestIndexRebuildOnException(exception, indexId);
            return false;
        }
    }

    private boolean doApply() {
        if (indexImpl.runUpdateForPersistentData(storageUpdate)) {
            if (FileBasedIndexImpl.LOG.isTraceEnabled()) {
                FileBasedIndexImpl.LOG.trace("index " + indexId + " update finished for " + fileInfo);
            }
            if (!isMock) {
                ConcurrencyUtil.withLock(
                    indexImpl.myReadLock,
                    () -> {
                        @SuppressWarnings("unchecked")
                        UpdatableIndex<?, ?, FileContent, FileIndexMetaData> index =
                            (UpdatableIndex<?, ?, FileContent, FileIndexMetaData>) indexImpl.getIndex(indexId);
                        index.setIndexedStateForFileOnFileIndexMetaData(inputId, fileIndexMetaData, wasIndexProvidedByExtension());
                    }
                );
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "SingleIndexValueApplier{" +
            "indexId=" + indexId +
            ", inputId=" + inputId +
            ", fileInfo='" + fileInfo + '\'' +
            '}';
    }
}
