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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.container.boot.ContainerPathManager;
import consulo.disposer.Disposable;
import consulo.index.io.ID;
import consulo.index.io.PersistentHashMap;
import consulo.logging.Logger;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persistent store for {@link OptionsMeta} keyed by {@code (indexUniqueId, fileId)}. Lives
 * under {@code ~/.consulo/system/caches/module-aware-index-meta/meta.dat}.
 *
 * <p>App-scoped service. Survives IOException during init by operating in degraded mode —
 * get returns {@code null} and put becomes a no-op, so callers never see exceptions from
 * corrupt or missing storage. The expected outcome of such failure is "everything looks
 * stale → reindex" which is the safe default.</p>
 */
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@Singleton
public final class ModuleAwareIndexMetaStorage implements Disposable {
    private static final Logger LOG = Logger.getInstance(ModuleAwareIndexMetaStorage.class);
    private static final String DIR_NAME = "module-aware-index-meta";
    private static final String FILE_NAME = "meta.dat";

    public static ModuleAwareIndexMetaStorage getInstance() {
        return Application.get().getInstance(ModuleAwareIndexMetaStorage.class);
    }

    private final @Nullable PersistentHashMap<MetaKey, OptionsMeta> myStorage;

    public ModuleAwareIndexMetaStorage() {
        myStorage = openStorage();
    }

    private static @Nullable PersistentHashMap<MetaKey, OptionsMeta> openStorage() {
        try {
            Path root = Path.of(ContainerPathManager.get().getSystemPath(), "caches", DIR_NAME);
            Files.createDirectories(root);
            File file = root.resolve(FILE_NAME).toFile();
            return new PersistentHashMap<>(file, MetaKeyDescriptor.INSTANCE, OptionsMetaExternalizer.INSTANCE);
        }
        catch (IOException e) {
            LOG.error("Failed to open module-aware-index-meta storage; operating in degraded mode", e);
            return null;
        }
    }

    public @Nullable OptionsMeta get(ID<?, ?> indexId, int fileId) {
        if (myStorage == null) {
            return null;
        }
        try {
            return myStorage.get(new MetaKey(indexId.getUniqueId(), fileId));
        }
        catch (IOException e) {
            LOG.warn("Failed to read OptionsMeta for " + indexId + ", fileId=" + fileId, e);
            return null;
        }
    }

    public void put(ID<?, ?> indexId, int fileId, OptionsMeta meta) {
        if (myStorage == null) {
            return;
        }
        try {
            myStorage.put(new MetaKey(indexId.getUniqueId(), fileId), meta);
        }
        catch (IOException e) {
            LOG.warn("Failed to store OptionsMeta for " + indexId + ", fileId=" + fileId, e);
        }
    }

    public void delete(ID<?, ?> indexId, int fileId) {
        if (myStorage == null) {
            return;
        }
        try {
            myStorage.remove(new MetaKey(indexId.getUniqueId(), fileId));
        }
        catch (IOException e) {
            LOG.warn("Failed to delete OptionsMeta for " + indexId + ", fileId=" + fileId, e);
        }
    }

    public void flush() {
        if (myStorage != null) {
            myStorage.force();
        }
    }

    @Override
    public void dispose() {
        if (myStorage != null) {
            try {
                myStorage.close();
            }
            catch (IOException e) {
                LOG.warn("Failed to close module-aware-index-meta storage", e);
            }
        }
    }
}
