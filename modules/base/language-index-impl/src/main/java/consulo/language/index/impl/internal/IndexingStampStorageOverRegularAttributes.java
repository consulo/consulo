// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

import consulo.virtualFileSystem.FileAttribute;
import consulo.virtualFileSystem.internal.FSRecordsProxy;
import org.jspecify.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class IndexingStampStorageOverRegularAttributes implements IndexingStampStorage {
    public static final FileAttribute PERSISTENCE = new FileAttribute("__index_stamps__", 2, false);

    private final FSRecordsProxy myVfs;

    public IndexingStampStorageOverRegularAttributes(FSRecordsProxy vfs) {
        myVfs = vfs;
    }

    @Override
    public void writeTimestamps(int fileId, TimestampsImmutable timestamps) throws IOException {
        try (DataOutputStream out = myVfs.writeAttribute(fileId, PERSISTENCE)) {
            timestamps.writeToStream(out);
        }
    }

    @Override
    public @Nullable TimestampsImmutable readTimestamps(int fileId) {
        try (DataInputStream stream = myVfs.readAttributeWithLock(fileId, PERSISTENCE)) {
            if (stream == null) {
                return null;
            }
            return TimestampsImmutable.readTimestamps(stream);
        }
        catch (IOException e) {
            myVfs.handleError(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        // noop
    }
}
