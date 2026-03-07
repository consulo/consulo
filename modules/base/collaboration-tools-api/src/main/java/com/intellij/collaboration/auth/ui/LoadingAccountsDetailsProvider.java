// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.auth.ui;

import com.intellij.collaboration.auth.Account;
import com.intellij.collaboration.auth.AccountDetails;
import com.intellij.collaboration.ui.icon.IconsProvider;
import com.intellij.util.concurrency.annotations.RequiresEdt;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.StateFlow;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public interface LoadingAccountsDetailsProvider<A extends Account, D extends AccountDetails> extends IconsProvider<A> {
    @Nonnull
    StateFlow<Boolean> getLoadingState();

    @Nonnull
    Flow<A> getLoadingCompletionFlow();

    @RequiresEdt
    @Nullable
    D getDetails(@Nonnull A account);

    @RequiresEdt
    @Nullable
    @Nls
    String getErrorText(@Nonnull A account);

    @RequiresEdt
    boolean checkErrorRequiresReLogin(@Nonnull A account);
}
