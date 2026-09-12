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
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.PersistentHashMap;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.data.DataInputOutputUtil;
import consulo.index.io.data.IOUtil;
import consulo.logging.Logger;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The option values the analyser recorded for a file, per provider: the primary variant first, then every secondary
 * variant the file is indexed under.
 */
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@Singleton
public final class ModuleAwareIndexOptionValueStorage implements Disposable {
    private static final Logger LOG = Logger.getInstance(ModuleAwareIndexOptionValueStorage.class);

    private static final String DIR_NAME = "module-aware-index-meta";
    private static final String FILE_NAME = "values.dat";
    private static final int LIST_FORMAT = -2;

    public record StoredOption(OptionsMeta.VariantTag tag, byte[] payload, String displayName) {
        public boolean sameValue(StoredOption other) {
            return tag == other.tag && Arrays.equals(payload, other.payload);
        }
    }

    private static final class StoredOptionsExternalizer implements DataExternalizer<List<StoredOption>> {
        static final StoredOptionsExternalizer INSTANCE = new StoredOptionsExternalizer();

        @Override
        public void save(DataOutput out, List<StoredOption> value) throws IOException {
            DataInputOutputUtil.writeINT(out, LIST_FORMAT);
            DataInputOutputUtil.writeINT(out, value.size());
            for (StoredOption option : value) {
                writeOption(out, option);
            }
        }

        @Override
        public List<StoredOption> read(DataInput in) throws IOException {
            int first = DataInputOutputUtil.readINT(in);
            if (first != LIST_FORMAT) {
                return List.of(readOption(in, first, ""));
            }
            int count = DataInputOutputUtil.readINT(in);
            List<StoredOption> options = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                int ordinal = DataInputOutputUtil.readINT(in);
                options.add(readOption(in, ordinal, null));
            }
            return List.copyOf(options);
        }

        private static void writeOption(DataOutput out, StoredOption option) throws IOException {
            DataInputOutputUtil.writeINT(out, option.tag().ordinal());
            DataInputOutputUtil.writeINT(out, option.payload().length);
            out.write(option.payload());
            IOUtil.writeUTF(out, option.displayName());
        }

        private static StoredOption readOption(DataInput in, int ordinal, @Nullable String legacyDisplayName) throws IOException {
            OptionsMeta.VariantTag[] tags = OptionsMeta.VariantTag.values();
            if (ordinal < 0 || ordinal >= tags.length) {
                throw new IOException("Unknown VariantTag ordinal: " + ordinal);
            }
            byte[] payload = new byte[DataInputOutputUtil.readINT(in)];
            in.readFully(payload);
            String displayName = legacyDisplayName != null ? legacyDisplayName : IOUtil.readUTF(in);
            return new StoredOption(tags[ordinal], payload, displayName);
        }
    }

    public static ModuleAwareIndexOptionValueStorage getInstance() {
        return Application.get().getInstance(ModuleAwareIndexOptionValueStorage.class);
    }

    private final @Nullable PersistentHashMap<String, List<StoredOption>> myStorage;

    public ModuleAwareIndexOptionValueStorage() {
        myStorage = openStorage();
    }

    private static @Nullable PersistentHashMap<String, List<StoredOption>> openStorage() {
        try {
            File file = ModuleAwareIndexStorages.cacheFile(DIR_NAME, FILE_NAME);
            return ModuleAwareIndexStorages.open(file, () -> new PersistentHashMap<>(file, EnumeratorStringDescriptor.INSTANCE, StoredOptionsExternalizer.INSTANCE));
        }
        catch (IOException e) {
            LOG.error("Failed to open module-aware option value storage; operating without recorded values", e);
            return null;
        }
    }

    private static String key(String providerId, int fileId) {
        return providerId + '#' + fileId;
    }

    public @Nullable StoredOption get(String providerId, int fileId) {
        List<StoredOption> variants = getVariants(providerId, fileId);
        return variants.isEmpty() ? null : variants.get(0);
    }

    public List<StoredOption> getVariants(String providerId, int fileId) {
        if (myStorage == null) {
            return List.of();
        }
        try {
            List<StoredOption> variants = myStorage.get(key(providerId, fileId));
            return variants == null ? List.of() : variants;
        }
        catch (IOException e) {
            LOG.warn("Failed to read option values for " + providerId + ", fileId=" + fileId, e);
            return List.of();
        }
    }

    public void putVariants(String providerId, int fileId, List<StoredOption> variants) {
        if (myStorage == null) {
            return;
        }
        try {
            myStorage.put(key(providerId, fileId), List.copyOf(variants));
        }
        catch (IOException e) {
            LOG.warn("Failed to store option values for " + providerId + ", fileId=" + fileId, e);
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
                LOG.warn("Failed to close module-aware option value storage", e);
            }
        }
    }
}
