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
package consulo.mcpServer.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.mcpServer.impl.internal.oauth.McpOAuthService;
import consulo.mcpServer.impl.internal.setting.McpOAuthClientState;
import consulo.mcpServer.impl.internal.setting.McpServerSettings;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-11-02
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class McpAuthenticator {
    private final McpServerSettings mySettings;
    private final McpAuthTokenManager myTokenManager;
    private final McpOAuthService myOAuthService;

    @Inject
    public McpAuthenticator(McpServerSettings settings, McpAuthTokenManager tokenManager, McpOAuthService oAuthService) {
        mySettings = settings;
        myTokenManager = tokenManager;
        myOAuthService = oAuthService;
    }

    public CompletableFuture<@Nullable McpPrincipal> authenticate(@Nullable String bearer) {
        if (bearer == null) {
            return CompletableFuture.completedFuture(null);
        }

        McpOAuthClientState client = myOAuthService.findClientByAccessToken(bearer);
        if (client != null) {
            return CompletableFuture.completedFuture(McpPrincipal.oauth(client.CLIENT_ID, client.CLIENT_NAME));
        }

        if (!mySettings.isStaticTokenEnabled()) {
            return CompletableFuture.completedFuture(null);
        }
        return myTokenManager.getToken()
            .thenApply(token -> McpAuthTokenManager.matches(token, bearer) ? McpPrincipal.STATIC_TOKEN : null);
    }
}
