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
package consulo.accountManagement.impl.internal.authorize;

import consulo.accountManagement.AccountAuthorizeConfig;
import consulo.accountManagement.AccountAuthorizeContext;
import consulo.accountManagement.AccountAuthorizeForm;
import consulo.accountManagement.AccountAuthorizeType;
import consulo.accountManagement.AccountBasicCredentials;
import consulo.accountManagement.AccountRequestDecorator;
import consulo.accountManagement.authorize.OAuthAuthorizeConfig;
import consulo.accountManagement.localize.AccountManagementLocalize;
import consulo.accountManagement.oauth.OAuthCredentials;
import consulo.accountManagement.oauth.OAuthCredentialsWithRefresh;
import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
@ExtensionImpl
public class OAuthAccountAuthorizeType implements AccountAuthorizeType<OAuthCredentials> {
    @Override
    public String getId() {
        return OAuthAuthorizeConfig.ID;
    }

    @Override
    public LocalizeValue getName() {
        return AccountManagementLocalize.authorizeTypeOauth();
    }

    @Override
    public LocalizeValue getAuthorizeActionText() {
        return AccountManagementLocalize.actionOauthLogIn();
    }

    @RequiredUIAccess
    @Override
    public AccountAuthorizeForm<OAuthCredentials> createForm(AccountAuthorizeContext context) {
        OAuthAuthorizeConfig config = (OAuthAuthorizeConfig) context.getConfig();
        Component component = VerticalLayout.create().add(Label.create(AccountManagementLocalize.labelOauthBrowser()));
        return new AccountAuthorizeForm<>() {
            @Override
            public @Nullable Component getComponent() {
                return component;
            }

            @RequiredUIAccess
            @Override
            public CompletableFuture<OAuthCredentials> authorize() {
                return config.authorize(context.getServerUrl(), context.getScopes()).thenApply(credentials -> credentials);
            }
        };
    }

    @Override
    public String serialize(OAuthCredentials credentials, AccountAuthorizeConfig config) {
        return ((OAuthAuthorizeConfig) config).serialize(credentials);
    }

    @Override
    public OAuthCredentials deserialize(String value, AccountAuthorizeConfig config) {
        return ((OAuthAuthorizeConfig) config).deserialize(value);
    }

    @Override
    public void decorate(OAuthCredentials credentials, AccountAuthorizeConfig config, AccountRequestDecorator decorator) {
        OAuthAuthorizeConfig oauthConfig = (OAuthAuthorizeConfig) config;
        decorator.header(oauthConfig.getHeaderName(), oauthConfig.getHeaderValuePrefix() + credentials.getAccessToken());
    }

    @Override
    public AccountBasicCredentials getBasicCredentials(OAuthCredentials credentials, AccountAuthorizeConfig config) {
        return new AccountBasicCredentials(null, credentials.getAccessToken());
    }

    @Override
    public boolean isExpired(OAuthCredentials credentials) {
        return credentials instanceof OAuthCredentialsWithRefresh withRefresh && !withRefresh.isAccessTokenValid();
    }

    @Override
    public CompletableFuture<OAuthCredentials> refresh(OAuthCredentials credentials, AccountAuthorizeConfig config) {
        return ((OAuthAuthorizeConfig) config).refresh(credentials).thenApply(refreshed -> refreshed);
    }
}
