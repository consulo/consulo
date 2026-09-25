// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.index.io;

import consulo.index.io.data.DataInputOutputUtil;
import consulo.util.io.BufferExposingByteArrayOutputStream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the value-storage append/read contract directly, without {@code PersistentHashMap} hiding value-chain details.
 */
class PersistentHashMapValueStorageContractTest {
    private static final int OPERATION_COUNT = 120;
    private static final int ACCESSOR_SECOND_PAYLOAD_SIZE = 4096;
    private static final int COMPRESSED_ACCESSOR_PAYLOAD_SIZE = 32768 + 128;

    private static final List<Long> SEEDS = List.of(1L, 2L, 5L, 20260528L);

    private static final int[] INTERESTING_PAYLOAD_SIZES = {
        0,
        1,
        2,
        15,
        127,
        1023,
        1024,
        1025,
        4095,
        4096,
        4097,
        32767,
        32768,
        32769,
    };

    @TempDir
    Path myTempDir;

    @Test
    @DisplayName("randomized append read and reopen contract for value chains")
    void randomizedAppendReadAndReopenContractForValueChains() throws IOException {
        for (TestConfig config : testConfigurations(false)) {
            for (long seed : SEEDS) {
                new ValueStorageScenario(myTempDir.resolve(config.myName + "-seed-" + seed + ".values"), config, seed).run();
            }
        }
    }

    @Test
    @DisplayName("randomized append read and reopen contract without value chains")
    void randomizedAppendReadAndReopenContractWithoutValueChains() throws IOException {
        for (TestConfig config : testConfigurations(true)) {
            for (long seed : SEEDS) {
                new ValueStorageScenario(myTempDir.resolve(config.myName + "-seed-" + seed + ".values"), config, seed).run();
            }
        }
    }

    @Test
    @DisplayName("append after writable reopen does not overwrite existing records")
    void appendAfterWritableReopenDoesNotOverwriteExistingRecords() throws IOException {
        for (TestConfig config : allTestConfigurations()) {
            Path storageFile = myTempDir.resolve(config.myName + "-append-after-reopen.values");
            byte[] firstPayload = {1, 2, 3, 4};
            byte[] secondPayload = new byte[4096];
            for (int index = 0; index < secondPayload.length; index++) {
                secondPayload[index] = (byte) (index % 251);
            }

            long firstAddress = withStorage(storageFile, config, false, null, storage -> {
                long address = appendPayload(storage, firstPayload, 0);
                storage.flush();
                return address;
            });

            long secondAddress = withStorage(storageFile, config, false, null, storage -> {
                assertRecord(storage, firstAddress, firstPayload, 1, config.myName + ": first record must survive writable reopen before next append");

                long address = appendPayload(storage, secondPayload, 0);
                assertTrue(
                    address > firstAddress,
                    config.myName + ": append after reopen must allocate after the already persisted first record"
                );
                storage.flush();
                return address;
            });

            withStorage(storageFile, config, true, null, storage -> {
                assertRecord(storage, firstAddress, firstPayload, 1, config.myName + ": first record must survive append after reopen");
                assertRecord(storage, secondAddress, secondPayload, 1, config.myName + ": second record must be readable after read-only reopen");
                return null;
            });
        }
    }

    @Test
    @DisplayName("read-only storage rejects appends but keeps existing records readable")
    void readOnlyStorageRejectsAppendsButKeepsExistingRecordsReadable() throws IOException {
        for (TestConfig config : allTestConfigurations()) {
            Path storageFile = myTempDir.resolve(config.myName + "-readonly-append.values");
            byte[] payload = {42, 43, 44};

            long address = withStorage(storageFile, config, false, null, storage -> {
                long appended = appendPayload(storage, payload, 0);
                storage.flush();
                return appended;
            });

            withStorage(storageFile, config, true, null, storage -> {
                assertTrue(storage.isReadOnly(), config.myName + ": precondition, storage must be read-only");
                assertRecord(storage, address, payload, 1, config.myName + ": read-only storage must read records written before reopen");

                assertThrows(
                    AssertionError.class,
                    () -> appendPayload(storage, new byte[]{45}, 0),
                    config.myName + ": read-only storage must reject append attempts"
                );
                return null;
            });
        }
    }

