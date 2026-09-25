// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.impl.internal;

import consulo.index.io.PagePool;
import consulo.index.io.storage.AbstractRecordsTable;
import consulo.index.io.storage.RecordIdIterator;
import consulo.index.io.storage.Storage;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

public class CompactStorageTest {
    @TempDir
    Path tempDir;
    private Path storagePath;

    protected Storage myStorage;

    @BeforeEach
    public void setUpStorage() throws IOException {
        if (storagePath == null) {
            storagePath = tempDir.resolve("storage").resolve("test-storage");
        }
        myStorage = createStorage(storagePath);
    }

    protected Storage createStorage(Path fileName) throws IOException {
        return new CompactStorage(fileName);
    }

    @AfterEach
    public void tearDown() {
        myStorage.close();
    }

    @Test
    public void testCompactAndIterators() throws IOException {
        IntList recordsList = IntLists.newArrayList();
        // 1000 records after deletion greater than 3M limit for init time compaction
        int recordCount = 2000;
        for (int i = 0; i < recordCount; ++i) {
            recordsList.add(createTestRecord(myStorage));
        }
        int physicalRecordCount = myStorage.getLiveRecordsCount();
        for (int i = 0; i < recordCount / 2; ++i) {
            myStorage.deleteRecord(recordsList.get(i));
        }
        int logicalRecordCount = countLiveLogicalRecords();
        assertThat(logicalRecordCount).isEqualTo(recordCount / 2);

        int removedRecordId = recordsList.get(0);
        assertThat(myStorage.readStream(removedRecordId).available()).describedAs("No content for reading removed record").isEqualTo(0);

        // compact is triggered
        myStorage.close();
        setUpStorage();
        assertThat(myStorage.getLiveRecordsCount()).isEqualTo(physicalRecordCount / 2);

        logicalRecordCount = 0;

        RecordIdIterator recordIdIterator = myStorage.createRecordIdIterator();
        while (recordIdIterator.hasNextId()) {
            boolean validId = recordIdIterator.validId();
            int nextId = recordIdIterator.nextId();
            if (!validId) {
                continue;
            }
            ++logicalRecordCount;
            checkTestRecord(nextId);
        }

        assertThat(logicalRecordCount).isEqualTo(recordCount / 2);
    }

    protected int countLiveLogicalRecords() throws IOException {
        RecordIdIterator recordIdIterator = myStorage.createRecordIdIterator();
        int logicalRecordCount = 0;

        while (recordIdIterator.hasNextId()) {
            boolean validId = recordIdIterator.validId();
            recordIdIterator.nextId();
            if (!validId) {
                continue;
            }
            ++logicalRecordCount;
        }
        return logicalRecordCount;
    }

    private static final int TIMES_LIMIT = 10000;

    static int createTestRecord(Storage storage) throws IOException {
        int r = storage.createNewRecord();

        try (DataOutputStream out = new DataOutputStream(storage.appendStream(r))) {
            Random random = new Random(r);
            for (int i = 0; i < TIMES_LIMIT; i++) {
                out.writeInt(random.nextInt());
            }
        }

        return r;
    }

    private void checkTestRecord(int id) throws IOException {
        try (DataInputStream stream = myStorage.readStream(id)) {
            Random random = new Random(id);
            for (int i = 0; i < TIMES_LIMIT; i++) {
                assertThat(stream.readInt()).isEqualTo(random.nextInt());
            }
        }
    }

    static final class CompactStorage extends Storage {
        CompactStorage(Path fileName) throws IOException {
            super(fileName);
        }

        @Override
        protected AbstractRecordsTable createRecordsTable(PagePool pool, Path recordsFile) throws IOException {
            return new CompactRecordsTable(recordsFile, pool, false);
        }
    }
}
