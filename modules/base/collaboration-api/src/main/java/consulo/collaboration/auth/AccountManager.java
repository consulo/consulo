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

import consulo.collaboration.util.ReadOnlyObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Account management service.
 *
 * @param <A>    account type
 * @param <Cred> account credentials
 */
public interface AccountManager<A extends Account, Cred> {
    /**
     * Subscribable set of accounts registered within application.
     */
    ReadOnlyObservableValue<Set<A>> getAccountsState();

    /**
     * Returns the current set of accounts.
     */
    default Set<A> getAccounts() {
        Set<A> accounts = getAccountsState().getValue();
        return accounts != null ? accounts : Set.of();
    }

    /**
     * Add/update account and its credentials.
     */
    void updateAccount(A account, Cred credentials);

    /**
     * Add/update/remove multiple accounts and their credentials.
     * Credentials are not updated if null value is passed.
     * Should only be used by a bulk update from settings.
     */
    void updateAccounts(Map<A, @Nullable Cred> accountsWithCredentials);

    /**
     * Remove an account and clear stored credentials.
     * Does nothing if account is not present.
     */
    void removeAccount(A account);

    /**
     * Retrieve credentials for account.
     */
    @Nullable Cred findCredentials(A account);
}
