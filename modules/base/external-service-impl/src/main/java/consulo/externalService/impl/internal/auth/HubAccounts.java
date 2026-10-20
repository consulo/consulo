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

import consulo.accountManagement.Account;
import consulo.accountManagement.AccountConnection;
import consulo.accountManagement.AccountListener;
import consulo.accountManagement.AccountManager;
import consulo.accountManagement.AccountType;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.externalService.ExternalServiceConfiguration;
import consulo.externalService.ExternalServiceConfigurationListener;
import consulo.logging.Logger;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class HubAccounts implements Disposable {
    private static final Logger LOG = Logger.getInstance(HubAccounts.class);

    private final Application myApplication;
    private final AccountManager myAccountManager;
    private final ExternalServiceLoginService myLoginService;
    private boolean myListening;

    @Inject
    public HubAccounts(Application application, AccountManager accountManager, ExternalServiceLoginService loginService) {
        myApplication = application;
        myAccountManager = accountManager;
        myLoginService = loginService;
    }

    public @Nullable AccountType<?> getType() {
        listen();
        return myAccountManager.findAccountType(HubAccountType.ID);
    }

    public @Nullable Account getActiveAccount() {
        AccountType<?> type = getType();
        return type == null ? null : myAccountManager.getActiveAccount(type, null);
    }

    public boolean isAuthorized() {
        Account account = getActiveAccount();
        return account != null && !account.isAuthFailed();
    }

    public @Nullable String getUserName() {
        Account account = getActiveAccount();
        return account == null ? null : account.getName();
    }

    public @Nullable AccountConnection connect() {
        Account account = getActiveAccount();
        if (account == null) {
            return null;
        }
        try {
            return myAccountManager.connect(account).get(30, TimeUnit.SECONDS);
        }
        catch (Exception e) {
            LOG.warn("Cannot connect hub account " + account, e);
            return null;
        }
    }

    public void markAuthFailed() {
        Account account = getActiveAccount();
        if (account != null) {
            myAccountManager.markAuthFailed(account);
        }
    }

    public CompletableFuture<Account> login() {
        AccountType<?> type = getType();
        if (type == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("No hub account type"));
        }
        return myLoginService.login().thenCompose(credentials -> myAccountManager.addAccount(type, HubAuthorizeConfig.INSTANCE, credentials, null));
    }

    public CompletableFuture<Account> importLegacy(String userName, String token) {
        AccountType<?> type = getType();
        if (type == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("No hub account type"));
        }
        return myAccountManager.addAccount(type, HubAuthorizeConfig.INSTANCE, new HubCredentials(userName, token), null);
    }

    public CompletableFuture<?> logout() {
        Account account = getActiveAccount();
        return account == null ? CompletableFuture.completedFuture(null) : myAccountManager.removeAccount(account, null);
    }

    private synchronized void listen() {
        if (myListening) {
            return;
        }
        myListening = true;
        myAccountManager.addListener(new AccountListener() {
            @Override
            public void accountsChanged() {
                fireConfigurationChanged();
            }

            @Override
            public void activeAccountChanged(AccountType<?> type, @Nullable Account account) {
                if (HubAccountType.ID.equals(type.getId())) {
                    fireConfigurationChanged();
                }
            }
        }, this);
    }

    private void fireConfigurationChanged() {
        myApplication.getMessageBus()
            .syncPublisher(ExternalServiceConfigurationListener.class)
            .configurationChanged(myApplication.getInstance(ExternalServiceConfiguration.class));
    }

    @Override
    public void dispose() {
    }
}
