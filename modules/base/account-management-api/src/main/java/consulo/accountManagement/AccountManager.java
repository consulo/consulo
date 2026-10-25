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
package consulo.accountManagement;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface AccountManager {
    List<AccountType<?>> getAccountTypes();

    @Nullable
    AccountType<?> findAccountType(String id);

    <S extends AccountScope> List<AccountFeature<S>> getFeatures(AccountType<S> type);

    List<? extends AccountAuthorizeConfig> getAuthorizeConfigs(AccountType<?> type);

    @Nullable
    AccountAuthorizeType<?> findAuthorizeType(AccountAuthorizeConfig config);

    @Nullable
    AccountAuthorizeType<?> findAuthorizeType(String id);

    List<Account> getAccounts(@Nullable Project project);

    List<Account> getAccounts(AccountType<?> type, @Nullable Project project);

    @Nullable
    Account findAccount(String id, @Nullable Project project);

    default @Nullable String getServerUrl(Account account) {
        String serverUrl = account.getServerUrl();
        if (serverUrl != null && !serverUrl.isEmpty()) {
            return serverUrl;
        }
        AccountType<?> type = findAccountType(account.getTypeId());
        return type == null ? null : type.getDefaultServerUrl();
    }

    @Nullable
    Account getActiveAccount(AccountType<?> type, @Nullable Project project);

    void setActiveAccount(AccountType<?> type, @Nullable Account account, @Nullable Project project);

    void moveAccount(Account account, AccountLevel level, @Nullable Project project);

    CompletableFuture<?> removeAccount(Account account, @Nullable Project project);

    CompletableFuture<AccountConnection> connect(Account account);

    <S extends AccountScope> AccountFeatureState getState(AccountFeature<S> feature, Account account);

    <S extends AccountScope> Set<S> getMissingScopes(AccountFeature<S> feature, Account account);

    void markAuthFailed(Account account);

    ObservableValue<@Nullable Image> getAvatar(Account account);

    @RequiredUIAccess
    CompletableFuture<@Nullable Account> requestLogin(
        AccountType<?> type,
        @Nullable AccountAuthorizeConfig config,
        @Nullable Project project,
        @Nullable Component parent
    );

    @RequiredUIAccess
    CompletableFuture<@Nullable Account> requestAuthorization(
        Account account,
        @Nullable AccountFeature<?> feature,
        @Nullable Project project,
        @Nullable Component parent
    );

    <C extends AccountCredentials> CompletableFuture<Account> addAccount(
        AccountType<?> type,
        AccountAuthorizeConfig config,
        C credentials,
        @Nullable String serverUrl
    );

    void addListener(AccountListener listener, Disposable parentDisposable);
}
