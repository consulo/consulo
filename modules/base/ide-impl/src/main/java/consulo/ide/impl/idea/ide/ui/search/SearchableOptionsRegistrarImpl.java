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

package consulo.ide.impl.idea.ide.ui.search;

import consulo.annotation.component.ServiceImpl;
import consulo.application.localize.ApplicationLocalize;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurableHit;
import consulo.configurable.SearchableConfigurable;
import consulo.configurable.SearchableOptionsRegistrar;
import consulo.container.plugin.PluginManager;
import consulo.externalService.plugin.PluginsConfigurable;
import consulo.ide.impl.idea.openapi.util.JDOMUtil;
import consulo.ide.localize.IdeLocalize;
import consulo.language.codeStyle.CodeStyle;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.interner.Interner;
import consulo.util.io.ResourceUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.Couple;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Document;
import org.jdom.Element;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author anna
 * @since 2006-02-07
 */
@Singleton
@ServiceImpl
public class SearchableOptionsRegistrarImpl extends SearchableOptionsRegistrar {
    private final Map<String, Set<OptionDescription>> myStorage =
        Collections.synchronizedMap(new HashMap<String, Set<OptionDescription>>(20, 0.9f));
    private final Map<String, String> myId2Name = Collections.synchronizedMap(new HashMap<String, String>(20, 0.9f));

    private final Set<String> myStopWords = Collections.synchronizedSet(new HashSet<String>());
    private final Map<Couple<String>, Set<String>> myHighlightOption2Synonym =
        Collections.synchronizedMap(new HashMap<Couple<String>, Set<String>>());
    private volatile boolean allTheseHugeFilesAreLoaded;

    private final Interner<String> myIdentifierTable = Interner.createStringInterner();

    private static final Logger LOG = Logger.getInstance(SearchableOptionsRegistrarImpl.class);
    public static final int LOAD_FACTOR = 20;

    private static final Pattern REG_EXP = Pattern.compile("[\\W&&[^-]]+");

    @Inject
    public SearchableOptionsRegistrarImpl() {
        try {
            //stop words
            String text =
                ResourceUtil.loadText(ResourceUtil.getResource(SearchableOptionsRegistrarImpl.class, "/search/", "ignore.txt"));
            String[] stopWords = text.split("[\\W]");
            ContainerUtil.addAll(myStopWords, stopWords);
        }
        catch (IOException e) {
            LOG.error(e);
        }
    }

