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
import consulo.accountManagement.AccountBasicCredentials;
import consulo.accountManagement.AccountAuthorizeContext;
import consulo.accountManagement.AccountAuthorizeForm;
import consulo.accountManagement.AccountAuthorizeType;
import consulo.accountManagement.AccountRequestDecorator;
import consulo.accountManagement.authorize.TokenAuthorizeConfig;
import consulo.accountManagement.localize.AccountManagementLocalize;
import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
@ExtensionImpl
public class TokenAccountAuthorizeType implements AccountAuthorizeType<SecretCredentials> {
    @Override
    public String getId() {
        return TokenAuthorizeConfig.ID;
    }

    @Override
    public LocalizeValue getName() {
        return AccountManagementLocalize.authorizeTypeToken();
    }

    @RequiredUIAccess
    @Override
    public AccountAuthorizeForm<SecretCredentials> createForm(AccountAuthorizeContext context) {
        TokenAuthorizeConfig config = (TokenAuthorizeConfig) context.getConfig();
        return new SecretAuthorizeForm(
            context,
            AccountManagementLocalize.labelToken(),
            () -> config.buildNewTokenUrl(context.getServerUrl(), context.getScopes())
        );
    }

    @Override
    public String serialize(SecretCredentials credentials) {
        return credentials.secret();
    }

    @Override
    public SecretCredentials deserialize(String value) {
        return new SecretCredentials(value);
    }

    @Override
    public void decorate(SecretCredentials credentials, AccountAuthorizeConfig config, AccountRequestDecorator decorator) {
        TokenAuthorizeConfig tokenConfig = (TokenAuthorizeConfig) config;
        decorator.header(tokenConfig.getHeaderName(), tokenConfig.getHeaderValuePrefix() + credentials.secret());
    }

    @Override
    public AccountBasicCredentials getBasicCredentials(SecretCredentials credentials, AccountAuthorizeConfig config) {
        return new AccountBasicCredentials(null, credentials.secret());
    }
}
