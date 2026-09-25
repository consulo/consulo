// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.index.io;

import consulo.index.io.data.IOUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ResilientFileChannelTest {
    @TempDir
    Path myTempDirectory;

    private final ByteBuffer myByteBuf1 = ByteBuffer.wrap(new byte[]{1, 2, 3, 4});
    private final ByteBuffer myByteBuf2 = ByteBuffer.wrap(new byte[]{5, 6, 7, 8});

    @AfterEach
    void clearInterruptedStatus() {
        Thread.interrupted();
    }

    @Test
    @DisplayName("without interruption RFC writes and reads as regular FileChannel")
    void withoutInterruptionRFCWritesAndReadsAsRegularFileChannel() throws IOException {
        try (ResilientFileChannel it = new ResilientFileChannel(newTempFile(), READ, WRITE, CREATE)) {
            it.write(myByteBuf1);
            it.write(myByteBuf2);

            ByteBuffer readBuffer1 = ByteBuffer.allocate(4);
            it.read(readBuffer1, 0);

            ByteBuffer readBuffer2 = ByteBuffer.allocate(4);
            it.read(readBuffer2, 4);

            assertEquals(IOUtil.toString(myByteBuf1), IOUtil.toString(readBuffer1));
            assertEquals(IOUtil.toString(myByteBuf2), IOUtil.toString(readBuffer2));
        }
    }

    @Test
    @DisplayName("read operations successfully reads its bytes after interrupt")
    void readOperationsSuccessfullyReadsItsBytesAfterInterrupt() throws IOException {
        try (ResilientFileChannel it = new ResilientFileChannel(newTempFile(), READ, WRITE, CREATE)) {
            it.write(myByteBuf1);
            it.write(myByteBuf2);

            ByteBuffer readBuffer1 = ByteBuffer.allocate(4);
            it.read(readBuffer1, 0);

            Thread currentThread = Thread.currentThread();
            currentThread.interrupt();

            ByteBuffer readBuffer2 = ByteBuffer.allocate(4);
            it.read(readBuffer2, 0);

            assertEquals(myByteBuf1, readBuffer1);
            assertEquals(myByteBuf2, readBuffer2);
        }
    }

    @Test
    @DisplayName("2 reads operations successfully read their bytes after interrupt")
    void twoReadsOperationsSuccessfullyReadTheirBytesAfterInterrupt() throws IOException {
        try (ResilientFileChannel it = new ResilientFileChannel(newTempFile(), READ, WRITE, CREATE)) {
            it.write(myByteBuf1);
            it.write(myByteBuf2);

            Thread currentThread = Thread.currentThread();
            currentThread.interrupt();

            ByteBuffer readBuffer1 = ByteBuffer.allocate(4);
            it.read(readBuffer1, 0);

            assertTrue(Thread.currentThread().isInterrupted());

            ByteBuffer readBuffer2 = ByteBuffer.allocate(4);
            it.read(readBuffer2, 0);

            assertEquals(myByteBuf1, readBuffer1);
            assertEquals(myByteBuf2, readBuffer2);
        }
    }

    @Test
    @DisplayName("2 reads operations successfully read their bytes via RFC freshly opened after interrupt")
    void twoReadsOperationsSuccessfullyReadTheirBytesViaRFCFreshlyOpenedAfterInterrupt() throws IOException {
        Path file = newTempFile();
        try (ResilientFileChannel it = new ResilientFileChannel(file, READ, WRITE, CREATE)) {
            it.write(myByteBuf1);
            it.write(myByteBuf2);
        }

        Thread.currentThread().interrupt();

        ByteBuffer readBuffer1 = ByteBuffer.allocate(4);
        ByteBuffer readBuffer2 = ByteBuffer.allocate(4);

        try (ResilientFileChannel it = new ResilientFileChannel(file, READ, WRITE, CREATE)) {
            it.read(readBuffer1, 0);
            it.read(readBuffer2, 0);
        }

        assertEquals(myByteBuf1, readBuffer1);
        assertEquals(myByteBuf2, readBuffer2);
    }

    @Test
    @DisplayName("attempt to write to read-only RFC throws exception")
    void attemptToWriteToReadOnlyRFCThrowsException() throws IOException {
        Path file = newTempFile();
        boolean secondOperationStarted = false;
        boolean exceptionHappened = false;

        try {
            try (ResilientFileChannel it = new ResilientFileChannel(file, READ)) {
                it.write(myByteBuf1);
                secondOperationStarted = true;
                it.write(myByteBuf2);
            }
            fail("Should not executed without exception");
        }
        catch (NonWritableChannelException e) {
            exceptionHappened = true;
        }

        assertTrue(exceptionHappened);
        assertFalse(secondOperationStarted);
    }

    @Test
    @DisplayName("independent writes from multiple threads could be read from multiple threads")
    void independentWritesFromMultipleThreadsCouldBeReadFromMultipleThreads() throws Exception {
        Path file = newTempFile();
        ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        try {
            try (ResilientFileChannel fileHandle = new ResilientFileChannel(file, READ, WRITE, CREATE)) {
                List<Future<?>> writes = new ArrayList<>();
                for (int i = 0; i <= 100; i++) {
                    writes.add(threadPool.submit(() -> {
                        long threadId = Thread.currentThread().threadId();
                        byte[] threadUniqueData = {(byte) threadId, (byte) threadId, (byte) threadId, (byte) threadId};

                        fileHandle.write(ByteBuffer.wrap(threadUniqueData), threadId * threadUniqueData.length);
                        return null;
                    }));
                }
                for (Future<?> write : writes) {
                    write.get(1, MINUTES);
                }

                Thread.currentThread().interrupt();
                fileHandle.size();
                assertTrue(Thread.interrupted());

                List<Future<?>> reads = new ArrayList<>();
                for (int i = 0; i <= 100; i++) {
                    reads.add(threadPool.submit(() -> {
                        long threadId = Thread.currentThread().threadId();
                        ByteBuffer expectedData = ByteBuffer.wrap(new byte[]{(byte) threadId, (byte) threadId, (byte) threadId, (byte) threadId});
                        ByteBuffer actualData = ByteBuffer.allocate(expectedData.capacity());

                        assertEquals(
                            actualData.capacity(),
                            fileHandle.read(actualData, threadId * actualData.capacity()),
                            "All expected bytes are read"
                        );

                        assertEquals(
                            IOUtil.toString(expectedData),
                            IOUtil.toString(actualData.rewind()),
                            "Bytes read should be same as written before"
                        );
                        return null;
                    }));
                }
                for (Future<?> read : reads) {
                    read.get(1, MINUTES);
                }
            }
        }
        finally {
            threadPool.shutdown();
            for (int i = 0; i < 60; i++) {
                if (threadPool.awaitTermination(1, SECONDS)) {
                    break;
                }
            }
            assertTrue(threadPool.isTerminated());
        }
    }

    @Test
    @DisplayName("thread interrupted status is not cleared while RFC work around interruption")
    void threadInterruptedStatusIsNotClearedWhileRFCWorkAroundInterruption() throws IOException {
        Path file = newTempFile();
        try (ResilientFileChannel it = new ResilientFileChannel(file, READ, WRITE, CREATE)) {
            it.write(myByteBuf1);
        }

        ByteBuffer readBuffer1 = ByteBuffer.allocate(myByteBuf1.capacity());

        Thread.currentThread().interrupt();
        try (ResilientFileChannel it = new ResilientFileChannel(file, READ, WRITE, CREATE)) {
            it.read(readBuffer1, 0);
        }
        assertTrue(
            Thread.currentThread().isInterrupted(),
            "Thread interrupted status is not cleared"
        );
    }

    @Test
    @DisplayName("RFChannel throws exception if closed before read")
    void rfChannelThrowsExceptionIfClosedBeforeRead() throws IOException {
        Path file = newTempFile();
        ByteBuffer readBuffer1 = ByteBuffer.allocate(myByteBuf1.capacity());

        ResilientFileChannel channel = new ResilientFileChannel(file, READ, WRITE, CREATE);
        channel.write(myByteBuf1);
        channel.close();
        try {
            channel.read(readBuffer1, 0);
        }
        catch (ClosedChannelException e) {
            //OK
        }
    }

    private Path newTempFile() throws IOException {
        return Files.createFile(myTempDirectory.resolve("test.txt"));
    }
}
