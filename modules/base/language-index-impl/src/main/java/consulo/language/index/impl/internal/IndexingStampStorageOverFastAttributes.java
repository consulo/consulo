// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

import consulo.container.boot.ContainerPathManager;
import consulo.language.index.impl.internal.perFileVersion.EnumeratedFastFileAttribute;
import consulo.virtualFileSystem.FileAttribute;
import consulo.virtualFileSystem.internal.FSRecordsProxy;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

public final class IndexingStampStorageOverFastAttributes implements IndexingStampStorage {
    public static final FileAttribute PERSISTENCE = new FileAttribute("__fast_index_stamps__", 0, true);

    private final FSRecordsProxy myVfs;
    private final EnumeratedFastFileAttribute<TimestampsImmutable> myPersistence;

    public IndexingStampStorageOverFastAttributes(FSRecordsProxy vfs) throws IOException {
        myVfs = vfs;

        Path dir = ContainerPathManager.get().getIndexRoot().toPath().resolve("fast_index_stamps");

        myPersistence = new EnumeratedFastFileAttribute<>(
            dir,
            PERSISTENCE,
            new TimestampsKeyDescriptor(),
            PersistentIndexingStampEnumerator::createTimestampsEnumerator
        );
    }

    @Override
    public void writeTimestamps(int fileId, TimestampsImmutable timestamps) throws IOException {
        myPersistence.writeEnumerated(fileId, timestamps);
    }

    @Override
    public @Nullable TimestampsImmutable readTimestamps(int fileId) {
        try {
            return myPersistence.readEnumerated(fileId);
        }
        catch (IOException e) {
            //TODO RC: why we blame VFS if there is something wrong with storages unrelated to VFS?
            //         It may make sense for IndexingStampStorageOverRegularAttributes there VFS file attributes
            //         are used to store indexing stamps -- but fast attributes storage are unrelated to VFS
            myVfs.handleError(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        myPersistence.close();
    }
}
