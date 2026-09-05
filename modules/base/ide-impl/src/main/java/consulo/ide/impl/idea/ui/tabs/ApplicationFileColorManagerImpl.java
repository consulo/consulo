/*
 * Copyright 2013-2024 consulo.io
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
package consulo.ide.impl.idea.ui.tabs;

import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.project.ui.view.tree.ApplicationFileColorManager;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 21-Jul-24
 */
@Singleton
@ServiceImpl
@State(name = "ApplicationFileColorManager", storages = @Storage("file.colors"))
public class ApplicationFileColorManagerImpl implements ApplicationFileColorManager, PersistentStateComponent<ApplicationFileColorManagerState> {

    private ApplicationFileColorManagerState myState = new ApplicationFileColorManagerState();

    @Override
    public @Nullable ApplicationFileColorManagerState getState() {
        return myState;
    }

    @Override
    public void loadState(ApplicationFileColorManagerState state) {
        myState = state;
    }

    @Override
    public boolean isEnabled() {
        return myState.enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        myState.enabled = enabled;
    }

    @Override
    public boolean isEnabledForTabs() {
        return myState.enabledForTabs;
    }

    @Override
    public void setEnabledForTabs(boolean enabled) {
        myState.enabledForTabs = enabled;
    }

    @Override
    public boolean isEnabledForProjectView() {
        return myState.enabledForProjectView;
    }

    @Override
    public void setEnabledForProjectView(boolean enabled) {
        myState.enabledForProjectView = enabled;
    }
}
