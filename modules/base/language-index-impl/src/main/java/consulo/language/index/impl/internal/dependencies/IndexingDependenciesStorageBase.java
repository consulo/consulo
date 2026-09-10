// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.dependencies;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public abstract class IndexingDependenciesStorageBase {
    public interface StorageFactory<T extends IndexingDependenciesStorageBase> {
        T create(FileChannel storage, Path path);
    }

    public interface ByteCountHandler {
        int handle(int bytes) throws IOException;
    }

    public interface VersionMismatchHandler {
        void handle(int expectedVersion, int actualVersion) throws IOException;
    }

    private static final long STORAGE_VERSION_OFFSET = 0L;

    public static final long FIRST_UNUSED_OFFSET = STORAGE_VERSION_OFFSET + Integer.BYTES;

    public static <T extends IndexingDependenciesStorageBase> T openOrInit(Path path, StorageFactory<T> storageFactory) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
        T storage = storageFactory.create(channel, path);

        storage.initIfNotInitialized();

        return storage;
    }

    protected final FileChannel myStorage;
    protected final Path myStoragePath;

    private final int myStorageVersion;

    protected IndexingDependenciesStorageBase(FileChannel storage, Path storagePath, int storageVersion) {
        myStorage = storage;
        myStoragePath = storagePath;
        myStorageVersion = storageVersion;
    }

    public boolean isOpen() {
        return myStorage.isOpen();
    }

    public int readIntOrExecute(long offset, ByteCountHandler otherwise) throws IOException {
        ByteBuffer fourBytes = ByteBuffer.allocate(Integer.BYTES);
        int read = myStorage.read(fourBytes.clear(), offset);
        if (read == Integer.BYTES) {
            return fourBytes.rewind().getInt();
        }
        else {
            return otherwise.handle(read);
        }
    }

    public void writeIntOrExecute(long offset, int value, ByteCountHandler otherwise) throws IOException {
        ByteBuffer fourBytes = ByteBuffer.allocate(Integer.BYTES);
        fourBytes.putInt(value);
        int wrote = myStorage.write(fourBytes.rewind(), offset);
        if (wrote != Integer.BYTES) {
            otherwise.handle(wrote);
        }
    }

    public void checkVersion(VersionMismatchHandler onVersionMismatch) throws IOException {
        int actualVersion = readVersion();
        if (actualVersion != myStorageVersion) {
            onVersionMismatch.handle(myStorageVersion, actualVersion);
        }
    }

    private int readVersion() throws IOException {
        return readIntOrExecute(STORAGE_VERSION_OFFSET, bytesRead -> {
            throw new IOException(tooFewBytesReadMsg(bytesRead, "storage version"));
        });
    }

    private void writeVersion() throws IOException {
        writeIntOrExecute(STORAGE_VERSION_OFFSET, myStorageVersion, bytesWritten -> {
            throw new IOException(tooFewBytesWrittenMsg(bytesWritten, "storage version"));
        });
    }

    public void initIfNotInitialized() throws IOException {
        boolean storageNotInitialized = !Files.exists(myStoragePath) || Files.size(myStoragePath) == 0L;
        if (storageNotInitialized) {
            resetStorage();
        }
    }

    public void resetStorage() throws IOException {
        writeVersion();
    }

    public void completeMigration() throws IOException {
        writeVersion();
    }

    public void close() throws IOException {
        myStorage.close();
    }

    public String tooFewBytesWrittenMsg(int bytes, String op) {
        return "Could not write " + op + " (only " + bytes + " bytes written). Storage path: " + myStoragePath;
    }

    public String tooFewBytesReadMsg(int bytes, String op) {
        return "Could not read " + op + " (only " + bytes + " bytes read). Storage path: " + myStoragePath;
    }
}
