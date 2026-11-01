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
package consulo.language.spellchecker.editor.impl.state;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.language.spellchecker.editor.dictionary.EditableDictionary;
import consulo.language.spellchecker.editor.impl.dictionary.UserDictionary;
import jakarta.inject.Singleton;

import java.util.Set;
import java.util.TreeSet;

// do not change component name - `CachedDictionaryState` is used historically and no easy way to change it.
@Singleton
@State(name = "CachedDictionaryState", storages = @Storage("cachedDictionary"))
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class AppDictionaryState implements PersistentStateComponent<AppDictionaryJsonState> {
    public static final String DEFAULT_NAME = "cached";

    private final EditableDictionary myDictionary = new UserDictionary(DEFAULT_NAME);

    public EditableDictionary getOrCreateDictionary() {
        return myDictionary;
    }

    @Override
    public synchronized AppDictionaryJsonState getState() {
        AppDictionaryJsonState state = new AppDictionaryJsonState();
        Set<String> words = myDictionary.getWords();
        if (words != null) {
            state.words = new TreeSet<>(words);
        }
        return state;
    }

    @Override
    public synchronized void loadState(AppDictionaryJsonState state) {
        myDictionary.replaceAll(state.words);
    }
}
