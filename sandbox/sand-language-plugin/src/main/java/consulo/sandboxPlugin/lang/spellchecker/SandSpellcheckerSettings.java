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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2026-11-01
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@State(name = "SandSpellcheckerSettings", storages = @Storage("sand-spellchecker"))
public class SandSpellcheckerSettings implements PersistentStateComponent<SandSpellcheckerSettingsState> {
    private SandSpellcheckerSettingsState myState = new SandSpellcheckerSettingsState();

    public boolean isIgnoreUpperCaseWords() {
        return myState.ignoreUpperCaseWords;
    }

    public void setIgnoreUpperCaseWords(boolean ignoreUpperCaseWords) {
        myState.ignoreUpperCaseWords = ignoreUpperCaseWords;
    }

    @Override
    public SandSpellcheckerSettingsState getState() {
        return myState;
    }

    @Override
    public void loadState(SandSpellcheckerSettingsState state) {
        myState = state;
    }
}
