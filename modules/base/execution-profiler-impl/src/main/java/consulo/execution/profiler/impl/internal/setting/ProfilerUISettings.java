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
package consulo.execution.profiler.impl.internal.setting;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@State(name = "ProfilerUISettings", storages = @Storage("profiler.ui"))
public class ProfilerUISettings implements PersistentStateComponent<ProfilerUISettings> {
    public @Nullable ProfilerSessionHost SESSION_HOST = ProfilerSessionHost.EDITOR;

    public ProfilerSessionHost getSessionHost() {
        ProfilerSessionHost host = SESSION_HOST;
        return host == null ? ProfilerSessionHost.EDITOR : host;
    }

    public void setSessionHost(ProfilerSessionHost host) {
        SESSION_HOST = host;
    }

    @Override
    public ProfilerUISettings getState() {
        return this;
    }

    @Override
    public void loadState(ProfilerUISettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
