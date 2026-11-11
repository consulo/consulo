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
package consulo.ai.impl.internal.chat.state;

import com.dslplatform.json.CompiledJson;

/**
 * A chat of the project as the workspace keeps it - which account it talks to and where it is filed, not what it said.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
@CompiledJson
public class AIChatEntry {
    public String ID = "";
    public String PROVIDER_NAME = "";
    public String MODEL_ID = "";
    public String TITLE = "";
    public boolean NAMED_BY_USER;
    /**
     * Empty for a chat at the top.
     */
    public String GROUP_ID = "";
    public String WORKING_DIRECTORY = "";
    /**
     * The effort of the model, empty for the provider's default.
     */
    public String EFFORT = "";
    public long CREATED;
    public long UPDATED;
}
