// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.auth;

import kotlinx.coroutines.flow.StateFlow;
import jakarta.annotation.Nonnull;

import java.util.Set;

/**
 * The same as the {@link AccountsRepository} but allows propagating external changes to the {@link AccountManagerBase}.
 */
public interface ObservableAccountsRepository<A extends Account> extends AccountsRepository<A> {
    /**
     * A flow of accounts that is exposed by the {@link AccountManagerBase}. The implementing class is responsible for updating the current value
     * when the {@link #getAccounts() accounts} are set and, for example, in
     * {@link com.intellij.openapi.components.PersistentStateComponent#loadState(Object) loadState}
     * and {@link com.intellij.openapi.components.PersistentStateComponent#noStateLoaded() noStateLoaded}.
     */
    @Nonnull
    StateFlow<Set<A>> getAccountsFlow();
}
