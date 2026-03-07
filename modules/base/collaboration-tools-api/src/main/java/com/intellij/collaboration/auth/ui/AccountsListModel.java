// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.auth.ui;

import com.intellij.collaboration.auth.Account;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.ListModel;
import java.util.Map;
import java.util.Set;

public interface AccountsListModel<A extends Account, Cred> {
    @Nonnull
    Set<A> getAccounts();

    void setAccounts(@Nonnull Set<A> accounts);

    @Nullable
    A getSelectedAccount();

    void setSelectedAccount(@Nullable A account);

    @Nonnull
    Map<A, Cred> getNewCredentials();

    @Nonnull
    ListModel<A> getAccountsListModel();

    void clearNewCredentials();

    interface WithDefault<A extends Account, Cred> extends AccountsListModel<A, Cred> {
        @Nullable
        A getDefaultAccount();

        void setDefaultAccount(@Nullable A account);
    }
}