    @Test
    @DisplayName("compaction mode reader keeps value chains readable and rejects appends")
    void compactionModeReaderKeepsValueChainsReadableAndRejectsAppends() throws IOException {
        for (TestConfig config : testConfigurations(false)) {
            Path storageFile = myTempDir.resolve(config.myName + "-compaction-mode-reader.values");
            byte[] firstChunk = new byte[1024];
            for (int index = 0; index < firstChunk.length; index++) {
                firstChunk[index] = (byte) index;
            }
            byte[] secondChunk = new byte[2048];
            for (int index = 0; index < secondChunk.length; index++) {
                secondChunk[index] = (byte) (index * 3);
            }
            byte[] expectedBytes = concat(firstChunk, secondChunk);

            withStorage(storageFile, config, false, null, storage -> {
                long firstAddress = appendPayload(storage, firstChunk, 0);
                long secondAddress = appendPayload(storage, secondChunk, firstAddress);
                storage.flush();

                storage.switchToCompactionMode();

                assertRecord(storage, secondAddress, expectedBytes, 2, config.myName + ": compaction-mode reader must read existing chunk chains");
                assertThrows(
                    AssertionError.class,
                    () -> appendPayload(storage, new byte[]{1}, 0),
                    config.myName + ": compaction mode must reject appends"
                );
                return null;
            });
        }
    }

    @Test
    @DisplayName("compactChunks rewrites value chain into a single equivalent chunk")
    void compactChunksRewritesValueChainIntoASingleEquivalentChunk() throws IOException {
        for (TestConfig config : testConfigurations(false)) {
            Path storageFile = myTempDir.resolve(config.myName + "-compact-chunks.values");
            byte[] firstChunk = "first-chunk".getBytes(StandardCharsets.UTF_8);
            byte[] secondChunk = "second-chunk".getBytes(StandardCharsets.UTF_8);
            byte[] expectedBytes = concat(firstChunk, secondChunk);

            withStorage(storageFile, config, false, null, storage -> {
                long firstAddress = appendPayload(storage, firstChunk, 0);
                long secondAddress = appendPayload(storage, secondChunk, firstAddress);
                PersistentHashMapValueStorage.ReadResult oldReadResult = storage.readBytes(secondAddress);

                assertEquals(2, oldReadResult.chunksCount, config.myName + ": precondition, value must be stored as two chunks");
                assertArrayEquals(expectedBytes, oldReadResult.buffer, config.myName + ": precondition, old value chain must contain both chunks");

                long compactedAddress = storage.compactChunks(out -> out.write(oldReadResult.buffer), oldReadResult);

                assertTrue(compactedAddress > secondAddress, config.myName + ": compacted chunk must be appended after the old chain");
                assertNotEquals(secondAddress, compactedAddress, config.myName + ": compaction must return a new value address");
                assertRecord(storage, compactedAddress, expectedBytes, 1, config.myName + ": compacted value must be a single equivalent chunk");
                assertRecord(storage,
                    secondAddress,
                    expectedBytes,
                    2,
                    config.myName + ": old value chain remains readable until map metadata is updated");
                return null;
            });
        }
    }

    @Test
    @DisplayName("value storage receives map StorageLockContext")
    void valueStorageReceivesMapStorageLockContext() throws IOException {
        StorageLockContext lockContext = new StorageLockContext(false);

        withPersistentMap(myTempDir.resolve("builder-context-map"), lockContext, map -> {
            PersistentHashMapValueStorage valueStorage = map.getValueStorage();
            assertSame(lockContext, valueStorage.getStorageLockContext(), "Value storage must use the StorageLockContext passed to PersistentHashMap");
        });
    }

    @Test
    @DisplayName("value storage receives thread-local StorageLockContext")
    void valueStorageReceivesThreadLocalStorageLockContext() throws IOException {
        StorageLockContext lockContext = new StorageLockContext(false);
        StorageLockContext previousContext = PagedFileStorage.THREAD_LOCAL_STORAGE_LOCK_CONTEXT.get();
        PagedFileStorage.THREAD_LOCAL_STORAGE_LOCK_CONTEXT.set(lockContext);
        try {
            withPersistentMap(myTempDir.resolve("thread-local-context-map"), null, map -> {
                PersistentHashMapValueStorage valueStorage = map.getValueStorage();
                assertSame(lockContext, valueStorage.getStorageLockContext(), "Value storage must use the resolved thread-local StorageLockContext");
            });
        }
        finally {
            if (previousContext == null) {
                PagedFileStorage.THREAD_LOCAL_STORAGE_LOCK_CONTEXT.remove();
            }
            else {
                PagedFileStorage.THREAD_LOCAL_STORAGE_LOCK_CONTEXT.set(previousContext);
            }
        }
    }

