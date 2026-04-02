// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.auth;

import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.EventListener;

/**
 * @param <A> account type
 */
public interface AccountsListener<A> extends EventListener {
    default void onAccountListChanged(@Nonnull Collection<A> old, @Nonnull Collection<A> newAccounts) {
    }

    default void onAccountCredentialsChanged(@Nonnull A account) {
    }
}
