// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.impl.internal;

import consulo.application.Application;
import consulo.application.HeavyProcessLatch;
import consulo.application.util.concurrent.SequentialTaskExecutor;
import consulo.application.util.function.ThrowableComputable;
import consulo.container.boot.ContainerPathManager;
import consulo.index.io.*;
import consulo.index.io.data.DataInputOutputUtil;
import consulo.index.io.data.DataOutputStream;
import consulo.index.io.data.IOUtil;
import consulo.index.io.storage.*;
import consulo.logging.Logger;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.primitive.ints.ConcurrentIntObjectMap;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import consulo.util.io.*;
import consulo.util.lang.BitUtil;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.SystemProperties;
import consulo.util.lang.function.ThrowableRunnable;
import consulo.virtualFileSystem.FileAttribute;
import consulo.virtualFileSystem.impl.internal.entry.VfsDependentEnum;
import consulo.virtualFileSystem.impl.internal.entry.VirtualDirectoryImpl;
import consulo.virtualFileSystem.impl.internal.entry.VirtualFileSystemEntry;
import consulo.virtualFileSystem.internal.CachedFileType;
import consulo.virtualFileSystem.internal.FlushingDaemon;
import consulo.virtualFileSystem.internal.NameId;
import consulo.virtualFileSystem.internal.PersistentFS;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author max
 */
public class FSRecords {
    private static final Logger LOG = Logger.getInstance(FSRecords.class);

    public static final boolean WE_HAVE_CONTENT_HASHES = SystemProperties.getBooleanProperty("idea.share.contents", true);
    public static final String VFS_FILES_EXTENSION = System.getProperty("idea.vfs.files.extension", ".dat");

    private static final boolean lazyVfsDataCleaning = SystemProperties.getBooleanProperty("idea.lazy.vfs.data.cleaning", true);
    private static final boolean backgroundVfsFlush = SystemProperties.getBooleanProperty("idea.background.vfs.flush", true);
    private static final boolean inlineAttributes = SystemProperties.getBooleanProperty("idea.inline.vfs.attributes", true);
    private static final boolean bulkAttrReadSupport = SystemProperties.getBooleanProperty("idea.bulk.attr.read", false);
    private static final boolean useCompressionUtil = SystemProperties.getBooleanProperty("idea.use.lightweight.compression.for.vfs", false);
    private static final boolean useSmallAttrTable = SystemProperties.getBooleanProperty("idea.use.small.attr.table.for.vfs", true);
    private static final boolean ourStoreRootsSeparately = SystemProperties.getBooleanProperty("idea.store.roots.separately", false);

    //TODO[anyone] when bumping the version, please delete `ourSymlinkTargetAttr_old` and use it's value for `ourSymlinkTargetAttr`
    private static final int VERSION = 54 +
        (WE_HAVE_CONTENT_HASHES ? 0x10 : 0) +
        (IOUtil.BYTE_BUFFERS_USE_NATIVE_BYTE_ORDER ? 0x37 : 0) +
        (bulkAttrReadSupport ? 0x27 : 0) +
        (inlineAttributes ? 0x31 : 0) +
        (ourStoreRootsSeparately ? 0x63 : 0) +
        (useCompressionUtil ? 0x7f : 0) +
        (useSmallAttrTable ? 0x31 : 0) +
        (PersistentHashMapValueStorage.COMPRESSION_ENABLED ? 0x15 : 0);

    private static final int PARENT_OFFSET = 0;
    private static final int PARENT_SIZE = 4;
    private static final int NAME_OFFSET = PARENT_OFFSET + PARENT_SIZE;
    private static final int NAME_SIZE = 4;
    private static final int FLAGS_OFFSET = NAME_OFFSET + NAME_SIZE;
    private static final int FLAGS_SIZE = 4;
    private static final int ATTR_REF_OFFSET = FLAGS_OFFSET + FLAGS_SIZE;
    private static final int ATTR_REF_SIZE = 4;
    private static final int CONTENT_OFFSET = ATTR_REF_OFFSET + ATTR_REF_SIZE;
    private static final int CONTENT_SIZE = 4;
    private static final int TIMESTAMP_OFFSET = CONTENT_OFFSET + CONTENT_SIZE;
    private static final int TIMESTAMP_SIZE = 8;
    private static final int MOD_COUNT_OFFSET = TIMESTAMP_OFFSET + TIMESTAMP_SIZE;
    private static final int MOD_COUNT_SIZE = 4;
    private static final int LENGTH_OFFSET = MOD_COUNT_OFFSET + MOD_COUNT_SIZE;
    private static final int LENGTH_SIZE = 8;

    private static final int RECORD_SIZE = LENGTH_OFFSET + LENGTH_SIZE;

    private static final byte[] ZEROES = new byte[RECORD_SIZE];

    private static final int HEADER_VERSION_OFFSET = 0;
    //private static final int HEADER_RESERVED_4BYTES_OFFSET = 4; // reserved
    private static final int HEADER_GLOBAL_MOD_COUNT_OFFSET = 8;
    private static final int HEADER_CONNECTION_STATUS_OFFSET = 12;
    private static final int HEADER_TIMESTAMP_OFFSET = 16;
    private static final int HEADER_SIZE = HEADER_TIMESTAMP_OFFSET + 8;

    private static final int CONNECTED_MAGIC = 0x12ad34e4;
    private static final int SAFELY_CLOSED_MAGIC = 0x1f2f3f4f;
    private static final int CORRUPTED_MAGIC = 0xabcf7f7f;

    private static final FileAttribute ourChildrenAttr = new FileAttribute("FsRecords.DIRECTORY_CHILDREN");
    private static final FileAttribute ourSymlinkTargetAttr = new FileAttribute("FsRecords.SYMLINK_TARGET_2");
    private static final FileAttribute ourSymlinkTargetAttr_old = new FileAttribute("FsRecords.SYMLINK_TARGET");

    private static final ReentrantReadWriteLock lock;
    private static final ReentrantReadWriteLock.ReadLock r;
    private static final ReentrantReadWriteLock.WriteLock w;

    private static volatile int ourLocalModificationCount;
    private static volatile boolean ourIsDisposed;

    private static final int FREE_RECORD_FLAG = 0x100;
    private static final int ALL_VALID_FLAGS = PersistentFS.ALL_VALID_FLAGS | FREE_RECORD_FLAG;
    private static final int MAX_INITIALIZATION_ATTEMPTS = 10;

    static {
        //noinspection ConstantConditions
        assert HEADER_SIZE <= RECORD_SIZE;

        lock = new ReentrantReadWriteLock();
        r = lock.readLock();
        w = lock.writeLock();
    }

    static void writeAttributesToRecord(int id, int parentId, @Nonnull FileAttributes attributes, @Nonnull String name) {
        writeAndHandleErrors(() -> {
            setName(id, name);

            setTimestamp(id, attributes.lastModified);
            setLength(id, attributes.isDirectory() ? -1L : attributes.length);

            setFlags(id, (attributes.isDirectory() ? PersistentFS.IS_DIRECTORY_FLAG : 0) |
                (attributes.isWritable() ? 0 : PersistentFS.IS_READ_ONLY) |
                (attributes.isSymLink() ? PersistentFS.IS_SYMLINK : 0) |
                (attributes.isSpecial() ? PersistentFS.IS_SPECIAL : 0) |
                (attributes.isHidden() ? PersistentFS.IS_HIDDEN : 0), true);
            setParent(id, parentId);
        });
    }

    @Contract("_->fail")
    public static void requestVfsRebuild(@Nonnull Throwable e) {
        DbConnection.handleError(e);
    }

    @Nonnull
    public static File basePath() {
        return new File(DbConnection.getCachesDir());
    }

    private static class DbConnection {
        private static boolean ourInitialized;

        private static PersistentStringEnumerator myNames;
        private static Storage myAttributes;
        private static RefCountingStorage myContents;
        private static ResizeableMappedFile myRecords;
        private static PersistentBTreeEnumerator<byte[]> myContentHashesEnumerator;
        private static File myRootsFile;
        private static final VfsDependentEnum<String> myAttributesList = new VfsDependentEnum<>("attrib", EnumeratorStringDescriptor.INSTANCE, 1);
        private static final IntList myFreeRecords = IntLists.newArrayList();

        private static volatile boolean myDirty;
        /**
         * accessed under {@link #r}/{@link #w}
         */
        private static ScheduledFuture<?> myFlushingFuture;
        /**
         * accessed under {@link #r}/{@link #w}
         */
        private static boolean myCorrupted;

