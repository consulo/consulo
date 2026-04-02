// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.auth;

import jakarta.annotation.Nonnull;

import java.util.Set;

/**
 * In most cases should be an instance of {@link com.intellij.openapi.components.PersistentStateComponent}.
 *
 * @deprecated Prefer implementing the {@link ObservableAccountsRepository} to propagate external changes to the {@link AccountManagerBase}.
 */
@Deprecated
public interface AccountsRepository<A extends Account> {
    @Nonnull
    Set<A> getAccounts();

    void setAccounts(@Nonnull Set<A> accounts);
}
