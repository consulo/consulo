// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.index.io;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenChannelsCacheTest {
    @Test
    @DisplayName("accessor views are stable and mode-bound")
    void accessorViewsAreStableAndModeBound() {
        OpenChannelsCache cache = new OpenChannelsCache("test-cache", 2, new RecordingChannelOpener());

        assertSame(cache.asReadOnly(), cache.asReadOnly(), "Read-only accessor view must be stable");
        assertSame(cache.asWritable(), cache.asWritable(), "Writable accessor view must be stable");
        assertNotSame(cache.asReadOnly(), cache.asWritable(), "Different modes must use different accessor views");
        assertTrue(cache.asReadOnly().isReadOnly(), "Read-only accessor view must report read-only mode");
        assertFalse(cache.asWritable().isReadOnly(), "Writable accessor view must report writable mode");
    }

    @Test
    @DisplayName("executeOp reuses cached channel for repeated read access")
    void executeOpReusesCachedChannelForRepeatedReadAccess(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("storage.bin");
        RecordingChannelOpener opener = new RecordingChannelOpener();
        OpenChannelsCache cache = new OpenChannelsCache("test-cache", 2, opener);
        ChannelsAccessor readOnlyAccessor = cache.asReadOnly();

        try {
            TrackingFileChannel firstChannel = readOnlyAccessor.executeOp(file, channel -> (TrackingFileChannel) channel);
            TrackingFileChannel secondChannel = readOnlyAccessor.executeOp(file, channel -> (TrackingFileChannel) channel);

            assertSame(firstChannel, secondChannel, "Repeated read-only access must reuse the cached descriptor");
            assertEquals(List.of(true), opener.openedModes(), "Only one read-only channel should be opened");
            assertFalse(firstChannel.wasClosed(), "Cached descriptor must stay open after operation release");
            assertEquals(1, cache.getStatistics().getLoad(), "First access should be counted as a cache load");
            assertEquals(1, cache.getStatistics().getHit(), "Second access should be counted as a cache hit");
            assertEquals(0, cache.getStatistics().getMiss(), "No eviction or read/write mode switch should happen");
        }
        finally {
            readOnlyAccessor.closeChannel(file);
        }
    }

    @Test
    @DisplayName("executeIdempotentOp reuses cached resilient channel")
    void executeIdempotentOpReusesCachedResilientChannel(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("storage.bin");
        RecordingChannelOpener opener = new RecordingChannelOpener();
        OpenChannelsCache cache = new OpenChannelsCache("test-cache", 2, opener);
        ChannelsAccessor readOnlyAccessor = cache.asReadOnly();

        try {
            TrackingFileChannel firstChannel = readOnlyAccessor.executeIdempotentOp(file, channel -> (TrackingFileChannel) channel);
            TrackingFileChannel secondChannel = readOnlyAccessor.executeIdempotentOp(file, channel -> (TrackingFileChannel) channel);

            assertSame(firstChannel, secondChannel, "Idempotent operations must reuse the cached descriptor");
            assertEquals(2, firstChannel.myIdempotentOperationCount, "Both operations should run through the same resilient channel");
            assertEquals(List.of(true), opener.openedModes(), "Only one read-only channel should be opened");
            assertFalse(firstChannel.wasClosed(), "Cached descriptor must stay open after operation release");
        }
        finally {
            readOnlyAccessor.closeChannel(file);
        }
    }

    @Test
    @DisplayName("read-only accessor returns channel that rejects writes")
    void readOnlyAccessorReturnsChannelThatRejectsWrites(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("storage.bin");
        RecordingChannelOpener opener = new RecordingChannelOpener();
        OpenChannelsCache cache = new OpenChannelsCache("test-cache", 2, opener);
        ChannelsAccessor readOnlyAccessor = cache.asReadOnly();

        try {
            readOnlyAccessor.executeOp(file, channel -> {
                assertThrows(NonWritableChannelException.class, () -> writeSingleByte(channel), "Read-only channel must reject writes");
                return null;
            });

            assertEquals(List.of(true), opener.openedModes(), "The descriptor should be opened in read-only mode");
        }
        finally {
            readOnlyAccessor.closeChannel(file);
        }
    }

    @Test
    @DisplayName("read-only accessor returns non-writable channel even if writable channel is cached")
    void readOnlyAccessorReturnsNonWritableChannelEvenIfWritableChannelIsCached(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("storage.bin");
        RecordingChannelOpener opener = new RecordingChannelOpener();
        OpenChannelsCache cache = new OpenChannelsCache("test-cache", 2, opener);
        ChannelsAccessor readOnlyAccessor = cache.asReadOnly();
        ChannelsAccessor writableAccessor = cache.asWritable();

        try {
            TrackingFileChannel writableChannel = writableAccessor.executeOp(file, channel -> {
                writeSingleByte(channel);
                return (TrackingFileChannel) channel;
            });

            assertFalse(writableChannel.myReadOnly, "Precondition: first cached descriptor must be writable");

            readOnlyAccessor.executeOp(file, channel -> {
                assertThrows(NonWritableChannelException.class,
                    () -> writeSingleByte(channel),
                    "Read-only request must not reuse a cached writable descriptor");
                return null;
            });

            assertEquals(List.of(false, true), opener.openedModes(), "Read-only request should open a separate descriptor");
        }
        finally {
            readOnlyAccessor.closeChannel(file);
            writableAccessor.closeChannel(file);
        }
    }

    @Test
    @DisplayName("unlocked descriptor is closed on shared-capacity eviction")
    void unlockedDescriptorIsClosedOnSharedCapacityEviction(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("storage.bin");
        RecordingChannelOpener opener = new RecordingChannelOpener();
        OpenChannelsCache cache = new OpenChannelsCache("test-cache", 1, opener);
        ChannelsAccessor readOnlyAccessor = cache.asReadOnly();
        ChannelsAccessor writableAccessor = cache.asWritable();

        try {
            TrackingFileChannel readOnlyChannel = readOnlyAccessor.executeOp(file, channel -> (TrackingFileChannel) channel);
            TrackingFileChannel writableChannel = writableAccessor.executeOp(file, channel -> (TrackingFileChannel) channel);

            assertTrue(readOnlyChannel.wasClosed(), "Shared capacity should evict the first unlocked descriptor regardless of mode");
            assertFalse(writableChannel.wasClosed(), "Newest descriptor should remain cached");
            assertEquals(List.of(true, false), opener.openedModes(), "Both modes should be opened once");
            assertEquals(1, cache.getStatistics().getLoad(), "First access should be counted as a cache load");
            assertEquals(1, cache.getStatistics().getMiss(), "Second access should be counted as a miss caused by eviction");
            assertEquals(1, cache.getStatistics().getCapacity(), "Owner statistics must report shared physical capacity once");
        }
        finally {
            readOnlyAccessor.closeChannel(file);
            writableAccessor.closeChannel(file);
        }
    }

    @Test
    @DisplayName("read-only and writable descriptors are cached independently under shared owner")
    void readOnlyAndWritableDescriptorsAreCachedIndependentlyUnderSharedOwner(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("storage.bin");
        RecordingChannelOpener opener = new RecordingChannelOpener();
        OpenChannelsCache cache = new OpenChannelsCache("test-cache", 2, opener);
        ChannelsAccessor readOnlyAccessor = cache.asReadOnly();
        ChannelsAccessor writableAccessor = cache.asWritable();

        try {
            TrackingFileChannel readOnlyChannel = readOnlyAccessor.executeOp(file, channel -> (TrackingFileChannel) channel);
            TrackingFileChannel writableChannel = writableAccessor.executeOp(file, channel -> (TrackingFileChannel) channel);
            TrackingFileChannel cachedReadOnlyChannel = readOnlyAccessor.executeOp(file, channel -> (TrackingFileChannel) channel);
            TrackingFileChannel cachedWritableChannel = writableAccessor.executeOp(file, channel -> (TrackingFileChannel) channel);

            assertNotSame(readOnlyChannel, writableChannel, "Read-only and writable descriptors must be cached separately");
            assertSame(readOnlyChannel, cachedReadOnlyChannel, "Read-only access should reuse the read-only descriptor");
            assertSame(writableChannel, cachedWritableChannel, "Writable access should reuse the writable descriptor");
            assertFalse(readOnlyChannel.wasClosed(), "Read-only descriptor must remain cached");
            assertFalse(writableChannel.myReadOnly, "Writable descriptor should be opened in writable mode");
            assertFalse(writableChannel.wasClosed(), "Writable descriptor must remain cached");
            assertEquals(List.of(true, false), opener.openedModes(), "Read-only and writable descriptors should be opened once each");
            assertEquals(2, cache.getStatistics().getLoad(), "First access for each read/write mode should be counted as a load");
            assertEquals(0, cache.getStatistics().getMiss(), "No eviction should happen when capacity fits both descriptors");
            assertEquals(2, cache.getStatistics().getHit(), "Second access for each read/write mode should be counted as a hit");
            assertEquals(2, cache.getStatistics().getCapacity(), "Owner statistics must report shared physical capacity once");
        }
        finally {
            readOnlyAccessor.closeChannel(file);
            writableAccessor.closeChannel(file);
        }
    }

    @Test
    @DisplayName("closeChannel on one view does not close descriptor from another mode")
    void closeChannelOnOneViewDoesNotCloseDescriptorFromAnotherMode(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("storage.bin");
        RecordingChannelOpener opener = new RecordingChannelOpener();
        OpenChannelsCache cache = new OpenChannelsCache("test-cache", 2, opener);
        ChannelsAccessor readOnlyAccessor = cache.asReadOnly();
        ChannelsAccessor writableAccessor = cache.asWritable();

        TrackingFileChannel readOnlyChannel = readOnlyAccessor.executeOp(file, channel -> (TrackingFileChannel) channel);
        TrackingFileChannel writableChannel = writableAccessor.executeOp(file, channel -> (TrackingFileChannel) channel);

        readOnlyAccessor.closeChannel(file);

        assertTrue(readOnlyChannel.wasClosed(), "Read-only view should close the read-only descriptor");
        assertFalse(writableChannel.wasClosed(), "Read-only view must not close the writable descriptor");

        writableAccessor.closeChannel(file);

        assertEquals(1, writableChannel.myCloseCount, "Writable view should close the writable descriptor exactly once");
    }

    @Test
    @DisplayName("opening channel does not block unrelated cache access")
    void openingChannelDoesNotBlockUnrelatedCacheAccess(@TempDir Path tempDir) throws Exception {
        Path blockedFile = tempDir.resolve("blocked.bin");
        Path probeFile = tempDir.resolve("probe.bin");
        CountDownLatch openEntered = new CountDownLatch(1);
        CountDownLatch openMayFinish = new CountDownLatch(1);
        ChannelsAccessor accessor = new OpenChannelsCache(
            "test-cache",
            2,
            new BlockingOpenChannelOpener(blockedFile, openEntered, openMayFinish)
        ).asWritable();

        CountDownLatch openFinished = new CountDownLatch(1);
        AtomicReference<Throwable> openFailure = new AtomicReference<>();
        Thread openThread = runInThread("test-channel-opening", openFinished, openFailure, () -> {
            accessor.executeOp(blockedFile, channel -> null);
        });

        CountDownLatch probeFinished = new CountDownLatch(1);
        AtomicReference<Throwable> probeFailure = new AtomicReference<>();
        Thread probeThread = null;
        boolean probeCompletedWhileOpenIsBlocked;
        try {
            assertTrue(openEntered.await(5, TimeUnit.SECONDS), "Channel opener must enter the blocked open call");

            probeThread = runInThread("test-cache-probe-while-opening", probeFinished, probeFailure, () -> {
                accessor.executeOp(probeFile, channel -> null);
            });
            probeCompletedWhileOpenIsBlocked = probeFinished.await(2, TimeUnit.SECONDS);
        }
        finally {
            openMayFinish.countDown();
            openThread.join(5_000);
            if (probeThread != null) {
                probeThread.join(5_000);
            }
            accessor.closeChannel(blockedFile);
            accessor.closeChannel(probeFile);
        }

        throwFailure("Blocked channel open failed", openFailure);
        throwFailure("Cache probe failed", probeFailure);
        assertTrue(openFinished.getCount() == 0L, "Blocked channel open must finish after the test releases it");
        assertTrue(
            probeCompletedWhileOpenIsBlocked,
            "Opening a channel must not keep OpenChannelsCache locked for unrelated paths"
        );
    }

    @Test
    @DisplayName("closing evicted channel does not block unrelated cache access")
    void closingEvictedChannelDoesNotBlockUnrelatedCacheAccess(@TempDir Path tempDir) throws Exception {
        Path evictedFile = tempDir.resolve("evicted.bin");
        Path nextFile = tempDir.resolve("next.bin");
        Path probeFile = tempDir.resolve("probe.bin");
        CountDownLatch closeEntered = new CountDownLatch(1);
        CountDownLatch closeMayFinish = new CountDownLatch(1);
        ChannelsAccessor accessor = new OpenChannelsCache(
            "test-cache",
            1,
            new BlockingCloseChannelOpener(evictedFile, closeEntered, closeMayFinish)
        ).asWritable();

        accessor.executeOp(evictedFile, channel -> null);

        CountDownLatch evictionFinished = new CountDownLatch(1);
        AtomicReference<Throwable> evictionFailure = new AtomicReference<>();
        Thread evictionThread = runInThread("test-channel-eviction", evictionFinished, evictionFailure, () -> {
            accessor.executeOp(nextFile, channel -> null);
        });

        CountDownLatch probeFinished = new CountDownLatch(1);
        AtomicReference<Throwable> probeFailure = new AtomicReference<>();
        Thread probeThread = null;
        boolean probeCompletedWhileCloseIsBlocked;
        try {
            assertTrue(closeEntered.await(5, TimeUnit.SECONDS), "Eviction must start closing the cached channel");

            probeThread = runInThread("test-cache-probe-while-closing", probeFinished, probeFailure, () -> {
                accessor.executeOp(probeFile, channel -> null);
            });
            probeCompletedWhileCloseIsBlocked = probeFinished.await(2, TimeUnit.SECONDS);
        }
        finally {
            closeMayFinish.countDown();
            evictionThread.join(5_000);
            if (probeThread != null) {
                probeThread.join(5_000);
            }
            accessor.closeChannel(evictedFile);
            accessor.closeChannel(nextFile);
            accessor.closeChannel(probeFile);
        }

        throwFailure("Evicting cached channel failed", evictionFailure);
        throwFailure("Cache probe failed", probeFailure);
        assertTrue(evictionFinished.getCount() == 0L, "Eviction must finish after the test releases close");
        assertTrue(
            probeCompletedWhileCloseIsBlocked,
            "Closing an evicted channel must not keep OpenChannelsCache locked for unrelated paths"
        );
    }

    @Test
    @DisplayName("StorageLockContext assertNoOpenChannels reports descriptors from both mode views")
    void storageLockContextAssertNoOpenChannelsReportsDescriptorsFromBothModeViews(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("storage.bin");
        OpenChannelsCache cache = new OpenChannelsCache("test-cache", 2, new RecordingChannelOpener());
        StorageLockContext context = new StorageLockContext(false, cache.asReadOnly(), cache.asWritable());
        ChannelsAccessor readOnlyAccessor = context.getChannelsAccessor(true);
        ChannelsAccessor writableAccessor = context.getChannelsAccessor(false);

        try {
            context.assertNoOpenChannels(file);

            writableAccessor.executeOp(file, channel -> null);
            AssertionError writableError = assertThrows(AssertionError.class, () -> context.assertNoOpenChannels(file));
            assertTrue(writableError.getMessage().contains("writable accessor"), writableError.getMessage());

            readOnlyAccessor.executeOp(file, channel -> null);
            AssertionError bothModesError = assertThrows(AssertionError.class, () -> context.assertNoOpenChannels(file));
            assertTrue(bothModesError.getMessage().contains("read-only accessor"), bothModesError.getMessage());
            assertTrue(bothModesError.getMessage().contains("writable accessor"), bothModesError.getMessage());
            assertTrue(bothModesError.getMessage().contains(file.toString()), bothModesError.getMessage());

            writableAccessor.closeChannel(file);
            AssertionError readOnlyError = assertThrows(AssertionError.class, () -> context.assertNoOpenChannels(file));
            assertTrue(readOnlyError.getMessage().contains("read-only accessor"), readOnlyError.getMessage());

            readOnlyAccessor.closeChannel(file);
            context.assertNoOpenChannels(file);
        }
        finally {
            readOnlyAccessor.closeChannel(file);
            writableAccessor.closeChannel(file);
        }
    }

    @Test
    @DisplayName("locked read-only descriptor keeps cached channel and caches writable descriptor separately")
    void lockedReadOnlyDescriptorKeepsCachedChannelAndCachesWritableDescriptorSeparately(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("storage.bin");
        RecordingChannelOpener opener = new RecordingChannelOpener();
        OpenChannelsCache cache = new OpenChannelsCache("test-cache", 2, opener);
        ChannelsAccessor readOnlyAccessor = cache.asReadOnly();
        ChannelsAccessor writableAccessor = cache.asWritable();

        AtomicReference<TrackingFileChannel> readOnlyChannelRef = new AtomicReference<>();
        AtomicReference<TrackingFileChannel> writableChannelRef = new AtomicReference<>();

        try {
            readOnlyAccessor.executeOp(file, outerChannel -> {
                TrackingFileChannel readOnlyChannel = (TrackingFileChannel) outerChannel;
                readOnlyChannelRef.set(readOnlyChannel);
                TrackingFileChannel writableChannel = writableAccessor.executeOp(file, nestedChannel -> (TrackingFileChannel) nestedChannel);
                writableChannelRef.set(writableChannel);

                assertNotSame(readOnlyChannel, writableChannel, "Nested writable request must use a separate descriptor");
                assertFalse(readOnlyChannel.wasClosed(), "Locked read-only descriptor must stay cached");
                assertFalse(writableChannel.wasClosed(), "Nested writable descriptor should be cached, not closed as temporary");
                return null;
            });

            TrackingFileChannel cachedWritableChannel = writableAccessor.executeOp(file, channel -> (TrackingFileChannel) channel);

            assertSame(writableChannelRef.get(), cachedWritableChannel, "Nested writable descriptor should be reusable after outer operation completes");
            assertFalse(readOnlyChannelRef.get().wasClosed(), "Read-only descriptor must remain cached until its view closes it");
            assertFalse(writableChannelRef.get().wasClosed(), "Writable descriptor must remain cached until its view closes it");
            assertEquals(List.of(true, false), opener.openedModes(), "Read-only and writable descriptors should be opened once each");
            assertEquals(2, cache.getStatistics().getLoad(), "First access for each read/write mode should be counted as a load");
            assertEquals(0, cache.getStatistics().getMiss(), "Nested writable access should not bypass or miss the cache");
            assertEquals(1, cache.getStatistics().getHit(), "Repeated writable access should be counted as a hit");
        }
        finally {
            readOnlyAccessor.closeChannel(file);
            writableAccessor.closeChannel(file);
        }

        assertTrue(readOnlyChannelRef.get().wasClosed(), "Read-only descriptor must be closed by read-only view");
        assertEquals(1, writableChannelRef.get().myCloseCount, "Writable descriptor must be closed by writable view exactly once");
    }

    private static final class RecordingChannelOpener implements FileChannelOpener {
        private final List<TrackingFileChannel> myOpened = new ArrayList<>();

        @Override
        public FileChannel open(Path path, boolean readOnly) {
            TrackingFileChannel channel = new TrackingFileChannel(readOnly);
            myOpened.add(channel);
            return channel;
        }

        List<Boolean> openedModes() {
            return myOpened.stream().map(it -> it.myReadOnly).toList();
        }
    }

    /**
     * Blocks in {@link #open}
     */
    private static final class BlockingOpenChannelOpener implements FileChannelOpener {
        private final Path myPath;
        private final CountDownLatch myOpenEntered;
        private final CountDownLatch myOpenMayFinish;

        private BlockingOpenChannelOpener(Path path, CountDownLatch openEntered, CountDownLatch openMayFinish) {
            myPath = path;
            myOpenEntered = openEntered;
            myOpenMayFinish = openMayFinish;
        }

        @Override
        public FileChannel open(Path path, boolean readOnly) {
            if (path.equals(myPath)) {
                myOpenEntered.countDown();
                awaitLatch(myOpenMayFinish);
            }
            return new TrackingFileChannel(readOnly);
        }
    }

    /**
     * Returned {@link FileChannel} blocks on {@link FileChannel#close}
     */
    private static final class BlockingCloseChannelOpener implements FileChannelOpener {
        private final Path myPath;
        private final CountDownLatch myCloseEntered;
        private final CountDownLatch myCloseMayFinish;

        private BlockingCloseChannelOpener(Path path, CountDownLatch closeEntered, CountDownLatch closeMayFinish) {
            myPath = path;
            myCloseEntered = closeEntered;
            myCloseMayFinish = closeMayFinish;
        }

        @Override
        public FileChannel open(Path path, boolean readOnly) {
            if (path.equals(myPath)) {
                return new BlockingCloseFileChannel(readOnly, myCloseEntered, myCloseMayFinish);
            }
            else {
                return new TrackingFileChannel(readOnly);
            }
        }
    }

    /**
     * Blocks during {@link #close}
     */
    private static final class BlockingCloseFileChannel extends TrackingFileChannel {
        private final CountDownLatch myCloseEntered;
        private final CountDownLatch myCloseMayFinish;

        private BlockingCloseFileChannel(boolean readOnly, CountDownLatch closeEntered, CountDownLatch closeMayFinish) {
            super(readOnly);
            myCloseEntered = closeEntered;
            myCloseMayFinish = closeMayFinish;
        }

        @Override
        protected void implCloseChannel() {
            myCloseEntered.countDown();
            awaitLatch(myCloseMayFinish);
            super.implCloseChannel();
        }
    }

    private static void writeSingleByte(FileChannel channel) throws IOException {
        channel.write(ByteBuffer.wrap(new byte[]{42}));
    }

    private static class TrackingFileChannel extends FileChannel implements Resilient {
        private final boolean myReadOnly;

        private int myCloseCount = 0;

        private int myIdempotentOperationCount = 0;

        private long myPosition = 0;

        private TrackingFileChannel(boolean readOnly) {
            myReadOnly = readOnly;
        }

        boolean wasClosed() {
            return myCloseCount > 0;
        }

        @Override
        public <T> T executeOperation(FileChannelInterruptsRetryer.FileChannelIdempotentOperation<T> operation) throws IOException {
            myIdempotentOperationCount++;
            return operation.execute(this);
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            ensureOpen();
            return -1;
        }

        @Override
        public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
            ensureOpen();
            return -1;
        }

        @Override
        public int read(ByteBuffer dst, long position) throws IOException {
            ensureOpen();
            return -1;
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            ensureOpen();
            ensureWritable();
            int bytesWritten = src.remaining();
            src.position(src.limit());
            myPosition += bytesWritten;
            return bytesWritten;
        }

        @Override
        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
            ensureOpen();
            ensureWritable();
            long bytesWritten = 0L;
            for (int i = offset; i < offset + length; i++) {
                bytesWritten += write(srcs[i]);
            }
            return bytesWritten;
        }

        @Override
        public int write(ByteBuffer src, long position) throws IOException {
            ensureOpen();
            ensureWritable();
            int bytesWritten = src.remaining();
            src.position(src.limit());
            return bytesWritten;
        }

        @Override
        public long position() throws IOException {
            ensureOpen();
            return myPosition;
        }

        @Override
        public FileChannel position(long newPosition) throws IOException {
            ensureOpen();
            myPosition = newPosition;
            return this;
        }

        @Override
        public long size() throws IOException {
            ensureOpen();
            return 0;
        }

        @Override
        public FileChannel truncate(long size) throws IOException {
            ensureOpen();
            if (myPosition > size) {
                myPosition = size;
            }
            return this;
        }

        @Override
        public void force(boolean metaData) throws IOException {
            ensureOpen();
        }

        @Override
        public long transferTo(long position, long count, WritableByteChannel target) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long transferFrom(ReadableByteChannel src, long position, long count) {
            throw new UnsupportedOperationException();
        }

        @Override
        public MappedByteBuffer map(MapMode mode, long position, long size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public FileLock lock(long position, long size, boolean shared) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @Nullable FileLock tryLock(long position, long size, boolean shared) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void implCloseChannel() {
            myCloseCount++;
        }

        private void ensureOpen() throws ClosedChannelException {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
        }

        private void ensureWritable() {
            if (myReadOnly) {
                throw new NonWritableChannelException();
            }
        }
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws Throwable;
    }

    private static Thread runInThread(String name,
                                      CountDownLatch finished,
                                      AtomicReference<Throwable> failure,
                                      ThrowingAction action) {
        Thread thread = new Thread(() -> {
            try {
                action.run();
            }
            catch (Throwable t) {
                failure.set(t);
            }
            finally {
                finished.countDown();
            }
        });
        thread.setName(name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static void throwFailure(String message, AtomicReference<Throwable> failure) {
        Throwable throwable = failure.get();
        if (throwable != null) {
            throw new AssertionError(message, throwable);
        }
    }

    private static void awaitLatch(CountDownLatch latch) {
        boolean interrupted = false;
        while (true) {
            try {
                if (latch.await(30, TimeUnit.SECONDS)) {
                    break;
                }
            }
            catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
