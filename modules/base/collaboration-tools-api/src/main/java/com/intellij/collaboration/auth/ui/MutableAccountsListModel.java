// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.auth.ui;

import com.intellij.collaboration.auth.Account;
import com.intellij.ui.CollectionListModel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

public abstract class MutableAccountsListModel<A extends Account, Cred>
    implements AccountsListModel<A, Cred> {

    @Nullable
    private A selectedAccount;
    private final Map<A, Cred> newCredentials = new LinkedHashMap<>();
    private final CollectionListModel<A> accountsListModel = new CollectionListModel<>();

    @Nonnull
    @Override
    public Set<A> getAccounts() {
        return new LinkedHashSet<>(accountsListModel.getItems());
    }

    @Override
    public void setAccounts(@Nonnull Set<A> accounts) {
        accountsListModel.removeAll();
        accountsListModel.add(new ArrayList<>(accounts));
    }

    @Nullable
    @Override
    public A getSelectedAccount() {
        return selectedAccount;
    }

    @Override
    public void setSelectedAccount(@Nullable A account) {
        this.selectedAccount = account;
    }

    @Nonnull
    @Override
    public Map<A, Cred> getNewCredentials() {
        return newCredentials;
    }

    @Nonnull
    @Override
    public CollectionListModel<A> getAccountsListModel() {
        return accountsListModel;
    }

    @Override
    public void clearNewCredentials() {
        newCredentials.clear();
    }

    public void add(@Nonnull A account, @Nonnull Cred cred) {
        accountsListModel.add(account);
        newCredentials.put(account, cred);
        notifyCredentialsChanged(account);
    }

    public void update(@Nonnull A account, @Nonnull Cred cred) {
        newCredentials.put(account, cred);
        notifyCredentialsChanged(account);
    }

    public void remove(@Nonnull A account) {
        accountsListModel.remove(account);
        newCredentials.remove(account);
        notifyCredentialsChanged(account);
    }

    private void notifyCredentialsChanged(@Nonnull A account) {
        accountsListModel.contentsChanged(account);
    }
}
