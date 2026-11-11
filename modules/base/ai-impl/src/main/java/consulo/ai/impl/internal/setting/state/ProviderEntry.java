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
package consulo.ai.impl.internal.setting.state;

import com.dslplatform.json.CompiledJson;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A configured instance as stored on disk. The API key is deliberately absent. The name of the class is the name of
 * its element in {@code ai.xml}, so it stays.
 *
 * @author VISTALL
 * @since 2026-08-04
 */
@CompiledJson
public class ProviderEntry {
    public String NAME = "";
    public String TYPE_ID = "";
    public Map<String, String> OPTIONS = new LinkedHashMap<>();
    /**
     * Added by itself for an account which was added - it goes away with the account, unless the user changed it.
     */
    public boolean GENERATED;

    public ProviderEntry() {
    }

    public ProviderEntry(String name, String typeId) {
        NAME = name;
        TYPE_ID = typeId;
    }
}
