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
package consulo.accountManagement.impl.internal.ui;

import consulo.accountManagement.AccountAuthorizeConfig;
import consulo.accountManagement.AccountAuthorizeContext;
import consulo.accountManagement.AccountAuthorizeForm;
import consulo.accountManagement.AccountAuthorizeType;
import consulo.accountManagement.AccountCredentials;
import consulo.accountManagement.AccountScope;
import consulo.accountManagement.AccountType;
import consulo.accountManagement.impl.internal.AccountConnectionImpl;
import consulo.accountManagement.impl.internal.AccountLoginResult;
import consulo.accountManagement.impl.internal.AccountManagerImpl;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
final class AccountLoginSession<C extends AccountCredentials> {
    private final AccountAuthorizeType<C> myAuthorizeType;
    private final AccountAuthorizeForm<C> myForm;

    private AccountLoginSession(AccountAuthorizeType<C> authorizeType, AccountAuthorizeForm<C> form) {
        myAuthorizeType = authorizeType;
        myForm = form;
    }

    @RequiredUIAccess
    static <C extends AccountCredentials> AccountLoginSession<C> create(AccountAuthorizeType<C> authorizeType, AccountAuthorizeContext context) {
        return new AccountLoginSession<>(authorizeType, authorizeType.createForm(context));
    }

    AccountAuthorizeForm<C> getForm() {
        return myForm;
    }

    @RequiredUIAccess
    CompletableFuture<AccountLoginResult> login(
        AccountType<?> type,
        AccountAuthorizeConfig config,
        @Nullable String serverUrl,
        Set<? extends AccountScope> scopes
    ) {
        Set<String> scopeIds = new LinkedHashSet<>();
        for (AccountScope scope : scopes) {
            scopeIds.add(scope.getId());
        }
        return myForm.authorize().thenCompose(credentials -> {
            AccountConnectionImpl<C> connection = new AccountConnectionImpl<>(serverUrl, myAuthorizeType, config, credentials);
            return AccountManagerImpl.completeLogin(type, config, connection, scopeIds);
        });
    }
}