    @Test
    @DisplayName("value storage reads and writes through StorageLockContext ChannelsAccessor")
    void valueStorageReadsAndWritesThroughStorageLockContextChannelsAccessor() throws IOException {
        Path storageFile = myTempDir.resolve("channel-accessor-backed-file-accessor.values");
        byte[] firstPayload = accessorFirstPayload();
        byte[] secondPayload = accessorSecondPayload();
        RecordingChannelsAccessor channelsAccessor = new RecordingChannelsAccessor();
        StorageLockContext lockContext = new StorageLockContext(false, channelsAccessor.myReadOnlyAccessor, channelsAccessor.myWritableAccessor);
        TestConfig config = new TestConfig("plain", false, false);

        long firstAddress = withStorage(storageFile, config, false, lockContext, storage -> {
            long address = appendPayload(storage, firstPayload, 0);
            storage.flush();
            return address;
        });
        assertEquals(
            1,
            channelsAccessor.forceOperations(storageFile).size(),
            "Initial append must preserve the single historical first-header force on the main value file"
        );
        channelsAccessor.clearChannelOperations();

        long secondAddress = withStorage(storageFile, config, false, lockContext, storage -> {
            assertRecord(storage, firstAddress, firstPayload, 1, "Value written through the custom accessor must be readable after reopen");
            long address = appendPayload(storage, secondPayload, firstAddress);
            storage.flush();
            return address;
        });

        withStorage(storageFile, config, true, lockContext, storage -> {
            assertRecord(storage,
                secondAddress,
                concat(firstPayload, secondPayload),
                2,
                "Value storage must read value chains through the custom accessor");
            return null;
        });
        assertEquals(
            0,
            channelsAccessor.forceOperations(storageFile).size(),
            "Main value file reads and logical flushes after the header workaround must not call FileChannel.force()"
        );

        assertEquals(0, channelsAccessor.myActiveChannels, "Recording accessor must not keep channels open after operations");
        assertEquals(0, channelsAccessor.myIdempotentOperations, "Adapter append protocol must not use retryable idempotent operations");
        assertTrue(channelsAccessor.myOperations.stream().anyMatch(it -> !it.readOnly()), "Adapter must use the supplied accessor for write operations");
        assertTrue(channelsAccessor.myOperations.stream().anyMatch(Operation::readOnly), "Adapter must use the supplied accessor for read operations");
        assertTrue(
            channelsAccessor.myChannelOperations.stream()
                .anyMatch(it -> it.path().equals(storageFile) && it.name().equals("read") && Boolean.FALSE.equals(it.readOnly())),
            "Writable value storage must read value bytes through the writable accessor channel"
        );
        assertTrue(
            channelsAccessor.myChannelOperations.stream()
                .anyMatch(it -> it.path().equals(storageFile) && it.name().equals("read") && Boolean.TRUE.equals(it.readOnly())),
            "Read-only value storage must read value bytes through the read-only accessor channel"
        );
        assertTrue(channelsAccessor.myClosedPaths.contains(storageFile), "Adapter disposal must close channels through the supplied accessor");
    }

