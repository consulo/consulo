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
package consulo.execution.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.execution.internal.RunConfigurationStartHistory;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import java.util.Set;

/**
 * @author VISTALL
 * @since 2025-10-20
 */
@ServiceImpl
@Singleton
@State(name = "RunConfigurationStartHistory", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class RunConfigurationStartHistoryImpl implements RunConfigurationStartHistory, PersistentStateComponent<RunConfigurationStartHistoryState> {
    private final RunConfigurationStartHistoryState myState = new RunConfigurationStartHistoryState();

    @Nullable
    @Override
    public RunConfigurationStartHistoryState getState() {
        return myState;
    }

    @Override
    public void loadState(RunConfigurationStartHistoryState state) {
        XmlSerializerUtil.copyBean(state, myState);
    }

    @Nonnull
    @Override
    public Set<String> getPinned() {
        return myState.getPinned();
    }

    @Nonnull
    @Override
    public Set<String> getHistory() {
        return myState.getHistory();
    }

    @Override
    public boolean isAllConfigurationsExpanded() {
        return myState.isAllConfigurationsExpanded();
    }

    @Override
    public void setAllConfigurationsExpanded(boolean allConfigurationsExpanded) {
        myState.setAllConfigurationsExpanded(allConfigurationsExpanded);
    }
}
