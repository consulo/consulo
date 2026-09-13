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
package consulo.language.index.impl.internal.moduleAware;

import consulo.container.boot.ContainerPathManager;
import consulo.index.io.PersistentHashMap;
import consulo.index.io.data.IOUtil;
import consulo.logging.Logger;
import consulo.util.lang.function.ThrowableSupplier;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ModuleAwareIndexStorages {
    private static final Logger LOG = Logger.getInstance(ModuleAwareIndexStorages.class);

    private ModuleAwareIndexStorages() {
    }

    public static File cacheFile(String dirName, String fileName) throws IOException {
        Path root = Path.of(ContainerPathManager.get().getSystemPath(), "caches", dirName);
        Files.createDirectories(root);
        return root.resolve(fileName).toFile();
    }

    public static <K, V> PersistentHashMap<K, V> open(File file, ThrowableSupplier<PersistentHashMap<K, V>, IOException> factory) throws IOException {
        return IOUtil.openCleanOrResetBroken(factory, () -> {
            LOG.warn("Resetting " + file + ": the storage was not closed correctly or its format changed");
            IOUtil.deleteAllFilesStartingWith(file);
        });
    }
}
