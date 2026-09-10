// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.dependencies;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

public class ProjectIndexingDependenciesStorage extends IndexingDependenciesStorageBase {
    private static final int CURRENT_STORAGE_VERSION = 1;
    private static final boolean DEFAULT_INCOMPLETE_SCANNING_MARK = false;
    private static final long INCOMPLETE_SCANNING_MARK_OFFSET = FIRST_UNUSED_OFFSET;

    public static final int DEFAULT_APP_INDEXING_REQUEST_ID_OF_LAST_COMPLETED_SCANNING = -1;

    private static final long APP_INDEXING_REQUEST_ID_OF_LAST_COMPLETED_SCANNING_OFFSET = INCOMPLETE_SCANNING_MARK_OFFSET + Integer.BYTES;
    private static final long FILE_SIZE = APP_INDEXING_REQUEST_ID_OF_LAST_COMPLETED_SCANNING_OFFSET + Integer.BYTES;

    public static ProjectIndexingDependenciesStorage openOrInit(Path path) throws IOException {
        return openOrInit(path, ProjectIndexingDependenciesStorage::new);
    }

    public ProjectIndexingDependenciesStorage(FileChannel storage, Path storagePath) {
        super(storage, storagePath, CURRENT_STORAGE_VERSION);
    }

    @Override
    public void resetStorage() throws IOException {
        synchronized (myStorage) {
            super.resetStorage();
            writeIncompleteScanningMark(DEFAULT_INCOMPLETE_SCANNING_MARK);
            writeAppIndexingRequestIdOfLastScanning(DEFAULT_APP_INDEXING_REQUEST_ID_OF_LAST_COMPLETED_SCANNING);
            myStorage.truncate(FILE_SIZE);
            myStorage.force(false);
        }
    }

    public boolean readIncompleteScanningMark() throws IOException {
        synchronized (myStorage) {
            return readIntOrExecute(INCOMPLETE_SCANNING_MARK_OFFSET, bytesRead -> {
                throw new IOException(tooFewBytesReadMsg(bytesRead, "incomplete scanning mark"));
            }) != 0;
        }
    }

    public void writeIncompleteScanningMark(boolean mark) throws IOException {
        synchronized (myStorage) {
            writeIntOrExecute(INCOMPLETE_SCANNING_MARK_OFFSET, mark ? 1 : 0, bytesWritten -> {
                throw new IOException(tooFewBytesWrittenMsg(bytesWritten, "incomplete scanning mark"));
            });
        }
    }

    public void writeAppIndexingRequestIdOfLastScanning(int appIndexingRequestId) throws IOException {
        synchronized (myStorage) {
            writeIntOrExecute(APP_INDEXING_REQUEST_ID_OF_LAST_COMPLETED_SCANNING_OFFSET, appIndexingRequestId, bytesWritten -> {
                throw new IOException(tooFewBytesWrittenMsg(bytesWritten, "app indexing request id"));
            });
            myStorage.force(false);
        }
    }

    public int readAppIndexingRequestIdOfLastScanning() throws IOException {
        synchronized (myStorage) {
            return readIntOrExecute(APP_INDEXING_REQUEST_ID_OF_LAST_COMPLETED_SCANNING_OFFSET, bytesRead -> {
                throw new IOException(tooFewBytesReadMsg(bytesRead, "app indexing request id"));
            });
        }
    }
}
