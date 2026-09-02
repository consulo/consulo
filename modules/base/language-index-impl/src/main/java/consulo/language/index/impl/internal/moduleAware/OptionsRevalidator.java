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

import consulo.language.internal.psi.stub.IndexOptionImpl;
import consulo.language.psi.stub.IndexOption;
import consulo.language.psi.stub.ModuleAwareIndexOptionProvider;
import consulo.module.Module;
import consulo.virtualFileSystem.VirtualFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static consulo.language.index.impl.internal.moduleAware.OptionsMeta.PerProviderMeta;
import static consulo.language.index.impl.internal.moduleAware.OptionsMeta.VariantTag;

/**
 * Decides whether a file needs reindexing for an options-sensitive index and builds the
 * fresh {@link OptionsMeta} snapshot to store after a successful reindex. See the
 * revalidation algorithm in {@code MODULE_AWARE_INDEX.md}.
 *
 * <p>The expensive part — asking every provider for its options and hashing the payload —
 * is factored into {@link #currentState} so callers can cache it per file
 * ({@link ModuleAwareIndexMetaRecorder}); comparison and snapshotting then work on the
 * precomputed map, subset per index by the requested provider ids.</p>
 */
public final class OptionsRevalidator {
    private OptionsRevalidator() {
    }

    /**
     * Computes the current per-provider state for every given provider. The result is
     * index-agnostic: callers subset it by an index's requested provider ids.
     */
    public static Map<String, PerProviderMeta> currentState(List<ModuleAwareIndexOptionProvider> providers,
                                                            Module module,
                                                            VirtualFile file) {
        Map<String, PerProviderMeta> state = new HashMap<>(providers.size());
        for (ModuleAwareIndexOptionProvider provider : providers) {
            IndexOption option = provider.getOptions(module, file);
            VariantTag tag = tagOf(option);
            int hash = tag == VariantTag.SharablePerOption
                ? IndexOptionHasher.hash((IndexOptionImpl.SharablePerOption<?>) option)
                : 0;
            state.put(provider.getId(), new PerProviderMeta(provider.getVersion(), tag, hash));
        }
        return state;
    }

    public static boolean needsReindex(int currentIndexVersion,
                                       OptionsMeta stored,
                                       Set<String> currentIds,
                                       Map<String, PerProviderMeta> currentState) {
        if (stored.indexVersion() != currentIndexVersion) {
            return true;
        }

        if (!currentIds.equals(stored.providers().keySet())) {
            return true;
        }

        for (String id : currentIds) {
            PerProviderMeta storedMeta = stored.providers().get(id);
            PerProviderMeta current = currentState.get(id);
            if (storedMeta == null || current == null) {
                return true;
            }
            if (!storedMeta.equals(current)) {
                return true;
            }
        }

        return false;
    }

    public static OptionsMeta snapshot(int currentIndexVersion,
                                       Set<String> currentIds,
                                       Map<String, PerProviderMeta> currentState) {
        Map<String, PerProviderMeta> providers = new HashMap<>(currentIds.size());
        for (String id : currentIds) {
            PerProviderMeta current = currentState.get(id);
            if (current != null) {
                providers.put(id, current);
            }
        }
        return new OptionsMeta(currentIndexVersion, Map.copyOf(providers));
    }

    static VariantTag tagOf(IndexOption option) {
        return switch (option) {
            case IndexOptionImpl.FullySharable ignored -> VariantTag.FullySharable;
            case IndexOptionImpl.UniqueToModule ignored -> VariantTag.UniqueToModule;
            case IndexOptionImpl.SharablePerOption<?> ignored -> VariantTag.SharablePerOption;
        };
    }
}
