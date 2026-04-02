// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.auth;

import consulo.component.persist.PersistentStateComponent;
import consulo.disposer.Disposable;
import consulo.project.Project;
import kotlinx.coroutines.CoroutineScope;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Stores default account for project.
 * To register - {@code @State(name = SERVICE_NAME_HERE, storages = [Storage(StoragePathMacros.WORKSPACE_FILE)], reportStatistic = false)}
 *
 * @param <A> account type
 */
public abstract class PersistentDefaultAccountHolder<A extends Account>
    implements PersistentStateComponent<PersistentDefaultAccountHolder.AccountState>, Disposable, DefaultAccountHolder<A> {

    private volatile A account;

    protected final Project project;

    protected PersistentDefaultAccountHolder(@Nonnull Project project, @SuppressWarnings("unused") @Nonnull CoroutineScope cs) {
        this.project = project;
    }

    /**
     * @deprecated A service coroutine scope should be provided
     */
    @Deprecated
    protected PersistentDefaultAccountHolder(@Nonnull Project project) {
        this.project = project;
    }

    @Override
    @Nullable
    public A getAccount() {
        A current = account;
        if (current != null && !accountManager().getAccountsState().getValue().contains(current)) {
            return null;
        }
        return current;
    }

    @Override
    public void setAccount(@Nullable A value) {
        if (value == null || accountManager().getAccountsState().getValue().contains(value)) {
            this.account = value;
        }
    }

    @Nonnull
    @Override
    public AccountState getState() {
        AccountState state = new AccountState();
        A current = getAccount();
        state.defaultAccountId = current != null ? current.getId() : null;
        return state;
    }

    @Override
    public void loadState(@Nonnull AccountState state) {
        if (state.defaultAccountId != null) {
            String id = state.defaultAccountId;
            account = accountManager().getAccountsState().getValue().stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .orElse(null);
        }
        else {
            account = null;
        }
    }

    @Nonnull
    protected abstract AccountManager<A, ?> accountManager();

    protected abstract void notifyDefaultAccountMissing();

    @Override
    public void dispose() {
    }

    public static class AccountState {
        @Nullable
        public String defaultAccountId;
    }
}
