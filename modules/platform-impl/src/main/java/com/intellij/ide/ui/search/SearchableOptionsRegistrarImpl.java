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

package com.intellij.ide.ui.search;

import com.intellij.codeStyle.CodeStyleFacade;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerConfigurable;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ResourceUtil;
import com.intellij.util.SingletonSet;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.StringInterner;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.DocumentEvent;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

/**
 * User: anna
 * Date: 07-Feb-2006
 */
public class SearchableOptionsRegistrarImpl extends SearchableOptionsRegistrar {
  private final Map<String, Set<OptionDescription>> myStorage = Collections.synchronizedMap(new THashMap<String, Set<OptionDescription>>(20, 0.9f));
  private final Map<String, String> myId2Name = Collections.synchronizedMap(new THashMap<String, String>(20, 0.9f));

  private final Set<String> myStopWords = Collections.synchronizedSet(new HashSet<String>());
  private final Map<Pair<String, String>, Set<String>> myHighlightOption2Synonym = Collections.synchronizedMap(new THashMap<Pair<String, String>, Set<String>>());
  private volatile boolean allTheseHugeFilesAreLoaded;

  @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
  private final StringInterner myIdentifierTable = new StringInterner() {
    @Override
    @NotNull
    public synchronized String intern(@NotNull final String name) {
      return super.intern(name);
    }
  };

  private static final Logger LOG = Logger.getInstance("#com.intellij.ide.ui.search.SearchableOptionsRegistrarImpl");
  public static final int LOAD_FACTOR = 20;
  @NonNls
  private static final Pattern REG_EXP = Pattern.compile("[\\W&&[^-]]+");

