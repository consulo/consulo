// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.virtualFileSystem.impl.internal.mapped;

import consulo.container.boot.ContainerPathManager;
import consulo.index.io.PagedFileStorage;
import consulo.index.io.ResizeableMappedFile;
import consulo.index.io.data.IOUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import consulo.virtualFileSystem.impl.internal.FSRecords;
import consulo.virtualFileSystem.internal.CleanableStorage;
import consulo.virtualFileSystem.internal.FSRecordsProxy;
import consulo.virtualFileSystem.internal.Unmappable;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.LongUnaryOperator;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Helper for creating {@link VirtualFileWithId}-associated storages based on memory-mapped file.
 * <p/>
 * Basic idea is: storage is a fixed-size header and a set of fixed-size records, one record per each
 * {@link VirtualFileWithId} known to VFS.
 * Header is up to 64 bytes, first 4 bytes is a version, next 8 is a VFS tag (creation timestamp),
 * the remaining 52 bytes could be used as needed (see {@link #writeIntHeaderField(int, int)}, {@link #writeLongHeaderField(int, long)}).
 * Record (row) is an arbitrary but fixed size, given in ctor (bytesPerRow).
 * <p/>
 * Keep in mind that CPUs universally support atomic/volatile access to N-bytes word only for N-aligned
 * offsets. Which means: if you want int64 field in your record, you need to compose record in such a way
 * this field's offset is always 8-byte-aligned. Which means bytesPerRow must be a factor of 8 and a
 * particular field offset in the record must also be a factor of 8.
 * <p/>
 * This is 'helper', not full-fledged implementation, nor a good abstraction/encapsulation -- i.e.
 * one still needs to understand the underlying code.
 */
public final class MappedFileStorageHelper implements Closeable, CleanableStorage, Unmappable {
    /**
     * Keeps a registry of all {@link MappedFileStorageHelper} -- prevents creating duplicates, i.e. >1 storage
     * for the same path.
     */
    //@GuardedBy(ourStoragesRegistry)
    private static final Map<Path, MappedFileStorageHelper> ourStoragesRegistry = new HashMap<>();

    private static final String FAST_ATTRIBUTES_DIR_NAME = "fastAttributes";

    private static final int CLEAR_CHUNK_SIZE = 64 * 1024;

    private static final PagedFileStorage.StorageLockContext ourStorageLockContext = new PagedFileStorage.StorageLockContext(false);

    public static MappedFileStorageHelper openHelper(FSRecordsProxy vfs,
                                                     Path absoluteStoragePath,
                                                     int bytesPerRow,
                                                     boolean checkFileIdsBelowMax) throws IOException {
        if (!absoluteStoragePath.isAbsolute()) {
            throw new IllegalArgumentException("absoluteStoragePath(=" + absoluteStoragePath + ") is not absolute");
        }
        if (bytesPerRow <= 0) {
            throw new IllegalArgumentException("bytesPerRow(=" + bytesPerRow + ") must be >0");
        }
        Path storageDir = absoluteStoragePath.getParent().normalize();

        Files.createDirectories(storageDir);

        synchronized (ourStoragesRegistry) {
            MappedFileStorageHelper alreadyExistingHelper = ourStoragesRegistry.get(absoluteStoragePath);
            if (alreadyExistingHelper != null && alreadyExistingHelper.isOpen()) {
                if (alreadyExistingHelper.myBytesPerRow != bytesPerRow) {
                    throw new IllegalStateException(
                        "StorageHelper[" + absoluteStoragePath + "] is already registered, " +
                            "but with .bytesPerRow(=" + bytesPerRow + ") != storage.bytesPerRow(=" + alreadyExistingHelper.myBytesPerRow + ")"
                    );
                }
                return alreadyExistingHelper;
            }

            ResizeableMappedFile mappedFile = new ResizeableMappedFile(
                absoluteStoragePath.toFile(),
                DEFAULT_PAGE_SIZE,
                ourStorageLockContext,
                DEFAULT_PAGE_SIZE,
                true,
                IOUtil.BYTE_BUFFERS_USE_NATIVE_BYTE_ORDER
            );
            try {
                MappedFileStorageHelper storageHelper = new MappedFileStorageHelper(
                    absoluteStoragePath,
                    mappedFile,
                    bytesPerRow,
                    vfs::getMaxId,
                    checkFileIdsBelowMax
                );
                ourStoragesRegistry.put(absoluteStoragePath, storageHelper);
                return storageHelper;
            }
            catch (Throwable t) {
                try {
                    mappedFile.close();
                }
                catch (Throwable closeEx) {
                    t.addSuppressed(closeEx);
                }
                throw t;
            }
        }
    }

    public static MappedFileStorageHelper openHelper(FSRecordsProxy vfs,
                                                     String storageName,
                                                     int bytesPerRow) throws IOException {
        return openHelper(vfs, storageName, bytesPerRow, true);
    }

    public static MappedFileStorageHelper openHelper(FSRecordsProxy vfs,
                                                     String storageName,
                                                     int bytesPerRow,
                                                     boolean checkFileIdsBelowMax) throws IOException {
        Path fastAttributesDir = ContainerPathManager.get().getIndexRoot().toPath().resolve(FAST_ATTRIBUTES_DIR_NAME);
        Path storagePath = fastAttributesDir.resolve(storageName).toAbsolutePath();
        return openHelper(vfs, storagePath, bytesPerRow, checkFileIdsBelowMax);
    }

    public static MappedFileStorageHelper openHelperAndVerifyVersions(FSRecordsProxy vfs,
                                                                      String storageName,
                                                                      int storageFormatVersion,
                                                                      int bytesPerRow) throws IOException {
        return openHelperAndVerifyVersions(vfs, storageName, storageFormatVersion, bytesPerRow, true);
    }

    public static MappedFileStorageHelper openHelperAndVerifyVersions(FSRecordsProxy vfs,
                                                                      String storageName,
                                                                      int storageFormatVersion,
                                                                      int bytesPerRow,
                                                                      boolean checkFileIdBelowMax) throws IOException {
        MappedFileStorageHelper helper = openHelper(vfs, storageName, bytesPerRow, checkFileIdBelowMax);
        verifyTagsAndVersions(helper, vfs.getCreationTimestamp(), storageFormatVersion);
        return helper;
    }

    public static MappedFileStorageHelper openHelperAndVerifyVersions(FSRecordsProxy vfs,
                                                                      Path absoluteStoragePath,
                                                                      int storageFormatVersion,
                                                                      int bytesPerRow,
                                                                      boolean checkFileIdBelowMax) throws IOException {
        MappedFileStorageHelper helper = openHelper(vfs, absoluteStoragePath, bytesPerRow, checkFileIdBelowMax);
        verifyTagsAndVersions(helper, vfs.getCreationTimestamp(), storageFormatVersion);
        return helper;
    }

    public static void verifyTagsAndVersions(MappedFileStorageHelper helper,
                                             long vfsCreationTag,
                                             int storageFormatVersion) throws IOException {
        if (helper.getVFSCreationTag() != vfsCreationTag) {
            helper.clear();
        }
        if (helper.getVersion() != storageFormatVersion) {
            helper.clear();
        }
        helper.setVFSCreationTag(vfsCreationTag);
        helper.setVersion(storageFormatVersion);
    }

    public static Map<Path, MappedFileStorageHelper> registeredStorages() {
        return ourStoragesRegistry;
    }

    //MAYBE:
    //    1) Versioning: better have 2 versions: INTERNAL_VERSION (i.e. header format version, managed by the class
    //       itself) and ATTRIBUTE_VERSION (arbitrary version tag managed bt client to track their way of use the attribute)
    //       It is OK to use 2 bytes for each version, and combine them both to a 4 byte header field.
    //    2) Default value: mapped file has 0 as default value. We could stay with it (i.e. define 0 as default value
    //       on API level), or allow to set defaultValue in ctor.
    //       To implement second option we need to store defaultValue in the header, and apply (0 <=> defaultValue)
    //       replacement on each read&write op. Now, it could be an overkill -- maybe default=0 is OK for the most
    //       clients, and no need for complications like that.
    //    3) allocatedRecordsCount: seems like here we don't need that number, because valid fileId range is limited
    //       by VFS -- anything in [1..VFS.maxAllocatedID] is valid, regardless of was apt slot allocated already, or
    //       not.
    //    4) connectionStatus: seems like we don't need it either, because updates are atomic, hence every saved state
    //       is at least self-consistent -- particular file's attribute was either updated or not, but not partially
    //       updated

    public static final class HeaderLayout {
        public static final int VERSION_OFFSET = 0;
        /** Next field is int64, must be 64-aligned for volatile access, so insert additional int32 */
        public static final int RESERVED_OFFSET = VERSION_OFFSET + Integer.BYTES;

        public static final int VFS_CREATION_TIMESTAMP_OFFSET = RESERVED_OFFSET + Integer.BYTES;

        public static final int FIRST_FREE_FIELD = VFS_CREATION_TIMESTAMP_OFFSET + Long.BYTES;

        //reserve [8 x int64] just for the case
        public static final int HEADER_SIZE = 8 * Long.BYTES;

        private HeaderLayout() {
        }
    }

    public static final int DEFAULT_PAGE_SIZE = 4 * 1024 * 1024;

    private final int myBytesPerRow;

    private final Path myStoragePath;

    private final ResizeableMappedFile myStorage;

    private final IntSupplier myMaxAllocatedFileIdSupplier;

    /**
     * If true, fileId arg of every method is checked to be in (0..maxId].
     * If false, fileId arg is checked only to be >0 (negative fileId ruins addressing schema).
     * FIXME RC: it MUST be set true all the time -- it should _never_ be _any_ fileId in use outside (0..maxAllocatedId].
     * False is a temporary backward compatibility option: it seems like in some use-cases somehow
     * the constraint is violated, but we have no time/hands to to find out why.
     */
    private final boolean myCheckFileIdsBelowMax;

    private final ReadWriteLock myStorageLock = new ReentrantReadWriteLock();

    private volatile boolean myClosed;

    private MappedFileStorageHelper(Path storagePath,
                                    ResizeableMappedFile storage,
                                    int bytesPerRow,
                                    IntSupplier maxRowsSupplier,
                                    boolean checkFileIdsBelowMax) {
        if (DEFAULT_PAGE_SIZE % bytesPerRow != 0) {
            throw new IllegalArgumentException(
                "bytesPerRow(=" + bytesPerRow + ") is not aligned with pageSize(=" + DEFAULT_PAGE_SIZE + "): rows must be page-aligned");
        }

        myStoragePath = storagePath;
        myStorage = storage;
        myBytesPerRow = bytesPerRow;
        myMaxAllocatedFileIdSupplier = maxRowsSupplier;
        myCheckFileIdsBelowMax = checkFileIdsBelowMax;
    }

    public boolean isOpen() {
        return !myClosed;
    }

    public Path storagePath() {
        return myStoragePath;
    }

    public int getVersion() throws IOException {
        return readIntHeaderField(HeaderLayout.VERSION_OFFSET);
    }

    public void setVersion(int version) throws IOException {
        writeIntHeaderField(HeaderLayout.VERSION_OFFSET, version);
    }

    public long getVFSCreationTag() throws IOException {
        return readLongHeaderField(HeaderLayout.VFS_CREATION_TIMESTAMP_OFFSET);
    }

    public void setVFSCreationTag(long vfsCreationTag) throws IOException {
        writeLongHeaderField(HeaderLayout.VFS_CREATION_TIMESTAMP_OFFSET, vfsCreationTag);
    }

    public int bytesPerRow() {
        return myBytesPerRow;
    }

    public int readIntField(VirtualFile vFile,
                            int fieldOffset) throws IOException {
        int fileId = extractFileId(vFile);
        return readIntField(fileId, fieldOffset);
    }

    public void writeIntField(VirtualFile vFile,
                              int fieldOffset,
                              int value) throws IOException {
        int fileId = extractFileId(vFile);
        writeIntField(fileId, fieldOffset, value);
    }

    public int updateIntField(VirtualFile vFile,
                              int fieldOffset,
                              IntUnaryOperator updateOperator) throws IOException {
        int fileId = extractFileId(vFile);
        return updateIntField(fileId, fieldOffset, updateOperator);
    }

    public long readLongField(VirtualFile vFile,
                              int fieldOffset) throws IOException {
        int fileId = extractFileId(vFile);
        return readLongField(fileId, fieldOffset);
    }

    public void writeLongField(VirtualFile vFile,
                               int fieldOffset,
                               long value) throws IOException {
        int fileId = extractFileId(vFile);
        writeLongField(fileId, fieldOffset, value);
    }

    public long updateLongField(VirtualFile vFile,
                                int fieldOffset,
                                LongUnaryOperator updateOperator) throws IOException {
        int fileId = extractFileId(vFile);
        return updateLongField(fileId, fieldOffset, updateOperator);
    }

    /**
     * Clears all storage content -- headers and data -- as-if storage was just created from 0.
     * It means all the fields have their default values (=0)
     * BEWARE: memory semantics of clear implementation is currently 'plain write', so it doesn't work
     * reliable with 'volatile' semantics of other fields accessors
     */
    public void clear() throws IOException {
        clearImpl(/*clearHeaders: */ true);
    }

    /**
     * Clears all storage records, but don't touch headers.
     * All records fields will have their default values (=0)
     * BEWARE: memory semantics of clear implementation is currently 'plain write', so it doesn't work
     * reliable with 'volatile' semantics of other fields accessors
     */
    public void clearRecords() throws IOException {
        clearImpl(/*clearHeaders: */ false);
    }

    private void clearImpl(boolean clearHeaders) throws IOException {
        long startOffsetInFile = clearHeaders ? 0 : HeaderLayout.HEADER_SIZE;
        //IDEA-330224: It is important to zeroize until the EOF, not until the current maxAllocatedID
        // rows, because storage could be created and filled with previous VFS there maxAllocatedID=1e6,
        // but now VFS rebuilds itself, and maxAllocatedID=1e3 -- and there are rows [1e3..1e6] filled
        // with outdated data, which won't be cleared
        myStorageLock.writeLock().lock();
        try {
            long length = Math.max(myStorage.length(), myStorage.getPagedFileStorage().length());
            if (length <= startOffsetInFile) {
                return;
            }

            byte[] zeroes = new byte[CLEAR_CHUNK_SIZE];
            long offset = startOffsetInFile;
            while (offset < length) {
                int chunkLength = (int) Math.min(zeroes.length, length - offset);
                myStorage.put(offset, zeroes, 0, chunkLength);
                offset += chunkLength;
            }
        }
        finally {
            myStorageLock.writeLock().unlock();
        }
    }

    /** It should be used in a rare occasions only */
    public void fsync() throws IOException {
        myStorage.force();
    }

    @Override
    public void close() throws IOException {
        //We don't fsync() by default on close -- leave it to OS to decide when to flush the pages
        synchronized (ourStoragesRegistry) {
            if (myClosed) {
                return;
            }
            myClosed = true;
            myStorage.close();

            ourStoragesRegistry.remove(myStoragePath);
        }
    }

    @Override
    public void closeAndUnsafelyUnmap() throws IOException {
        close();
    }

    /** Closes the file, releases the mapped buffers, and 'make the best effort' to delete the file. */
    @Override
    public void closeAndClean() throws IOException {
        closeAndUnsafelyUnmap();
        IOUtil.deleteAllFilesStartingWith(myStoragePath.toFile());
    }

    @Override
    public String toString() {
        return "MappedFileStorageHelper[" + myStoragePath + "]";
    }

    public short readShortField(int fileId,
                                int fieldOffsetInRow) throws IOException {
        long offsetInFile = toOffsetInFile(fileId) + fieldOffsetInRow;
        myStorageLock.readLock().lock();
        try {
            if (offsetInFile + Short.BYTES > myStorage.length()) {
                return 0;
            }
            return myStorage.getShort(offsetInFile);
        }
        finally {
            myStorageLock.readLock().unlock();
        }
    }

    public void writeShortField(int fileId,
                                int fieldOffsetInRow,
                                short attributeValue) throws IOException {
        long offsetInFile = toOffsetInFile(fileId) + fieldOffsetInRow;
        myStorageLock.writeLock().lock();
        try {
            myStorage.putShort(offsetInFile, attributeValue);
        }
        finally {
            myStorageLock.writeLock().unlock();
        }
    }

    public int readIntField(int fileId,
                            int fieldOffsetInRow) throws IOException {
        long offsetInFile = toOffsetInFile(fileId) + fieldOffsetInRow;
        return readIntAt(offsetInFile);
    }

    public void writeIntField(int fileId,
                              int fieldOffsetInRow,
                              int attributeValue) throws IOException {
        long offsetInFile = toOffsetInFile(fileId) + fieldOffsetInRow;
        myStorageLock.writeLock().lock();
        try {
            myStorage.putInt(offsetInFile, attributeValue);
        }
        finally {
            myStorageLock.writeLock().unlock();
        }
    }

    public int updateIntField(int fileId,
                              int fieldOffsetInRow,
                              IntUnaryOperator updateOperator) throws IOException {
        long offsetInFile = toOffsetInFile(fileId) + fieldOffsetInRow;
        myStorageLock.writeLock().lock();
        try {
            int currentValue = readIntAt(offsetInFile);
            int newValue = updateOperator.applyAsInt(currentValue);
            myStorage.putInt(offsetInFile, newValue);
            return currentValue;
        }
        finally {
            myStorageLock.writeLock().unlock();
        }
    }

    public long readLongField(int fileId,
                              int fieldOffsetInRow) throws IOException {
        long offsetInFile = toOffsetInFile(fileId) + fieldOffsetInRow;
        return readLongAt(offsetInFile);
    }

    public void writeLongField(int fileId,
                               int fieldOffsetInRow,
                               long attributeValue) throws IOException {
        long offsetInFile = toOffsetInFile(fileId) + fieldOffsetInRow;
        myStorageLock.writeLock().lock();
        try {
            myStorage.putLong(offsetInFile, attributeValue);
        }
        finally {
            myStorageLock.writeLock().unlock();
        }
    }

    public long updateLongField(int fileId,
                                int fieldOffsetInRow,
                                LongUnaryOperator updateOperator) throws IOException {
        long offsetInFile = toOffsetInFile(fileId) + fieldOffsetInRow;
        myStorageLock.writeLock().lock();
        try {
            long currentValue = readLongAt(offsetInFile);
            long newValue = updateOperator.applyAsLong(currentValue);
            myStorage.putLong(offsetInFile, newValue);
            return currentValue;
        }
        finally {
            myStorageLock.writeLock().unlock();
        }
    }

    //MAYBE RC: add accessors for headerShortField, headerByteField?

    public int readIntHeaderField(int headerRelativeOffset) throws IOException {
        checkHeaderFieldOffset(headerRelativeOffset);
        return readIntAt(headerRelativeOffset);
    }

    public long readLongHeaderField(int headerRelativeOffset) throws IOException {
        checkHeaderFieldOffset(headerRelativeOffset);
        return readLongAt(headerRelativeOffset);
    }

    public void writeIntHeaderField(int headerRelativeOffset,
                                    int headerFieldValue) throws IOException {
        checkHeaderFieldOffset(headerRelativeOffset);
        myStorageLock.writeLock().lock();
        try {
            myStorage.putInt(headerRelativeOffset, headerFieldValue);
        }
        finally {
            myStorageLock.writeLock().unlock();
        }
    }

    public void writeLongHeaderField(int headerRelativeOffset,
                                     long headerFieldValue) throws IOException {
        checkHeaderFieldOffset(headerRelativeOffset);
        myStorageLock.writeLock().lock();
        try {
            myStorage.putLong(headerRelativeOffset, headerFieldValue);
        }
        finally {
            myStorageLock.writeLock().unlock();
        }
    }

    // ============== implementation: ======================================================================

    private int readIntAt(long offsetInFile) {
        myStorageLock.readLock().lock();
        try {
            if (offsetInFile + Integer.BYTES > myStorage.length()) {
                return 0;
            }
            return myStorage.getInt(offsetInFile);
        }
        finally {
            myStorageLock.readLock().unlock();
        }
    }

    private long readLongAt(long offsetInFile) {
        myStorageLock.readLock().lock();
        try {
            if (offsetInFile + Long.BYTES > myStorage.length()) {
                return 0;
            }
            return myStorage.getLong(offsetInFile);
        }
        finally {
            myStorageLock.readLock().unlock();
        }
    }

    private long toOffsetInFile(int fileId) {
        checkFileIdValid(fileId);
        long offsetInFile = (fileId - FSRecords.ROOT_FILE_ID) * (long) myBytesPerRow + HeaderLayout.HEADER_SIZE;
        if (offsetInFile < 0) {
            throw new AssertionError("fileId(=" + fileId + ") x bytesPerRow(=" + myBytesPerRow + ") is too big: " +
                "offsetInFile(=" + offsetInFile + ") must be positive");
        }
        return offsetInFile;
    }

    private void checkFileIdValid(int fileId) {
        if (myCheckFileIdsBelowMax) {
            int maxAllocatedID = maxAllocatedFileID();
            if (fileId < FSRecords.ROOT_FILE_ID || fileId > maxAllocatedID) {
                throw new IllegalArgumentException(
                    "fileId[#" + fileId + "] is outside of allocated range [" + FSRecords.ROOT_FILE_ID + ".." + maxAllocatedID + "]");
            }
        }
        else {
            if (fileId < FSRecords.ROOT_FILE_ID) {
                throw new IllegalArgumentException(
                    "fileId[#" + fileId + "] is invalid, must be >=" + FSRecords.ROOT_FILE_ID);
            }
        }
    }

    private int maxAllocatedFileID() {
        return myMaxAllocatedFileIdSupplier.getAsInt();
    }

    private static void checkHeaderFieldOffset(int headerRelativeOffset) {
        if (headerRelativeOffset >= HeaderLayout.HEADER_SIZE) {
            throw new IllegalArgumentException(
                "Header offset(=" + headerRelativeOffset + ") is outside of header[0.." + HeaderLayout.HEADER_SIZE + ")");
        }
    }

    private static int extractFileId(VirtualFile vFile) {
        if (!(vFile instanceof VirtualFileWithId fileWithId)) {
            throw new IllegalArgumentException(vFile + " must be VirtualFileWithId");
        }

        return fileWithId.getId();
    }
}
