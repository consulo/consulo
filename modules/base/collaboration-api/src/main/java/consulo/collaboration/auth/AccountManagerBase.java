// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
/*
 * Copyright 2013-2025 consulo.io
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
package consulo.collaboration.auth;

import consulo.collaboration.util.ObservableValue;
import consulo.collaboration.util.ReadOnlyObservableValue;
import consulo.logging.Logger;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Base class for account management application service.
 * Accounts are stored in {@link #accountsRepository()}.
 * Credentials are stored in {@link #credentialsRepository()}.
 */
public abstract class AccountManagerBase<A extends Account, Cred> implements AccountManager<A, Cred> {
    private final Logger myLogger;
    private final ReentrantLock myLock = new ReentrantLock();
    private final ObservableValue<Set<A>> myAccountsState;

    protected AccountManagerBase(Logger logger) {
        myLogger = logger;
        myAccountsState = new ObservableValue<>(Set.copyOf(accountsRepository().getAccounts()));
    }

    protected abstract AccountsRepository<A> accountsRepository();

    protected abstract CredentialsRepository<A, Cred> credentialsRepository();

    @Override
    public ReadOnlyObservableValue<Set<A>> getAccountsState() {
        return myAccountsState.asReadOnly();
    }

    @Override
    public void updateAccounts(Map<A, @Nullable Cred> accountsWithCredentials) {
        myLock.lock();
        try {
            Set<A> currentSet = accountsRepository().getAccounts();
            Set<A> removed = new LinkedHashSet<>(currentSet);
            removed.removeAll(accountsWithCredentials.keySet());

            for (A account : removed) {
                saveCredentialsSafe(account, null);
            }

            for (Map.Entry<A, @Nullable Cred> entry : accountsWithCredentials.entrySet()) {
                Cred credentials = entry.getValue();
                if (credentials != null) {
                    saveCredentialsSafe(entry.getKey(), credentials);
                }
            }

            Set<A> added = new LinkedHashSet<>(accountsWithCredentials.keySet());
            added.removeAll(currentSet);

            if (!added.isEmpty() || !removed.isEmpty()) {
                accountsRepository().setAccounts(accountsWithCredentials.keySet());
                myAccountsState.setValue(Set.copyOf(accountsWithCredentials.keySet()));
                myLogger.debug("Account list changed to: " + accountsRepository().getAccounts());
            }
        }
        finally {
            myLock.unlock();
        }
    }

    @Override
    public void updateAccount(A account, Cred credentials) {
        myLock.lock();
        try {
            Set<A> currentSet = accountsRepository().getAccounts();
            boolean newAccount = !currentSet.contains(account);

            Set<A> newSet;
            if (!newAccount) {
                // remove and add an account to update auxiliary fields
                newSet = new LinkedHashSet<>(currentSet);
                newSet.remove(account);
                newSet.add(account);
            }
            else {
                myLogger.debug("Added new account: " + account);
                newSet = new LinkedHashSet<>(currentSet);
                newSet.add(account);
            }

            saveCredentialsSafe(account, credentials);
            accountsRepository().setAccounts(newSet);
            myAccountsState.setValue(Set.copyOf(newSet));
            myLogger.debug("Updated credentials for account: " + account);
        }
        finally {
            myLock.unlock();
        }
    }

    @Override
    public void removeAccount(A account) {
        myLock.lock();
        try {
            Set<A> currentSet = accountsRepository().getAccounts();
            Set<A> newSet = new LinkedHashSet<>(currentSet);
            if (newSet.remove(account)) {
                saveCredentialsSafe(account, null);
                accountsRepository().setAccounts(newSet);
                myAccountsState.setValue(Set.copyOf(newSet));
                myLogger.debug("Removed account: " + account);
            }
        }
        finally {
            myLock.unlock();
        }
    }

    private void saveCredentialsSafe(A account, @Nullable Cred credentials) {
        try {
            credentialsRepository().persistCredentials(account, credentials);
        }
        catch (Exception e) {
            myLogger.warn(e);
        }
    }

    @Override
    public @Nullable Cred findCredentials(A account) {
        return credentialsRepository().retrieveCredentials(account);
    }
}
