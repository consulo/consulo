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
import consulo.disposer.Disposable;
import consulo.index.io.EnumeratorIntegerDescriptor;
import consulo.index.io.PersistentHashMap;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.data.DataInputOutputUtil;
import consulo.logging.Logger;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The variants a file of a library was last indexed under, with their payloads, primary first.
 * <p>
 * The index of a file is one for the whole application, but its variants come from the project which indexes it. A
 * header reached by two projects configured differently - every system header once two C/C++ projects are imported -
 * would lose one project's variant whenever the other indexed it, and each project would find the other's record
 * "drifted" and reindex it again. Remembering the variants lets the next indexing keep them next to its own: each
 * project's variant is built once, and switching projects reindexes nothing.
 */
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@Singleton
public final class ModuleAwareIndexKnownVariants implements Disposable {
    private static final Logger LOG = Logger.getInstance(ModuleAwareIndexKnownVariants.class);
    private static final String DIR_NAME = "module-aware-index-known-variants";
    private static final String FILE_NAME = "variants.dat";

    private static final DataExternalizer<List<VariantDescriptor>> EXTERNALIZER = new DataExternalizer<>() {
        @Override
        public void save(DataOutput out, List<VariantDescriptor> value) throws IOException {
            DataInputOutputUtil.writeINT(out, value.size());
            for (VariantDescriptor descriptor : value) {
                VariantDescriptor.write(out, descriptor);
            }
        }

        @Override
        public List<VariantDescriptor> read(DataInput in) throws IOException {
            int count = DataInputOutputUtil.readINT(in);
            List<VariantDescriptor> descriptors = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                descriptors.add(VariantDescriptor.read(in));
            }
            return descriptors;
        }
    };

    public static ModuleAwareIndexKnownVariants getInstance() {
        return Application.get().getInstance(ModuleAwareIndexKnownVariants.class);
    }

    private final @Nullable PersistentHashMap<Integer, List<VariantDescriptor>> myStorage;

    public ModuleAwareIndexKnownVariants() {
        myStorage = openStorage();
    }

    private static @Nullable PersistentHashMap<Integer, List<VariantDescriptor>> openStorage() {
        try {
            File file = ModuleAwareIndexStorages.cacheFile(DIR_NAME, FILE_NAME);
            return ModuleAwareIndexStorages.open(file, () -> new PersistentHashMap<>(file.toPath(), EnumeratorIntegerDescriptor.INSTANCE, EXTERNALIZER));
        }
        catch (IOException e) {
            LOG.error("Failed to open " + DIR_NAME + " storage; files reached by several projects will be reindexed on switches", e);
            return null;
        }
    }

    public List<VariantDescriptor> get(int fileId) {
        if (myStorage == null) {
            return List.of();
        }
        try {
            List<VariantDescriptor> descriptors = myStorage.get(fileId);
            return descriptors == null ? List.of() : descriptors;
        }
        catch (IOException e) {
            LOG.warn("Failed to read the known variants of fileId=" + fileId, e);
            return List.of();
        }
    }

    public void put(int fileId, List<VariantDescriptor> descriptors) {
        if (myStorage == null) {
            return;
        }
        try {
            myStorage.put(fileId, List.copyOf(descriptors));
        }
        catch (IOException e) {
            LOG.warn("Failed to store the known variants of fileId=" + fileId, e);
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
                LOG.warn("Failed to close " + DIR_NAME + " storage", e);
            }
        }
    }
}
