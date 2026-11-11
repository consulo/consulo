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
package consulo.ai.impl.internal.setting;

import consulo.ai.impl.internal.setting.state.AISettingsState;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import jakarta.inject.Singleton;

/**
 * Keeps the configured instances and the default selection - see {@link AISettingsState}.
 *
 * @author VISTALL
 * @since 2026-08-04
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@State(name = "AISettings", storages = @Storage("ai"))
public class AISettings implements PersistentStateComponent<AISettingsState> {
    private AISettingsState myState = new AISettingsState();

    @Override
    public AISettingsState getState() {
        return myState;
    }

    @Override
    public void loadState(AISettingsState state) {
        myState = state;
    }
}
