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
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-11-02
 */
@CompiledJson
public class McpOAuthClientState {
    public String CLIENT_ID = "";
    public String CLIENT_NAME = "";
    public @Nullable List<String> REDIRECT_URIS = new ArrayList<>();
    public @Nullable String REFRESH_TOKEN_HASH;
    public long APPROVED_AT;
    public long LAST_USED_AT;

    public static McpOAuthClientState create(String clientId, String clientName, List<String> redirectUris) {
        McpOAuthClientState state = new McpOAuthClientState();
        state.CLIENT_ID = clientId;
        state.CLIENT_NAME = clientName;
        state.REDIRECT_URIS = new ArrayList<>(redirectUris);
        return state;
    }

    public List<String> redirectUris() {
        List<String> redirectUris = REDIRECT_URIS;
        return redirectUris == null ? List.of() : redirectUris;
    }
}
