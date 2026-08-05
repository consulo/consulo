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

import consulo.index.io.data.DataExternalizer;
import consulo.index.io.data.DataInputOutputUtil;
import consulo.index.io.data.IOUtil;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static consulo.language.index.impl.internal.moduleAware.OptionsMeta.PerProviderMeta;
import static consulo.language.index.impl.internal.moduleAware.OptionsMeta.VariantTag;

/**
 * Persists {@link OptionsMeta} alongside an options-sensitive index. Entries are written
 * sorted by provider id — gives deterministic bytes so the stored meta diff stably across
 * runs.
 */
public final class OptionsMetaExternalizer implements DataExternalizer<OptionsMeta> {
    public static final OptionsMetaExternalizer INSTANCE = new OptionsMetaExternalizer();

    private static final int FORMAT_VERSION = 1;

    private OptionsMetaExternalizer() {
    }

    @Override
    public void save(DataOutput out, OptionsMeta value) throws IOException {
        DataInputOutputUtil.writeINT(out, FORMAT_VERSION);
        DataInputOutputUtil.writeINT(out, value.indexVersion());

        List<Map.Entry<String, PerProviderMeta>> entries = new ArrayList<>(value.providers().entrySet());
        entries.sort(Comparator.comparing(Map.Entry::getKey));

        DataInputOutputUtil.writeINT(out, entries.size());
        for (Map.Entry<String, PerProviderMeta> entry : entries) {
            IOUtil.writeUTF(out, entry.getKey());
            PerProviderMeta meta = entry.getValue();
            DataInputOutputUtil.writeINT(out, meta.providerVersion());
            DataInputOutputUtil.writeINT(out, meta.variantTag().ordinal());
            DataInputOutputUtil.writeINT(out, meta.optionsHash());
        }
    }

    @Override
    public OptionsMeta read(DataInput in) throws IOException {
        int format = DataInputOutputUtil.readINT(in);
        if (format != FORMAT_VERSION) {
            throw new IOException("Unsupported OptionsMeta format version: " + format);
        }
        int indexVersion = DataInputOutputUtil.readINT(in);

        int size = DataInputOutputUtil.readINT(in);
        Map<String, PerProviderMeta> providers = new HashMap<>(size);
        VariantTag[] tags = VariantTag.values();
        for (int i = 0; i < size; i++) {
            String providerId = IOUtil.readUTF(in);
            int providerVersion = DataInputOutputUtil.readINT(in);
            int tagOrdinal = DataInputOutputUtil.readINT(in);
            int optionsHash = DataInputOutputUtil.readINT(in);

            if (tagOrdinal < 0 || tagOrdinal >= tags.length) {
                throw new IOException("Unknown VariantTag ordinal: " + tagOrdinal);
            }
            providers.put(providerId, new PerProviderMeta(providerVersion, tags[tagOrdinal], optionsHash));
        }
        return new OptionsMeta(indexVersion, Map.copyOf(providers));
    }
}
