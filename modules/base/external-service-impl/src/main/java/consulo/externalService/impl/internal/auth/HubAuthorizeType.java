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
package consulo.externalService.impl.internal.auth;

import consulo.accountManagement.AccountAuthorizeConfig;
import consulo.accountManagement.AccountAuthorizeContext;
import consulo.accountManagement.AccountAuthorizeForm;
import consulo.accountManagement.AccountAuthorizeType;
import consulo.accountManagement.AccountRequestDecorator;
import consulo.annotation.component.ExtensionImpl;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
@ExtensionImpl
public class HubAuthorizeType implements AccountAuthorizeType<HubCredentials> {
    public static final String ID = "consuloHub";

    private static final char SEPARATOR = '\n';

    private final Provider<ExternalServiceLoginService> myLoginService;

    @Inject
    public HubAuthorizeType(Provider<ExternalServiceLoginService> loginService) {
        myLoginService = loginService;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public LocalizeValue getName() {
        return ExternalServiceLocalize.hubAuthorizeTypeBrowser();
    }

    @RequiredUIAccess
    @Override
    public AccountAuthorizeForm<HubCredentials> createForm(AccountAuthorizeContext context) {
        Label hint = Label.create(ExternalServiceLocalize.hubAuthorizeHint());
        return new AccountAuthorizeForm<>() {
            @Override
            public @Nullable Component getComponent() {
                return hint;
            }

            @RequiredUIAccess
            @Override
            public CompletableFuture<HubCredentials> authorize() {
                return myLoginService.get().login();
            }
        };
    }

    @Override
    public String serialize(HubCredentials credentials) {
        return credentials.userName() + SEPARATOR + credentials.token();
    }

    @Override
    public HubCredentials deserialize(String value) {
        int index = value.indexOf(SEPARATOR);
        return index < 0 ? new HubCredentials("", value) : new HubCredentials(value.substring(0, index), value.substring(index + 1));
    }

    @Override
    public @Nullable String getUserName(HubCredentials credentials) {
        return credentials.userName();
    }

    @Override
    public void decorate(HubCredentials credentials, AccountAuthorizeConfig config, AccountRequestDecorator decorator) {
        decorator.header("Authorization", "Bearer " + credentials.token());
    }
}