        private static final AttrPageAwareCapacityAllocationPolicy REASONABLY_SMALL = new AttrPageAwareCapacityAllocationPolicy();

        public static void connect() {
            writeAndHandleErrors(() -> {
                if (!ourInitialized) {
                    init();
                    setupFlushing();
                    ourInitialized = true;
                }
            });
        }

        private static void scanFreeRecords() {
            int fileLength = (int) myRecords.length();
            LOG.assertTrue(fileLength % RECORD_SIZE == 0, "invalid file size: " + fileLength);

            int count = fileLength / RECORD_SIZE;
            for (int n = 2; n < count; n++) {
                if (BitUtil.isSet(getFlags(n), FREE_RECORD_FLAG)) {
                    myFreeRecords.add(n);
                }
            }
        }

        static int getFreeRecord() {
            return myFreeRecords.isEmpty() ? 0 : myFreeRecords.removeByIndex(myFreeRecords.size() - 1);
        }

        private static void createBrokenMarkerFile(@Nullable Throwable reason) {
            File brokenMarker = getCorruptionMarkerFile();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (@SuppressWarnings("ImplicitDefaultCharsetUsage") PrintStream stream = new PrintStream(out)) {
                new Exception().printStackTrace(stream);
                if (reason != null) {
                    stream.print("\nReason:\n");
                    reason.printStackTrace(stream);
                }
            }
            LOG.info("Creating VFS corruption marker; Trace=\n" + out);

            try (@SuppressWarnings("ImplicitDefaultCharsetUsage") FileWriter writer = new FileWriter(brokenMarker)) {
                writer.write("These files are corrupted and must be rebuilt from the scratch on next startup");
            }
            catch (IOException ignored) {
            }  // No luck.
        }

        private static File getCorruptionMarkerFile() {
            return new File(basePath(), "corruption.marker");
        }

        private static void init() {
            Exception exception = null;
            for (int i = 0; i < MAX_INITIALIZATION_ATTEMPTS; i++) {
                exception = tryInit();
                if (exception == null) {
                    return;
                }
            }
            throw new RuntimeException("Can't initialize filesystem storage", exception);
        }

        @Nullable
        private static Exception tryInit() {
            File basePath = basePath().getAbsoluteFile();
            if (!(basePath.isDirectory() || basePath.mkdirs())) {
                return new RuntimeException("Cannot create storage directory: " + basePath);
            }

            File namesFile = new File(basePath, "names" + VFS_FILES_EXTENSION);
            final File attributesFile = new File(basePath, "attrib" + VFS_FILES_EXTENSION);
            final File contentsFile = new File(basePath, "content" + VFS_FILES_EXTENSION);
            File contentsHashesFile = new File(basePath, "contentHashes" + VFS_FILES_EXTENSION);
            File recordsFile = new File(basePath, "records" + VFS_FILES_EXTENSION);
            myRootsFile = ourStoreRootsSeparately ? new File(basePath, "roots" + VFS_FILES_EXTENSION) : null;

            File vfsDependentEnumBaseFile = VfsDependentEnum.getBaseFile();

            if (!namesFile.exists()) {
                invalidateIndex("'" + namesFile.getPath() + "' does not exist");
            }

            try {
                if (getCorruptionMarkerFile().exists()) {
                    invalidateIndex("corruption marker found");
                    throw new IOException("Corruption marker file found");
                }

                PagedFileStorage.StorageLockContext storageLockContext = new PagedFileStorage.StorageLockContext(false);
                myNames = new PersistentStringEnumerator(namesFile, storageLockContext);

                myAttributes = new Storage(attributesFile.getPath(), REASONABLY_SMALL) {
                    @Override
                    protected AbstractRecordsTable createRecordsTable(PagePool pool, File recordsFile) throws IOException {
                        return inlineAttributes && useSmallAttrTable ? new CompactRecordsTable(recordsFile, pool, false) : super.createRecordsTable(pool, recordsFile);
                    }
                };

                myContents = new RefCountingStorage(contentsFile.getPath(), CapacityAllocationPolicy.FIVE_PERCENT_FOR_GROWTH, useCompressionUtil) {
                    @Nonnull
                    @Override
                    protected ExecutorService createExecutor() {
                        return SequentialTaskExecutor.createSequentialApplicationPoolExecutor("FSRecords Pool");
                    }
                };

                // sources usually zipped with 4x ratio
                myContentHashesEnumerator = WE_HAVE_CONTENT_HASHES ? new ContentHashesUtil.HashEnumerator(contentsHashesFile, storageLockContext) : null;

                boolean aligned = PagedFileStorage.BUFFER_SIZE % RECORD_SIZE == 0;
                if (!aligned) {
                    LOG.error("Buffer size " + PagedFileStorage.BUFFER_SIZE + " is not aligned for record size " + RECORD_SIZE);
                }
                myRecords = new ResizeableMappedFile(recordsFile, 20 * 1024, storageLockContext, PagedFileStorage.BUFFER_SIZE, aligned, IOUtil.BYTE_BUFFERS_USE_NATIVE_BYTE_ORDER);

                boolean initial = myRecords.length() == 0;

                if (initial) {
                    cleanRecord(0); // Clean header
                    cleanRecord(1); // Create root record
                    setCurrentVersion();
                }

                int version = getVersion();
                if (version != VERSION) {
                    throw new IOException("FS repository version mismatch: actual=" + version + " expected=" + VERSION);
                }

                if (myRecords.getInt(HEADER_CONNECTION_STATUS_OFFSET) != SAFELY_CLOSED_MAGIC) {
                    throw new IOException("FS repository wasn't safely shut down");
                }
                if (initial) {
                    markDirty();
                }
                scanFreeRecords();
                getAttributeId(ourChildrenAttr.getId()); // trigger writing / loading of vfs attribute ids in top level write action
                return null;
            }
            catch (Exception e) { // IOException, IllegalArgumentException
                LOG.info("Filesystem storage is corrupted or does not exist. [Re]Building. Reason: " + e.getMessage());
                try {
                    closeFiles();

                    boolean deleted = FileUtil.delete(getCorruptionMarkerFile());
                    deleted &= IOUtil.deleteAllFilesStartingWith(namesFile);
                    deleted &= AbstractStorage.deleteFiles(attributesFile.getPath());
                    deleted &= AbstractStorage.deleteFiles(contentsFile.getPath());
                    deleted &= IOUtil.deleteAllFilesStartingWith(contentsHashesFile);
                    deleted &= IOUtil.deleteAllFilesStartingWith(recordsFile);
                    deleted &= IOUtil.deleteAllFilesStartingWith(vfsDependentEnumBaseFile);
                    deleted &= myRootsFile == null || IOUtil.deleteAllFilesStartingWith(myRootsFile);

                    if (!deleted) {
                        throw new IOException("Cannot delete filesystem storage files");
                    }
                }
                catch (IOException e1) {
                    Runnable warnAndShutdown = () -> {
                        if (Application.get().isUnitTestMode()) {
                            //noinspection CallToPrintStackTrace
                            e1.printStackTrace();
                        }
                        else {
                            String message = "Files in " + basePath.getPath() + " are locked.\n" + Application.get().getName() + " will not be able to start up.";
                            if (!Application.get().isHeadlessEnvironment()) {
                                JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), message, "Fatal Error", JOptionPane.ERROR_MESSAGE);
                            }
                            else {
                                //noinspection UseOfSystemOutOrSystemErr
                                System.err.println(message);
                            }
                        }
                        Runtime.getRuntime().halt(1);
                    };

                    if (EventQueue.isDispatchThread()) {
                        warnAndShutdown.run();
                    }
                    else {
                        //noinspection SSBasedInspection
                        SwingUtilities.invokeLater(warnAndShutdown);
                    }

                    throw new RuntimeException("Can't rebuild filesystem storage ", e1);
                }