    @Test
    @DisplayName("compressed value storage side files use StorageLockContext ChannelsAccessor")
    void compressedValueStorageSideFilesUseStorageLockContextChannelsAccessor() throws IOException {
        Path storageFile = myTempDir.resolve("compressed-channel-accessor-side-files.values");
        Path chunkLengthFile = storageFile.resolveSibling(storageFile.getFileName() + ".s");
        Path incompleteChunkFile = storageFile.resolveSibling(storageFile.getFileName() + ".at");
        byte[] payload = new byte[COMPRESSED_ACCESSOR_PAYLOAD_SIZE];
        for (int index = 0; index < payload.length; index++) {
            payload[index] = (byte) (index % 251);
        }
        RecordingChannelsAccessor channelsAccessor = new RecordingChannelsAccessor();
        StorageLockContext lockContext = new StorageLockContext(false, channelsAccessor.myReadOnlyAccessor, channelsAccessor.myWritableAccessor);
        TestConfig config = new TestConfig("compressed", false, true);

        long address = withStorage(storageFile, config, false, lockContext, storage -> {
            long appended = appendPayload(storage, payload, 0);
            storage.flush();
            return appended;
        });
        assertEquals(
            1,
            channelsAccessor.forceOperations(storageFile).size(),
            "Compressed storage must preserve only the historical first-header force on the main value file"
        );
        assertEquals(
            0,
            channelsAccessor.forceOperations(chunkLengthFile).size(),
            "Compressed chunk-length appender flush must not force the chunk-length side-file"
        );
        assertEquals(
            0,
            channelsAccessor.forceOperations(incompleteChunkFile).size(),
            "Compressed incomplete tail write must not force the incomplete chunk side-file"
        );
        channelsAccessor.clearChannelOperations();

        withStorage(storageFile, config, true, lockContext, storage -> {
            assertRecord(storage, address, payload, 1, "Compressed value storage must read side files through the custom accessor");
            return null;
        });
        assertEquals(
            0,
            channelsAccessor.forceOperations(storageFile).size(),
            "Compressed chunk reads must flush the main appender without forcing the main value file"
        );
        assertEquals(
            0,
            channelsAccessor.forceOperations(chunkLengthFile).size(),
            "Compressed chunk length reads must not force the chunk-length side-file"
        );

        assertTrue(
            channelsAccessor.myOperations.stream().anyMatch(it -> it.path().equals(chunkLengthFile) && !it.readOnly()),
            "Compressed storage must append chunk lengths through the supplied accessor"
        );
        assertTrue(
            channelsAccessor.myOperations.stream().anyMatch(it -> it.path().equals(chunkLengthFile) && it.readOnly()),
            "Compressed storage must read chunk length table through the supplied accessor"
        );
        assertTrue(
            channelsAccessor.myOperations.stream().anyMatch(it -> it.path().equals(incompleteChunkFile) && !it.readOnly()),
            "Compressed storage must write incomplete chunk file through the supplied accessor"
        );
        assertTrue(
            channelsAccessor.myOperations.stream().anyMatch(it -> it.path().equals(incompleteChunkFile) && it.readOnly()),
            "Compressed storage must read incomplete chunk file through the supplied accessor"
        );
        assertTrue(channelsAccessor.myClosedPaths.contains(chunkLengthFile),
            "Compressed storage must close chunk length channels through the accessor");
        assertTrue(channelsAccessor.myClosedPaths.contains(incompleteChunkFile),
            "Compressed storage must close incomplete chunk channels through the accessor");
        assertEquals(0, channelsAccessor.myIdempotentOperations, "Compressed side-file access must not use retryable idempotent operations");
    }

    @Test
    @DisplayName("compressed incomplete chunk clear does not force when side file becomes empty")
    void compressedIncompleteChunkClearDoesNotForceWhenSideFileBecomesEmpty() throws IOException {
        Path storageFile = myTempDir.resolve("compressed-unforced-incomplete-tail-clear.values");
        Path incompleteChunkFile = storageFile.resolveSibling(storageFile.getFileName() + ".at");
        RecordingChannelsAccessor channelsAccessor = new RecordingChannelsAccessor();
        StorageLockContext lockContext = new StorageLockContext(false, channelsAccessor.myReadOnlyAccessor, channelsAccessor.myWritableAccessor);
        TestConfig config = new TestConfig("compressed-hasNoChunks-true", true, true);

        withStorage(storageFile, config, false, lockContext, storage -> {
            appendPayload(storage, new byte[]{1}, 0);
            storage.flush();

            int payloadLength = payloadLengthToFillCompressedPage(storage.getSize(), config.myHasNoChunks);
            byte[] payload = new byte[payloadLength];
            for (int index = 0; index < payload.length; index++) {
                payload[index] = (byte) (index % 251);
            }
            appendPayload(storage, payload, 0);
            storage.flush();

            assertEquals(
                CompressedAppendableFile.PAGE_LENGTH,
                storage.getSize(),
                "precondition: second append must finish the compressed page and clear the incomplete tail"
            );
            return null;
        });

        List<ChannelOperation> incompleteTailOperations = channelsAccessor.myChannelOperations.stream()
            .filter(it -> it.path().equals(incompleteChunkFile))
            .toList();
        int truncateIndex = -1;
        for (int i = 0; i < incompleteTailOperations.size(); i++) {
            ChannelOperation operation = incompleteTailOperations.get(i);
            if (operation.name().equals("truncate") && Long.valueOf(0L).equals(operation.size())) {
                truncateIndex = i;
                break;
            }
        }
        assertTrue(truncateIndex >= 0, "Compressed storage must truncate the incomplete chunk side-file when a page is completed");
        assertFalse(
            incompleteTailOperations.subList(truncateIndex + 1, incompleteTailOperations.size()).stream().anyMatch(it -> it.name().equals("force")),
            "Incomplete chunk side-file clear must not force after truncating the side-file to 0"
        );
    }

