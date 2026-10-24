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
package consulo.accountManagement.impl.internal;

import consulo.accountManagement.AccountAuthorizeConfig;
import consulo.accountManagement.AccountAuthorizeType;
import consulo.accountManagement.AccountBasicCredentials;
import consulo.accountManagement.AccountConnection;
import consulo.accountManagement.AccountCredentials;
import consulo.accountManagement.AccountRequestDecorator;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
public final class AccountConnectionImpl<C extends AccountCredentials> implements AccountConnection {
    private final @Nullable String myServerUrl;
    private final AccountAuthorizeType<C> myAuthorizeType;
    private final AccountAuthorizeConfig myConfig;
    private final C myCredentials;

    public AccountConnectionImpl(@Nullable String serverUrl, AccountAuthorizeType<C> authorizeType, AccountAuthorizeConfig config, C credentials) {
        myServerUrl = serverUrl;
        myAuthorizeType = authorizeType;
        myConfig = config;
        myCredentials = credentials;
    }

    public AccountAuthorizeType<C> getAuthorizeType() {
        return myAuthorizeType;
    }

    public C getCredentials() {
        return myCredentials;
    }

    public String serialize() {
        return myAuthorizeType.serialize(myCredentials);
    }

    @Override
    public @Nullable String getServerUrl() {
        return myServerUrl;
    }

    @Override
    public @Nullable String getUserName() {
        return myAuthorizeType.getUserName(myCredentials);
    }

    @Override
    public void decorate(AccountRequestDecorator decorator) {
        myAuthorizeType.decorate(myCredentials, myConfig, decorator);
    }

    @Override
    public @Nullable AccountBasicCredentials getBasicCredentials() {
        return myAuthorizeType.getBasicCredentials(myCredentials, myConfig);
    }
}