  public SearchableOptionsRegistrarImpl() {
    if (ApplicationManager.getApplication().isCommandLine() ||
        ApplicationManager.getApplication().isUnitTestMode()) return;
    try {
      //stop words
      final String text = ResourceUtil.loadText(ResourceUtil.getResource(SearchableOptionsRegistrarImpl.class, "/search/", "ignore.txt"));
      final String[] stopWords = text.split("[\\W]");
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
      final URL indexResource = ResourceUtil.getResource(SearchableOptionsRegistrar.class, "/search/", "searchableOptions.xml");
      if (indexResource == null) {
        LOG.info("No /search/searchableOptions.xml found, settings search won't work!");
        return;
      }

      Document document =
        JDOMUtil.loadDocument(indexResource);
      Element root = document.getRootElement();
      List configurables = root.getChildren("configurable");
      for (final Object o : configurables) {
        final Element configurable = (Element)o;
        final String id = configurable.getAttributeValue("id");
        final String groupName = configurable.getAttributeValue("configurable_name");
        final List options = configurable.getChildren("option");
        for (Object o1 : options) {
          Element optionElement = (Element)o1;
          final String option = optionElement.getAttributeValue("name");
          final String path = optionElement.getAttributeValue("path");
          final String hit = optionElement.getAttributeValue("hit");
          putOptionWithHelpId(option, id, groupName, hit, path);
        }
      }

      //synonyms
      document = JDOMUtil.loadDocument(ResourceUtil.getResource(SearchableOptionsRegistrar.class, "/search/", "synonyms.xml"));
      root = document.getRootElement();
      configurables = root.getChildren("configurable");
      for (final Object o : configurables) {
        final Element configurable = (Element)o;
        final String id = configurable.getAttributeValue("id");
        final String groupName = configurable.getAttributeValue("configurable_name");
        final List synonyms = configurable.getChildren("synonym");
        for (Object o1 : synonyms) {
          Element synonymElement = (Element)o1;
          final String synonym = synonymElement.getTextNormalize();
          if (synonym != null) {
            Set<String> words = getProcessedWords(synonym);
            for (String word : words) {
              putOptionWithHelpId(word, id, groupName, synonym, null);
            }
          }
        }
        final List options = configurable.getChildren("option");
        for (Object o1 : options) {
          Element optionElement = (Element)o1;
          final String option = optionElement.getAttributeValue("name");
          final List list = optionElement.getChildren("synonym");
          for (Object o2 : list) {
            Element synonymElement = (Element)o2;
            final String synonym = synonymElement.getTextNormalize();
            if (synonym != null) {
              Set<String> words = getProcessedWords(synonym);
              for (String word : words) {
                putOptionWithHelpId(word, id, groupName, synonym, null);
              }
              final Pair<String, String> key = Pair.create(option, id);
              Set<String> foundSynonyms = myHighlightOption2Synonym.get(key);
              if (foundSynonyms == null) {
                foundSynonyms = new THashSet<String>();
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

    for (IdeaPluginDescriptor plugin : PluginManagerCore.getPlugins()) {
      final Set<String> words = getProcessedWordsWithoutStemming(plugin.getName());
      final String description = plugin.getDescription();
      if (description != null) {
        words.addAll(getProcessedWordsWithoutStemming(description));
      }
      for (String word : words) {
        addOption(word, null, plugin.getName(), PluginManagerConfigurable.ID, IdeBundle.message("title.plugins"));
      }
    }
  }

  private synchronized void putOptionWithHelpId(String option, final String id, final String groupName, String hit, final String path) {
    if (isStopWord(option)) return;
    String stopWord = PorterStemmerUtil.stem(option);
    if (stopWord == null) return;
    if (isStopWord(stopWord)) return;
    if (!myId2Name.containsKey(id) && groupName != null) {
      myId2Name.put(myIdentifierTable.intern(id), myIdentifierTable.intern(groupName));
    }

    OptionDescription description =
      new OptionDescription(null, myIdentifierTable.intern(id).trim(), hit != null ? myIdentifierTable.intern(hit).trim() : null,
                            path != null ? myIdentifierTable.intern(path).trim() : null);
    Set<OptionDescription> configs = myStorage.get(option);
    if (configs == null) {
      configs = new SingletonSet<OptionDescription>(description);
      myStorage.put(new String(option), configs);
    }
    else if (configs instanceof SingletonSet){
      configs = new THashSet<OptionDescription>(configs);
      configs.add(description);
      myStorage.put(new String(option), configs);
    }
    else {
      configs.add(description);
    }
  }

  @Override
  @NotNull
  public ConfigurableHit getConfigurables(Configurable[] allConfigurables,
                                            final DocumentEvent.EventType type,
                                            Set<Configurable> configurables,
                                            String option,
                                            Project project) {

    final ConfigurableHit hits = new ConfigurableHit();
    final Set<Configurable> contentHits = hits.getContentHits();

    Set<String> options = getProcessedWordsWithoutStemming(option);
    if (configurables == null) {
      contentHits.addAll(SearchUtil.expandGroup(allConfigurables));
    }
    else {
      contentHits.addAll(configurables);
    }

    String optionToCheck = option.trim().toLowerCase();
    for (Configurable each : contentHits) {
      if (each.getDisplayName() == null) continue;
      final String displayName = each.getDisplayName().toLowerCase();
      final List<String> allWords = StringUtil.getWordsIn(displayName);
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

    final Set<Configurable> currentConfigurables = new HashSet<Configurable>(contentHits);
    if (options.isEmpty()) { //operate with substring
      String[] components = REG_EXP.split(optionToCheck);
      if (components.length > 0) {
        Collections.addAll(options, components);
      } else {
        options.add(option);
      }
    }
    Set<String> helpIds = null;
    for (String opt : options) {
      final Set<OptionDescription> optionIds = getAcceptableDescriptions(opt);
      if (optionIds == null) {
        contentHits.clear();
        return hits;
      }
      final Set<String> ids = new HashSet<String>();
      for (OptionDescription id : optionIds) {
        ids.add(id.getConfigurableId());
      }
      if (helpIds == null) {
        helpIds = ids;
      }
      helpIds.retainAll(ids);
    }
    if (helpIds != null) {
      for (Iterator<Configurable> it = contentHits.iterator(); it.hasNext();) {
        Configurable configurable = it.next();
        if (CodeStyleFacade.getInstance(project).isUnsuitableCodeStyleConfigurable(configurable)) {
          it.remove();
          continue;
        }
        if (!(configurable instanceof SearchableConfigurable && helpIds.contains(((SearchableConfigurable)configurable).getId()))) {
          it.remove();
        }
      }
    }
    if (currentConfigurables.equals(contentHits) && !(configurables == null && type == DocumentEvent.EventType.CHANGE)) {
      return getConfigurables(allConfigurables, DocumentEvent.EventType.CHANGE, null, option, project);
    }
    return hits;
  }


  @Nullable
  public synchronized Set<OptionDescription> getAcceptableDescriptions(final String prefix) {
    if (prefix == null) return null;
    final String stemmedPrefix = PorterStemmerUtil.stem(prefix);
    if (StringUtil.isEmptyOrSpaces(stemmedPrefix)) return null;
    loadHugeFilesIfNecessary();
    Set<OptionDescription> result = null;
    for (Map.Entry<String, Set<OptionDescription>> entry : myStorage.entrySet()) {
      final Set<OptionDescription> descriptions = entry.getValue();
      if (descriptions != null) {
        final String option = entry.getKey();
        if (!option.startsWith(prefix) && !option.startsWith(stemmedPrefix)) {
          final String stemmedOption = PorterStemmerUtil.stem(option);
          if (stemmedOption != null && !stemmedOption.startsWith(prefix) && !stemmedOption.startsWith(stemmedPrefix)) {
            continue;
          }
        }
        if (result == null) {
          result = new THashSet<OptionDescription>();
        }
        result.addAll(descriptions);
      }
    }
    return result;
  }

  @Override
  @Nullable
  public String getInnerPath(SearchableConfigurable configurable, @NonNls String option) {
    loadHugeFilesIfNecessary();
    Set<OptionDescription> path = null;
    final Set<String> words = getProcessedWordsWithoutStemming(option);
    for (String word : words) {
      Set<OptionDescription> configs = getAcceptableDescriptions(word);
      if (configs == null) return null;
      final Set<OptionDescription> paths = new HashSet<OptionDescription>();
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
        final String hit = description.getHit();
        if (hit != null) {
          boolean theBest = true;
          for (String word : words) {
            if (!hit.contains(word)) {
              theBest = false;
            }
          }
          if (theBest) return description.getPath();
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
  public Set<String> getSynonym(final String option, @NotNull final SearchableConfigurable configurable) {
    loadHugeFilesIfNecessary();
    return myHighlightOption2Synonym.get(Pair.create(option, configurable.getId()));
  }

  @Override
  public Map<String, Set<String>> findPossibleExtension(@NotNull String prefix, final Project project) {
    loadHugeFilesIfNecessary();
    final boolean perProject = CodeStyleFacade.getInstance(project).projectUsesOwnSettings();
    final Map<String, Set<String>> result = new THashMap<String, Set<String>>();
    int count = 0;
    final Set<String> prefixes = getProcessedWordsWithoutStemming(prefix);
    for (String opt : prefixes) {
      Set<OptionDescription> configs = getAcceptableDescriptions(opt);
      if (configs == null) continue;
      for (OptionDescription description : configs) {
        String groupName = myId2Name.get(description.getConfigurableId());
        if (perProject) {
          if (Comparing.strEqual(groupName, ApplicationBundle.message("title.global.code.style"))) {
            groupName = ApplicationBundle.message("title.project.code.style");
          }
        }
        else {
          if (Comparing.strEqual(groupName, ApplicationBundle.message("title.project.code.style"))) {
            groupName = ApplicationBundle.message("title.global.code.style");
          }
        }
        Set<String> foundHits = result.get(groupName);
        if (foundHits == null) {
          foundHits = new THashSet<String>();
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
  public void addOption(String option, String path, final String hit, final String configurableId, final String configurableDisplayName) {
    putOptionWithHelpId(option, configurableId, configurableDisplayName, hit, path);
  }

  @Override
  public Set<String> getProcessedWordsWithoutStemming(@NotNull String text) {
    Set<String> result = new HashSet<String>();
    @NonNls final String toLowerCase = text.toLowerCase();
    final String[] options = REG_EXP.split(toLowerCase);
    for (String opt : options) {
      if (isStopWord(opt)) continue;
      final String processed = PorterStemmerUtil.stem(opt);
      if (isStopWord(processed)) continue;
      result.add(opt);
    }
    return result;
  }

  @Override
  public Set<String> getProcessedWords(@NotNull String text) {
    Set<String> result = new HashSet<String>();
    @NonNls final String toLowerCase = text.toLowerCase();
    final String[] options = REG_EXP.split(toLowerCase);
    for (String opt : options) {
      if (isStopWord(opt)) continue;
      opt = PorterStemmerUtil.stem(opt);
      if (opt == null) continue;
      result.add(opt);
    }
    return result;
  }

  @Override
  public Set<String> replaceSynonyms(Set<String> options, SearchableConfigurable configurable) {
    final Set<String> result = new HashSet<String>(options);
    for (String option : options) {
      final Set<String> synonyms = getSynonym(option, configurable);
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
