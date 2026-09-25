// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.index.io.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

public abstract class StorageTestBase {
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
        return new Storage(fileName);
    }

    @AfterEach
    public void tearDown() {
        myStorage.close();
    }
}