    /**
     * Keeps the generated operation stream reproducible and reports enough context when a generated case fails.
     */
    private static final class ValueStorageScenario {
        private final Path myStorageFile;
        private final TestConfig myConfig;
        private final long mySeed;
        private final Random myRandom;
        private final List<Record> myRecords = new ArrayList<>();
        private PersistentHashMapValueStorage myStorage;
        private int myPayloadNo = 0;

        private ValueStorageScenario(Path storageFile, TestConfig config, long seed) throws IOException {
            myStorageFile = storageFile;
            myConfig = config;
            mySeed = seed;
            myRandom = new Random(seed);
            myStorage = openStorage(false);
        }

        void run() throws IOException {
            try {
                for (int operationNo = 0; operationNo < OPERATION_COUNT; operationNo++) {
                    try {
                        performOperation(operationNo);
                    }
                    catch (Throwable error) {
                        throw new AssertionError(caseName(operationNo) + ": generated operation failed", error);
                    }
                }

                flushAndReopen("final writable reopen");
                reopenReadOnlyAndVerify("final read-only reopen");
            }
            finally {
                myStorage.dispose();
            }
        }

        private void performOperation(int operationNo) throws IOException {
            int operation = myRecords.isEmpty() ? 0 : myRandom.nextInt(100);
            if (operation < 40) {
                appendNewRecord(operationNo);
            }
            else if (operation < 65 && !myConfig.myHasNoChunks) {
                appendChunk(operationNo);
            }
            else if (operation < 85) {
                readBackRandomRecord(operationNo);
            }
            else if (operation < 95) {
                flushAndReopen("operation " + operationNo);
            }
            else {
                reopenReadOnlyAndVerify("operation " + operationNo);
            }
        }

        private void appendNewRecord(int operationNo) throws IOException {
            byte[] payload = nextPayload();
            long tailAddress = myStorage.appendBytes(payload, 0, payload.length, 0);
            Record record = new Record(tailAddress, payload, 1);
            myRecords.add(record);
            assertRecord(myRecords.size() - 1, record, caseName(operationNo) + " after appendNew");
        }

        private void appendChunk(int operationNo) throws IOException {
            int recordIndex = myRandom.nextInt(myRecords.size());
            Record previous = myRecords.get(recordIndex);
            byte[] payload = nextPayload();
            long tailAddress = myStorage.appendBytes(payload, 0, payload.length, previous.tailAddress());

            Record updated = new Record(
                tailAddress,
                concat(previous.expectedBytes(), payload),
                previous.expectedChunksCount() + 1
            );
            myRecords.set(recordIndex, updated);
            assertRecord(recordIndex, updated, caseName(operationNo) + " after appendChunk");
        }

        private void readBackRandomRecord(int operationNo) throws IOException {
            int recordIndex = myRandom.nextInt(myRecords.size());
            assertRecord(recordIndex, myRecords.get(recordIndex), caseName(operationNo) + " readBack");
        }

        private void flushAndReopen(String checkpoint) throws IOException {
            //TODO RC: do we need separate .flush() before .dispose()?
            //         shouldn't .dispose() do flush() inside?
            myStorage.flush();
            myStorage.dispose();
            myStorage = openStorage(false);
            assertAllRecords(caseName(null) + " after " + checkpoint);
        }

