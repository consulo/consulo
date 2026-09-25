// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.index.io;

import consulo.util.io.FileUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class CompressedAppendableFileTest {
    @Test
    void testCreateParentDirWhenSave(@TempDir Path tempDir) throws Exception {
        Path compressedFile = tempDir.resolve("Test.compressed");
        withCompressedAppendableFile(compressedFile, appendableFile -> {
            byte[] byteArray = new byte[1];
            appendableFile.append(byteArray, 1);
            appendableFile.force();
            FileUtil.delete(compressedFile.getParent().toFile());
            appendableFile.append(byteArray, 1);
            return null;
        });
    }

    @Test
    void testSizeUpdateBug(@TempDir Path tempDir) throws Exception {
        Path compressedFile = tempDir.resolve("Test.compressed");
        byte[] singleByteArray = new byte[1];
        withCompressedAppendableFile(compressedFile, appendableFile -> {
            appendableFile.append(singleByteArray, singleByteArray.length);
            return null;
        });

        byte[] multiByteArray = new byte[CompressedAppendableFile.PAGE_LENGTH - 1];
        withCompressedAppendableFile(compressedFile, appendableFile -> {
            appendableFile.append(multiByteArray, multiByteArray.length);
            return null;
        });

        withCompressedAppendableFile(compressedFile, appendableFile -> {
            assertThat((int) appendableFile.length()).isEqualTo(CompressedAppendableFile.PAGE_LENGTH);
            return null;
        });
    }

    @Test
    void testConcurrencyStress(@TempDir Path tempDir) throws Exception {
        Path compressedFile = tempDir.resolve("Test.compressed");
        AtomicLong bytesWritten = new AtomicLong();
        ExecutorService executorService = Executors.newCachedThreadPool();
        try {
            withCompressedAppendableFile(compressedFile, appendableFile -> {
                int max = 1000 * CompressedAppendableFile.PAGE_LENGTH;
                CountDownLatch startLatch = new CountDownLatch(1);
                int numberOfThreads = 3;
                CountDownLatch proceedLatch = new CountDownLatch(numberOfThreads);

                Callable<Void> writer = () -> {
                    startLatch.await();
                    try {
                        byte[] byteArray = new byte[3];

                        for (int i = 1; i <= max; i++) {
                            byteArray[0] = (byte) (i & 0xFF);
                            byteArray[1] = (byte) (i + 1 & 0xFF);
                            byteArray[2] = (byte) (i + 2 & 0xFF);
                            appendableFile.append(byteArray, byteArray.length);
                            bytesWritten.addAndGet(byteArray.length);
                            //if (i % 100 == 0) TimeoutUtil.sleep(1);
                        }
                    }
                    finally {
                        proceedLatch.countDown();
                    }
                    return null;
                };

                List<Future<?>> futures = new ArrayList<>();
                for (int i = 0; i < numberOfThreads; i++) {
                    futures.add(executorService.submit(writer));
                }

                Callable<Void> flusher = () -> {
                    startLatch.await();
                    while (proceedLatch.getCount() != 0L) {
                        Thread.sleep(1);
                    }
                    return null;
                };
                Future<Void> thread = executorService.submit(flusher);
                try {
                    startLatch.countDown();
                    proceedLatch.await();

                    assertThat(appendableFile.length()).isEqualTo(bytesWritten.get());
                }
                finally {
                    thread.get();
                    for (Future<?> future : futures) {
                        future.get();
                    }
                }
                return null;
            });

            withCompressedAppendableFile(compressedFile, appendableFile -> {
                assertThat(appendableFile.length()).isEqualTo(bytesWritten.get());
                return null;
            });
        }
        finally {
            executorService.shutdownNow();
        }
    }

    @FunctionalInterface
    private interface CompressedFileAction<T> {
        T run(CompressedAppendableFile appendableFile) throws Exception;
    }

    private static <T> T withCompressedAppendableFile(Path path, CompressedFileAction<T> action) throws Exception {
        CompressedAppendableFile appendableFile = new CompressedAppendableFile(path);
        try {
            return action.run(appendableFile);
        }
        finally {
            appendableFile.dispose();
        }
    }
}
