/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.it.vfs;

import consulo.virtualFileSystem.FileAttribute;
import consulo.virtualFileSystem.impl.internal.SpecializedFileAttributes;
import consulo.virtualFileSystem.impl.internal.SpecializedFileAttributes.IntFileAttributeAccessor;
import consulo.virtualFileSystem.impl.internal.SpecializedFileAttributes.LongFileAttributeAccessor;
import consulo.virtualFileSystem.internal.FSRecordsProxy;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author VISTALL
 * @since 2026-09-06
 */
public class FastFileAttributesTest {
    private static final int MAX_FILE_ID = 200_000;
    private static final int FILE_IDS_TO_CHECK = 100_000;

    private static class TestFSRecordsProxy implements FSRecordsProxy {
        private final long myCreationTimestamp;
        private final List<Closeable> myCloseables = new ArrayList<>();
        private final List<FileIdIndexedStorage> myStorages = new ArrayList<>();

        private TestFSRecordsProxy(long creationTimestamp) {
            myCreationTimestamp = creationTimestamp;
        }

        @Override
        public void handleError(Throwable e) throws RuntimeException, Error {
            throw new RuntimeException(e);
        }

        @Override
        public DataOutputStream writeAttribute(int fileId, FileAttribute att) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @Nullable DataInputStream readAttributeWithLock(int fileId, FileAttribute att) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getCreationTimestamp() {
            return myCreationTimestamp;
        }

        @Override
        public int getMaxId() {
            return MAX_FILE_ID;
        }

        @Override
        public void addCloseable(Closeable closeable) {
            myCloseables.add(closeable);
        }

        @Override
        public void addFileIdIndexedStorage(FileIdIndexedStorage storage) {
            myStorages.add(storage);
        }
    }

    private static long valueOf(int fileId) {
        return fileId * 1_000_003L + 17L;
    }

    @Test
    public void longAttributeSurvivesReopen(@TempDir Path tempDir) throws Exception {
        FileAttribute attribute = new FileAttribute("test.fast.long.reopen", 1, true);
        Path storagePath = tempDir.resolve("fastAttributes").resolve("test.fast.long.reopen.dat").toAbsolutePath();

        TestFSRecordsProxy vfs = new TestFSRecordsProxy(1000L);

        LongFileAttributeAccessor accessor = SpecializedFileAttributes.specializeAsFastLong(vfs, attribute, storagePath);
        for (int fileId = 1; fileId <= FILE_IDS_TO_CHECK; fileId++) {
            accessor.write(fileId, valueOf(fileId));
        }
        for (int fileId = 1; fileId <= FILE_IDS_TO_CHECK; fileId++) {
            assertEquals(valueOf(fileId), accessor.read(fileId, 0), "fileId=" + fileId);
        }
        accessor.close();

        LongFileAttributeAccessor reopened = SpecializedFileAttributes.specializeAsFastLong(vfs, attribute, storagePath);
        try {
            for (int fileId = 1; fileId <= FILE_IDS_TO_CHECK; fileId++) {
                assertEquals(valueOf(fileId), reopened.read(fileId, 0), "fileId=" + fileId);
            }
        }
        finally {
            reopened.close();
        }
    }

    @Test
    public void versionMismatchClearsStorage(@TempDir Path tempDir) throws Exception {
        FileAttribute attribute = new FileAttribute("test.fast.long.version", 1, true);
        Path storagePath = tempDir.resolve("fastAttributes").resolve("test.fast.long.version.dat").toAbsolutePath();

        TestFSRecordsProxy vfs = new TestFSRecordsProxy(1000L);

        LongFileAttributeAccessor accessor = SpecializedFileAttributes.specializeAsFastLong(vfs, attribute, storagePath);
        for (int fileId = 1; fileId <= FILE_IDS_TO_CHECK; fileId++) {
            accessor.write(fileId, valueOf(fileId));
        }
        accessor.close();

        LongFileAttributeAccessor reopened =
            SpecializedFileAttributes.specializeAsFastLong(vfs, attribute.newVersion(2), storagePath);
        try {
            for (int fileId = 1; fileId <= FILE_IDS_TO_CHECK; fileId++) {
                assertEquals(0L, reopened.read(fileId, 0), "fileId=" + fileId);
            }
        }
        finally {
            reopened.close();
        }
    }

    @Test
    public void vfsCreationTagMismatchClearsStorage(@TempDir Path tempDir) throws Exception {
        FileAttribute attribute = new FileAttribute("test.fast.long.tag", 1, true);
        Path storagePath = tempDir.resolve("fastAttributes").resolve("test.fast.long.tag.dat").toAbsolutePath();

        LongFileAttributeAccessor accessor =
            SpecializedFileAttributes.specializeAsFastLong(new TestFSRecordsProxy(1000L), attribute, storagePath);
        for (int fileId = 1; fileId <= FILE_IDS_TO_CHECK; fileId++) {
            accessor.write(fileId, valueOf(fileId));
        }
        accessor.close();

        LongFileAttributeAccessor reopened =
            SpecializedFileAttributes.specializeAsFastLong(new TestFSRecordsProxy(2000L), attribute, storagePath);
        try {
            for (int fileId = 1; fileId <= FILE_IDS_TO_CHECK; fileId++) {
                assertEquals(0L, reopened.read(fileId, 0), "fileId=" + fileId);
            }
        }
        finally {
            reopened.close();
        }
    }

    @Test
    public void intAttributeSurvivesReopen(@TempDir Path tempDir) throws Exception {
        FileAttribute attribute = new FileAttribute("test.fast.int.reopen", 1, true);
        Path storagePath = tempDir.resolve("fastAttributes").resolve("test.fast.int.reopen.dat").toAbsolutePath();

        TestFSRecordsProxy vfs = new TestFSRecordsProxy(1000L);

        IntFileAttributeAccessor accessor = SpecializedFileAttributes.specializeAsFastInt(vfs, attribute, storagePath);
        for (int fileId = 1; fileId <= FILE_IDS_TO_CHECK; fileId++) {
            accessor.write(fileId, fileId * 3 + 1);
        }
        accessor.close();

        IntFileAttributeAccessor reopened = SpecializedFileAttributes.specializeAsFastInt(vfs, attribute, storagePath);
        try {
            for (int fileId = 1; fileId <= FILE_IDS_TO_CHECK; fileId++) {
                assertEquals(fileId * 3 + 1, reopened.read(fileId, 0), "fileId=" + fileId);
            }
        }
        finally {
            reopened.close();
        }
    }
}
