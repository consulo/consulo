/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.configurable.impl.internal.search;

import consulo.configurable.search.SearchableOptionContributor;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.configurable.Configurable;
import consulo.configurable.search.ConfigurableHit;
import consulo.configurable.SearchableConfigurable;
import consulo.configurable.search.SearchableOptionsRegistrar;
import consulo.configurable.search.SearchableConfigurableTexts;
import consulo.configurable.search.SearchStemmer;
import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.configurable.search.OptionDescription;
import consulo.util.interner.Interner;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.util.ULocale;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.*;

/**
 * @author anna
 * @since 2006-02-07
 */
@Singleton
@ServiceImpl
public class SearchableOptionsRegistrarImpl extends SearchableOptionsRegistrar {
    /**
     * The stems of the words of {@link #myStorage} - computed once, when a word is indexed.
     */
    private final Map<String, String> myStems = new HashMap<>();
    /**
     * The stemmer of the current locale, found once a locale - null until it is looked for.
     */
    private volatile @Nullable Optional<SearchStemmer> myLocaleStemmer;
    private final Map<String, Set<OptionDescription>> myStorage =
        Collections.synchronizedMap(new HashMap<String, Set<OptionDescription>>(20, 0.9f));
    private final Map<String, String> myId2Name = Collections.synchronizedMap(new HashMap<String, String>(20, 0.9f));

    private volatile boolean myIndexLoaded;

    private final Interner<String> myIdentifierTable = Interner.createStringInterner();

    private static final Logger LOG = Logger.getInstance(SearchableOptionsRegistrarImpl.class);
    public static final int LOAD_FACTOR = 20;


    private final Application myApplication;

    @Inject
    public SearchableOptionsRegistrarImpl(Application application) {
        myApplication = application;
        // the index holds the texts in the locale they were resolved in
        LocalizeManager.get().addListener((oldLocale, newLocale) -> resetIndex(), application);
    }

    /**
     * Builds the index once - from the texts generated for the configurables at compile time, resolved in the
     * current locale, the plugins and the contributors. Built again when the locale changes.
     */
    private void loadIndexIfNecessary() {
        if (myIndexLoaded) {
            return;
        }
        myIndexLoaded = true;

        myApplication.getExtensionPoint(SearchableConfigurableTexts.class).forEach(texts -> {
            try {
                List<LocalizeValue> values = texts.getTexts();
                String id = texts.getConfigurableId();
                String groupName = values.isEmpty() ? null : values.getFirst().get();
                for (LocalizeValue value : values) {
                    String hit = value.get();
                    for (String word : getProcessedWordsWithoutStemming(hit)) {
                        putOptionWithHelpId(word, id, groupName, hit, null);
                    }
                }
            }
            catch (Exception e) {
                LOG.error("Can't index the texts of " + texts.getClass().getName(), e);
            }
        });

        myApplication.getExtensionPoint(SearchableOptionContributor.class).forEach(
            contributor -> contributor.processOptions((text, hit, path, configurableId, configurableName) -> {
                String hitText = hit.get();
                String pageName = configurableName.get();
                for (String word : getProcessedWordsWithoutStemming(text.get())) {
                    putOptionWithHelpId(word, configurableId, pageName, hitText, path);
                }
            })
        );
    }

    private synchronized void resetIndex() {
        myStorage.clear();
        myStems.clear();
        myLocaleStemmer = null;
        myId2Name.clear();
        myIndexLoaded = false;
    }

    private synchronized void putOptionWithHelpId(String option, String id, @Nullable String groupName, String hit, @Nullable String path) {
        String stem = stem(option);
        if (stem == null) {
            return;
        }
        if (!myId2Name.containsKey(id) && groupName != null) {
            myId2Name.put(myIdentifierTable.intern(id), myIdentifierTable.intern(groupName));
        }

        OptionDescription description =
            new OptionDescription(
                null,
                myIdentifierTable.intern(id).trim(),
                hit != null ? myIdentifierTable.intern(hit).trim() : null,
                path != null ? myIdentifierTable.intern(path).trim() : null
            );
        Set<OptionDescription> configs = myStorage.get(option);
        if (configs == null) {
            configs = new HashSet<>(Set.of(description));
            myStorage.put(new String(option), configs);
            myStems.put(option, stem);
        }
        else {
            configs.add(description);
        }
    }

