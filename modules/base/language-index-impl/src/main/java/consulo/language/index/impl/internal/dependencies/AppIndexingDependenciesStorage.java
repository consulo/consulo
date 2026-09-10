// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.dependencies;

import consulo.language.index.impl.internal.dependencies.IndexingDependenciesFingerprint.FingerprintImpl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

public class AppIndexingDependenciesStorage extends IndexingDependenciesStorageBase {
    private static final int CURRENT_STORAGE_VERSION = 1;
    private static final int DEFAULT_INDEXING_REQUEST = 1; // not 0, because all the files should be scanned at least once
    private static final long INDEXING_REQUEST_OFFSET = FIRST_UNUSED_OFFSET;
    private static final int INDEXING_REQUEST_SIZE = Integer.BYTES;
    private static final long APP_FINGERPRINT_OFFSET = INDEXING_REQUEST_OFFSET + INDEXING_REQUEST_SIZE;
    private static final int APP_FINGERPRINT_SIZE = IndexingDependenciesFingerprint.FINGERPRINT_SIZE_IN_BYTES;
    private static final long FILE_SIZE = APP_FINGERPRINT_OFFSET + APP_FINGERPRINT_SIZE;

    private static final FingerprintImpl DEFAULT_FINGERPRINT = IndexingDependenciesFingerprint.NULL_FINGERPRINT;

    public static AppIndexingDependenciesStorage openOrInit(Path path) throws IOException {
        return openOrInit(path, AppIndexingDependenciesStorage::new);
    }

    public AppIndexingDependenciesStorage(FileChannel storage, Path storagePath) {
        super(storage, storagePath, CURRENT_STORAGE_VERSION);
    }

    @Override
    public void resetStorage() throws IOException {
        synchronized (myStorage) {
            super.resetStorage();
            writeRequestId(DEFAULT_INDEXING_REQUEST);
            writeAppFingerprint(DEFAULT_FINGERPRINT);
            myStorage.truncate(FILE_SIZE);
            myStorage.force(false);
        }
    }

    public int readRequestId() throws IOException {
        synchronized (myStorage) {
            return readIntOrExecute(INDEXING_REQUEST_OFFSET, bytesRead -> {
                throw new IOException(tooFewBytesReadMsg(bytesRead, "indexing stamp"));
            });
        }
    }

    public void writeRequestId(int requestId) throws IOException {
        synchronized (myStorage) {
            writeIntOrExecute(INDEXING_REQUEST_OFFSET, requestId, bytesWritten -> {
                throw new IOException(tooFewBytesWrittenMsg(bytesWritten, "indexing stamp"));
            });
            myStorage.force(false);
        }
    }

    public void writeAppFingerprint(FingerprintImpl fingerprint) throws IOException {
        synchronized (myStorage) {
            int bytesWritten = myStorage.write(fingerprint.toByteBuffer(), APP_FINGERPRINT_OFFSET);
            if (bytesWritten != APP_FINGERPRINT_SIZE) {
                throw new IOException(tooFewBytesWrittenMsg(bytesWritten, "indexing stamp"));
            }
            myStorage.force(false);
        }
    }

    public FingerprintImpl readAppFingerprint() throws IOException {
        synchronized (myStorage) {
            ByteBuffer buffer = ByteBuffer.allocate(APP_FINGERPRINT_SIZE);
            int bytesRead = myStorage.read(buffer, APP_FINGERPRINT_OFFSET);
            if (bytesRead != APP_FINGERPRINT_SIZE) {
                throw new IOException(tooFewBytesReadMsg(bytesRead, "indexing stamp"));
            }

            return FingerprintImpl.of(buffer);
        }
    }
}
