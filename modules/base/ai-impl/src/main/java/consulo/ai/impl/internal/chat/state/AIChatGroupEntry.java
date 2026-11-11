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
 * A group the user filed chats in, as the workspace keeps it.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
@CompiledJson
public class AIChatGroupEntry {
    public String ID = "";
    public String NAME = "";
    /**
     * Empty for a group at the top.
     */
    public String PARENT_ID = "";
}
