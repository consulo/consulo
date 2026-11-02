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
package consulo.mcpServer.impl.internal.setting;

import com.dslplatform.json.CompiledJson;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-11-02
 */
@CompiledJson
public class McpServerSettingsState {
    public boolean ENABLED = false;
    public int PORT = McpServerSettings.DEFAULT_PORT;
    public boolean REQUIRE_APPROVAL = true;
    public boolean STATIC_TOKEN_ENABLED = false;
    public @Nullable Set<String> DISABLED_TOOLS = new HashSet<>();
    public @Nullable Set<String> ALWAYS_ALLOWED_TOOLS = new HashSet<>();
    public @Nullable List<McpOAuthClientState> OAUTH_CLIENTS = new ArrayList<>();
}
