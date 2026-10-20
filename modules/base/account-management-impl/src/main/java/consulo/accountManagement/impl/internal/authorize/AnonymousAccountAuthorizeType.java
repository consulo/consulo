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
import consulo.accountManagement.AccountRequestDecorator;
import consulo.accountManagement.authorize.AnonymousAuthorizeConfig;
import consulo.accountManagement.localize.AccountManagementLocalize;
import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
@ExtensionImpl
public class AnonymousAccountAuthorizeType implements AccountAuthorizeType<AnonymousCredentials> {
    @Override
    public String getId() {
        return AnonymousAuthorizeConfig.ID;
    }

    @Override
    public LocalizeValue getName() {
        return AccountManagementLocalize.authorizeTypeAnonymous();
    }

    @Override
    public String getCredentialSourceId() {
        return NONE_SOURCE;
    }

    @RequiredUIAccess
    @Override
    public AccountAuthorizeForm<AnonymousCredentials> createForm(AccountAuthorizeContext context) {
        return new AccountAuthorizeForm<>() {
            @Override
            public @Nullable Component getComponent() {
                return null;
            }

            @RequiredUIAccess
            @Override
            public CompletableFuture<AnonymousCredentials> authorize() {
                return CompletableFuture.completedFuture(AnonymousCredentials.INSTANCE);
            }
        };
    }

    @Override
    public String serialize(AnonymousCredentials credentials) {
        return "";
    }

    @Override
    public AnonymousCredentials deserialize(String value) {
        return AnonymousCredentials.INSTANCE;
    }

    @Override
    public void decorate(AnonymousCredentials credentials, AccountAuthorizeConfig config, AccountRequestDecorator decorator) {
    }
}