    private void loadHugeFilesIfNecessary() {
        if (allTheseHugeFilesAreLoaded) {
            return;
        }
        allTheseHugeFilesAreLoaded = true;
        try {
            //index
            URL indexResource = ResourceUtil.getResource(SearchableOptionsRegistrar.class, "/search/", "searchableOptions.xml");
            if (indexResource == null) {
                LOG.info("No /search/searchableOptions.xml found, settings search won't work!");
                return;
            }

            Document document = JDOMUtil.loadDocument(indexResource);
            Element root = document.getRootElement();
            List configurables = root.getChildren("configurable");
            for (Object o : configurables) {
                Element configurable = (Element)o;
                String id = configurable.getAttributeValue("id");
                String groupName = configurable.getAttributeValue("configurable_name");
                List options = configurable.getChildren("option");
                for (Object o1 : options) {
                    Element optionElement = (Element)o1;
                    String option = optionElement.getAttributeValue("name");
                    String path = optionElement.getAttributeValue("path");
                    String hit = optionElement.getAttributeValue("hit");
                    putOptionWithHelpId(option, id, groupName, hit, path);
                }
            }

            //synonyms
            document = JDOMUtil.loadDocument(ResourceUtil.getResource(SearchableOptionsRegistrar.class, "/search/", "synonyms.xml"));
            root = document.getRootElement();
            configurables = root.getChildren("configurable");
            for (Object o : configurables) {
                Element configurable = (Element)o;
                String id = configurable.getAttributeValue("id");
                String groupName = configurable.getAttributeValue("configurable_name");
                List synonyms = configurable.getChildren("synonym");
                for (Object o1 : synonyms) {
                    Element synonymElement = (Element)o1;
                    String synonym = synonymElement.getTextNormalize();
                    if (synonym != null) {
                        Set<String> words = getProcessedWords(synonym);
                        for (String word : words) {
                            putOptionWithHelpId(word, id, groupName, synonym, null);
                        }
                    }
                }
                List options = configurable.getChildren("option");
                for (Object o1 : options) {
                    Element optionElement = (Element)o1;
                    String option = optionElement.getAttributeValue("name");
                    List list = optionElement.getChildren("synonym");
                    for (Object o2 : list) {
                        Element synonymElement = (Element)o2;
                        String synonym = synonymElement.getTextNormalize();
                        if (synonym != null) {
                            Set<String> words = getProcessedWords(synonym);
                            for (String word : words) {
                                putOptionWithHelpId(word, id, groupName, synonym, null);
                            }
                            Couple<String> key = Couple.of(option, id);
                            Set<String> foundSynonyms = myHighlightOption2Synonym.get(key);
                            if (foundSynonyms == null) {
                                foundSynonyms = new HashSet<>();
                                myHighlightOption2Synonym.put(key, foundSynonyms);
                            }
                            foundSynonyms.add(synonym);
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            LOG.error(e);
        }

        PluginManager.forEachEnabledPlugin(plugin -> {
            Set<String> words = getProcessedWordsWithoutStemming(plugin.getName());
            String description = plugin.getDescription();
            if (description != null) {
                words.addAll(getProcessedWordsWithoutStemming(description));
            }
            for (String word : words) {
                addOption(word, null, plugin.getName(), PluginsConfigurable.CONFIGURABLE_ID, IdeLocalize.titlePlugins().get());
            }
        });

        for (SearchableOptionContributor contributor : SearchableOptionContributor.EP_NAME.getExtensionList()) {
            contributor.processOptions(new SearchableOptionProcessor() {
                @Override
                public void addOption(String option, String path, String hit, String configurableId, String configurableDisplayName) {
                    SearchableOptionsRegistrarImpl.this.addOption(option, path, hit, configurableId, configurableDisplayName);
                }

                @Override
                public Set<String> getProcessedWordsWithoutStemming(@Nonnull String text) {
                    return SearchableOptionsRegistrarImpl.this.getProcessedWordsWithoutStemming(text);
                }

                @Override
                public Set<String> getProcessedWords(@Nonnull String text) {
                    return SearchableOptionsRegistrarImpl.this.getProcessedWords(text);
                }
            });
        }
    }

    private synchronized void putOptionWithHelpId(String option, String id, String groupName, String hit, String path) {
        if (isStopWord(option)) {
            return;
        }
        String stopWord = PorterStemmerUtil.stem(option);
        if (stopWord == null) {
            return;
        }
        if (isStopWord(stopWord)) {
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
        }
        else {
            configs.add(description);
        }
    }

    @Override
    @Nonnull
    public ConfigurableHit getConfigurables(
        Configurable[] allConfigurables,
        boolean changed,
        Set<Configurable> configurables,
        String option,
        Project project
    ) {
        ConfigurableHit hits = new ConfigurableHit();
        Set<Configurable> contentHits = hits.getContentHits();

        Set<String> options = getProcessedWordsWithoutStemming(option);
        if (configurables == null) {
            contentHits.addAll(SearchUtil.expandGroup(allConfigurables));
        }
        else {
            contentHits.addAll(configurables);
        }

        String optionToCheck = option.trim().toLowerCase();
        for (Configurable each : contentHits) {
            if (each.getDisplayName() == null) {
                continue;
            }
            String displayName = each.getDisplayName().toLowerCase();
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
            String[] components = REG_EXP.split(optionToCheck);
            if (components.length > 0) {
                Collections.addAll(options, components);
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
                if (!(configurable instanceof SearchableConfigurable && helpIds.contains(configurable.getId()))) {
                    it.remove();
                }
            }
        }
        if (currentConfigurables.equals(contentHits) && !(configurables == null && changed)) {
            return getConfigurables(allConfigurables, true, null, option, project);
        }
        return hits;
    }


    @Nullable
    public synchronized Set<OptionDescription> getAcceptableDescriptions(String prefix) {
        if (prefix == null) {
            return null;
        }
        String stemmedPrefix = PorterStemmerUtil.stem(prefix);
        if (StringUtil.isEmptyOrSpaces(stemmedPrefix)) {
            return null;
        }
        loadHugeFilesIfNecessary();
        Set<OptionDescription> result = null;
        for (Map.Entry<String, Set<OptionDescription>> entry : myStorage.entrySet()) {
            Set<OptionDescription> descriptions = entry.getValue();
            if (descriptions != null) {
                String option = entry.getKey();
                if (!option.startsWith(prefix) && !option.startsWith(stemmedPrefix)) {
                    String stemmedOption = PorterStemmerUtil.stem(option);
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
    @Nullable
    public String getInnerPath(SearchableConfigurable configurable, String option) {
        loadHugeFilesIfNecessary();
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
    public boolean isStopWord(String word) {
        return myStopWords.contains(word);
    }

    @Override
    public Set<String> getSynonym(String option, @Nonnull SearchableConfigurable configurable) {
        loadHugeFilesIfNecessary();
        return myHighlightOption2Synonym.get(Couple.of(option, configurable.getId()));
    }

    @Override
    public Map<String, Set<String>> findPossibleExtension(@Nonnull String prefix, Project project) {
        loadHugeFilesIfNecessary();
        boolean perProject = CodeStyle.usesOwnSettings(project);
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
                if (perProject) {
                    if (Comparing.strEqual(groupName, ApplicationLocalize.titleGlobalCodeStyle().get())) {
                        groupName = ApplicationLocalize.titleProjectCodeStyle().get();
                    }
                }
                else {
                    if (Comparing.strEqual(groupName, ApplicationLocalize.titleProjectCodeStyle().get())) {
                        groupName = ApplicationLocalize.titleGlobalCodeStyle().get();
                    }
                }
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
    public void addOption(String option, String path, String hit, String configurableId, String configurableDisplayName) {
        putOptionWithHelpId(option, configurableId, configurableDisplayName, hit, path);
    }

    @Override
    public Set<String> getProcessedWordsWithoutStemming(@Nonnull String text) {
        Set<String> result = new HashSet<>();
        String toLowerCase = text.toLowerCase();
        String[] options = REG_EXP.split(toLowerCase);
        for (String opt : options) {
            if (isStopWord(opt)) {
                continue;
            }
            String processed = PorterStemmerUtil.stem(opt);
            if (isStopWord(processed)) {
                continue;
            }
            result.add(opt);
        }
        return result;
    }

    @Override
    public Set<String> getProcessedWords(@Nonnull String text) {
        Set<String> result = new HashSet<>();
        String toLowerCase = text.toLowerCase();
        String[] options = REG_EXP.split(toLowerCase);
        for (String opt : options) {
            if (isStopWord(opt)) {
                continue;
            }
            opt = PorterStemmerUtil.stem(opt);
            if (opt == null) {
                continue;
            }
            result.add(opt);
        }
        return result;
    }

    @Override
    public Set<String> replaceSynonyms(Set<String> options, SearchableConfigurable configurable) {
        Set<String> result = new HashSet<>(options);
        for (String option : options) {
            Set<String> synonyms = getSynonym(option, configurable);
            if (synonyms != null) {
                result.addAll(synonyms);
            }
            else {
                result.add(option);
            }
        }
        return result;
    }
}