        private void reopenReadOnlyAndVerify(String checkpoint) throws IOException {
            //TODO RC: do we need separate .flush() before .dispose()?
            //         shouldn't .dispose() do flush() inside?
            myStorage.flush();
            myStorage.dispose();

            myStorage = openStorage(true);
            assertTrue(myStorage.isReadOnly(), caseName(null) + " " + checkpoint + ": storage must be opened in read-only mode");
            assertAllRecords(caseName(null) + " during " + checkpoint);

            myStorage.dispose();
            myStorage = openStorage(false);
            assertAllRecords(caseName(null) + " after returning from " + checkpoint);
        }

        private void assertAllRecords(String message) throws IOException {
            for (int recordIndex = 0; recordIndex < myRecords.size(); recordIndex++) {
                assertRecord(recordIndex, myRecords.get(recordIndex), message);
            }
        }

        private void assertRecord(int recordIndex, Record record, String message) throws IOException {
            PersistentHashMapValueStorage.ReadResult result = myStorage.readBytes(record.tailAddress());
            assertArrayEquals(
                record.expectedBytes(),
                result.buffer,
                message + ": record[" + recordIndex + "] bytes must match bytes appended to its chunk chain"
            );
            assertEquals(
                record.expectedChunksCount(),
                result.chunksCount,
                message + ": record[" + recordIndex + "] chunk count must match number of appended chunks"
            );
        }

        private PersistentHashMapValueStorage openStorage(boolean readOnly) throws IOException {
            return PersistentHashMapValueStorage.create(myStorageFile, myConfig.options(readOnly));
        }

        private byte[] nextPayload() {
            int size;
            if (myPayloadNo < INTERESTING_PAYLOAD_SIZES.length) {
                size = INTERESTING_PAYLOAD_SIZES[myPayloadNo];
            }
            else {
                size = switch (myRandom.nextInt(10)) {
                    case 0 -> 0;
                    case 1 -> 1;
                    case 2 -> 1024 + myRandom.nextInt(128);
                    case 3 -> 4096 + myRandom.nextInt(256);
                    case 4 -> 32768 + myRandom.nextInt(128);
                    default -> myRandom.nextInt(512);
                };
            }
            myPayloadNo++;

            byte[] bytes = new byte[size];
            myRandom.nextBytes(bytes);
            return bytes;
        }

        private String caseName(@Nullable Integer operationNo) {
            StringBuilder builder = new StringBuilder();
            builder.append("[");
            builder.append(myConfig.myName);
            builder.append(", seed=");
            builder.append(mySeed);
            if (operationNo != null) {
                builder.append(", operation=");
                builder.append(operationNo);
            }
            builder.append("]");
            return builder.toString();
        }
    }

    /**
     * Holds the current tail address and model value for one generated logical record.
     */
    private record Record(long tailAddress, byte[] expectedBytes, int expectedChunksCount) {
    }

    /**
     * Captures storage creation flags that materially affect value-storage layout and append protocol.
     */
    private static final class TestConfig {
        private final String myName;
        private final boolean myHasNoChunks;
        private final boolean myUseCompression;

        private TestConfig(String name, boolean hasNoChunks, boolean useCompression) {
            myName = name;
            myHasNoChunks = hasNoChunks;
            myUseCompression = useCompression;
        }

        PersistentHashMapValueStorage.CreationTimeOptions options(boolean readOnly) {
            return new PersistentHashMapValueStorage.CreationTimeOptions(
                readOnly,
                /*compactChunksWithValueDeserialization = */false,
                myHasNoChunks,
                myUseCompression
            );
        }
    }

    private record Operation(Path path, boolean readOnly) {
    }

    private record ChannelOperation(Path path, String name, @Nullable Long size, @Nullable Boolean readOnly) {
        private ChannelOperation(Path path, String name) {
            this(path, name, null, null);
        }
    }

    private static final class RecordingChannelsAccessor {
        private final ChannelsAccessor myReadOnlyAccessor = new Accessor(/*readOnly = */true);
        private final ChannelsAccessor myWritableAccessor = new Accessor(/*readOnly = */false);

