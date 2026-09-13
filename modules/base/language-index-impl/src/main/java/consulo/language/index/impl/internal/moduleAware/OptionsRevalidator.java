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

import consulo.language.index.impl.internal.moduleAware.OptionsMeta.PerProviderMeta;
import consulo.language.index.impl.internal.moduleAware.OptionsMeta.VariantTag;
import consulo.language.internal.psi.stub.IndexOptionImpl;
import consulo.language.psi.stub.IndexOption;
import consulo.language.psi.stub.ModuleAwareIndexOptionProvider;
import consulo.module.Module;
import consulo.virtualFileSystem.VirtualFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class OptionsRevalidator {
    private OptionsRevalidator() {
    }

    /**
     * Computes the current per-provider state of every variant the file is indexed under, primary first. The result
     * is index-agnostic: callers subset it by an index's requested provider ids.
     */
    public static List<Map<String, PerProviderMeta>> currentState(List<ModuleAwareIndexOptionProvider> providers,
                                                                  Module module,
                                                                  VirtualFile file,
                                                                  int fileId) {
        Map<String, Integer> versions = new HashMap<>(providers.size());
        for (ModuleAwareIndexOptionProvider provider : providers) {
            versions.put(provider.getId(), provider.getVersion());
        }
        List<VariantDescriptor> descriptors = ModuleAwareIndexVariants.descriptorsFor(providers, module, file, fileId);
        List<Map<String, PerProviderMeta>> state = new ArrayList<>(descriptors.size());
        for (VariantDescriptor descriptor : descriptors) {
            state.add(descriptor.toMeta(versions));
        }
        return state;
    }

    public static boolean needsReindex(int currentIndexVersion,
                                       OptionsMeta stored,
                                       Set<String> currentIds,
                                       List<Map<String, PerProviderMeta>> currentState) {
        if (stored.indexVersion() != currentIndexVersion) {
            return true;
        }
        return !stored.sameVariants(snapshot(currentIndexVersion, currentIds, currentState));
    }

    public static OptionsMeta snapshot(int currentIndexVersion,
                                       Set<String> currentIds,
                                       List<Map<String, PerProviderMeta>> currentState) {
        List<Map<String, PerProviderMeta>> variants = new ArrayList<>(currentState.size());
        for (Map<String, PerProviderMeta> state : currentState) {
            Map<String, PerProviderMeta> providers = new HashMap<>(currentIds.size());
            for (String id : currentIds) {
                PerProviderMeta current = state.get(id);
                if (current != null) {
                    providers.put(id, current);
                }
            }
            variants.add(Map.copyOf(providers));
        }
        return new OptionsMeta(currentIndexVersion, variants);
    }

    static VariantTag tagOf(IndexOption option) {
        return switch (option) {
            case IndexOptionImpl.FullySharable ignored -> VariantTag.FullySharable;
            case IndexOptionImpl.UniqueToModule ignored -> VariantTag.UniqueToModule;
            case IndexOptionImpl.SharablePerOption<?> ignored -> VariantTag.SharablePerOption;
        };
    }
}
