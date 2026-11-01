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
package consulo.language.spellchecker.editor.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.util.matcher.NameUtil;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.WriteCommandAction;
import consulo.language.spellchecker.editor.SpellCheckerManager;
import consulo.language.spellchecker.editor.SpellcheckerEngine;
import consulo.language.spellchecker.editor.SpellcheckerEngineManager;
import consulo.language.spellchecker.editor.dictionary.BundledDictionaryProvider;
import consulo.language.spellchecker.editor.dictionary.DictionaryLayer;
import consulo.language.spellchecker.editor.dictionary.EditableDictionary;
import consulo.language.spellchecker.editor.impl.dictionary.ProjectDictionary;
import consulo.language.spellchecker.editor.impl.dictionary.ProjectDictionaryLayer;
import consulo.language.spellchecker.editor.impl.setting.SpellCheckerSettings;
import consulo.language.spellchecker.editor.impl.state.AppDictionaryState;
import consulo.language.spellchecker.editor.impl.state.ProjectDictionaryState;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.undoRedo.BasicUndoableAction;
import consulo.undoRedo.ProjectUndoManager;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-11-01
 */
@Singleton
@ServiceImpl
public class SpellCheckerManagerImpl implements SpellCheckerManager {
    private static final Logger LOG = Logger.getInstance(SpellCheckerManagerImpl.class);

    private static final int MAX_SUGGESTIONS_PER_WORD = 5;

    private final Project myProject;
    private final Application myApplication;
    private final SpellcheckerEngineManager myEngineManager;
    private final SpellCheckerSettings mySettings;
    private final ProjectDictionaryState myProjectDictionaryState;
    private final AppDictionaryState myAppDictionaryState;

    private volatile @Nullable Set<String> myBundledWords;

    @Inject
    public SpellCheckerManagerImpl(
        Project project,
        Application application,
        SpellcheckerEngineManager engineManager,
        SpellCheckerSettings settings,
        ProjectDictionaryState projectDictionaryState,
        AppDictionaryState appDictionaryState
    ) {
        myProject = project;
        myApplication = application;
        myEngineManager = engineManager;
        mySettings = settings;
        myProjectDictionaryState = projectDictionaryState;
        myAppDictionaryState = appDictionaryState;
    }

    public ProjectDictionary getProjectDictionary() {
        ProjectDictionary projectDictionary = myProjectDictionaryState.getProjectDictionary();
        if (projectDictionary.getActiveName() == null) {
            projectDictionary.setActiveName(Platform.current().user().name());
        }
        return projectDictionary;
    }

    public EditableDictionary getAppDictionary() {
        return myAppDictionaryState.getOrCreateDictionary();
    }

    @Override
    public boolean hasProblem(String word) {
        if (isAccepted(word)) {
            return false;
        }
        SpellcheckerEngine engine = myEngineManager.getActiveEngine();
        return engine != null && engine.hasProblem(myProject, word);
    }

    private boolean isAccepted(String word) {
        String lowerCase = word.toLowerCase(Locale.ROOT);
        if (getBundledWords().contains(lowerCase)) {
            return true;
        }
        return isInUserDictionary(getProjectDictionary(), word, lowerCase) || isInUserDictionary(getAppDictionary(), word, lowerCase);
    }

    private static boolean isInUserDictionary(EditableDictionary dictionary, String word, String lowerCase) {
        return Boolean.TRUE.equals(dictionary.contains(word)) || Boolean.TRUE.equals(dictionary.contains(lowerCase));
    }

    @Override
    public List<String> getSuggestions(String text) {
        SpellcheckerEngine engine = myEngineManager.getActiveEngine();
        if (engine == null) {
            return List.of();
        }

        // identifiers are split into words, and suggestions of the misspelled words are combined with the correct ones
        String[] words = NameUtil.nameToWords(text);
        List<List<String>> variants = new ArrayList<>(words.length);
        for (String word : words) {
            if (hasProblem(word)) {
                variants.add(getWordSuggestions(engine, word));
            }
            else {
                variants.add(List.of(word));
            }
        }

        List<String> result = new ArrayList<>();
        combine(variants, 0, "", result);
        return new ArrayList<>(new LinkedHashSet<>(result));
    }

    private List<String> getWordSuggestions(SpellcheckerEngine engine, String word) {
        List<String> suggestions = new ArrayList<>(engine.getSuggestions(myProject, word));
        if (suggestions.size() > MAX_SUGGESTIONS_PER_WORD) {
            suggestions = new ArrayList<>(suggestions.subList(0, MAX_SUGGESTIONS_PER_WORD));
        }
        if (Strings.isCapitalized(word)) {
            Strings.capitalize(suggestions);
        }
        else if (Strings.isUpperCase(word)) {
            Strings.upperCase(suggestions);
        }
        return suggestions;
    }

    private static void combine(List<List<String>> variants, int index, String prefix, List<String> result) {
        if (index == variants.size()) {
            result.add(prefix);
            return;
        }
        for (String variant : variants.get(index)) {
            combine(variants, index + 1, prefix + variant, result);
        }
    }

    @Override
    public void acceptWordAsCorrect(String word) {
        acceptWordAsCorrect(word, null, new ProjectDictionaryLayer(myProject));
    }

