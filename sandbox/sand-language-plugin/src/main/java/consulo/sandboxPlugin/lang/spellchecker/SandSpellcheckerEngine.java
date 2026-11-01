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
package consulo.sandboxPlugin.lang.spellchecker;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.ConfigurableBuilder;
import consulo.configurable.UnnamedConfigurable;
import consulo.language.spellchecker.editor.SpellcheckerEngine;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Reference {@link SpellcheckerEngine}: a small own word list with edit distance suggestions.
 * <p>
 * The word list is loaded on the first call only, so the engine costs nothing while another engine is selected.
 *
 * @author VISTALL
 * @since 2026-11-01
 */
@ExtensionImpl
public class SandSpellcheckerEngine implements SpellcheckerEngine {
    private static final Logger LOG = Logger.getInstance(SandSpellcheckerEngine.class);

    private static final int MAX_DISTANCE = 2;
    private static final int MAX_SUGGESTIONS = 5;

    private final SandSpellcheckerSettings mySettings;

    private volatile @Nullable Set<String> myWords;

    @Inject
    public SandSpellcheckerEngine(SandSpellcheckerSettings settings) {
        mySettings = settings;
    }

    @Override
    public String getId() {
        return "sand";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Sand (reference)");
    }

    @Override
    public Set<Locale> getLocales() {
        return Set.of(Locale.ENGLISH);
    }

    @Override
    public boolean hasProblem(Project project, String word) {
        if (mySettings.isIgnoreUpperCaseWords() && word.equals(word.toUpperCase(Locale.ROOT))) {
            return false;
        }
        return !getWords().contains(word.toLowerCase(Locale.ROOT));
    }

    @Override
    public List<String> getSuggestions(Project project, String word) {
        String lowerCase = word.toLowerCase(Locale.ROOT);
        List<String> candidates = new ArrayList<>();
        for (String known : getWords()) {
            if (distance(lowerCase, known) <= MAX_DISTANCE) {
                candidates.add(known);
            }
        }
        candidates.sort(Comparator.<String>comparingInt(it -> distance(lowerCase, it)).thenComparing(Comparator.naturalOrder()));
        return candidates.size() > MAX_SUGGESTIONS ? candidates.subList(0, MAX_SUGGESTIONS) : candidates;
    }

    @Override
    public UnnamedConfigurable createConfigurable() {
        return ConfigurableBuilder.newBuilder()
            .checkBox(
                LocalizeValue.localizeTODO("Ignore all-uppercase words"),
                mySettings::isIgnoreUpperCaseWords,
                mySettings::setIgnoreUpperCaseWords
            )
            .buildUnnamed();
    }

    private Set<String> getWords() {
        Set<String> words = myWords;
        if (words == null) {
            words = loadWords();
            myWords = words;
        }
        return words;
    }

    private static Set<String> loadWords() {
        Set<String> words = new HashSet<>();
        try (InputStream stream = SandSpellcheckerEngine.class.getResourceAsStream("sand-words.txt")) {
            if (stream == null) {
                LOG.warn("sand-words.txt not found");
                return words;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                String word = line.trim();
                if (!word.isEmpty()) {
                    words.add(word.toLowerCase(Locale.ROOT));
                }
            }
        }
        catch (IOException e) {
            LOG.warn(e);
        }
        LOG.info("Sand spellchecker word list loaded: " + words.size() + " words");
        return words;
    }

    private static int distance(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }
}
