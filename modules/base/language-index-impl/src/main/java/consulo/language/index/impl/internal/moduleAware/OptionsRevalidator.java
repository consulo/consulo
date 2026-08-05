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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static consulo.language.index.impl.internal.moduleAware.OptionsMeta.PerProviderMeta;
import static consulo.language.index.impl.internal.moduleAware.OptionsMeta.VariantTag;

/**
 * Decides whether a file needs reindexing for an options-sensitive index and builds the
 * fresh {@link OptionsMeta} snapshot to store after a successful reindex. See the
 * revalidation algorithm in {@code MODULE_AWARE_INDEX.md}.
 */
public final class OptionsRevalidator {
    private OptionsRevalidator() {
    }

    public static boolean needsReindex(int currentIndexVersion,
                                       OptionsMeta stored,
                                       List<ModuleAwareIndexOptionProvider> currentProviders,
                                       Module module,
                                       VirtualFile file) {
        if (stored.indexVersion() != currentIndexVersion) {
            return true;
        }

        Set<String> currentIds = new HashSet<>(currentProviders.size());
        for (ModuleAwareIndexOptionProvider provider : currentProviders) {
            currentIds.add(provider.getId());
        }
        if (!currentIds.equals(stored.providers().keySet())) {
            return true;
        }

        for (ModuleAwareIndexOptionProvider provider : currentProviders) {
            PerProviderMeta perMeta = stored.providers().get(provider.getId());
            if (perMeta == null) {
                return true;
            }
            if (perMeta.providerVersion() != provider.getVersion()) {
                return true;
            }

            IndexOption option = provider.getOptions(module, file);
            VariantTag currentTag = tagOf(option);
            if (perMeta.variantTag() != currentTag) {
                return true;
            }

            if (currentTag == VariantTag.SharablePerOption) {
                int currentHash = IndexOptionHasher.hash((IndexOptionImpl.SharablePerOption<?>) option);
                if (perMeta.optionsHash() != currentHash) {
                    return true;
                }
            }
        }

        return false;
    }

    public static OptionsMeta snapshot(int currentIndexVersion,
                                       List<ModuleAwareIndexOptionProvider> currentProviders,
                                       Module module,
                                       VirtualFile file) {
        Map<String, PerProviderMeta> providers = new HashMap<>(currentProviders.size());
        for (ModuleAwareIndexOptionProvider provider : currentProviders) {
            IndexOption option = provider.getOptions(module, file);
            VariantTag tag = tagOf(option);
            int hash = tag == VariantTag.SharablePerOption
                ? IndexOptionHasher.hash((IndexOptionImpl.SharablePerOption<?>) option)
                : 0;
            providers.put(provider.getId(), new PerProviderMeta(provider.getVersion(), tag, hash));
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