    @Override
    public void acceptWordAsCorrect(String word, @Nullable VirtualFile file, DictionaryLayer dictionaryLayer) {
        String transformed = transformWord(word);
        EditableDictionary dictionary = dictionaryLayer.getDictionary();
        if (file != null) {
            WriteCommandAction.writeCommandAction(myProject).run(() -> {
                ProjectUndoManager.getInstance(myProject).undoableActionPerformed(new BasicUndoableAction(file) {
                    @Override
                    public void undo() {
                        removeWordFromDictionary(dictionary, transformed);
                    }

                    @Override
                    public void redo() {
                        addWordToDictionary(dictionary, transformed);
                    }
                });
            });
        }
        addWordToDictionary(dictionary, transformed);
    }

    private void addWordToDictionary(EditableDictionary dictionary, String transformed) {
        dictionary.addToDictionary(transformed);
        restartInspections();
    }

    private void removeWordFromDictionary(EditableDictionary dictionary, String transformed) {
        dictionary.removeFromDictionary(transformed);
        restartInspections();
    }

    private static String transformWord(String word) {
        return Strings.isCapitalized(word) && !Strings.isMixedCase(word) ? word.toLowerCase(Locale.ROOT) : word;
    }

    @Override
    public Set<String> getUserDictionaryWords() {
        Set<String> words = new HashSet<>();
        addEditableWords(getProjectDictionary(), words);
        addEditableWords(getAppDictionary(), words);
        return words;
    }

    private static void addEditableWords(EditableDictionary dictionary, Set<String> words) {
        Set<String> editableWords = dictionary.getEditableWords();
        if (editableWords != null) {
            words.addAll(editableWords);
        }
    }

    @Override
    public void updateUserDictionary(Collection<String> words) {
        Set<String> userDictionaryWords = getUserDictionaryWords();

        // new for project dictionary
        ProjectDictionary projectDictionary = getProjectDictionary();
        for (String word : words) {
            if (!userDictionaryWords.contains(word)) {
                projectDictionary.addToDictionary(word);
            }
        }

        Set<String> wordSet = new HashSet<>(words);

        // deleted from project dictionary
        removeDeletedWords(projectDictionary, wordSet);

        // deleted from application dictionary
        removeDeletedWords(getAppDictionary(), wordSet);

        restartInspections();
    }

    private static void removeDeletedWords(EditableDictionary dictionary, Set<String> wordSet) {
        Set<String> editableWords = dictionary.getEditableWords();
        if (editableWords == null) {
            return;
        }
        for (String word : new ArrayList<>(editableWords)) {
            if (!wordSet.contains(word)) {
                dictionary.removeFromDictionary(word);
            }
        }
    }

    @Override
    public void reloadDictionaries() {
        myBundledWords = null;
        restartInspections();
    }

    private Set<String> getBundledWords() {
        Set<String> bundledWords = myBundledWords;
        if (bundledWords == null) {
            bundledWords = loadBundledWords();
            myBundledWords = bundledWords;
        }
        return bundledWords;
    }

    private Set<String> loadBundledWords() {
        Set<String> words = new HashSet<>();
        Consumer<String> consumer = line -> {
            String word = line.trim();
            if (!word.isEmpty()) {
                words.add(word.toLowerCase(Locale.ROOT));
            }
        };

        Set<String> disabledBundled = mySettings.getBundledDisabledDictionariesPaths();
        myApplication.getExtensionPoint(BundledDictionaryProvider.class).forEach(provider -> provider.loadDictionaries((name, stream) -> {
            if (disabledBundled.contains(name)) {
                closeQuietly(stream);
                return;
            }
            readLines(stream, name, consumer);
        }));

        Set<String> disabledDictionaries = mySettings.getDisabledDictionariesPaths();
        for (String folder : mySettings.getDictionaryFoldersPaths()) {
            for (String path : findDictionaryFiles(folder)) {
                if (disabledDictionaries.contains(path)) {
                    continue;
                }
                try {
                    readLines(new FileInputStream(path), path, consumer);
                }
                catch (IOException e) {
                    LOG.warn("Couldn't load dictionary '" + path + "'", e);
                }
            }
        }
        return words;
    }

    public static List<String> findDictionaryFiles(String folder) {
        List<String> result = new ArrayList<>();
        File rootFile = new File(folder);
        if (rootFile.isDirectory()) {
            FileUtil.processFilesRecursively(rootFile, file -> {
                if (!file.isDirectory() && file.getPath().endsWith(".dic")) {
                    result.add(file.getPath());
                }
                return true;
            });
        }
        return result;
    }

    private static void readLines(InputStream stream, String name, Consumer<String> consumer) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                consumer.accept(line);
            }
        }
        catch (IOException e) {
            LOG.warn("Couldn't read dictionary '" + name + "'", e);
        }
    }

    private static void closeQuietly(InputStream stream) {
        try {
            stream.close();
        }
        catch (IOException ignored) {
        }
    }

    /**
     * @return names of all bundled dictionaries
     */
    public static Set<String> getBundledDictionaryNames(Application application) {
        Set<String> names = new TreeSet<>();
        application.getExtensionPoint(BundledDictionaryProvider.class).forEach(provider -> provider.loadDictionaries((name, stream) -> {
            names.add(name);
            closeQuietly(stream);
        }));
        return names;
    }

    private void restartInspections() {
        myApplication.invokeLater(() -> {
            if (!myProject.isDisposed()) {
                DaemonCodeAnalyzer.getInstance(myProject).restart();
            }
        });
    }
}
