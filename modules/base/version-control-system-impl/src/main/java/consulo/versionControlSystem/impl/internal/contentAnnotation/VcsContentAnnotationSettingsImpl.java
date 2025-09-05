/*
 * Copyright 2013-2025 consulo.io
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
package consulo.versionControlSystem.impl.internal.contentAnnotation;

import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.versionControlSystem.contentAnnotation.VcsContentAnnotationSettings;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2025-09-03
 */
@Singleton
@State(name = "VcsContentAnnotationSettings", storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)})
@ServiceImpl
public class VcsContentAnnotationSettingsImpl implements VcsContentAnnotationSettings, PersistentStateComponent<VcsContentAnnotationSettingsState> {
    private VcsContentAnnotationSettingsState myState = new VcsContentAnnotationSettingsState();

    @Override
    public VcsContentAnnotationSettingsState getState() {
        return myState;
    }

    @Override
    public void loadState(VcsContentAnnotationSettingsState state) {
        myState = state;
    }

    @Override
    public long getLimit() {
        return myState.myLimit;
    }

    @Override
    public int getLimitDays() {
        return (int) (myState.myLimit / VcsContentAnnotationSettingsState.ourMillisecondsInDay);
    }

    @Override
    public void setLimit(int limit) {
        myState.myLimit = VcsContentAnnotationSettingsState.ourMillisecondsInDay * limit;
    }

    @Override
    public boolean isShow() {
        return myState.myShow1;
    }

    @Override
    public void setShow(boolean value) {
        myState.myShow1 = value;
    }
}
