// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.perFileVersion;

import consulo.util.io.FileUtil;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Consumer;

class VfsCreationStampChecker {
    private final Path myVfsCreationTimestampPath;

    VfsCreationStampChecker(Path vfsCreationTimestampPath) {
        myVfsCreationTimestampPath = vfsCreationTimestampPath;
    }

    void runIfVfsCreationStampMismatch(long expectedVfsCreationTimestamp, Consumer<String> cleanup) throws IOException {
        if (Files.exists(myVfsCreationTimestampPath.getParent())) {
            // directory exists. Check VFS creation timestamp and drop the file if it is outdated
            @Nullable String cleanupReason = null;
            if (Files.isRegularFile(myVfsCreationTimestampPath)) {
                byte[] read;
                try (InputStream stream = Files.newInputStream(myVfsCreationTimestampPath)) {
                    read = stream.readNBytes(Long.BYTES);
                }
                if (read.length != Long.BYTES) {
                    cleanupReason = myVfsCreationTimestampPath + " has only " + read.length + " bytes (" + Arrays.toString(read) + ")";
                }
                else {
                    long storedTimestamp = ByteBuffer.wrap(read).getLong(0);
                    if (expectedVfsCreationTimestamp != storedTimestamp) {
                        cleanupReason = "expected VFS creation timestamp = " + expectedVfsCreationTimestamp
                            + ", stored VFS creation timestamp = " + storedTimestamp;
                    }
                }
            }
            else {
                cleanupReason = myVfsCreationTimestampPath + " is not a file";
            }

            if (cleanupReason != null) {
                cleanup.accept(cleanupReason);
            }
        }
    }

    void createVfsTimestampMarkerFileIfAbsent(long expectedVfsCreationTimestamp) throws IOException {
        if (!Files.isRegularFile(myVfsCreationTimestampPath)) {
            byte[] expectedVfsCreationTimestampBytes = new byte[Long.BYTES];
            ByteBuffer.wrap(expectedVfsCreationTimestampBytes).putLong(0, expectedVfsCreationTimestamp);
            try (OutputStream out = Files.newOutputStream(myVfsCreationTimestampPath)) {
                out.write(expectedVfsCreationTimestampBytes);
            }
        }
    }

    void deleteCreationStamp() {
        if (Files.exists(myVfsCreationTimestampPath)) {
            FileUtil.deleteWithRenaming(myVfsCreationTimestampPath.toFile());
        }
    }
}
