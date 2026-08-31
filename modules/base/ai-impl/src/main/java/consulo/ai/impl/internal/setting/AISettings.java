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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

/**
 * The configured instances and the default selection. API keys are never stored here - they live in
 * the password safe, see {@link consulo.ai.AICredentials}.
 *
 * @author VISTALL
 * @since 2026-08-04
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@State(name = "AISettings", storages = @Storage("ai.xml"))
public class AISettings implements PersistentStateComponent<AISettings> {
    /**
     * A configured instance as stored on disk. The API key is deliberately absent.
     */
    public static class ProviderEntry {
        public String NAME = "";
        public String TYPE_ID = "";

        public ProviderEntry() {
        }

        public ProviderEntry(String name, String typeId) {
            NAME = name;
            TYPE_ID = typeId;
        }
    }

    public List<ProviderEntry> PROVIDERS = new ArrayList<>();
    public String DEFAULT_PROVIDER_NAME = "";
    public String DEFAULT_MODEL_ID = "";

    @Override
    public AISettings getState() {
        return this;
    }

    @Override
    public void loadState(AISettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