        private final List<Operation> myOperations = new ArrayList<>();
        private final List<ChannelOperation> myChannelOperations = new ArrayList<>();
        private final List<Path> myClosedPaths = new ArrayList<>();
        private int myActiveChannels = 0;
        private int myIdempotentOperations = 0;

        private final class Accessor implements ChannelsAccessor {
            private final boolean myReadOnly;

            private Accessor(boolean readOnly) {
                myReadOnly = readOnly;
            }

            @Override
            public boolean isReadOnly() {
                return myReadOnly;
            }

            @Override
            public <T> T executeOp(Path path, FileChannelOperation<T> operation) throws IOException {
                myOperations.add(new Operation(path, myReadOnly));
                if (!myReadOnly) {
                    Files.createDirectories(path.getParent());
                }

                myActiveChannels++;
                try {
                    try (FileChannel channel = myReadOnly ? FileChannel.open(path, READ) : FileChannel.open(path, READ, WRITE, CREATE)) {
                        return operation.execute(new RecordingFileChannel(path, myReadOnly, channel, myChannelOperations));
                    }
                }
                finally {
                    myActiveChannels--;
                }
            }

            @Override
            public <T> T executeIdempotentOp(Path path,
                                             FileChannelInterruptsRetryer.FileChannelIdempotentOperation<T> operation) throws IOException {
                myIdempotentOperations++;
                return executeOp(path, operation::execute);
            }

            @Override
            public void closeChannel(Path path) {
                myClosedPaths.add(path);
            }
        }

        /**
         * Starts a new assertion window while preserving accessor lifecycle counters.
         */
        void clearChannelOperations() {
            myChannelOperations.clear();
        }

        /**
         * Selects physical force calls for one file so tests can keep main-file and side-file expectations separate.
         */
        List<ChannelOperation> forceOperations(Path path) {
            return myChannelOperations.stream()
                .filter(it -> it.path().equals(path) && it.name().equals("force"))
                .toList();
        }

        private static final class RecordingFileChannel extends FileChannel {
            private final Path myPath;
            private final boolean myReadOnly;
            private final FileChannel myDelegate;
            private final List<ChannelOperation> myOperations;

            private RecordingFileChannel(Path path, boolean readOnly, FileChannel delegate, List<ChannelOperation> operations) {
                myPath = path;
                myReadOnly = readOnly;
                myDelegate = delegate;
                myOperations = operations;
            }

            @Override
            public int read(ByteBuffer dst) throws IOException {
                myOperations.add(new ChannelOperation(myPath, "read", null, myReadOnly));
                return myDelegate.read(dst);
            }

            @Override
            public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
                return myDelegate.read(dsts, offset, length);
            }

            @Override
            public int read(ByteBuffer dst, long position) throws IOException {
                myOperations.add(new ChannelOperation(myPath, "read", null, myReadOnly));
                return myDelegate.read(dst, position);
            }

            @Override
            public int write(ByteBuffer src) throws IOException {
                return myDelegate.write(src);
            }

            @Override
            public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
                return myDelegate.write(srcs, offset, length);
            }

            @Override
            public int write(ByteBuffer src, long position) throws IOException {
                return myDelegate.write(src, position);
            }

            @Override
            public long position() throws IOException {
                return myDelegate.position();
            }

            @Override
            public FileChannel position(long newPosition) throws IOException {
                myDelegate.position(newPosition);
                return this;
            }

            @Override
            public long size() throws IOException {
                return myDelegate.size();
            }

            @Override
            public FileChannel truncate(long size) throws IOException {
                myOperations.add(new ChannelOperation(myPath, "truncate", size, null));
                myDelegate.truncate(size);
                return this;
            }

            @Override
            public void force(boolean metaData) throws IOException {
                myOperations.add(new ChannelOperation(myPath, "force"));
                myDelegate.force(metaData);
            }

