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
package consulo.ui.impl.style;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.disposer.Disposable;
import consulo.ui.style.StyleManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-29
 */
@Singleton
@State(name = "LafManager", storages = @Storage(value = "laf", roamingType = RoamingType.PER_OS))
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class StyleManagerService implements PersistentStateComponent<StyleManagerState>, Disposable {
    private final PersistentStyleManagerImpl<StyleImpl> myStyleManager;

    @SuppressWarnings("unchecked")
    @Inject
    public StyleManagerService() {
        myStyleManager = (PersistentStyleManagerImpl<StyleImpl>) StyleManager.get();
    }

    @Nullable
    @Override
    public StyleManagerState getState() {
        return myStyleManager.getState();
    }

    @Override
    public void loadState(StyleManagerState state) {
        myStyleManager.loadState(state);
    }

    @Override
    public void afterLoad(boolean first) {
        myStyleManager.afterLoad(this);
    }

    @Override
    public void dispose() {

    }
}