                return e;
            }
        }

        private static void invalidateIndex(@Nonnull String reason) {
            LOG.info("Marking VFS as corrupted: " + reason);
            File indexRoot = ContainerPathManager.get().getIndexRoot();
            if (indexRoot.exists()) {
                String[] children = indexRoot.list();
                if (children != null && children.length > 0) {
                    // create index corruption marker only if index directory exists and is non-empty
                    // It is incorrect to consider non-existing indices "corrupted"
                    FileUtil.createIfDoesntExist(new File(ContainerPathManager.get().getIndexRoot(), "corruption.marker"));
                }
            }
        }

        @Nonnull
        private static String getCachesDir() {
            String dir = System.getProperty("caches_dir");
            return dir == null ? ContainerPathManager.get().getSystemPath() + "/caches/" : dir;
        }

        private static void markDirty() {
            assert lock.isWriteLocked();
            if (!myDirty) {
                myDirty = true;
                myRecords.putInt(HEADER_CONNECTION_STATUS_OFFSET, CONNECTED_MAGIC);
            }
        }

        private static void setupFlushing() {
            if (!backgroundVfsFlush) {
                return;
            }

            myFlushingFuture = FlushingDaemon.everyFiveSeconds(new Runnable() {
                private int lastModCount;

                @Override
                public void run() {
                    if (lastModCount == ourLocalModificationCount) {
                        flush();
                    }
                    lastModCount = ourLocalModificationCount;
                }
            });
        }

        private static void doForce() {
            // avoid NPE when close has already taken place
            if (myNames != null && myFlushingFuture != null) {
                myNames.force();
                myAttributes.force();
                myContents.force();
                if (myContentHashesEnumerator != null) {
                    myContentHashesEnumerator.force();
                }
                markClean();
                myRecords.force();
            }
        }

        // must not be run under write lock to avoid other clients wait for read lock
        private static void flush() {
            if (isDirty() && !HeavyProcessLatch.INSTANCE.isRunning()) {
                readAndHandleErrors(() -> {
                    doForce();
                    return null;
                });
            }
        }

        public static boolean isDirty() {
            return myDirty || myNames.isDirty() || myAttributes.isDirty() || myContents.isDirty() || myRecords.isDirty() || myContentHashesEnumerator != null && myContentHashesEnumerator.isDirty();
        }


        private static int getVersion() {
            int recordsVersion = myRecords.getInt(HEADER_VERSION_OFFSET);
            if (myAttributes.getVersion() != recordsVersion || myContents.getVersion() != recordsVersion) {
                return -1;
            }

            return recordsVersion;
        }

        private static long getTimestamp() {
            return myRecords.getLong(HEADER_TIMESTAMP_OFFSET);
        }

        private static void setCurrentVersion() {
            myRecords.putInt(HEADER_VERSION_OFFSET, VERSION);
            myRecords.putLong(HEADER_TIMESTAMP_OFFSET, System.currentTimeMillis());
            myAttributes.setVersion(VERSION);
            myContents.setVersion(VERSION);
            myRecords.putInt(HEADER_CONNECTION_STATUS_OFFSET, SAFELY_CLOSED_MAGIC);
        }

        static void cleanRecord(int id) {
            myRecords.put(((long) id) * RECORD_SIZE, ZEROES, 0, RECORD_SIZE);
        }

        private static PersistentStringEnumerator getNames() {
            return myNames;
        }

        private static void closeFiles() throws IOException {
            if (myFlushingFuture != null) {
                myFlushingFuture.cancel(false);
                myFlushingFuture = null;
            }

            if (myNames != null) {
                myNames.close();
                myNames = null;
            }

            if (myAttributes != null) {
                myAttributes.close();
                myAttributes = null;
            }

            if (myContents != null) {
                myContents.close();
                myContents = null;
            }

            if (myContentHashesEnumerator != null) {
                myContentHashesEnumerator.close();
                myContentHashesEnumerator = null;
            }

            if (myRecords != null) {
                markClean();
                myRecords.close();
                myRecords = null;
            }
            ourInitialized = false;
        }

        // either called from FlushingDaemon thread under read lock, or from handleError under write lock
        private static void markClean() {
            assert lock.isWriteLocked() || lock.getReadHoldCount() != 0;
            if (myDirty) {
                myDirty = false;
                // writing here under read lock is safe because no-one else read or write at this offset (except at startup)
                myRecords.putInt(HEADER_CONNECTION_STATUS_OFFSET, myCorrupted ? CORRUPTED_MAGIC : SAFELY_CLOSED_MAGIC);
            }
        }

        private static final int RESERVED_ATTR_ID = bulkAttrReadSupport ? 1 : 0;
        private static final int FIRST_ATTR_ID_OFFSET = bulkAttrReadSupport ? RESERVED_ATTR_ID : 0;

        private static int getAttributeId(@Nonnull String attId) throws IOException {
            // do not invoke FSRecords.requestVfsRebuild under read lock to avoid deadlock
            return myAttributesList.getIdRaw(attId, false) + FIRST_ATTR_ID_OFFSET;
        }

        @Contract("_->fail")
        private static void handleError(@Nonnull Throwable e) throws RuntimeException, Error {
            assert lock.getReadHoldCount() == 0;
            if (!ourIsDisposed) { // No need to forcibly mark VFS corrupted if it is already shut down
                w.lock(); // lock manually to avoid handleError() recursive calls
                try {
                    if (!myCorrupted) {
                        createBrokenMarkerFile(e);
                        myCorrupted = true;
                        doForce();
                    }
                }
                finally {
                    w.unlock();
                }
            }

            ExceptionUtil.rethrow(e);
        }

        private static class AttrPageAwareCapacityAllocationPolicy extends CapacityAllocationPolicy {
            boolean myAttrPageRequested;

            @Override
            public int calculateCapacity(int requiredLength) {   // 20% for growth
                return Math.max(myAttrPageRequested ? 8 : 32, Math.min((int) (requiredLength * 1.2), (requiredLength / 1024 + 1) * 1024));
            }
        }
    }

    private FSRecords() {
    }

    static void connect() {
        DbConnection.connect();
    }

    public static long getCreationTimestamp() {
        return readAndHandleErrors(DbConnection::getTimestamp);
    }

    private static ResizeableMappedFile getRecords() {
        ResizeableMappedFile records = DbConnection.myRecords;
        assert records != null : "Vfs must be initialized";
        return records;
    }

    private static PersistentBTreeEnumerator<byte[]> getContentHashesEnumerator() {
        return DbConnection.myContentHashesEnumerator;
    }

    private static RefCountingStorage getContentStorage() {
        return DbConnection.myContents;
    }

    private static Storage getAttributesStorage() {
        return DbConnection.myAttributes;
    }

    private static PersistentStringEnumerator getNames() {
        return DbConnection.getNames();
    }

    // todo: Address  / capacity store in records table, size store with payload
    public static int createRecord() {
        return writeAndHandleErrors(() -> {
            DbConnection.markDirty();

            int free = DbConnection.getFreeRecord();
            if (free == 0) {
                int fileLength = length();
                LOG.assertTrue(fileLength % RECORD_SIZE == 0);
                int newRecord = fileLength / RECORD_SIZE;
                DbConnection.cleanRecord(newRecord);
                assert fileLength + RECORD_SIZE == length();
                return newRecord;
            }
            else {
                if (lazyVfsDataCleaning) {
                    deleteContentAndAttributes(free);
                }
                DbConnection.cleanRecord(free);
                return free;
            }
        });
    }

    private static int length() {
        return (int) getRecords().length();
    }

    public static int getMaxId() {
        return readAndHandleErrors(() -> length() / RECORD_SIZE);
    }

    static void deleteRecordRecursively(int id) {
        writeAndHandleErrors(() -> {
            incModCount(id);
            if (lazyVfsDataCleaning) {
                markAsDeletedRecursively(id);
            }
            else {
                doDeleteRecursively(id);
            }
        });
    }

    private static void markAsDeletedRecursively(int id) {
        for (int subRecord : list(id)) {
            markAsDeletedRecursively(subRecord);
        }

        markAsDeleted(id);
    }

    private static void markAsDeleted(int id) {
        writeAndHandleErrors(() -> {
            DbConnection.markDirty();
            addToFreeRecordsList(id);
        });
    }

    private static void doDeleteRecursively(int id) {
        for (int subRecord : list(id)) {
            doDeleteRecursively(subRecord);
        }

        deleteRecord(id);
    }

    private static void deleteRecord(int id) {
        writeAndHandleErrors(() -> {
            DbConnection.markDirty();
            deleteContentAndAttributes(id);

            DbConnection.cleanRecord(id);
            addToFreeRecordsList(id);
        });
    }

    private static void deleteContentAndAttributes(int id) throws IOException {
        int content_page = getContentRecordId(id);
        if (content_page != 0) {
            if (WE_HAVE_CONTENT_HASHES) {
                getContentStorage().releaseRecord(content_page, false);
            }
            else {
                getContentStorage().releaseRecord(content_page);
            }
        }

        int att_page = getAttributeRecordId(id);
        if (att_page != 0) {
            try (final DataInputStream attStream = getAttributesStorage().readStream(att_page)) {
                if (bulkAttrReadSupport) {
                    skipRecordHeader(attStream, DbConnection.RESERVED_ATTR_ID, id);
                }

                while (attStream.available() > 0) {
                    DataInputOutputUtil.readINT(attStream);// Attribute ID;
                    int attAddressOrSize = DataInputOutputUtil.readINT(attStream);

                    if (inlineAttributes) {
                        if (attAddressOrSize < MAX_SMALL_ATTR_SIZE) {
                            attStream.skipBytes(attAddressOrSize);
                            continue;
                        }
                        attAddressOrSize -= MAX_SMALL_ATTR_SIZE;
                    }
                    getAttributesStorage().deleteRecord(attAddressOrSize);
                }
            }
            getAttributesStorage().deleteRecord(att_page);
        }
    }

    private static void addToFreeRecordsList(int id) {
        // DbConnection.addFreeRecord(id); // important! Do not add fileId to free list until restart
        setFlags(id, FREE_RECORD_FLAG, false);
    }

    private static final int ROOT_RECORD_ID = 1;

    @Nonnull
    @TestOnly
    static int[] listRoots() {
        return readAndHandleErrors(() -> {
            if (ourStoreRootsSeparately) {
                IntList result = IntLists.newArrayList();

                try (@SuppressWarnings("ImplicitDefaultCharsetUsage") LineNumberReader stream = new LineNumberReader(new BufferedReader(new InputStreamReader(new FileInputStream(DbConnection.myRootsFile))))) {
                    String str;
                    while ((str = stream.readLine()) != null) {
                        int index = str.indexOf(' ');
                        int id = Integer.parseInt(str.substring(0, index));
                        result.add(id);
                    }
                }
                catch (FileNotFoundException ignored) {
                }

                return result.toArray();
            }

            try (DataInputStream input = readAttribute(ROOT_RECORD_ID, ourChildrenAttr)) {
                if (input == null) {
                    return ArrayUtil.EMPTY_INT_ARRAY;
                }
                int count = DataInputOutputUtil.readINT(input);
                int[] result = ArrayUtil.newIntArray(count);
                int prevId = 0;
                for (int i = 0; i < count; i++) {
                    DataInputOutputUtil.readINT(input); // Name
                    prevId = result[i] = DataInputOutputUtil.readINT(input) + prevId; // Id
                }
                return result;
            }
        });
    }

    @TestOnly
    static void force() {
        writeAndHandleErrors(DbConnection::doForce);
    }

    @TestOnly
    static boolean isDirty() {
        return readAndHandleErrors(DbConnection::isDirty);
    }

    private static void saveNameIdSequenceWithDeltas(int[] names, int[] ids, DataOutputStream output) throws IOException {
        DataInputOutputUtil.writeINT(output, names.length);
        int prevId = 0;
        int prevNameId = 0;
        for (int i = 0; i < names.length; i++) {
            DataInputOutputUtil.writeINT(output, names[i] - prevNameId);
            DataInputOutputUtil.writeINT(output, ids[i] - prevId);
            prevId = ids[i];
            prevNameId = names[i];
        }
    }

    static int findRootRecord(@Nonnull String rootUrl) {
        return writeAndHandleErrors(() -> {
            if (ourStoreRootsSeparately) {
                try (@SuppressWarnings("ImplicitDefaultCharsetUsage") LineNumberReader stream = new LineNumberReader(new BufferedReader(new InputStreamReader(new FileInputStream(DbConnection.myRootsFile))))) {
                    String str;
                    while ((str = stream.readLine()) != null) {
                        int index = str.indexOf(' ');

                        if (str.substring(index + 1).equals(rootUrl)) {
                            return Integer.parseInt(str.substring(0, index));
                        }
                    }
                }
                catch (FileNotFoundException ignored) {
                }

                DbConnection.markDirty();
                try (@SuppressWarnings("ImplicitDefaultCharsetUsage") Writer stream = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(DbConnection.myRootsFile, true)))) {
                    int id = createRecord();
                    stream.write(id + " " + rootUrl + "\n");
                    return id;
                }
            }

            int root = getNames().tryEnumerate(rootUrl);

            int[] names = ArrayUtil.EMPTY_INT_ARRAY;
            int[] ids = ArrayUtil.EMPTY_INT_ARRAY;
            try (final DataInputStream input = readAttribute(ROOT_RECORD_ID, ourChildrenAttr)) {
                if (input != null) {
                    int count = DataInputOutputUtil.readINT(input);
                    names = ArrayUtil.newIntArray(count);
                    ids = ArrayUtil.newIntArray(count);
                    int prevId = 0;
                    int prevNameId = 0;

                    for (int i = 0; i < count; i++) {
                        int name = DataInputOutputUtil.readINT(input) + prevNameId;
                        int id = DataInputOutputUtil.readINT(input) + prevId;
                        if (name == root) {
                            return id;
                        }

                        prevNameId = names[i] = name;
                        prevId = ids[i] = id;
                    }
                }
            }

            DbConnection.markDirty();
            root = getNames().enumerate(rootUrl);

            int id;
            try (DataOutputStream output = writeAttribute(ROOT_RECORD_ID, ourChildrenAttr)) {
                id = createRecord();

                int index = Arrays.binarySearch(ids, id);
                ids = ArrayUtil.insert(ids, -index - 1, id);
                names = ArrayUtil.insert(names, -index - 1, root);

                saveNameIdSequenceWithDeltas(names, ids, output);
            }

            return id;
        });
    }

    static void deleteRootRecord(int id) {
        writeAndHandleErrors(() -> {
            DbConnection.markDirty();
            if (ourStoreRootsSeparately) {
                List<String> rootsThatLeft = new ArrayList<>();
                try (@SuppressWarnings("ImplicitDefaultCharsetUsage") LineNumberReader stream = new LineNumberReader(new BufferedReader(new InputStreamReader(new FileInputStream(DbConnection.myRootsFile))))) {
                    String str;
                    while ((str = stream.readLine()) != null) {
                        int index = str.indexOf(' ');
                        int rootId = Integer.parseInt(str.substring(0, index));
                        if (rootId != id) {
                            rootsThatLeft.add(str);
                        }
                    }
                }
                catch (FileNotFoundException ignored) {
                }

                try (@SuppressWarnings("ImplicitDefaultCharsetUsage") Writer stream = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(DbConnection.myRootsFile)))) {
                    for (String line : rootsThatLeft) {
                        stream.write(line);
                        stream.write("\n");
                    }
                }
                return;
            }

            int[] names;
            int[] ids;
            try (final DataInputStream input = readAttribute(ROOT_RECORD_ID, ourChildrenAttr)) {
                assert input != null;
                int count = DataInputOutputUtil.readINT(input);

                names = ArrayUtil.newIntArray(count);
                ids = ArrayUtil.newIntArray(count);
                int prevId = 0;
                int prevNameId = 0;
                for (int i = 0; i < count; i++) {
                    names[i] = DataInputOutputUtil.readINT(input) + prevNameId;
                    ids[i] = DataInputOutputUtil.readINT(input) + prevId;
                    prevId = ids[i];
                    prevNameId = names[i];
                }
            }

            int index = ArrayUtil.find(ids, id);
            assert index >= 0;

            names = ArrayUtil.remove(names, index);
            ids = ArrayUtil.remove(ids, index);

            try (DataOutputStream output = writeAttribute(ROOT_RECORD_ID, ourChildrenAttr)) {
                saveNameIdSequenceWithDeltas(names, ids, output);
            }
        });
    }

    @Nonnull
    static int[] list(int id) {
        return readAndHandleErrors(() -> {
            try (final DataInputStream input = readAttribute(id, ourChildrenAttr)) {
                if (input == null) {
                    return ArrayUtil.EMPTY_INT_ARRAY;
                }
                int count = DataInputOutputUtil.readINT(input);
                int[] result = ArrayUtil.newIntArray(count);
                int prevId = id;
                for (int i = 0; i < count; i++) {
                    prevId = result[i] = DataInputOutputUtil.readINT(input) + prevId;
                }
                return result;
            }
        });
    }

    static boolean mayHaveChildren(int id) {
        return readAndHandleErrors(() -> {
            try (final DataInputStream input = readAttribute(id, ourChildrenAttr)) {
                if (input == null) {
                    return true;
                }
                int count = DataInputOutputUtil.readINT(input);
                return count != 0;
            }
        });
    }

    // returns NameId[] sorted by NameId.id
    @Nonnull
    public static NameId[] listAll(int parentId) {
        assert parentId > 0 : parentId;
        return readAndHandleErrors(() -> {
            try (final DataInputStream input = readAttribute(parentId, ourChildrenAttr)) {
                if (input == null) {
                    return NameId.EMPTY_ARRAY;
                }

                int count = DataInputOutputUtil.readINT(input);
                NameId[] result = count == 0 ? NameId.EMPTY_ARRAY : new NameId[count];
                int prevId = parentId;
                for (int i = 0; i < count; i++) {
                    int id = DataInputOutputUtil.readINT(input) + prevId;
                    prevId = id;
                    int nameId = doGetNameId(id);
                    result[i] = new NameId(id, nameId, FileNameCache.getVFileName(nameId, FSRecords::doGetNameByNameId));
                }
                return result;
            }
        });
    }

    static boolean wereChildrenAccessed(int id) {
        return readAndHandleErrors(() -> findAttributePage(id, ourChildrenAttr, false) != 0);
    }

    private static <T> T readAndHandleErrors(@Nonnull ThrowableComputable<T, ?> action) {
        assert lock.getReadHoldCount() == 0; // otherwise DbConnection.handleError(e) (requires write lock) could fail
        try {
            r.lock();
            try {
                return action.compute();
            }
            finally {
                r.unlock();
            }
        }
        catch (Throwable e) {
            DbConnection.handleError(e);
            throw new RuntimeException(e);
        }
    }

    private static <T> T writeAndHandleErrors(@Nonnull ThrowableComputable<T, ?> action) {
        try {
            w.lock();
            return action.compute();
        }
        catch (Throwable e) {
            DbConnection.handleError(e);
            throw new RuntimeException(e);
        }
        finally {
            w.unlock();
        }
    }

    private static void writeAndHandleErrors(@Nonnull ThrowableRunnable<?> action) {
        try {
            w.lock();
            action.run();
        }
        catch (Throwable e) {
            DbConnection.handleError(e);
            throw new RuntimeException(e);
        }
        finally {
            w.unlock();
        }
    }

    static void updateList(int id, @Nonnull int[] childIds) {
        assert id > 0 : id;
        Arrays.sort(childIds);
        writeAndHandleErrors(() -> {
            DbConnection.markDirty();
            try (DataOutputStream record = writeAttribute(id, ourChildrenAttr)) {
                DataInputOutputUtil.writeINT(record, childIds.length);

                int prevId = id;
                for (int childId : childIds) {
                    assert childId > 0 : childId;
                    if (childId == id) {
                        LOG.error("Cyclic parent child relations");
                    }
                    else {
                        int delta = childId - prevId;
                        assert prevId == id || delta > 0 : delta;
                        DataInputOutputUtil.writeINT(record, delta);
                        prevId = childId;
                    }
                }
            }
        });
    }

    @Nullable
    static String readSymlinkTarget(int id) {
        return readAndHandleErrors(() -> {
            try (DataInputStream stream = readAttribute(id, ourSymlinkTargetAttr)) {
                if (stream != null) {
                    return StringUtil.nullize(IOUtil.readUTF(stream));
                }
            }
            try (DataInputStream stream = readAttribute(id, ourSymlinkTargetAttr_old)) {
                if (stream != null) {
                    return StringUtil.nullize(stream.readUTF());
                }
            }
            return null;
        });
    }

    static void storeSymlinkTarget(int id, @Nullable String symlinkTarget) {
        writeAndHandleErrors(() -> {
            DbConnection.markDirty();
            try (DataOutputStream stream = writeAttribute(id, ourSymlinkTargetAttr)) {
                IOUtil.writeUTF(stream, StringUtil.notNullize(symlinkTarget));
            }
        });
    }

    private static void incModCount(int id) {
        incLocalModCount();
        int count = doGetModCount() + 1;
        getRecords().putInt(HEADER_GLOBAL_MOD_COUNT_OFFSET, count);

        setModCount(id, count);
    }

    private static void incLocalModCount() {
        DbConnection.markDirty();
        //noinspection NonAtomicOperationOnVolatileField
        ourLocalModificationCount++;
        CachedFileType.clearCache();
    }

    static int getLocalModCount() {
        return ourLocalModificationCount; // This is volatile, only modified under Application.runWriteAction() lock.
    }

    static int getModCount() {
        return readAndHandleErrors(FSRecords::doGetModCount);
    }

    private static int doGetModCount() {
        return getRecords().getInt(HEADER_GLOBAL_MOD_COUNT_OFFSET);
    }

    public static int getParent(int id) {
        return readAndHandleErrors(() -> {
            int parentId = getRecordInt(id, PARENT_OFFSET);
            if (parentId == id) {
                LOG.error("Cyclic parent child relations in the database. id = " + id);
                return 0;
            }

            return parentId;
        });
    }

    @Nullable
    static VirtualFileSystemEntry findFileById(int id, @Nonnull ConcurrentIntObjectMap<VirtualFileSystemEntry> idToDirCache) {
        class ParentFinder implements ThrowableComputable<Void, Throwable> {
            @Nullable
            private IntList path;
            private VirtualFileSystemEntry foundParent;

            @Override
            public Void compute() {
                int currentId = id;
                while (true) {
                    int parentId = getRecordInt(currentId, PARENT_OFFSET);
                    if (parentId == 0) {
                        break;
                    }
                    if (parentId == currentId || path != null && path.size() % 128 == 0 && path.contains(parentId)) {
                        LOG.error("Cyclic parent child relations in the database. id = " + parentId);
                        break;
                    }
                    foundParent = idToDirCache.get(parentId);
                    if (foundParent != null) {
                        break;
                    }

                    currentId = parentId;
                    if (path == null) {
                        path = IntLists.newArrayList();
                    }
                    path.add(currentId);
                }
                return null;
            }

            private VirtualFileSystemEntry findDescendantByIdPath() {
                VirtualFileSystemEntry parent = foundParent;
                if (path != null) {
                    for (int i = path.size() - 1; i >= 0; i--) {
                        parent = findChild(parent, path.get(i));
                    }
                }

                return findChild(parent, id);
            }

            private VirtualFileSystemEntry findChild(VirtualFileSystemEntry parent, int childId) {
                if (!(parent instanceof VirtualDirectoryImpl)) {
                    return null;
                }
                VirtualFileSystemEntry child = ((VirtualDirectoryImpl) parent).doFindChildById(childId);
                if (child instanceof VirtualDirectoryImpl) {
                    VirtualFileSystemEntry old = idToDirCache.putIfAbsent(childId, child);
                    if (old != null) {
                        child = old;
                    }
                }
                return child;
            }
        }

        ParentFinder finder = new ParentFinder();
        readAndHandleErrors(finder);
        return finder.findDescendantByIdPath();
    }

    static void setParent(int id, int parentId) {
        if (id == parentId) {
            LOG.error("Cyclic parent/child relations");
            return;
        }

        writeAndHandleErrors(() -> {
            incModCount(id);
            putRecordInt(id, PARENT_OFFSET, parentId);
        });
    }

    public static int getNameId(int id) {
        return readAndHandleErrors(() -> doGetNameId(id));
    }

    private static int doGetNameId(int id) {
        return getRecordInt(id, NAME_OFFSET);
    }

    public static int getNameId(@Nonnull String name) {
        return readAndHandleErrors(() -> getNames().enumerate(name));
    }

    public static String getName(int id) {
        return getNameSequence(id).toString();
    }

    @Nonnull
    static CharSequence getNameSequence(int id) {
        return readAndHandleErrors(() -> doGetNameSequence(id));
    }

    @Nonnull
    private static CharSequence doGetNameSequence(int id) throws IOException {
        int nameId = getRecordInt(id, NAME_OFFSET);
        return nameId == 0 ? "" : FileNameCache.getVFileName(nameId, FSRecords::doGetNameByNameId);
    }

    public static String getNameByNameId(int nameId) {
        return readAndHandleErrors(() -> doGetNameByNameId(nameId));
    }

    private static String doGetNameByNameId(int nameId) throws IOException {
        return nameId == 0 ? "" : getNames().valueOf(nameId);
    }

    static void setName(int id, @Nonnull String name) {
        writeAndHandleErrors(() -> {
            incModCount(id);
            int nameId = getNames().enumerate(name);
            putRecordInt(id, NAME_OFFSET, nameId);
        });
    }

    static int getFlags(int id) {
        return readAndHandleErrors(() -> doGetFlags(id));
    }

    private static int doGetFlags(int id) {
        return getRecordInt(id, FLAGS_OFFSET);
    }

    static void setFlags(int id, int flags, boolean markAsChange) {
        writeAndHandleErrors(() -> {
            if (markAsChange) {
                incModCount(id);
            }
            putRecordInt(id, FLAGS_OFFSET, flags);
        });
    }

    static long getLength(int id) {
        return readAndHandleErrors(() -> getRecords().getLong(getOffset(id, LENGTH_OFFSET)));
    }

    static void setLength(int id, long len) {
        writeAndHandleErrors(() -> {
            ResizeableMappedFile records = getRecords();
            int lengthOffset = getOffset(id, LENGTH_OFFSET);
            if (records.getLong(lengthOffset) != len) {
                incModCount(id);
                records.putLong(lengthOffset, len);
            }
        });
    }

    static long getTimestamp(int id) {
        return readAndHandleErrors(() -> getRecords().getLong(getOffset(id, TIMESTAMP_OFFSET)));
    }

    static void setTimestamp(int id, long value) {
        writeAndHandleErrors(() -> {
            int timeStampOffset = getOffset(id, TIMESTAMP_OFFSET);
            ResizeableMappedFile records = getRecords();
            if (records.getLong(timeStampOffset) != value) {
                incModCount(id);
                records.putLong(timeStampOffset, value);
            }
        });
    }

    static int getModCount(int id) {
        return readAndHandleErrors(() -> getRecordInt(id, MOD_COUNT_OFFSET));
    }

    private static void setModCount(int id, int value) {
        putRecordInt(id, MOD_COUNT_OFFSET, value);
    }

    private static int getContentRecordId(int fileId) {
        return getRecordInt(fileId, CONTENT_OFFSET);
    }

    private static void setContentRecordId(int id, int value) {
        putRecordInt(id, CONTENT_OFFSET, value);
    }

    private static int getAttributeRecordId(int id) {
        return getRecordInt(id, ATTR_REF_OFFSET);
    }

    private static void setAttributeRecordId(int id, int value) {
        putRecordInt(id, ATTR_REF_OFFSET, value);
    }

    private static int getRecordInt(int id, int offset) {
        return getRecords().getInt(getOffset(id, offset));
    }

    private static void putRecordInt(int id, int offset, int value) {
        getRecords().putInt(getOffset(id, offset), value);
    }

    private static int getOffset(int id, int offset) {
        return id * RECORD_SIZE + offset;
    }

    @Nullable
    static DataInputStream readContent(int fileId) {
        int page = readAndHandleErrors(() -> {
            checkFileIsValid(fileId);
            return getContentRecordId(fileId);
        });
        if (page == 0) {
            return null;
        }
        try {
            return doReadContentById(page);
        }
        catch (OutOfMemoryError outOfMemoryError) {
            throw outOfMemoryError;
        }
        catch (Throwable e) {
            DbConnection.handleError(e);
        }
        return null;
    }

    @Nonnull
    static DataInputStream readContentById(int contentId) {
        try {
            return doReadContentById(contentId);
        }
        catch (Throwable e) {
            DbConnection.handleError(e);
        }
        return null;
    }

    @Nonnull
    private static DataInputStream doReadContentById(int contentId) throws IOException {
        DataInputStream stream = getContentStorage().readStream(contentId);
        if (useCompressionUtil) {
            byte[] bytes = CompressionUtil.readCompressed(stream);
            stream = new DataInputStream(new UnsyncByteArrayInputStream(bytes));
        }

        return stream;
    }

    @Nullable
    public static DataInputStream readAttributeWithLock(int fileId, @Nonnull FileAttribute att) {
        return readAndHandleErrors(() -> {
            try (DataInputStream stream = readAttribute(fileId, att)) {
                if (stream != null && att.isVersioned()) {
                    try {
                        int actualVersion = DataInputOutputUtil.readINT(stream);
                        if (actualVersion != att.getVersion()) {
                            return null;
                        }
                    }
                    catch (IOException e) {
                        return null;
                    }
                }
                return stream;
            }
        });
    }

    // must be called under r or w lock
    @Nullable
    private static DataInputStream readAttribute(int fileId, @Nonnull FileAttribute attribute) throws IOException {
        checkFileIsValid(fileId);

        int recordId = getAttributeRecordId(fileId);
        if (recordId == 0) {
            return null;
        }
        int encodedAttrId = DbConnection.getAttributeId(attribute.getId());

        Storage storage = getAttributesStorage();

        int page = 0;

        try (DataInputStream attrRefs = storage.readStream(recordId)) {
            if (bulkAttrReadSupport) {
                skipRecordHeader(attrRefs, DbConnection.RESERVED_ATTR_ID, fileId);
            }

            while (attrRefs.available() > 0) {
                int attIdOnPage = DataInputOutputUtil.readINT(attrRefs);
                int attrAddressOrSize = DataInputOutputUtil.readINT(attrRefs);

                if (attIdOnPage != encodedAttrId) {
                    if (inlineAttributes && attrAddressOrSize < MAX_SMALL_ATTR_SIZE) {
                        attrRefs.skipBytes(attrAddressOrSize);
                    }
                }
                else {
                    if (inlineAttributes && attrAddressOrSize < MAX_SMALL_ATTR_SIZE) {
                        byte[] b = new byte[attrAddressOrSize];
                        attrRefs.readFully(b);
                        return new DataInputStream(new UnsyncByteArrayInputStream(b));
                    }
                    page = inlineAttributes ? attrAddressOrSize - MAX_SMALL_ATTR_SIZE : attrAddressOrSize;
                    break;
                }
            }
        }

        if (page == 0) {
            return null;
        }
        DataInputStream stream = getAttributesStorage().readStream(page);
        if (bulkAttrReadSupport) {
            skipRecordHeader(stream, encodedAttrId, fileId);
        }
        return stream;
    }

    // Vfs small attrs: store inline:
    // file's AttrId -> [size, capacity] attr record (RESERVED_ATTR_ID fileId)? (attrId ((smallAttrSize smallAttrData) | (attr record)) )
    // other attr record: (AttrId, fileId) ? attrData
    private static final int MAX_SMALL_ATTR_SIZE = 64;

    private static int findAttributePage(int fileId, @Nonnull FileAttribute attr, boolean toWrite) throws IOException {
        checkFileIsValid(fileId);

        int recordId = getAttributeRecordId(fileId);
        int encodedAttrId = DbConnection.getAttributeId(attr.getId());
        boolean directoryRecord = false;

        Storage storage = getAttributesStorage();

        if (recordId == 0) {
            if (!toWrite) {
                return 0;
            }

            recordId = storage.createNewRecord();
            setAttributeRecordId(fileId, recordId);
            directoryRecord = true;
        }
        else {
            try (DataInputStream attrRefs = storage.readStream(recordId)) {
                if (bulkAttrReadSupport) {
                    skipRecordHeader(attrRefs, DbConnection.RESERVED_ATTR_ID, fileId);
                }

                while (attrRefs.available() > 0) {
                    int attIdOnPage = DataInputOutputUtil.readINT(attrRefs);
                    int attrAddressOrSize = DataInputOutputUtil.readINT(attrRefs);

                    if (attIdOnPage == encodedAttrId) {
                        if (inlineAttributes) {
                            return attrAddressOrSize < MAX_SMALL_ATTR_SIZE ? -recordId : attrAddressOrSize - MAX_SMALL_ATTR_SIZE;
                        }
                        else {
                            return attrAddressOrSize;
                        }
                    }
                    else {
                        if (inlineAttributes && attrAddressOrSize < MAX_SMALL_ATTR_SIZE) {
                            attrRefs.skipBytes(attrAddressOrSize);
                        }
                    }
                }
            }
        }

        if (toWrite) {
            try (AbstractStorage.AppenderStream appender = storage.appendStream(recordId)) {
                if (bulkAttrReadSupport) {
                    if (directoryRecord) {
                        DataInputOutputUtil.writeINT(appender, DbConnection.RESERVED_ATTR_ID);
                        DataInputOutputUtil.writeINT(appender, fileId);
                    }
                }

                DataInputOutputUtil.writeINT(appender, encodedAttrId);
                int attrAddress = storage.createNewRecord();
                DataInputOutputUtil.writeINT(appender, inlineAttributes ? attrAddress + MAX_SMALL_ATTR_SIZE : attrAddress);
                DbConnection.REASONABLY_SMALL.myAttrPageRequested = true;
                return attrAddress;
            }
            finally {
                DbConnection.REASONABLY_SMALL.myAttrPageRequested = false;
            }
        }

        return 0;
    }

    private static void skipRecordHeader(DataInputStream refs, int expectedRecordTag, int expectedFileId) throws IOException {
        int attId = DataInputOutputUtil.readINT(refs);// attrId
        assert attId == expectedRecordTag || expectedRecordTag == 0;
        int fileId = DataInputOutputUtil.readINT(refs);// fileId
        assert expectedFileId == fileId || expectedFileId == 0;
    }

    private static void writeRecordHeader(int recordTag, int fileId, @Nonnull DataOutputStream appender) throws IOException {
        DataInputOutputUtil.writeINT(appender, recordTag);
        DataInputOutputUtil.writeINT(appender, fileId);
    }

    private static void checkFileIsValid(int fileId) throws IOException {
        assert fileId > 0 : fileId;
        if (!lazyVfsDataCleaning) {
            assert !BitUtil.isSet(doGetFlags(fileId), FREE_RECORD_FLAG)
                : "Accessing attribute of a deleted page: " + fileId + ":" + doGetNameSequence(fileId);
        }
    }

    static int acquireFileContent(int fileId) {
        return writeAndHandleErrors(() -> {
            int record = getContentRecordId(fileId);
            if (record > 0) {
                getContentStorage().acquireRecord(record);
            }
            return record;
        });
    }

    static void releaseContent(int contentId) {
        writeAndHandleErrors(() -> getContentStorage().releaseRecord(contentId, !WE_HAVE_CONTENT_HASHES));
    }

    static int getContentId(int fileId) {
        return readAndHandleErrors(() -> getContentRecordId(fileId));
    }

    @Nonnull
    static DataOutputStream writeContent(int fileId, boolean readOnly) {
        return new ContentOutputStream(fileId, readOnly);
    }

    private static final MessageDigest myDigest = ContentHashesUtil.createHashDigest();

    static void writeContent(int fileId, ByteArraySequence bytes, boolean readOnly) {
        //noinspection IOResourceOpenedButNotSafelyClosed
        new ContentOutputStream(fileId, readOnly).writeBytes(bytes);
    }

    static int storeUnlinkedContent(byte[] bytes) {
        return writeAndHandleErrors(() -> {
            int recordId;
            if (WE_HAVE_CONTENT_HASHES) {
                recordId = findOrCreateContentRecord(bytes, 0, bytes.length);
                if (recordId > 0) {
                    return recordId;
                }
                recordId = -recordId;
            }
            else {
                recordId = getContentStorage().acquireNewRecord();
            }
            try (AbstractStorage.StorageDataOutput output = getContentStorage().writeStream(recordId, true)) {
                output.write(bytes);
            }
            return recordId;
        });
    }

    @Nonnull
    public static DataOutputStream writeAttribute(int fileId, @Nonnull FileAttribute att) {
        DataOutputStream stream = new AttributeOutputStream(fileId, att);
        if (att.isVersioned()) {
            try {
                DataInputOutputUtil.writeINT(stream, att.getVersion());
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return stream;
    }

    private static class ContentOutputStream extends DataOutputStream {
        final int myFileId;
        final boolean myFixedSize;

        private ContentOutputStream(int fileId, boolean readOnly) {
            super(new BufferExposingByteArrayOutputStream());
            myFileId = fileId;
            myFixedSize = readOnly;
        }

        @Override
        public void close() throws IOException {
            super.close();

            BufferExposingByteArrayOutputStream _out = (BufferExposingByteArrayOutputStream) out;
            writeBytes(_out.toByteArraySequence());
        }

        private void writeBytes(ByteArraySequence bytes) {
            writeAndHandleErrors(() -> {
                RefCountingStorage contentStorage = getContentStorage();
                checkFileIsValid(myFileId);

                int page;
                boolean fixedSize;
                if (WE_HAVE_CONTENT_HASHES) {
                    page = findOrCreateContentRecord(bytes.getBytes(), bytes.getOffset(), bytes.getLength());

                    if (page < 0 || getContentId(myFileId) != page) {
                        incModCount(myFileId);
                        setContentRecordId(myFileId, page > 0 ? page : -page);
                    }

                    setContentRecordId(myFileId, page > 0 ? page : -page);

                    if (page > 0) {
                        return;
                    }
                    page = -page;
                    fixedSize = true;
                }
                else {
                    incModCount(myFileId);
                    page = getContentRecordId(myFileId);
                    if (page == 0 || contentStorage.getRefCount(page) > 1) {
                        page = contentStorage.acquireNewRecord();
                        setContentRecordId(myFileId, page);
                    }
                    fixedSize = myFixedSize;
                }

                ByteArraySequence newBytes;
                if (useCompressionUtil) {
                    BufferExposingByteArrayOutputStream out = new BufferExposingByteArrayOutputStream();
                    try (DataOutputStream outputStream = new DataOutputStream(out)) {
                        CompressionUtil.writeCompressed(outputStream, bytes.getBytes(), bytes.getOffset(), bytes.getLength());
                    }
                    newBytes = out.toByteArraySequence();
                }
                else {
                    newBytes = bytes;
                }
                contentStorage.writeBytes(page, newBytes, fixedSize);
            });
        }
    }

    private static final boolean DUMP_STATISTICS = WE_HAVE_CONTENT_HASHES;  // TODO: remove once not needed
    private static long totalContents;
    private static long totalReuses;
    private static long time;
    private static int contents;
    private static int reuses;

    private static int findOrCreateContentRecord(byte[] bytes, int offset, int length) throws IOException {
        assert WE_HAVE_CONTENT_HASHES;

        long started = DUMP_STATISTICS ? System.nanoTime() : 0;
        myDigest.reset();
        myDigest.update(String.valueOf(length - offset).getBytes(Charset.defaultCharset()));
        myDigest.update("\0".getBytes(Charset.defaultCharset()));
        myDigest.update(bytes, offset, length);
        byte[] digest = myDigest.digest();
        long done = DUMP_STATISTICS ? System.nanoTime() - started : 0;
        time += done;

        ++contents;
        totalContents += length;

        if (DUMP_STATISTICS && (contents & 0x3FFF) == 0) {
            LOG.info("Contents:" + contents + " of " + totalContents + ", reuses:" + reuses + " of " + totalReuses + " for " + time / 1000000);
        }
        PersistentBTreeEnumerator<byte[]> hashesEnumerator = getContentHashesEnumerator();
        int largestId = hashesEnumerator.getLargestId();
        int page = hashesEnumerator.enumerate(digest);

        if (page <= largestId) {
            ++reuses;
            getContentStorage().acquireRecord(page);
            totalReuses += length;

            return page;
        }
        else {
            int newRecord = getContentStorage().acquireNewRecord();
            assert page == newRecord : "Unexpected content storage modification: page=" + page + "; newRecord=" + newRecord;

            return -page;
        }
    }

    private static class AttributeOutputStream extends DataOutputStream {
        @Nonnull
        private final FileAttribute myAttribute;
        private final int myFileId;

        private AttributeOutputStream(int fileId, @Nonnull FileAttribute attribute) {
            super(new BufferExposingByteArrayOutputStream());
            myFileId = fileId;
            myAttribute = attribute;
        }

        @Override
        public void close() throws IOException {
            super.close();
            writeAndHandleErrors(() -> {
                BufferExposingByteArrayOutputStream _out = (BufferExposingByteArrayOutputStream) out;

                if (inlineAttributes && _out.size() < MAX_SMALL_ATTR_SIZE) {
                    rewriteDirectoryRecordWithAttrContent(_out);
                    incLocalModCount();
                }
                else {
                    incLocalModCount();
                    int page = findAttributePage(myFileId, myAttribute, true);
                    if (inlineAttributes && page < 0) {
                        rewriteDirectoryRecordWithAttrContent(new BufferExposingByteArrayOutputStream());
                        page = findAttributePage(myFileId, myAttribute, true);
                    }

                    if (bulkAttrReadSupport) {
                        BufferExposingByteArrayOutputStream stream = new BufferExposingByteArrayOutputStream();
                        out = stream;
                        writeRecordHeader(DbConnection.getAttributeId(myAttribute.getId()), myFileId, this);
                        write(_out.getInternalBuffer(), 0, _out.size());
                        getAttributesStorage().writeBytes(page, stream.toByteArraySequence(), myAttribute.isFixedSize());
                    }
                    else {
                        getAttributesStorage().writeBytes(page, _out.toByteArraySequence(), myAttribute.isFixedSize());
                    }
                }
            });
        }

        void rewriteDirectoryRecordWithAttrContent(@Nonnull BufferExposingByteArrayOutputStream _out) throws IOException {
            int recordId = getAttributeRecordId(myFileId);
            assert inlineAttributes;
            int encodedAttrId = DbConnection.getAttributeId(myAttribute.getId());

            Storage storage = getAttributesStorage();
            BufferExposingByteArrayOutputStream unchangedPreviousDirectoryStream = null;
            boolean directoryRecord = false;


            if (recordId == 0) {
                recordId = storage.createNewRecord();
                setAttributeRecordId(myFileId, recordId);
                directoryRecord = true;
            }
            else {
                try (DataInputStream attrRefs = storage.readStream(recordId)) {

                    DataOutputStream dataStream = null;

                    try {
                        int remainingAtStart = attrRefs.available();
                        if (bulkAttrReadSupport) {
                            unchangedPreviousDirectoryStream = new BufferExposingByteArrayOutputStream();
                            dataStream = new DataOutputStream(unchangedPreviousDirectoryStream);
                            int attId = DataInputOutputUtil.readINT(attrRefs);
                            assert attId == DbConnection.RESERVED_ATTR_ID;
                            int fileId = DataInputOutputUtil.readINT(attrRefs);
                            assert myFileId == fileId;

                            writeRecordHeader(attId, fileId, dataStream);
                        }
                        while (attrRefs.available() > 0) {
                            int attIdOnPage = DataInputOutputUtil.readINT(attrRefs);
                            int attrAddressOrSize = DataInputOutputUtil.readINT(attrRefs);

                            if (attIdOnPage != encodedAttrId) {
                                if (dataStream == null) {
                                    unchangedPreviousDirectoryStream = new BufferExposingByteArrayOutputStream();
                                    //noinspection IOResourceOpenedButNotSafelyClosed
                                    dataStream = new DataOutputStream(unchangedPreviousDirectoryStream);
                                }
                                DataInputOutputUtil.writeINT(dataStream, attIdOnPage);
                                DataInputOutputUtil.writeINT(dataStream, attrAddressOrSize);

                                if (attrAddressOrSize < MAX_SMALL_ATTR_SIZE) {
                                    byte[] b = new byte[attrAddressOrSize];
                                    attrRefs.readFully(b);
                                    dataStream.write(b);
                                }
                            }
                            else {
                                if (attrAddressOrSize < MAX_SMALL_ATTR_SIZE) {
                                    if (_out.size() == attrAddressOrSize) {
                                        // update inplace when new attr has the same size
                                        int remaining = attrRefs.available();
                                        storage.replaceBytes(recordId, remainingAtStart - remaining, _out.toByteArraySequence());
                                        return;
                                    }
                                    attrRefs.skipBytes(attrAddressOrSize);
                                }
                            }
                        }
                    }
                    finally {
                        if (dataStream != null) {
                            dataStream.close();
                        }
                    }
                }
            }

            try (AbstractStorage.StorageDataOutput directoryStream = storage.writeStream(recordId)) {
                if (directoryRecord) {
                    if (bulkAttrReadSupport) {
                        writeRecordHeader(DbConnection.RESERVED_ATTR_ID, myFileId, directoryStream);
                    }
                }
                if (unchangedPreviousDirectoryStream != null) {
                    directoryStream.write(unchangedPreviousDirectoryStream.getInternalBuffer(), 0, unchangedPreviousDirectoryStream.size());
                }
                if (_out.size() > 0) {
                    DataInputOutputUtil.writeINT(directoryStream, encodedAttrId);
                    DataInputOutputUtil.writeINT(directoryStream, _out.size());
                    directoryStream.write(_out.getInternalBuffer(), 0, _out.size());
                }
            }
        }
    }

    static void dispose() {
        writeAndHandleErrors(() -> {
            try {
                DbConnection.doForce();
                DbConnection.closeFiles();
            }
            finally {
                ourIsDisposed = true;
            }
        });
    }

    public static void invalidateCaches() {
        DbConnection.createBrokenMarkerFile(null);
    }

    public static void checkSanity() {
        long t = System.currentTimeMillis();

        int recordCount = readAndHandleErrors(() -> {
            int fileLength = length();
            assert fileLength % RECORD_SIZE == 0;
            return fileLength / RECORD_SIZE;
        });

        IntList usedAttributeRecordIds = IntLists.newArrayList();
        IntList validAttributeIds = IntLists.newArrayList();
        for (int id = 2; id < recordCount; id++) {
            int flags = getFlags(id);
            LOG.assertTrue((flags & ~ALL_VALID_FLAGS) == 0, "Invalid flags: 0x" + Integer.toHexString(flags) + ", id: " + id);
            int currentId = id;
            boolean isFreeRecord = readAndHandleErrors(() -> DbConnection.myFreeRecords.contains(currentId));
            if (BitUtil.isSet(flags, FREE_RECORD_FLAG)) {
                LOG.assertTrue(isFreeRecord, "Record, marked free, not in free list: " + id);
            }
            else {
                LOG.assertTrue(!isFreeRecord, "Record, not marked free, in free list: " + id);
                checkRecordSanity(id, recordCount, usedAttributeRecordIds, validAttributeIds);
            }
        }

        t = System.currentTimeMillis() - t;
        LOG.info("Sanity check took " + t + " ms");
    }

    private static void checkRecordSanity(int id, int recordCount, IntList usedAttributeRecordIds, IntList validAttributeIds) {
        int parentId = getParent(id);
        assert parentId >= 0 && parentId < recordCount;
        if (parentId > 0 && getParent(parentId) > 0) {
            int parentFlags = getFlags(parentId);
            assert !BitUtil.isSet(parentFlags, FREE_RECORD_FLAG) : parentId + ": " + Integer.toHexString(parentFlags);
            assert BitUtil.isSet(parentFlags, PersistentFS.IS_DIRECTORY_FLAG) : parentId + ": " + Integer.toHexString(parentFlags);
        }

        CharSequence name = getNameSequence(id);
        LOG.assertTrue(parentId == 0 || name.length() != 0, "File with empty name found under " + getNameSequence(parentId) + ", id=" + id);

        writeAndHandleErrors(() -> {
            checkContentsStorageSanity(id);
            checkAttributesStorageSanity(id, usedAttributeRecordIds, validAttributeIds);
        });

        long length = getLength(id);
        assert length >= -1 : "Invalid file length found for " + name + ": " + length;
    }

    private static void checkContentsStorageSanity(int id) {
        int recordId = getContentRecordId(id);
        assert recordId >= 0;
        if (recordId > 0) {
            getContentStorage().checkSanity(recordId);
        }
    }

    private static void checkAttributesStorageSanity(int id, IntList usedAttributeRecordIds, IntList validAttributeIds) {
        int attributeRecordId = getAttributeRecordId(id);

        assert attributeRecordId >= 0;
        if (attributeRecordId > 0) {
            try {
                checkAttributesSanity(attributeRecordId, usedAttributeRecordIds, validAttributeIds);
            }
            catch (IOException ex) {
                DbConnection.handleError(ex);
            }
        }
    }

    private static void checkAttributesSanity(int attributeRecordId, IntList usedAttributeRecordIds, IntList validAttributeIds) throws IOException {
        assert !usedAttributeRecordIds.contains(attributeRecordId);
        usedAttributeRecordIds.add(attributeRecordId);

        try (DataInputStream dataInputStream = getAttributesStorage().readStream(attributeRecordId)) {
            if (bulkAttrReadSupport) {
                skipRecordHeader(dataInputStream, 0, 0);
            }

            while (dataInputStream.available() > 0) {
                int attId = DataInputOutputUtil.readINT(dataInputStream);

                if (!validAttributeIds.contains(attId)) {
                    //assert !getNames().valueOf(attId).isEmpty();
                    validAttributeIds.add(attId);
                }

                int attDataRecordIdOrSize = DataInputOutputUtil.readINT(dataInputStream);

                if (inlineAttributes) {
                    if (attDataRecordIdOrSize < MAX_SMALL_ATTR_SIZE) {
                        dataInputStream.skipBytes(attDataRecordIdOrSize);
                        continue;
                    }
                    else {
                        attDataRecordIdOrSize -= MAX_SMALL_ATTR_SIZE;
                    }
                }
                assert !usedAttributeRecordIds.contains(attDataRecordIdOrSize);
                usedAttributeRecordIds.add(attDataRecordIdOrSize);

                getAttributesStorage().checkSanity(attDataRecordIdOrSize);
            }
        }
    }

    @Contract("_->fail")
    public static void handleError(Throwable e) throws RuntimeException, Error {
        DbConnection.handleError(e);
    }
}