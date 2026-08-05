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
import consulo.language.internal.psi.stub.IndexOptionImpl;
import consulo.language.psi.stub.IndexOption;
import consulo.language.psi.stub.ModuleAwareIndexOptionProvider;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.virtualFileSystem.VirtualFile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Composes {@link IndexOption}s returned by multiple providers into one effective option
 * for storage. Tier precedence: {@code UniqueToModule > SharablePerOption > FullySharable}.
 *
 * <p>See the composition rules section of {@code MODULE_AWARE_INDEX.md}.</p>
 */
public final class IndexOptionComposer {
    private IndexOptionComposer() {
    }

    public static IndexOption compose(Module module,
                                      VirtualFile file,
                                      List<ModuleAwareIndexOptionProvider> providers) {
        if (providers.isEmpty()) {
            return IndexOption.fullySharable();
        }

        List<IndexOption> contributions = new ArrayList<>(providers.size());
        for (ModuleAwareIndexOptionProvider provider : providers) {
            contributions.add(provider.getOptions(module, file));
        }
        return compose(contributions);
    }

    static IndexOption compose(List<IndexOption> contributions) {
        List<IndexOptionImpl.UniqueToModule> uniques = new ArrayList<>();
        List<IndexOptionImpl.SharablePerOption<?>> sharables = new ArrayList<>();

        for (IndexOption option : contributions) {
            switch (option) {
                case IndexOptionImpl.FullySharable ignored -> {
                }
                case IndexOptionImpl.UniqueToModule u -> uniques.add(u);
                case IndexOptionImpl.SharablePerOption<?> s -> sharables.add(s);
            }
        }

        if (!uniques.isEmpty()) {
            return IndexOption.uniqueToModule(mergeLabels(uniques, sharables));
        }

        if (sharables.isEmpty()) {
            return IndexOption.fullySharable();
        }

        if (sharables.size() == 1) {
            IndexOptionImpl.SharablePerOption<?> only = sharables.get(0);
            return only;
        }

        return buildComposite(sharables);
    }

    private static LocalizeValue mergeLabels(List<IndexOptionImpl.UniqueToModule> uniques,
                                             List<IndexOptionImpl.SharablePerOption<?>> sharables) {
        List<LocalizeValue> parts = new ArrayList<>(uniques.size() + sharables.size());
        for (IndexOptionImpl.UniqueToModule u : uniques) {
            parts.add(u.displayName());
        }
        for (IndexOptionImpl.SharablePerOption<?> s : sharables) {
            parts.add(s.displayName());
        }
        return LocalizeValue.join(", ", parts.toArray(LocalizeValue[]::new));
    }

    private static IndexOption buildComposite(List<IndexOptionImpl.SharablePerOption<?>> sharables) {
        List<Record> values = new ArrayList<>(sharables.size());
        List<DataExternalizer<?>> externalizers = new ArrayList<>(sharables.size());
        List<LocalizeValue> labels = new ArrayList<>(sharables.size());

        for (IndexOptionImpl.SharablePerOption<?> s : sharables) {
            values.add(s.value());
            externalizers.add(s.externalizer());
            labels.add(s.displayName());
        }

        CompositePayload payload = new CompositePayload(List.copyOf(values));
        CompositeExternalizer externalizer = new CompositeExternalizer(List.copyOf(externalizers));
        LocalizeValue label = LocalizeValue.join(", ", labels.toArray(LocalizeValue[]::new));
        return IndexOption.sharablePerOption(payload, externalizer, label);
    }

    public record CompositePayload(List<Record> values) {
    }

    private static final class CompositeExternalizer implements DataExternalizer<CompositePayload> {
        private final List<DataExternalizer<?>> myExternalizers;

        CompositeExternalizer(List<DataExternalizer<?>> externalizers) {
            myExternalizers = externalizers;
        }

        @Override
        public void save(DataOutput out, CompositePayload value) throws IOException {
            List<Record> values = value.values();
            for (int i = 0; i < myExternalizers.size(); i++) {
                writeOne(out, myExternalizers.get(i), values.get(i));
            }
        }

        @Override
        public CompositePayload read(DataInput in) throws IOException {
            List<Record> values = new ArrayList<>(myExternalizers.size());
            for (DataExternalizer<?> externalizer : myExternalizers) {
                values.add((Record) externalizer.read(in));
            }
            return new CompositePayload(List.copyOf(values));
        }

        @SuppressWarnings("unchecked")
        private static <T> void writeOne(DataOutput out, DataExternalizer<T> externalizer, Object value) throws IOException {
            externalizer.save(out, (T) value);
        }
    }
}