    @Override
    public ConfigurableHit getConfigurables(
        Configurable[] allConfigurables,
        boolean changed,
        @Nullable Set<Configurable> configurables,
        String option,
        Project project
    ) {
        ConfigurableHit hits = new ConfigurableHit();
        Set<Configurable> contentHits = hits.getContentHits();

        Set<String> options = getProcessedWordsWithoutStemming(option);
        if (configurables == null) {
            contentHits.addAll(expandGroup(allConfigurables));
        }
        else {
            contentHits.addAll(configurables);
        }

        String optionToCheck = option.trim().toLowerCase();
        for (Configurable each : contentHits) {
            if (each.getDisplayName().isEmpty()) {
                continue;
            }
            String displayName = each.getDisplayName().toLowerCase().get();
            List<String> allWords = StringUtil.getWordsIn(displayName);
            if (displayName.contains(optionToCheck)) {
                hits.getNameFullHits().add(each);
                hits.getNameHits().add(each);
            }
            for (String eachWord : allWords) {
                if (eachWord.startsWith(optionToCheck)) {
                    hits.getNameHits().add(each);
                    break;
                }
            }

            if (options.isEmpty()) {
                hits.getNameHits().add(each);
                hits.getNameFullHits().add(each);
            }
        }

        Set<Configurable> currentConfigurables = new HashSet<>(contentHits);
        if (options.isEmpty()) { //operate with substring
            List<String> components = words(optionToCheck);
            if (!components.isEmpty()) {
                options.addAll(components);
            }
            else {
                options.add(option);
            }
        }
        Set<String> helpIds = null;
        for (String opt : options) {
            Set<OptionDescription> optionIds = getAcceptableDescriptions(opt);
            if (optionIds == null) {
                contentHits.clear();
                return hits;
            }
            Set<String> ids = new HashSet<>();
            for (OptionDescription id : optionIds) {
                ids.add(id.getConfigurableId());
            }
            if (helpIds == null) {
                helpIds = ids;
            }
            helpIds.retainAll(ids);
        }
        if (helpIds != null) {
            for (Iterator<Configurable> it = contentHits.iterator(); it.hasNext(); ) {
                Configurable configurable = it.next();
                if (!helpIds.contains(idOf(configurable))) {
                    it.remove();
                }
            }
        }
        if (currentConfigurables.equals(contentHits) && !(configurables == null && changed)) {
            return getConfigurables(allConfigurables, true, null, option, project);
        }
        return hits;
    }

    /**
     * The id of any configurable - a plain one may have none, and then it is found by its name only.
     */
    private static @Nullable String idOf(Configurable configurable) {
        try {
            return configurable.getId();
        }
        catch (AbstractMethodError e) {
            return null;
        }
    }

    /**
     * The configurables and the children of the composite ones - those of a hidden parent left out.
     */
    private static List<Configurable> expandGroup(Configurable[] configurables) {
        List<Configurable> result = new ArrayList<>(Arrays.asList(configurables));
        for (Configurable each : configurables) {
            addChildren(each, result);
        }
        result.removeIf(configurable -> configurable instanceof SearchableConfigurable.Parent parent && !parent.isVisible());
        return result;
    }

    private static void addChildren(Configurable configurable, List<Configurable> list) {
        if (configurable instanceof Configurable.Composite composite) {
            for (Configurable child : composite.getConfigurables()) {
                list.add(child);
                addChildren(child, list);
            }
        }
    }

    @Override
    public synchronized @Nullable Set<OptionDescription> getAcceptableDescriptions(String prefix) {
        if (prefix == null) {
            return null;
        }
        String stemmedPrefix = stem(prefix);
        if (StringUtil.isEmptyOrSpaces(stemmedPrefix)) {
            return null;
        }
        loadIndexIfNecessary();
        Set<OptionDescription> result = null;
        for (Map.Entry<String, Set<OptionDescription>> entry : myStorage.entrySet()) {
            Set<OptionDescription> descriptions = entry.getValue();
            if (descriptions != null) {
                String option = entry.getKey();
                if (!option.startsWith(prefix) && !option.startsWith(stemmedPrefix)) {
                    String stemmedOption = myStems.get(option);
                    if (stemmedOption != null && !stemmedOption.startsWith(prefix) && !stemmedOption.startsWith(stemmedPrefix)) {
                        continue;
                    }
                }
                if (result == null) {
                    result = new HashSet<>();
                }
                result.addAll(descriptions);
            }
        }
        return result;
    }

