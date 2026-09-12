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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What an index entry of a file was built under: one per-provider state for each stored variant, the primary variant
 * first.
 */
public record OptionsMeta(int indexVersion, List<Map<String, PerProviderMeta>> variants) {
    public OptionsMeta {
        variants = List.copyOf(variants);
    }

    public record PerProviderMeta(int providerVersion, VariantTag variantTag, int optionsHash) {
    }

    public enum VariantTag {
        FullySharable,
        UniqueToModule,
        SharablePerOption
    }

    public Map<String, PerProviderMeta> primary() {
        return variants.isEmpty() ? Map.of() : variants.get(0);
    }

    /**
     * @return whether the same variants are recorded: the same primary, and the same set of secondaries
     */
    public boolean sameVariants(OptionsMeta other) {
        if (variants.size() != other.variants.size() || !primary().equals(other.primary())) {
            return false;
        }
        Set<Map<String, PerProviderMeta>> mine = new HashSet<>(variants.subList(1, variants.size()));
        Set<Map<String, PerProviderMeta>> theirs = new HashSet<>(other.variants.subList(1, other.variants.size()));
        return mine.equals(theirs);
    }
}
