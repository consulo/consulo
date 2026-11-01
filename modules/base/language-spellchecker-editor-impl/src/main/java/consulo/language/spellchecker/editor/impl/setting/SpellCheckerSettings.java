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
package consulo.language.spellchecker.editor.impl.setting;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Set;

@Singleton
@State(name = "SpellCheckerSettings", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class SpellCheckerSettings implements PersistentStateComponent<SpellCheckerSettingsState> {
    private SpellCheckerSettingsState myState = new SpellCheckerSettingsState();

    public List<String> getDictionaryFoldersPaths() {
        return myState.dictionaryFoldersPaths;
    }

    public void setDictionaryFoldersPaths(List<String> dictionaryFoldersPaths) {
        myState.dictionaryFoldersPaths = dictionaryFoldersPaths;
    }

    public Set<String> getDisabledDictionariesPaths() {
        return myState.disabledDictionariesPaths;
    }

    public void setDisabledDictionariesPaths(Set<String> disabledDictionariesPaths) {
        myState.disabledDictionariesPaths = disabledDictionariesPaths;
    }

    public Set<String> getBundledDisabledDictionariesPaths() {
        return myState.bundledDisabledDictionariesPaths;
    }

    public void setBundledDisabledDictionariesPaths(Set<String> bundledDisabledDictionariesPaths) {
        myState.bundledDisabledDictionariesPaths = bundledDisabledDictionariesPaths;
    }

    @Override
    public SpellCheckerSettingsState getState() {
        return myState;
    }

    @Override
    public void loadState(SpellCheckerSettingsState state) {
        myState = state;
    }
}
