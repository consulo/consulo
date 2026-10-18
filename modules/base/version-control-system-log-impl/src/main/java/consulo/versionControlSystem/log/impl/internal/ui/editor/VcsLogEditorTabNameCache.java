/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors.
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
package consulo.versionControlSystem.log.impl.internal.ui.editor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ui.UISettings;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * Remembers the names of the log editor tabs, so reopened tabs show them before the log is loaded.
 */
@Singleton
@State(name = "Vcs.Log.Editor.Tab.Names", storages = @Storage(value = "vcs.log.editor.tab.names", roamingType = RoamingType.DISABLED))
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class VcsLogEditorTabNameCache implements PersistentStateComponent<VcsLogEditorTabNameCache.State> {
    private State myState = new State();

    public @Nullable String getTabName(String path) {
        return myState.PATH_TO_TAB_NAME.get(path);
    }

    public void putTabName(String path, String tabName) {
        myState.PATH_TO_TAB_NAME.remove(path);
        myState.PATH_TO_TAB_NAME.put(path, tabName);

        int limit = UISettings.getInstance().getRecentFilesLimit();
        Iterator<String> iterator = myState.PATH_TO_TAB_NAME.keySet().iterator();
        while (myState.PATH_TO_TAB_NAME.size() > limit && iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(State state) {
        myState = state;
    }

    public static class State {
        public LinkedHashMap<String, String> PATH_TO_TAB_NAME = new LinkedHashMap<>();
    }
}
