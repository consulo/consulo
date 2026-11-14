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
package consulo.mcpServer.impl.internal.ai;

import consulo.ai.AIMcpServer;
import consulo.ai.AIMcpServerProvider;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.builtinWebServer.custom.CustomPortServerManager;
import consulo.mcpServer.impl.internal.McpAuthTokenManager;
import consulo.mcpServer.impl.internal.http.McpCustomPortServerManager;
import consulo.mcpServer.impl.internal.setting.McpServerSettings;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.util.List;
import java.util.Map;

/**
 * The MCP server of the IDE for the agents of the AI chats, signed in with a token of their own.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ExtensionImpl
public class McpAIMcpServerProvider implements AIMcpServerProvider {
    private static final String NAME = "consulo";

    private final Application myApplication;
    private final Provider<McpServerSettings> mySettings;
    private final Provider<McpAuthTokenManager> myTokenManager;

    @Inject
    public McpAIMcpServerProvider(Application application, Provider<McpServerSettings> settings, Provider<McpAuthTokenManager> tokenManager) {
        myApplication = application;
        mySettings = settings;
        myTokenManager = tokenManager;
    }

    @Override
    public List<AIMcpServer> getServers() {
        McpServerSettings settings = mySettings.get();
        // a port another process holds is not ours - an agent would knock at someone else's server
        McpCustomPortServerManager server = myApplication.getExtensionPoint(CustomPortServerManager.class).findExtension(McpCustomPortServerManager.class);
        if (!settings.isEnabled() || server == null || !server.isBound()) {
            return List.of();
        }
        String url = "http://127.0.0.1:" + settings.getPort() + "/mcp";
        String authorization = McpAuthTokenManager.toAuthorizationValue(myTokenManager.get().getAgentToken());
        return List.of(new AIMcpServer(NAME, url, Map.of(McpAuthTokenManager.AUTHORIZATION_HEADER, authorization)));
    }
}
