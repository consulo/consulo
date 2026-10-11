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
package consulo.ide.impl.ui.app.impl.settings;

import consulo.configurable.Configurable;
import consulo.configurable.SearchableConfigurable;
import consulo.configurable.SearchableOptionsRegistrar;
import consulo.configurable.internal.ConfigurableSearchWalker;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OptionsLiveIndex {
    private final Map<String, List<OptionsLiveIndexEntry>> myEntries = new LinkedHashMap<>();
    private final Map<String, Configurable> myConfigurables = new LinkedHashMap<>();

    @RequiredUIAccess
    public void index(Configurable configurable, Component content) {
        String id = getId(configurable);

        List<OptionsLiveIndexEntry> entries = new ArrayList<>();
        ConfigurableSearchWalker.collectTexts(content, (text, tabPath) -> entries.add(new OptionsLiveIndexEntry(id, text, tabPath)));

        myEntries.put(id, List.copyOf(entries));
        myConfigurables.put(id, configurable);
    }

    public Set<Configurable> findHits(String query) {
        String trimmed = query.trim();
        if (trimmed.isEmpty() || myEntries.isEmpty()) {
            return Set.of();
        }

        Set<String> words = SearchableOptionsRegistrar.getInstance().getProcessedWordsWithoutStemming(trimmed);
        List<String> queryWords = words.isEmpty() ? List.of(trimmed) : new ArrayList<>(words);

        Set<Configurable> result = new LinkedHashSet<>();
        for (Map.Entry<String, List<OptionsLiveIndexEntry>> entry : myEntries.entrySet()) {
            Configurable configurable = myConfigurables.get(entry.getKey());
            if (configurable != null && matchesAll(configurable, entry.getValue(), queryWords)) {
                result.add(configurable);
            }
        }
        return result;
    }

    private static boolean matchesAll(Configurable configurable, List<OptionsLiveIndexEntry> entries, List<String> queryWords) {
        SearchableConfigurable searchable = searchableOf(configurable);
        for (String word : queryWords) {
            if (!matchesAny(entries, word, searchable)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesAny(List<OptionsLiveIndexEntry> entries, String word, @Nullable SearchableConfigurable searchable) {
        for (OptionsLiveIndexEntry entry : entries) {
            if (matches(entry.text(), word, searchable)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(LocalizeValue value, String word, @Nullable SearchableConfigurable searchable) {
        for (String text : ConfigurableSearchWalker.textsOf(value)) {
            if (OptionsSearchMatcher.matches(text, word, false, searchable)) {
                return true;
            }
        }
        return false;
    }

    public static @Nullable SearchableConfigurable searchableOf(Configurable configurable) {
        if (!(configurable instanceof SearchableConfigurable searchable)) {
            return null;
        }

        try {
            searchable.getId();
            return searchable;
        }
        catch (AbstractMethodError e) {
            return null;
        }
    }

    private static String getId(Configurable configurable) {
        try {
            return configurable.getId();
        }
        catch (AbstractMethodError e) {
            return configurable.getClass().getName();
        }
    }
}
