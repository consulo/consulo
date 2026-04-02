// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.auth;

import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.StateFlow;
import kotlin.coroutines.Continuation;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Account management service
 *
 * @param <A>    account type
 * @param <Cred> account credentials
 */
public interface AccountManager<A extends Account, Cred> {
    /**
     * Subscribable set of accounts registered within application
     */
    @Nonnull
    StateFlow<Set<A>> getAccountsState();

    /**
     * Contains whether the account manager can persist credentials.
     * If it cannot, one might need to notify the user of a way to
     * fix this.
     */
    @Nonnull
    Flow<Boolean> getCanPersistCredentials();

    /**
     * Add/update account and its credentials
     */
    @Nullable
    Object updateAccount(@Nonnull A account, @Nonnull Cred credentials, @Nonnull Continuation<? super kotlin.Unit> continuation);

    /**
     * Add/update/remove multiple accounts and their credentials.
     * Credentials are not updated if null value is passed.
     * Should only be used by a bulk update from settings.
     */
    @Nullable
    Object updateAccounts(@Nonnull Map<A, ? extends Cred> accountsWithCredentials, @Nonnull Continuation<? super kotlin.Unit> continuation);

    /**
     * Remove an account and clear stored credentials.
     * Does nothing if account is not present.
     */
    @Nullable
    Object removeAccount(@Nonnull A account, @Nonnull Continuation<? super kotlin.Unit> continuation);

    /**
     * Retrieve credentials for account
     */
    @Nullable
    Object findCredentials(@Nonnull A account, @Nonnull Continuation<? super Cred> continuation);

    /**
     * Flow of account credentials
     */
    @Nonnull
    Flow<Cred> getCredentialsFlow(@Nonnull A account);

    /**
     * Flow of account credentials with the latest state.
     * Credentials are acquired and updated under {@code scope}.
     */
    @Nullable
    Object getCredentialsState(
        @Nonnull CoroutineScope scope,
        @Nonnull A account,
        @Nonnull Continuation<? super StateFlow<Cred>> continuation
    );

    /**
     * Find an account by predicate
     */
    @Nullable
    static <A extends Account> A findAccountOrNull(@Nonnull AccountManager<A, ?> accountManager, @Nonnull Predicate<A> predicate) {
        Set<A> accounts = accountManager.getAccountsState().getValue();
        A found = null;
        for (A account : accounts) {
            if (predicate.test(account)) {
                if (found != null) {
                    return null; // more than one match - singleOrNull returns null
                }
                found = account;
            }
        }
        return found;
    }
}
