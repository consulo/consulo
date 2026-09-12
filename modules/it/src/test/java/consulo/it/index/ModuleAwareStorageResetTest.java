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
package consulo.it.index;

import consulo.application.Application;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.PersistentHashMap;
import consulo.it.HeadlessApplicationExtension;
import consulo.language.index.impl.internal.moduleAware.ModuleAwareIndexStorages;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(HeadlessApplicationExtension.class)
public class ModuleAwareStorageResetTest {
    private static final int DIRTY_MAGIC = 0xbabe1977;

    @Test
    public void cleanStorageReopensWithItsContent(Application application) throws Exception {
        File file = Files.createTempDirectory("consulo-it-storage-clean").resolve("meta.dat").toFile();

        PersistentHashMap<String, String> first = ModuleAwareIndexStorages.open(file, () -> newMap(file));
        first.put("k", "v");
        first.close();

        PersistentHashMap<String, String> second = ModuleAwareIndexStorages.open(file, () -> newMap(file));
        try {
            assertThat(second.get("k")).isEqualTo("v");
        }
        finally {
            second.close();
        }
    }

    @Test
    public void storageLeftDirtyByAKilledSessionIsReset(Application application) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-storage-dirty");
        File file = directory.resolve("meta.dat").toFile();

        PersistentHashMap<String, String> first = ModuleAwareIndexStorages.open(file, () -> newMap(file));
        first.put("k", "v");
        first.close();
        try (RandomAccessFile header = new RandomAccessFile(file, "rw")) {
            header.writeInt(DIRTY_MAGIC);
        }

        PersistentHashMap<String, String> reopened = ModuleAwareIndexStorages.open(file, () -> newMap(file));
        try {
            assertThat(reopened.get("k")).isNull();
            reopened.put("k2", "v2");
            assertThat(reopened.get("k2")).isEqualTo("v2");
        }
        finally {
            reopened.close();
        }
    }

    private static PersistentHashMap<String, String> newMap(File file) throws java.io.IOException {
        return new PersistentHashMap<>(file, EnumeratorStringDescriptor.INSTANCE, EnumeratorStringDescriptor.INSTANCE);
    }
}
