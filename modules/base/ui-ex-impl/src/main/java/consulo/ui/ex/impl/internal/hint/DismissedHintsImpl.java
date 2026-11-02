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
package consulo.ui.ex.impl.internal.hint;

import consulo.annotation.component.ServiceImpl;
import consulo.ui.ex.internal.DismissedHints;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2026-11-02
 */
@Singleton
@State(name = "DismissedHints", storages = @Storage("dismissedHints"))
@ServiceImpl
public class DismissedHintsImpl implements DismissedHints, PersistentStateComponent<DismissedHintsState> {
    private DismissedHintsState myState = new DismissedHintsState();

    @Override
    public boolean isDismissed(String id) {
        return myState.dismissed.contains(id);
    }

    @Override
    public void dismiss(String id) {
        myState.dismissed.add(id);
    }

    @Override
    public DismissedHintsState getState() {
        return myState;
    }

    @Override
    public void loadState(DismissedHintsState state) {
        myState = state;
    }
}
