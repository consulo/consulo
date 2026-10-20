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
package consulo.accountManagement.impl.internal.credential;

import consulo.accountManagement.Account;
import consulo.accountManagement.AccountAuthorizeType;
import consulo.accountManagement.AccountCredentialSource;
import consulo.accountManagement.localize.AccountManagementLocalize;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.credentialStorage.CredentialAttributes;
import consulo.credentialStorage.Credentials;
import consulo.credentialStorage.PasswordSafe;
import consulo.localize.LocalizeValue;
import consulo.util.lang.StringUtil;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
@ExtensionImpl
public class PasswordSafeAccountCredentialSource implements AccountCredentialSource {
    private static final String SERVICE_PREFIX = "Consulo Accounts - ";

    private final Provider<PasswordSafe> myPasswordSafe;
    private final Provider<ApplicationConcurrency> myConcurrency;

    @Inject
    public PasswordSafeAccountCredentialSource(Provider<PasswordSafe> passwordSafe, Provider<ApplicationConcurrency> concurrency) {
        myPasswordSafe = passwordSafe;
        myConcurrency = concurrency;
    }

    @Override
    public String getId() {
        return AccountAuthorizeType.PASSWORD_SAFE_SOURCE;
    }

    @Override
    public LocalizeValue getName() {
        return AccountManagementLocalize.credentialSourcePasswordSafe();
    }

    @Override
    public CompletableFuture<@Nullable String> load(Account account) {
        return myPasswordSafe.get().getAsync(attributes(account)).thenApply(credentials -> {
            String value = credentials == null ? null : credentials.getPasswordAsString();
            return StringUtil.isEmpty(value) ? null : value;
        });
    }

    @Override
    public CompletableFuture<?> store(Account account, @Nullable String value) {
        CredentialAttributes attributes = attributes(account);
        return CompletableFuture.runAsync(
            () -> myPasswordSafe.get().set(attributes, value == null ? null : new Credentials(account.getId(), value)),
            myConcurrency.get().executor()
        );
    }

    private static CredentialAttributes attributes(Account account) {
        return new CredentialAttributes(SERVICE_PREFIX + account.getTypeId(), account.getId());
    }
}
