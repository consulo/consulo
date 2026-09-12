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

import consulo.index.io.data.DataInputOutputUtil;
import consulo.index.io.data.IOUtil;
import org.jspecify.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Identifies one stored stub variant of a file: the option every applicable provider contributed to it. Two variants
 * are the same when all their provider payloads are equal.
 */
public record VariantDescriptor(List<ProviderOption> options, String displayName) {
    public VariantDescriptor {
        options = List.copyOf(options);
    }

    public record ProviderOption(String providerId, OptionsMeta.VariantTag tag, byte[] payload) {
        @Override
        public boolean equals(Object other) {
            return other instanceof ProviderOption that
                && providerId.equals(that.providerId)
                && tag == that.tag
                && Arrays.equals(payload, that.payload);
        }

        @Override
        public int hashCode() {
            return Objects.hash(providerId, tag, Arrays.hashCode(payload));
        }

        public int optionsHash() {
            return tag == OptionsMeta.VariantTag.SharablePerOption ? Arrays.hashCode(payload) : 0;
        }
    }

    public @Nullable ProviderOption option(String providerId) {
        for (ProviderOption option : options) {
            if (option.providerId().equals(providerId)) {
                return option;
            }
        }
        return null;
    }

    public Map<String, byte[]> payloadMap() {
        Map<String, byte[]> payloads = new HashMap<>(options.size());
        for (ProviderOption option : options) {
            payloads.put(option.providerId(), option.payload());
        }
        return Map.copyOf(payloads);
    }

    /**
     * @return whether every provider of this variant has exactly the given payload; providers absent from
     * {@code payloads} do not match
     */
    public boolean matches(@Nullable Map<String, byte[]> payloads) {
        if (payloads == null) {
            return false;
        }
        for (ProviderOption option : options) {
            if (!Arrays.equals(option.payload(), payloads.get(option.providerId()))) {
                return false;
            }
        }
        return true;
    }

    public VariantDescriptor with(ProviderOption replacement, String displayName) {
        List<ProviderOption> replaced = new ArrayList<>(options.size());
        for (ProviderOption option : options) {
            replaced.add(option.providerId().equals(replacement.providerId()) ? replacement : option);
        }
        return new VariantDescriptor(replaced, displayName);
    }

    public Map<String, OptionsMeta.PerProviderMeta> toMeta(Map<String, Integer> providerVersions) {
        Map<String, OptionsMeta.PerProviderMeta> meta = new HashMap<>(options.size());
        for (ProviderOption option : options) {
            Integer version = providerVersions.get(option.providerId());
            if (version != null) {
                meta.put(option.providerId(), new OptionsMeta.PerProviderMeta(version, option.tag(), option.optionsHash()));
            }
        }
        return meta;
    }

    public String key() {
        StringBuilder builder = new StringBuilder();
        for (ProviderOption option : options) {
            builder.append(option.providerId()).append('=').append(option.tag().ordinal()).append(':')
                .append(Integer.toHexString(Arrays.hashCode(option.payload()))).append(';');
        }
        return builder.toString();
    }

    public static void write(DataOutput out, VariantDescriptor descriptor) throws IOException {
        DataInputOutputUtil.writeINT(out, descriptor.options().size());
        for (ProviderOption option : descriptor.options()) {
            IOUtil.writeUTF(out, option.providerId());
            DataInputOutputUtil.writeINT(out, option.tag().ordinal());
            DataInputOutputUtil.writeINT(out, option.payload().length);
            out.write(option.payload());
        }
        IOUtil.writeUTF(out, descriptor.displayName());
    }

    public static VariantDescriptor read(DataInput in) throws IOException {
        int count = DataInputOutputUtil.readINT(in);
        List<ProviderOption> options = new ArrayList<>(count);
        OptionsMeta.VariantTag[] tags = OptionsMeta.VariantTag.values();
        for (int i = 0; i < count; i++) {
            String providerId = IOUtil.readUTF(in);
            int ordinal = DataInputOutputUtil.readINT(in);
            if (ordinal < 0 || ordinal >= tags.length) {
                throw new IOException("Unknown VariantTag ordinal: " + ordinal);
            }
            byte[] payload = new byte[DataInputOutputUtil.readINT(in)];
            in.readFully(payload);
            options.add(new ProviderOption(providerId, tags[ordinal], payload));
        }
        return new VariantDescriptor(options, IOUtil.readUTF(in));
    }
}