            @Override
            public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
                return myDelegate.transferTo(position, count, target);
            }

            @Override
            public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
                return myDelegate.transferFrom(src, position, count);
            }

            @Override
            public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
                return myDelegate.map(mode, position, size);
            }

            @Override
            public FileLock lock(long position, long size, boolean shared) throws IOException {
                return myDelegate.lock(position, size, shared);
            }

            @Override
            public @Nullable FileLock tryLock(long position, long size, boolean shared) throws IOException {
                return myDelegate.tryLock(position, size, shared);
            }

            @Override
            protected void implCloseChannel() throws IOException {
                myDelegate.close();
            }
        }
    }

    @FunctionalInterface
    private interface StorageAction<T> {
        T run(PersistentHashMapValueStorage storage) throws IOException;
    }

    @FunctionalInterface
    private interface MapAction {
        void run(PersistentHashMap<String, String> map) throws IOException;
    }

    private static List<TestConfig> testConfigurations(boolean hasNoChunks) {
        return List.of(
            new TestConfig("plain-hasNoChunks-" + hasNoChunks, hasNoChunks, false),
            new TestConfig("compressed-hasNoChunks-" + hasNoChunks, hasNoChunks, true)
        );
    }

    private static List<TestConfig> allTestConfigurations() {
        List<TestConfig> configurations = new ArrayList<>(testConfigurations(false));
        configurations.addAll(testConfigurations(true));
        return configurations;
    }

    private static byte[] accessorFirstPayload() {
        return new byte[]{1, 2, 3, 4};
    }

    private static byte[] accessorSecondPayload() {
        byte[] payload = new byte[ACCESSOR_SECOND_PAYLOAD_SIZE];
        for (int index = 0; index < payload.length; index++) {
            payload[index] = (byte) (index % 251);
        }
        return payload;
    }

    private static int payloadLengthToFillCompressedPage(long currentSize, boolean hasNoChunks) throws IOException {
        int bytesUntilPageEnd = CompressedAppendableFile.PAGE_LENGTH - (int) (currentSize % CompressedAppendableFile.PAGE_LENGTH);
        for (int payloadLength = 0; payloadLength <= bytesUntilPageEnd; payloadLength++) {
            if (valueStorageRecordSize(payloadLength, hasNoChunks) == bytesUntilPageEnd) {
                return payloadLength;
            }
        }
        throw new IllegalStateException("Unable to choose payload size to fill compressed page from currentSize=" + currentSize);
    }

    private static int valueStorageRecordSize(int payloadLength, boolean hasNoChunks) throws IOException {
        BufferExposingByteArrayOutputStream stream = new BufferExposingByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(stream)) {
            DataInputOutputUtil.writeINT(output, payloadLength);
            if (!hasNoChunks) {
                DataInputOutputUtil.writeLONG(output, 0);
            }
        }
        return stream.size() + payloadLength;
    }

    private static <T> T withStorage(Path storageFile,
                                     TestConfig config,
                                     boolean readOnly,
                                     @Nullable StorageLockContext lockContext,
                                     StorageAction<T> action) throws IOException {
        PersistentHashMapValueStorage storage = lockContext == null
            ? PersistentHashMapValueStorage.create(storageFile, config.options(readOnly))
            : PersistentHashMapValueStorage.create(storageFile, config.options(readOnly), lockContext);
        try {
            return action.run(storage);
        }
        finally {
            storage.dispose();
        }
    }

    private static void withPersistentMap(Path storageFile,
                                          @Nullable StorageLockContext lockContext,
                                          MapAction action) throws IOException {
        PersistentHashMap<String, String> map = new PersistentHashMap<>(
            storageFile,
            EnumeratorStringDescriptor.INSTANCE,
            EnumeratorStringDescriptor.INSTANCE,
            lockContext
        );
        try {
            action.run(map);
        }
        finally {
            map.close();
        }
    }

    private static long appendPayload(PersistentHashMapValueStorage storage, byte[] payload, long previousTailAddress) throws IOException {
        return storage.appendBytes(payload, 0, payload.length, previousTailAddress);
    }

    private static void assertRecord(PersistentHashMapValueStorage storage,
                                     long tailAddress,
                                     byte[] expectedBytes,
                                     int expectedChunksCount,
                                     String message) throws IOException {
        PersistentHashMapValueStorage.ReadResult result = storage.readBytes(tailAddress);
        assertArrayEquals(expectedBytes, result.buffer, message + ": bytes must match bytes appended to the value chain");
        assertEquals(expectedChunksCount, result.chunksCount, message + ": chunk count must match the expected value layout");
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] result = new byte[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
