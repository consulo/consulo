/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.ui.app.impl.settings;

import consulo.configurable.SearchableConfigurable;
import consulo.configurable.SearchableOptionsRegistrar;
import org.jspecify.annotations.Nullable;

import java.util.Set;

public final class OptionsSearchMatcher {
    private OptionsSearchMatcher() {
    }

    public static boolean matches(
        @Nullable String text,
        @Nullable String option,
        boolean force,
        @Nullable SearchableConfigurable configurable
    ) {
        if (text == null || option == null || option.isEmpty()) {
            return false;
        }
        SearchableOptionsRegistrar searchableOptionsRegistrar = SearchableOptionsRegistrar.getInstance();
        Set<String> words = searchableOptionsRegistrar.getProcessedWords(option);
        Set<String> options = configurable != null ? searchableOptionsRegistrar.replaceSynonyms(words, configurable) : words;
        if (options.isEmpty()) {
            return text.toLowerCase().contains(option.toLowerCase());
        }
        Set<String> tokens = searchableOptionsRegistrar.getProcessedWords(text);
        if (!force) {
            options.retainAll(tokens);
            boolean highlight = !options.isEmpty();
            return highlight || text.toLowerCase().contains(option.toLowerCase());
        }
        else {
            options.removeAll(tokens);
            return options.isEmpty();
        }
    }
}
