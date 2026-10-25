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
package consulo.sandboxPlugin.ide.account;

import consulo.accountManagement.AccountAuthorizeConfig;
import consulo.accountManagement.AccountConnection;
import consulo.accountManagement.AccountDetails;
import consulo.accountManagement.AccountScope;
import consulo.accountManagement.AccountType;
import consulo.accountManagement.authorize.AnonymousAuthorizeConfig;
import consulo.accountManagement.authorize.BasicAuthorizeConfig;
import consulo.accountManagement.authorize.OAuthAuthorizeConfig;
import consulo.accountManagement.authorize.TokenAuthorizeConfig;
import consulo.accountManagement.oauth.OAuthCredentials;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
@ExtensionImpl
public class SandHubAccountType implements AccountType<SandHubScope> {
    public static final String ID = "sandHub";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Sand Hub");
    }

    @Override
    public Image getIcon() {
        return PlatformIconGroup.generalWeb();
    }

    @Override
    public Class<SandHubScope> getScopeClass() {
        return SandHubScope.class;
    }

    @Override
    public Set<SandHubScope> getBaseScopes() {
        return Set.of(SandHubScope.READ_USER);
    }

    @Override
    public String getDefaultServerUrl() {
        return "https://hub.sand.example";
    }

    @Override
    public boolean isServerEditable() {
        return true;
    }

    @Override
    public List<? extends AccountAuthorizeConfig> getAuthorizeConfigs() {
        return List.of(
            new TokenAuthorizeConfig() {
                @Override
                public boolean isDefault() {
                    return true;
                }

                @Override
                public @Nullable String buildNewTokenUrl(@Nullable String serverUrl, Set<? extends AccountScope> scopes) {
                    List<String> ids = new ArrayList<>();
                    for (AccountScope scope : scopes) {
                        ids.add(scope.getId());
                    }
                    return (serverUrl == null ? getDefaultServerUrl() : serverUrl) + "/settings/tokens/new?description=Consulo&scopes=" + String.join(",", ids);
                }
            },
            new OAuthAuthorizeConfig() {
                @Override
                public CompletableFuture<? extends OAuthCredentials> authorize(@Nullable String serverUrl, Set<? extends AccountScope> scopes) {
                    return Application.get().getInstance(SandHubOAuthService.class).authorize();
                }
            },
            new BasicAuthorizeConfig() {
            },
            new AnonymousAuthorizeConfig() {
            }
        );
    }

    @Override
    public CompletableFuture<AccountDetails> loadDetails(AccountConnection connection) {
        return CompletableFuture.supplyAsync(() -> {
            String token = SandAccountRequest.of(connection).bearerToken();
            String login = token != null ? loginOf(token) : connection.getUserName();
            if (login != null && login.contains("fail")) {
                throw new IllegalStateException("401 Unauthorized - Sand Hub rejected '" + login + "'");
            }
            if (login == null) {
                return new AccountDetails("Anonymous", null, null);
            }
            return new AccountDetails(capitalize(login), login, null);
        }, CompletableFuture.delayedExecutor(600, TimeUnit.MILLISECONDS));
    }

    @Override
    public CompletableFuture<Set<SandHubScope>> loadGrantedScopes(AccountConnection connection, Set<SandHubScope> requested) {
        String token = SandAccountRequest.of(connection).bearerToken();
        if (token == null) {
            return CompletableFuture.completedFuture(connection.getUserName() == null ? Set.of() : requested);
        }
        int index = token.indexOf(':');
        if (index < 0) {
            return CompletableFuture.completedFuture(requested);
        }
        Set<SandHubScope> granted = new LinkedHashSet<>(getBaseScopes());
        for (String id : token.substring(index + 1).split(",")) {
            for (SandHubScope scope : SandHubScope.values()) {
                if (scope.getId().equals(id.trim())) {
                    granted.add(scope);
                }
            }
        }
        return CompletableFuture.completedFuture(granted);
    }

    private static String loginOf(String token) {
        int index = token.indexOf(':');
        return index < 0 ? token : token.substring(0, index);
    }

    private static String capitalize(String login) {
        StringBuilder builder = new StringBuilder();
        for (String part : login.split("[._-]")) {
            if (part.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.isEmpty() ? login : builder.toString();
    }
}