    @Override
    public @Nullable String getInnerPath(SearchableConfigurable configurable, String option) {
        loadIndexIfNecessary();
        Set<OptionDescription> path = null;
        Set<String> words = getProcessedWordsWithoutStemming(option);
        for (String word : words) {
            Set<OptionDescription> configs = getAcceptableDescriptions(word);
            if (configs == null) {
                return null;
            }
            Set<OptionDescription> paths = new HashSet<>();
            for (OptionDescription config : configs) {
                if (Comparing.strEqual(config.getConfigurableId(), configurable.getId())) {
                    paths.add(config);
                }
            }
            if (path == null) {
                path = paths;
            }
            path.retainAll(paths);
        }
        if (path == null || path.isEmpty()) {
            return null;
        }
        else {
            OptionDescription result = null;
            for (OptionDescription description : path) {
                String hit = description.getHit();
                if (hit != null) {
                    boolean theBest = true;
                    for (String word : words) {
                        if (!hit.contains(word)) {
                            theBest = false;
                        }
                    }
                    if (theBest) {
                        return description.getPath();
                    }
                }
                result = description;
            }
            return result != null ? result.getPath() : null;
        }
    }

    @Override
    public Map<String, Set<String>> findPossibleExtension(String prefix, Project project) {
        loadIndexIfNecessary();
        Map<String, Set<String>> result = new HashMap<>();
        int count = 0;
        Set<String> prefixes = getProcessedWordsWithoutStemming(prefix);
        for (String opt : prefixes) {
            Set<OptionDescription> configs = getAcceptableDescriptions(opt);
            if (configs == null) {
                continue;
            }
            for (OptionDescription description : configs) {
                String groupName = myId2Name.get(description.getConfigurableId());
                Set<String> foundHits = result.get(groupName);
                if (foundHits == null) {
                    foundHits = new HashSet<>();
                    result.put(groupName, foundHits);
                }
                foundHits.add(description.getHit());
                count++;
            }
        }
        if (count > LOAD_FACTOR) {
            result.clear();
        }
        return result;
    }

    @Override
    public Set<String> getProcessedWordsWithoutStemming(String text) {
        return new HashSet<>(words(text));
    }

    @Override
    public Set<String> getProcessedWords(String text) {
        Set<String> result = new HashSet<>();
        for (String word : words(text)) {
            String stemmed = stem(word);
            if (stemmed != null && !stemmed.isEmpty()) {
                result.add(stemmed);
            }
        }
        return result;
    }

    /**
     * The words of the text, lower cased - found by ICU in the rules of the current locale, so a language written
     * without spaces is split into words too.
     */
    private static List<String> words(String text) {
        Locale locale = LocalizeManager.get().getLocale();
        BreakIterator iterator = BreakIterator.getWordInstance(ULocale.forLocale(locale));
        iterator.setText(text);

        List<String> words = new ArrayList<>();
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            if (iterator.getRuleStatus() != BreakIterator.WORD_NONE) {
                words.add(text.substring(start, end).toLowerCase(locale));
            }
        }
        return words;
    }

    /**
     * The stem of an English word - the Porter stemmer knows English only, so a word of another language is kept as
     * it is. Null for a word which is no word.
     */
    @Override
    public @Nullable String stem(String word) {
        if (word.isEmpty()) {
            return null;
        }
        if (isAscii(word)) {
            return PorterStemmerUtil.stem(word);
        }
        SearchStemmer stemmer = localeStemmer();
        return stemmer == null ? word : stemmer.stem(word);
    }

    /**
     * The stemmer of the language of the current locale - null when no plugin brings one.
     */
    private @Nullable SearchStemmer localeStemmer() {
        Optional<SearchStemmer> stemmer = myLocaleStemmer;
        if (stemmer == null) {
            String language = LocalizeManager.get().getLocale().getLanguage();
            stemmer = Optional.ofNullable(myApplication.getExtensionPoint(SearchStemmer.class)
                .findFirstSafe(it -> !Locale.ENGLISH.getLanguage().equals(language) && it.getLocale().getLanguage().equals(language)));
            myLocaleStemmer = stemmer;
        }
        return stemmer.orElse(null);
    }

    private static boolean isAscii(String word) {
        for (int i = 0; i < word.length(); i++) {
            if (word.charAt(i) > 127) {
                return false;
            }
        }
        return true;
    }
}
