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
package consulo.language.spellchecker.editor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.language.spellchecker.editor.dictionary.DictionaryLayer;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Project entry point of the platform spellchecking: user/project dictionaries and bundled word lists first, then the active
 * {@link SpellcheckerEngine}.
 *
 * @author VISTALL
 * @since 2026-11-01
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface SpellCheckerManager {
    static SpellCheckerManager getInstance(Project project) {
        return project.getInstance(SpellCheckerManager.class);
    }

    boolean hasProblem(String word);

    /**
     * @param text word or identifier; identifiers are split into words and suggestions are combined
     */
    List<String> getSuggestions(String text);

    /**
     * Saves the word to the project dictionary.
     */
    void acceptWordAsCorrect(String word);

    /**
     * Saves the word to the given dictionary layer. Undoable when {@code file} is set.
     */
    void acceptWordAsCorrect(String word, @Nullable VirtualFile file, DictionaryLayer dictionaryLayer);

    /**
     * @return words saved by the user to the project and application dictionaries
     */
    Set<String> getUserDictionaryWords();

    void updateUserDictionary(Collection<String> words);

    /**
     * Reloads bundled word lists and custom dictionary folders and restarts highlighting.
     */
    void reloadDictionaries();
}
